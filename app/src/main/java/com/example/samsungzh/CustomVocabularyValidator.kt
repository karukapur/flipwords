package com.example.samsungzh

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.text.Normalizer

object CustomVocabularyValidator {
    const val MAX_INPUT_CHARS = 120

    private val requiredJsonFields = setOf("hanzi", "pinyin", "english")
    private val whitespace = Regex("\\s+")
    private val pinyinCharacters = Regex(
        "^[A-Za-züÜāáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜńňǹḿ'’ -]+$",
    )
    private val toneMarks = setOf(
        'ā', 'á', 'ǎ', 'à', 'ē', 'é', 'ě', 'è', 'ī', 'í', 'ǐ', 'ì',
        'ō', 'ó', 'ǒ', 'ò', 'ū', 'ú', 'ǔ', 'ù', 'ǖ', 'ǘ', 'ǚ', 'ǜ',
        'ń', 'ň', 'ǹ', 'ḿ',
    )
    private val neutralOnlySyllables = setOf(
        "a", "ba", "de", "di", "ge", "le", "li", "ma", "me", "men", "ne",
        "shang", "xia", "xie", "zhe", "zi",
    )
    private val simplifiedOnlyCharacters = (
        "谢学习欢为时间师东气饭觉书听说兴题帮机电国现车发关问钱贵买卖带队订账据" +
            "边楼会决计划经验惯环运动别处这个里吗写来点运龙门云网开长张万与业专严丧" +
            "两丰临丽举义乌乐乔乡亲仅从仓仪们价众优伙伞伟传伤伦体余佣侠侣侥侦侧侨" +
            "侩俩俭债倾偿储儿兑党兰兽冈册军农冲冻净凉减凑凤凭凯击凿刍刘则刚创删剂" +
            "办务劝劳势勋区医华协单卖卢卫厅历厉压厌县叁参双变叙叶号叹吓吕员呛呜咏" +
            "咙响哑哗哟唤啧喷嘱团园围图圆圣场坏块坚坛坝坞坟坠垄垒垦垫垭垯垱垲埙" +
            "垴埘埙堑堕墙壮声壳壶寿够梦夺奋奖妆妇妈妩姗姜娄娱婴孙宁宝实宠审宪宫" +
            "宽宾寝对寻导将尔尘尝层屉届属岁岂岛岭岗岚岿峄峡峣峥峦崭巩币帅帐帘帜带" +
            "帮并庄庆庐库应庙庞废广归当录彦彻径忆忧怀态总恋恶恼恳悬惊惧惨惩惫惬" +
            "惭惯愤愿戏户执扩扫扬扰抚抛抢护报担拟拢拣拥拦拨择挂挚挛挝挞挟挠挡挣" +
            "挤挥损捡换捣据掳掷掺掼揽搀搁搂搅携摄摆摇摊撑撵敌数斋斩断无旧显晓暂" +
            "术朴权条来杨杰极构枪标栈栋栏树样档桥桦桨桩梦检椭楼榄榇榈槛横樱欢欧歼" +
            "毁毕毙毡氢汇汉汤沟没沣沤沥沦沧沪泞泪泼泽洁洒洼浅浆浇浊测济浓浔涂涌" +
            "涡涣涤润涧涨涩淀渊渐渔渗温湾湿溃溅滚滞满滤滥滨滩潇潜澜濑濒灭灯灵灾" +
            "灿炀炉炖炜炼烟烦烧烨烫烬热爱爷牍牵犹狈狞独狭狮狱猎猪猫献玛环现电画" +
            "畅疗疟疡疮疯痪瘫皑皱盏盐监盖盘眍着睁瞒瞩矫矿码砖砚砺砾础硅硕确碍碱" +
            "礼祷祸离种积称稳穷窑窜窝窥窦竞笃笋笔笺笼筑筛筹签简箩篓篮篱类籼粜粮" +
            "紧纠红纤约级纪纫纬纯纱纲纳纵纶纷纸纹纺纽线练组绅细织终绊绍绎经绑绒" +
            "结绕绘给络绝绞统绢绣继绩绪续绰绳维绵绷绸综绿缀缅缆缉缎缓缔缕编缘缚" +
            "缝缠缤缩缴网罗罚罢羁翘耸耻聂聋职联肃肠肤肿胀胁胆胜胶脉脏脑脚脱脸腊" +
            "腻腾舆舰舱艰艳艺节芜芦苇苹范茎茧荆荐荡荣荤荧药莱莲获莹莺萝营萧萨葱" +
            "蒋蓝蓟蔷蔹蔺蕴薮虑虚虫虽虾蚀蚂蚕蛊蛎蛏蛮蜕蜗蝇蝉蝼蝾衅衔补衬袄袜袭" +
            "装裆裤见观规觅视览觉觊觋觌觎觏觐觑触誉誊计订认讥讨让训议讯记讲讳讴" +
            "讶许论讼讽设访诀证评诅识诈诉诊词译试诗诚话诞诡询诣该详诧诫诬语误诱" +
            "诲说诵请诸诺读课诽谁调谅谈谊谋谍谎谏谐谓谚谜谢谣谤谦谨谩谱谴谷豁贝" +
            "贞负贡财责贤败账货质贩贪贫贬购贮贯贰贱贲贳贴贵贷贸费贺贻贼贾贿赀赁" +
            "赂赃资赅赈赉赊赋赌赎赏赐赔赖赘赚赛赞赠赡赢赵赶趋跃践跷跸跹跻踊踪踬" +
            "蹑蹒蹿躏躯轨轧轩转轮软轰轴轻载轿较辅辆辈辉辊辋辍辎辏辐辑输辘辖辗辙" +
            "辞辩辫边辽达迁过迈还进远违连迟迩迳迹适选逊递逻遗遥邓邮邹邻郑郓郦郧" +
            "酝酱酿释里鉴针钉钊钋钌钍钎钏钐钒钓钔钕钗钙钛钜钝钞钟钠钡钢钥钦钧钨" +
            "钩钮钱钳钻钾铁铃铅铆铜铝铠铢铣铭铲银铸铺链销锁锄锅锈锋锐锑锒错锚锡" +
            "锣锤锥锦锨锭键锯锰锹锻锼镀镇镊镐镑镒镓镔镖镜镝镞镣镦镧镨镪镫镭镯镰" +
            "镳镶长门闪闭问闯闰闲闳间闵闸闹闺闻闽阀阁阂阅阉阎阐阑阒阔队阳阴阵" +
            "阶际陆陇陈陉陕陨险随隐隶难雏雠雳雾霁霉静韦韧韩页顶顷项顺须顽顾顿颁" +
            "颂预颅领颇颈颊颐频颓颖颗题颜额颠颤风飒飘飞饥饭饮饰饱饲饵饶饷饺饼饿" +
            "馁馅馆馈馋馍马驭驮驯驰驱驳驴驶驷驸驹驻驼驾骂骄骆骇骈骋验骏骑骗骚" +
            "骡骤髅鬓魇鱼鲁鲂鲍鲎鲐鲔鲛鲜鲟鲠鲢鲣鲤鲥鲦鲧鲨鲩鲫鲭鲮鲰鲱鲲鲳鲸" +
            "鲵鲶鲷鲻鳄鳅鳆鳇鳌鳍鳎鳏鳐鳓鳔鳕鳖鳗鳜鳝鳞鸟鸡鸣鸥鸦鸨鸩鸪鸬鸭鸯" +
            "鸱鸳鸵鸽鸾鸿鹃鹅鹊鹌鹏鹑鹕鹗鹘鹞鹤鹦鹰麦麸黄黉黩齐齿龃龄龅龆龇龈" +
            "龉龊龋龌龟"
        ).toSet()

