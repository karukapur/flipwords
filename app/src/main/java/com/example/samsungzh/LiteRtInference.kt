package com.example.samsungzh

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Collections

internal enum class LiteRtBackend {
    GPU,
    CPU,
}

internal fun interface LiteRtPromptExecutor {
    suspend fun execute(modelFile: File, prompt: String, backend: LiteRtBackend): String
}

/**
 * LiteRT-LM models are large enough that loading a second engine in this process can exhaust the
 * device. Every feature that creates an engine must do so inside this coordinator.
 */
internal object LiteRtInferenceCoordinator {
    private val engineMutex = Mutex()

    suspend fun <T> runSerialized(block: suspend () -> T): T = engineMutex.withLock { block() }
}

internal suspend fun LiteRtPromptExecutor.executeWithGpuFallback(
    modelFile: File,
    prompt: String,
): String {
    return try {
        execute(modelFile = modelFile, prompt = prompt, backend = LiteRtBackend.GPU)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (gpuError: Throwable) {
        gpuError.throwIfFatal()
        try {
            execute(modelFile = modelFile, prompt = prompt, backend = LiteRtBackend.CPU)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (cpuError: Throwable) {
            cpuError.throwIfFatal()
            cpuError.addSuppressed(gpuError)
            throw cpuError
        }
    }
}

internal fun Throwable.throwIfFatal() {
    if (this is VirtualMachineError || this is ThreadDeath) throw this
}

internal object ReflectiveLiteRtPromptExecutor : LiteRtPromptExecutor {
    override suspend fun execute(modelFile: File, prompt: String, backend: LiteRtBackend): String {
        currentCoroutineContext().ensureActive()
        val packageName = "com.google.ai.edge.litertlm"
        val backendClass = Class.forName("$packageName.Backend")
        val backendInstance = createBackend(packageName, backend)
        val engineConfig = createEngineConfig(
            packageName = packageName,
            backendClass = backendClass,
            modelFile = modelFile,
            backend = backendInstance,
        )
        val engine = Class.forName("$packageName.Engine")
            .getConstructor(Class.forName("$packageName.EngineConfig"))
            .newInstance(engineConfig)

        return try {
            engine.javaClass.getMethod("initialize").invoke(engine)
            currentCoroutineContext().ensureActive()
            val conversationConfig = Class.forName("$packageName.ConversationConfig")
                .getConstructor()
                .newInstance()
            val conversation = engine.javaClass
                .getMethod("createConversation", conversationConfig.javaClass)
                .invoke(engine, conversationConfig)
                ?: error("LiteRT-LM did not create a conversation.")

            try {
                @Suppress("UNCHECKED_CAST")
                val messages = conversation.javaClass
                    .getMethod("sendMessageAsync", String::class.java, Map::class.java)
                    .invoke(
                        conversation,
                        prompt,
                        Collections.emptyMap<String, Any>(),
                    ) as? Flow<Any?>
                    ?: error("LiteRT-LM did not return an asynchronous message stream.")
                val response = StringBuilder()
                try {
                    messages.collect { message -> response.append(message?.toString().orEmpty()) }
                } catch (cancelled: CancellationException) {
                    try {
                        conversation.javaClass.getMethod("cancelProcess").invoke(conversation)
                    } catch (cancelFailure: Throwable) {
                        cancelFailure.throwIfFatal()
                        cancelled.addSuppressed(cancelFailure)
                    }
                    throw cancelled
                }
                response.toString()
            } finally {
                conversation.closeIfPossible()
            }
        } finally {
            engine.closeIfPossible()
        }
    }

    private fun createBackend(packageName: String, backend: LiteRtBackend): Any =
        when (backend) {
            LiteRtBackend.GPU -> Class.forName("$packageName.Backend\$GPU")
                .getConstructor()
                .newInstance()

            LiteRtBackend.CPU -> Class.forName("$packageName.Backend\$CPU")
                .getConstructor(Int::class.javaObjectType, Int::class.javaObjectType)
                .newInstance(null, null)
        }

    private fun createEngineConfig(
        packageName: String,
        backendClass: Class<*>,
        modelFile: File,
        backend: Any,
    ): Any =
        Class.forName("$packageName.EngineConfig")
            .getConstructor(
                String::class.java,
                backendClass,
                backendClass,
                backendClass,
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                String::class.java,
            )
            .newInstance(
                modelFile.absolutePath,
                backend,
                null,
                null,
                null,
                null,
                modelFile.parentFile?.absolutePath,
            )

    private fun Any.closeIfPossible() {
        if (this is AutoCloseable) close()
    }
}
