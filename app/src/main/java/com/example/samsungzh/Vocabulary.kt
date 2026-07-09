package com.example.samsungzh

object Vocabulary {
    private const val TARGET_COUNT = 500

    val words: List<WordEntry> = createWords()

    private fun createWords(): List<WordEntry> {
        val entriesByHanzi = LinkedHashMap<String, WordEntry>()

        fun add(entry: WordEntry) {
            if (!entriesByHanzi.containsKey(entry.hanzi)) {
                entriesByHanzi[entry.hanzi] = entry
            }
        }

        coreWords().forEach(::add)
        simpleTerms().forEach { add(WordEntry(it.hanzi, it.pinyin, it.english)) }
        generatedPhrases().forEach(::add)

        val result = entriesByHanzi.values.take(TARGET_COUNT)
        check(result.size == TARGET_COUNT) {
            "Expected $TARGET_COUNT vocabulary entries but generated ${result.size}"
        }
        return result
    }

    private fun coreWords() = listOf(
        WordEntry("你好", "ni hao", "hello"),
        WordEntry("謝謝", "xie xie", "thank you"),
        WordEntry("學習", "xue xi", "to study"),
        WordEntry("中文", "zhong wen", "Chinese language"),
        WordEntry("朋友", "peng you", "friend"),
        WordEntry("今天", "jin tian", "today"),
        WordEntry("明天", "ming tian", "tomorrow"),
        WordEntry("昨天", "zuo tian", "yesterday"),
        WordEntry("喜歡", "xi huan", "to like"),
        WordEntry("知道", "zhi dao", "to know"),
        WordEntry("記得", "ji de", "to remember"),
        WordEntry("您好", "nin hao", "hello"),
        WordEntry("可以", "ke yi", "can; may"),
        WordEntry("因為", "yin wei", "because"),
        WordEntry("所以", "suo yi", "therefore"),
        WordEntry("時間", "shi jian", "time"),
        WordEntry("工作", "gong zuo", "work"),
        WordEntry("學校", "xue xiao", "school"),
        WordEntry("老師", "lao shi", "teacher"),
        WordEntry("學生", "xue sheng", "student"),
        WordEntry("東西", "dong xi", "thing"),
        WordEntry("天氣", "tian qi", "weather"),
        WordEntry("吃飯", "chi fan", "to eat"),
        WordEntry("喝水", "he shui", "to drink water"),
        WordEntry("睡覺", "shui jiao", "to sleep"),
        WordEntry("看書", "kan shu", "to read"),
        WordEntry("聽見", "ting jian", "to hear"),
        WordEntry("說話", "shuo hua", "to speak"),
        WordEntry("漂亮", "piao liang", "beautiful"),
        WordEntry("高興", "gao xing", "happy"),
        WordEntry("重要", "zhong yao", "important"),
        WordEntry("問題", "wen ti", "question; problem"),
        WordEntry("幫忙", "bang mang", "to help"),
        WordEntry("手機", "shou ji", "mobile phone"),
        WordEntry("電腦", "dian nao", "computer"),
        WordEntry("城市", "cheng shi", "city"),
        WordEntry("國家", "guo jia", "country"),
        WordEntry("家人", "jia ren", "family"),
        WordEntry("早上", "zao shang", "morning"),
        WordEntry("晚上", "wan shang", "evening"),
        WordEntry("現在", "xian zai", "now"),
        WordEntry("以後", "yi hou", "after; later"),
        WordEntry("捷運", "jie yun", "MRT; metro"),
        WordEntry("公車", "gong che", "bus"),
        WordEntry("計程車", "ji cheng che", "taxi"),
        WordEntry("腳踏車", "jiao ta che", "bicycle"),
        WordEntry("機車", "ji che", "scooter"),
        WordEntry("便當", "bian dang", "boxed meal"),
        WordEntry("飲料", "yin liao", "drink"),
        WordEntry("早餐", "zao can", "breakfast"),
        WordEntry("午餐", "wu can", "lunch"),
        WordEntry("晚餐", "wan can", "dinner"),
        WordEntry("夜市", "ye shi", "night market"),
        WordEntry("便利商店", "bian li shang dian", "convenience store"),
        WordEntry("發票", "fa piao", "receipt; invoice"),
        WordEntry("悠遊卡", "you you ka", "EasyCard"),
        WordEntry("不好意思", "bu hao yi si", "excuse me; sorry"),
        WordEntry("沒關係", "mei guan xi", "it is okay"),
        WordEntry("請問", "qing wen", "may I ask"),
        WordEntry("麻煩你", "ma fan ni", "please trouble you"),
        WordEntry("沒問題", "mei wen ti", "no problem"),
        WordEntry("等一下", "deng yi xia", "wait a moment"),
        WordEntry("慢慢來", "man man lai", "take your time"),
        WordEntry("再說一次", "zai shuo yi ci", "say it again"),
        WordEntry("聽不懂", "ting bu dong", "cannot understand by listening"),
        WordEntry("看不懂", "kan bu dong", "cannot understand by reading"),
        WordEntry("怎麼說", "zen me shuo", "how do you say it"),
        WordEntry("多少錢", "duo shao qian", "how much money"),
        WordEntry("太貴了", "tai gui le", "too expensive"),
        WordEntry("便宜一點", "pian yi yi dian", "a little cheaper"),
        WordEntry("不用袋子", "bu yong dai zi", "no bag needed"),
        WordEntry("內用", "nei yong", "for here"),
        WordEntry("外帶", "wai dai", "to go"),
        WordEntry("加辣", "jia la", "add spice"),
        WordEntry("不要辣", "bu yao la", "not spicy"),
        WordEntry("少冰", "shao bing", "less ice"),
        WordEntry("半糖", "ban tang", "half sugar"),
        WordEntry("無糖", "wu tang", "no sugar"),
        WordEntry("排隊", "pai dui", "to line up"),
        WordEntry("訂位", "ding wei", "to reserve a seat"),
        WordEntry("結帳", "jie zhang", "to pay the bill"),
        WordEntry("收據", "shou ju", "receipt"),
        WordEntry("地址", "di zhi", "address"),
        WordEntry("附近", "fu jin", "nearby"),
        WordEntry("左邊", "zuo bian", "left side"),
        WordEntry("右邊", "you bian", "right side"),
        WordEntry("前面", "qian mian", "in front"),
        WordEntry("後面", "hou mian", "behind"),
        WordEntry("樓上", "lou shang", "upstairs"),
        WordEntry("樓下", "lou xia", "downstairs"),
        WordEntry("入口", "ru kou", "entrance"),
        WordEntry("出口", "chu kou", "exit"),
        WordEntry("上班", "shang ban", "to go to work"),
        WordEntry("下班", "xia ban", "to get off work"),
        WordEntry("開會", "kai hui", "to have a meeting"),
        WordEntry("安排", "an pai", "to arrange"),
        WordEntry("決定", "jue ding", "to decide"),
        WordEntry("計畫", "ji hua", "plan"),
        WordEntry("經驗", "jing yan", "experience"),
        WordEntry("機會", "ji hui", "opportunity"),
        WordEntry("習慣", "xi guan", "habit"),
        WordEntry("生活", "sheng huo", "life"),
        WordEntry("環境", "huan jing", "environment"),
        WordEntry("文化", "wen hua", "culture"),
        WordEntry("健康", "jian kang", "health"),
        WordEntry("運動", "yun dong", "exercise; sport"),
        WordEntry("旅行", "lv xing", "travel"),
        WordEntry("照片", "zhao pian", "photo"),
        WordEntry("消息", "xiao xi", "news; information"),
        WordEntry("安全", "an quan", "safe; safety"),
        WordEntry("方便", "fang bian", "convenient"),
        WordEntry("特別", "te bie", "special"),
        WordEntry("清楚", "qing chu", "clear"),
        WordEntry("需要", "xu yao", "to need"),
    )