    fun normalizeInput(rawInput: String): String =
        Normalizer.normalize(rawInput.trim(), Normalizer.Form.NFC)

    fun inputValidationError(rawInput: String): String? {
        val input = normalizeInput(rawInput)
        return when {
            input.isEmpty() -> "Enter a word or short phrase."
            input.length > MAX_INPUT_CHARS -> "Input must be $MAX_INPUT_CHARS characters or fewer."
            else -> null
        }
    }

    fun normalize(candidate: ResolvedVocabularyCandidate): ResolvedVocabularyCandidate =
        ResolvedVocabularyCandidate(
            hanzi = normalizeText(candidate.hanzi),
            pinyin = normalizeText(candidate.pinyin),
            english = normalizeText(candidate.english),
        )

    /** Returns null when the candidate can safely become a learning card. */
    fun validationError(candidate: ResolvedVocabularyCandidate): String? {
        val normalized = normalize(candidate)
        return when {
            normalized.hanzi.isEmpty() -> "Traditional Chinese is required."
            normalized.hanzi.length > GeneratedVocabularyValidator.MAX_HANZI_CHARS ->
                "Traditional Chinese must be 1 to ${GeneratedVocabularyValidator.MAX_HANZI_CHARS} characters."
            normalized.hanzi.any { !isCjk(it) } ->
                "Traditional Chinese must contain only Han characters."
            normalized.hanzi.any { it in simplifiedOnlyCharacters && it !in traditionalSharedCharacters } ->
                "Use Traditional Chinese characters."
            normalized.pinyin.isEmpty() -> "Pinyin is required."
            normalized.pinyin.length > GeneratedVocabularyValidator.MAX_PINYIN_CHARS ->
                "Pinyin must be ${GeneratedVocabularyValidator.MAX_PINYIN_CHARS} characters or fewer."
            !pinyinCharacters.matches(normalized.pinyin) ->
                "Use pinyin letters and tone marks, not tone numbers."
            !hasValidToneNotation(normalized.pinyin) ->
                "Add pinyin tone marks; neutral-tone syllables may stay unmarked."
            normalized.english.isEmpty() -> "English meaning is required."
            normalized.english.length > GeneratedVocabularyValidator.MAX_ENGLISH_CHARS ->
                "English meaning must be ${GeneratedVocabularyValidator.MAX_ENGLISH_CHARS} characters or fewer."
            normalized.english.any { it.isISOControl() } ->
                "English meaning contains an unsupported character."
            else -> null
        }
    }