    private fun simpleTerms(): List<Term> =
        places() + foodAndDrinks() + items() + people() + actions() + adjectives()

    private fun generatedPhrases(): List<WordEntry> {
        val phrases = mutableListOf<WordEntry>()

        places().forEach { place ->
            phrases.add(entry("請問${place.hanzi}在哪裡", "qing wen ${place.pinyin} zai na li", "where is the ${place.english}"))
            phrases.add(entry("${place.hanzi}怎麼走", "${place.pinyin} zen me zou", "how do I get to the ${place.english}"))
            phrases.add(entry("我要去${place.hanzi}", "wo yao qu ${place.pinyin}", "I want to go to the ${place.english}"))
            phrases.add(entry("我在${place.hanzi}", "wo zai ${place.pinyin}", "I am at the ${place.english}"))
        }

        foodAndDrinks().forEach { food ->
            phrases.add(entry("我要${food.hanzi}", "wo yao ${food.pinyin}", "I want ${food.english}"))
        }
        foods().forEach { food ->
            phrases.add(entry("${food.hanzi}很好吃", "${food.pinyin} hen hao chi", "the ${food.english} is tasty"))
        }
        drinks().forEach { drink ->
            phrases.add(entry("${drink.hanzi}很好喝", "${drink.pinyin} hen hao he", "the ${drink.english} tastes good"))
        }

        buyableItems().forEach { item ->
            phrases.add(entry("我要買${item.hanzi}", "wo yao mai ${item.pinyin}", "I want to buy ${item.english}"))
        }
        neededItems().forEach { item ->
            phrases.add(entry("我需要${item.hanzi}", "wo xu yao ${item.pinyin}", "I need ${item.english}"))
            phrases.add(entry("我找不到${item.hanzi}", "wo zhao bu dao ${item.pinyin}", "I cannot find ${item.english}"))
        }

        activityPhrases().forEach { action ->
            phrases.add(entry("我想${action.hanzi}", "wo xiang ${action.pinyin}", "I want to ${action.english}"))
            phrases.add(entry("可以${action.hanzi}嗎", "ke yi ${action.pinyin} ma", "can I ${action.english}"))
        }

        timeTerms().forEach { time ->
            dailyPlans().forEach { plan ->
                phrases.add(entry("${time.hanzi}我要${plan.hanzi}", "${time.pinyin} wo yao ${plan.pinyin}", "${time.english}, I want to ${plan.english}"))
            }
        }

        people().take(10).forEach { person ->
            places().take(12).forEach { place ->
                phrases.add(entry("${person.hanzi}在${place.hanzi}", "${person.pinyin} zai ${place.pinyin}", "${person.english} is at the ${place.english}"))
            }
        }

        usefulQuestions().forEach { phrases.add(it) }
        adjectivePhrases().forEach { phrases.add(it) }

        return phrases
    }

    private fun places() = listOf(
        term("捷運站", "jie yun zhan", "MRT station"),
        term("公車站", "gong che zhan", "bus stop"),
        term("火車站", "huo che zhan", "train station"),
        term("高鐵站", "gao tie zhan", "high-speed rail station"),
        term("機場", "ji chang", "airport"),
        term("餐廳", "can ting", "restaurant"),
        term("咖啡店", "ka fei dian", "coffee shop"),
        term("茶店", "cha dian", "tea shop"),
        term("夜市", "ye shi", "night market"),
        term("便利商店", "bian li shang dian", "convenience store"),
        term("超市", "chao shi", "supermarket"),
        term("藥局", "yao ju", "pharmacy"),
        term("醫院", "yi yuan", "hospital"),
        term("診所", "zhen suo", "clinic"),
        term("銀行", "yin hang", "bank"),
        term("郵局", "you ju", "post office"),
        term("學校", "xue xiao", "school"),
        term("公司", "gong si", "company"),
        term("辦公室", "ban gong shi", "office"),
        term("圖書館", "tu shu guan", "library"),
        term("公園", "gong yuan", "park"),
        term("洗手間", "xi shou jian", "restroom"),
        term("飯店", "fan dian", "hotel"),
        term("市場", "shi chang", "market"),
    )