    /** Accepts exactly one JSON object, either directly or inside a one-element array. */
    fun parseCandidate(rawOutput: String): ResolvedVocabularyCandidate? {
        val root = parseExactJsonValue(rawOutput) ?: return null
        val jsonObject = when (root) {
            is JSONObject -> root
            is JSONArray -> {
                if (root.length() != 1) return null
                root.opt(0) as? JSONObject ?: return null
            }
            else -> return null
        }
        if (jsonObject.keys().asSequence().toSet() != requiredJsonFields) return null

        val hanzi = jsonObject.opt("hanzi") as? String ?: return null
        val pinyin = jsonObject.opt("pinyin") as? String ?: return null
        val english = jsonObject.opt("english") as? String ?: return null
        val candidate = normalize(ResolvedVocabularyCandidate(hanzi, pinyin, english))
        return candidate.takeIf { validationError(it) == null }
    }

    private fun parseExactJsonValue(rawOutput: String): Any? {
        val trimmed = rawOutput.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            val tokener = JSONTokener(trimmed)
            val value = tokener.nextValue()
            if (tokener.nextClean() != '\u0000') null else value
        }.getOrNull()
    }

    private fun normalizeText(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFC).replace(whitespace, " ")

    private fun isCjk(character: Char): Boolean =
        character.code in CJK_UNIFIED || character.code in CJK_EXTENSION_A

    private fun hasValidToneNotation(pinyin: String): Boolean {
        val syllables = pinyin.lowercase()
            .split(' ', '-', '\'', '’')
            .filter { it.isNotBlank() }
        return syllables.isNotEmpty() && syllables.all { syllable ->
            syllable.count { it in toneMarks } == 1 || syllable in neutralOnlySyllables
        }
    }

    // These glyphs are also legitimate Traditional characters in Taiwan in their own right.
    // A character-only simplified check must not reject words such as 里程, 里長, or 姜茶.
    private val traditionalSharedCharacters = setOf('里', '姜', '余', '范', '后', '乾', '面')

    private val CJK_UNIFIED = 0x4E00..0x9FFF
    private val CJK_EXTENSION_A = 0x3400..0x4DBF
}