    private fun foodAndDrinks() = foods() + drinks()

    private fun foods() = listOf(
        term("蛋餅", "dan bing", "egg pancake"),
        term("飯糰", "fan tuan", "rice roll"),
        term("小籠包", "xiao long bao", "soup dumplings"),
        term("牛肉麵", "niu rou mian", "beef noodle soup"),
        term("滷肉飯", "lu rou fan", "braised pork rice"),
        term("雞排", "ji pai", "fried chicken cutlet"),
        term("鍋貼", "guo tie", "potstickers"),
        term("水餃", "shui jiao", "dumplings"),
        term("麵", "mian", "noodles"),
        term("飯", "fan", "rice"),
        term("湯", "tang", "soup"),
        term("青菜", "qing cai", "vegetables"),
        term("水果", "shui guo", "fruit"),
        term("甜點", "tian dian", "dessert"),
        term("蛋糕", "dan gao", "cake"),
        term("冰淇淋", "bing qi lin", "ice cream"),
        term("便當", "bian dang", "boxed meal"),
        term("麵包", "mian bao", "bread"),
        term("三明治", "san ming zhi", "sandwich"),
        term("早餐", "zao can", "breakfast"),
        term("午餐", "wu can", "lunch"),
        term("晚餐", "wan can", "dinner"),
    )

    private fun drinks() = listOf(
        term("水", "shui", "water"),
        term("茶", "cha", "tea"),
        term("咖啡", "ka fei", "coffee"),
        term("奶茶", "nai cha", "milk tea"),
        term("珍珠奶茶", "zhen zhu nai cha", "bubble tea"),
        term("果汁", "guo zhi", "juice"),
        term("豆漿", "dou jiang", "soy milk"),
        term("飲料", "yin liao", "drink"),
        term("熱茶", "re cha", "hot tea"),
        term("冰咖啡", "bing ka fei", "iced coffee"),
    )

    private fun items() = listOf(
        term("充電器", "chong dian qi", "charger"),
        term("鑰匙", "yao shi", "keys"),
        term("錢包", "qian bao", "wallet"),
        term("雨傘", "yu san", "umbrella"),
        term("外套", "wai tao", "jacket"),
        term("帽子", "mao zi", "hat"),
        term("鞋子", "xie zi", "shoes"),
        term("筆", "bi", "pen"),
        term("筆記本", "bi ji ben", "notebook"),
        term("車票", "che piao", "ticket"),
        term("座位", "zuo wei", "seat"),
        term("房間", "fang jian", "room"),
        term("電梯", "dian ti", "elevator"),
        term("樓梯", "lou ti", "stairs"),
        term("地圖", "di tu", "map"),
        term("行李", "xing li", "luggage"),
        term("背包", "bei bao", "backpack"),
        term("口罩", "kou zhao", "mask"),
        term("藥", "yao", "medicine"),
        term("眼鏡", "yan jing", "glasses"),
        term("衣服", "yi fu", "clothes"),
        term("禮物", "li wu", "gift"),
        term("手機", "shou ji", "mobile phone"),
        term("電腦", "dian nao", "computer"),
        term("悠遊卡", "you you ka", "EasyCard"),
        term("發票", "fa piao", "receipt lottery invoice"),
        term("收據", "shou ju", "receipt"),
        term("書", "shu", "book"),
    )

    private fun buyableItems() =
        (foods() + drinks() + items().take(18)).distinctBy { it.hanzi }

    private fun neededItems() =
        (items() + places().take(4)).distinctBy { it.hanzi }

    private fun people() = listOf(
        term("我", "wo", "I"),
        term("你", "ni", "you"),
        term("他", "ta", "he"),
        term("她", "ta", "she"),
        term("我們", "wo men", "we"),
        term("你們", "ni men", "you all"),
        term("他們", "ta men", "they"),
        term("朋友", "peng you", "friend"),
        term("家人", "jia ren", "family member"),
        term("老師", "lao shi", "teacher"),
        term("學生", "xue sheng", "student"),
        term("同事", "tong shi", "coworker"),
        term("老闆", "lao ban", "boss"),
        term("客人", "ke ren", "customer"),
        term("醫生", "yi sheng", "doctor"),
        term("司機", "si ji", "driver"),
        term("店員", "dian yuan", "clerk"),
        term("鄰居", "lin ju", "neighbor"),
    )

    private fun actions() = listOf(
        term("買", "mai", "buy"),
        term("找", "zhao", "look for"),
        term("帶", "dai", "bring"),
        term("用", "yong", "use"),
        term("看", "kan", "look at"),
        term("聽", "ting", "listen"),
        term("說", "shuo", "speak"),
        term("學", "xue", "learn"),
        term("練習", "lian xi", "practice"),
        term("了解", "liao jie", "understand"),
        term("準備", "zhun bei", "prepare"),
        term("安排", "an pai", "arrange"),
        term("決定", "jue ding", "decide"),
        term("等", "deng", "wait"),
        term("問", "wen", "ask"),
        term("休息", "xiu xi", "rest"),
        term("拍照", "pai zhao", "take photos"),
        term("訂位", "ding wei", "reserve a seat"),
        term("結帳", "jie zhang", "pay the bill"),
        term("排隊", "pai dui", "line up"),
    )

    private fun activityPhrases() = listOf(
        term("休息", "xiu xi", "rest"),
        term("運動", "yun dong", "exercise"),
        term("旅行", "lv xing", "travel"),
        term("拍照", "pai zhao", "take photos"),
        term("練習中文", "lian xi zhong wen", "practice Chinese"),
        term("看書", "kan shu", "read"),
        term("聽音樂", "ting yin yue", "listen to music"),
        term("說中文", "shuo zhong wen", "speak Chinese"),
        term("問問題", "wen wen ti", "ask a question"),
        term("幫忙", "bang mang", "help"),
        term("訂位", "ding wei", "reserve a seat"),
        term("結帳", "jie zhang", "pay the bill"),
        term("排隊", "pai dui", "line up"),
        term("上班", "shang ban", "go to work"),
        term("下班", "xia ban", "get off work"),
        term("開會", "kai hui", "have a meeting"),
        term("安排時間", "an pai shi jian", "arrange a time"),
        term("決定日期", "jue ding ri qi", "decide a date"),
        term("計畫旅行", "ji hua lv xing", "plan a trip"),
        term("去夜市", "qu ye shi", "go to a night market"),
    )

    private fun adjectives() = listOf(
        term("好", "hao", "good"),
        term("不錯", "bu cuo", "not bad"),
        term("很棒", "hen bang", "great"),
        term("快", "kuai", "fast"),
        term("慢", "man", "slow"),
        term("忙", "mang", "busy"),
        term("累", "lei", "tired"),
        term("舒服", "shu fu", "comfortable"),
        term("簡單", "jian dan", "simple"),
        term("困難", "kun nan", "difficult"),
        term("熱", "re", "hot"),
        term("冷", "leng", "cold"),
        term("新", "xin", "new"),
        term("舊", "jiu", "old"),
        term("近", "jin", "near"),
        term("遠", "yuan", "far"),
        term("開心", "kai xin", "happy"),
        term("緊張", "jin zhang", "nervous"),
        term("準時", "zhun shi", "on time"),
        term("安靜", "an jing", "quiet"),
    )

    private fun timeTerms() = listOf(
        term("今天", "jin tian", "today"),
        term("明天", "ming tian", "tomorrow"),
        term("週末", "zhou mo", "this weekend"),
        term("下週", "xia zhou", "next week"),
        term("早上", "zao shang", "in the morning"),
        term("中午", "zhong wu", "at noon"),
        term("下午", "xia wu", "in the afternoon"),
        term("晚上", "wan shang", "in the evening"),
    )

    private fun dailyPlans() = listOf(
        term("上班", "shang ban", "go to work"),
        term("去學校", "qu xue xiao", "go to school"),
        term("看電影", "kan dian ying", "watch a movie"),
        term("吃晚餐", "chi wan can", "eat dinner"),
        term("買咖啡", "mai ka fei", "buy coffee"),
        term("運動", "yun dong", "exercise"),
        term("休息", "xiu xi", "rest"),
        term("開會", "kai hui", "have a meeting"),
        term("學中文", "xue zhong wen", "study Chinese"),
        term("去夜市", "qu ye shi", "go to a night market"),
    )

    private fun usefulQuestions() = listOf(
        entry("你有空嗎", "ni you kong ma", "are you free"),
        entry("你要不要一起去", "ni yao bu yao yi qi qu", "do you want to go together"),
        entry("這個可以嗎", "zhe ge ke yi ma", "is this okay"),
        entry("這裡可以坐嗎", "zhe li ke yi zuo ma", "can I sit here"),
        entry("可以刷卡嗎", "ke yi shua ka ma", "can I pay by card"),
        entry("可以用悠遊卡嗎", "ke yi yong you you ka ma", "can I use EasyCard"),
        entry("可以外帶嗎", "ke yi wai dai ma", "can I take it to go"),
        entry("可以內用嗎", "ke yi nei yong ma", "can I eat here"),
        entry("你可以幫我嗎", "ni ke yi bang wo ma", "can you help me"),
        entry("這個怎麼用", "zhe ge zen me yong", "how do I use this"),
        entry("下一班車幾點", "xia yi ban che ji dian", "what time is the next bus or train"),
        entry("這班車到台北嗎", "zhe ban che dao tai bei ma", "does this bus or train go to Taipei"),
        entry("附近有洗手間嗎", "fu jin you xi shou jian ma", "is there a restroom nearby"),
        entry("附近有便利商店嗎", "fu jin you bian li shang dian ma", "is there a convenience store nearby"),
        entry("可以說慢一點嗎", "ke yi shuo man yi dian ma", "can you speak more slowly"),
        entry("可以寫下來嗎", "ke yi xie xia lai ma", "can you write it down"),
    )

    private fun adjectivePhrases() = listOf(
        entry("天氣很熱", "tian qi hen re", "the weather is hot"),
        entry("天氣很冷", "tian qi hen leng", "the weather is cold"),
        entry("交通很方便", "jiao tong hen fang bian", "transportation is convenient"),
        entry("這裡很安靜", "zhe li hen an jing", "it is quiet here"),
        entry("這家店很漂亮", "zhe jia dian hen piao liang", "this shop is pretty"),
        entry("這個便當很便宜", "zhe ge bian dang hen pian yi", "this boxed meal is cheap"),
        entry("這杯飲料太甜", "zhe bei yin liao tai tian", "this drink is too sweet"),
        entry("中文有一點難", "zhong wen you yi dian nan", "Chinese is a little difficult"),
        entry("今天很忙", "jin tian hen mang", "today is busy"),
        entry("我有一點累", "wo you yi dian lei", "I am a little tired"),
        entry("這個問題很重要", "zhe ge wen ti hen zhong yao", "this question is important"),
        entry("你的中文很清楚", "ni de zhong wen hen qing chu", "your Chinese is clear"),
    )

    private fun entry(hanzi: String, pinyin: String, english: String) =
        WordEntry(hanzi, pinyin, english)

    private fun term(hanzi: String, pinyin: String, english: String) =
        Term(hanzi, pinyin, english)

    private data class Term(
        val hanzi: String,
        val pinyin: String,
        val english: String,
    )
}
