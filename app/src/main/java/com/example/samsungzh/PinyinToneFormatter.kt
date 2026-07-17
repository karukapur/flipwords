package com.example.samsungzh

object PinyinToneFormatter {
    fun format(word: WordEntry): String {
        if (word.pinyin.any { it in TONE_MARKS }) return word.pinyin

        val hanzi = word.hanzi.filter { it.code in CJK_RANGE }
        val syllables = word.pinyin.split(' ').filter { it.isNotBlank() }
        if (hanzi.length != syllables.size) return word.pinyin

        val formatted = hanzi.toList().zip(syllables).map { (char, syllable) ->
            tonedSyllables["$char|$syllable"] ?: syllable
        }
        return formatted.joinToString(" ")
    }

    private val tonedSyllables = mapOf(
        "一|yi" to "yī", "三|san" to "sān", "上|shang" to "shàng", "下|xia" to "xià",
        "不|bu" to "bù", "中|zhong" to "zhōng", "了|le" to "le", "了|liao" to "liǎo",
        "事|shi" to "shì", "交|jiao" to "jiāo", "亮|liang" to "liàng", "人|ren" to "rén",
        "今|jin" to "jīn", "他|ta" to "tā", "以|yi" to "yǐ", "休|xiu" to "xiū",
        "位|wei" to "wèi", "作|zuo" to "zuò", "你|ni" to "nǐ", "來|lai" to "lái",
        "便|bian" to "biàn", "便|pian" to "pián", "係|xi" to "xì", "個|ge" to "ge",
        "們|men" to "men", "健|jian" to "jiàn", "傘|san" to "sǎn", "備|bei" to "bèi",
        "充|chong" to "chōng", "入|ru" to "rù", "內|nei" to "nèi", "全|quan" to "quán",
        "公|gong" to "gōng", "再|zai" to "zài", "冰|bing" to "bīng", "冷|leng" to "lěng",
        "出|chu" to "chū", "別|bie" to "bié", "利|li" to "lì", "到|dao" to "dào",
        "刷|shua" to "shuā", "前|qian" to "qián", "加|jia" to "jiā", "動|dong" to "dòng",
        "包|bao" to "bāo", "化|hua" to "huà", "北|bei" to "běi", "匙|shi" to "shi",
        "午|wu" to "wǔ", "半|ban" to "bàn", "卡|ka" to "kǎ", "去|qu" to "qù",
        "友|you" to "yǒu", "口|kou" to "kǒu", "可|ke" to "kě", "台|tai" to "tái",
        "右|you" to "yòu", "司|si" to "sī", "吃|chi" to "chī", "同|tong" to "tóng",
        "咖|ka" to "kā", "員|yuan" to "yuán", "商|shang" to "shāng", "問|wen" to "wèn",
        "啡|fei" to "fēi", "喜|xi" to "xǐ", "喝|he" to "hē", "單|dan" to "dān",
        "嗎|ma" to "ma", "器|qi" to "qì", "因|yin" to "yīn", "困|kun" to "kùn",
        "國|guo" to "guó", "園|yuan" to "yuán", "圖|tu" to "tú", "在|zai" to "zài",
        "地|di" to "dì", "址|zhi" to "zhǐ", "坐|zuo" to "zuò", "城|cheng" to "chéng",
        "場|chang" to "chǎng", "境|jing" to "jìng", "外|wai" to "wài", "多|duo" to "duō",
        "夜|ye" to "yè", "天|tian" to "tiān", "太|tai" to "tài", "套|tao" to "tào",
        "奶|nai" to "nǎi", "她|ta" to "tā", "好|hao" to "hǎo", "子|zi" to "zi",
        "學|xue" to "xué", "安|an" to "ān", "定|ding" to "dìng", "宜|yi" to "yí",
        "客|ke" to "kè", "室|shi" to "shì", "家|jia" to "jiā", "寫|xie" to "xiě",
        "小|xiao" to "xiǎo", "少|shao" to "shǎo", "局|ju" to "jú", "居|ju" to "jū",
        "工|gong" to "gōng", "左|zuo" to "zuǒ", "市|shi" to "shì", "師|shi" to "shī",
        "帳|zhang" to "zhàng", "帶|dai" to "dài", "帽|mao" to "mào", "幫|bang" to "bāng",
        "幾|ji" to "jǐ", "店|dian" to "diàn", "座|zuo" to "zuò", "康|kang" to "kāng",
        "廳|ting" to "tīng", "張|zhang" to "zhāng", "影|ying" to "yǐng", "很|hen" to "hěn",
        "後|hou" to "hòu", "心|xin" to "xīn", "忙|mang" to "máng", "快|kuai" to "kuài",
        "怎|zen" to "zěn", "思|si" to "si", "悠|you" to "yōu", "意|yi" to "yì",
        "慢|man" to "màn", "慣|guan" to "guàn", "懂|dong" to "dǒng", "我|wo" to "wǒ",
        "房|fang" to "fáng", "所|suo" to "suǒ", "手|shou" to "shǒu", "找|zhao" to "zhǎo",
        "拍|pai" to "pāi", "捷|jie" to "jié", "排|pai" to "pái", "據|ju" to "jù",
        "收|shou" to "shōu", "文|wen" to "wén", "料|liao" to "liào", "新|xin" to "xīn",
        "方|fang" to "fāng", "旅|lv" to "lǚ", "日|ri" to "rì", "早|zao" to "zǎo",
        "明|ming" to "míng", "昨|zuo" to "zuó", "時|shi" to "shí", "晚|wan" to "wǎn",
        "書|shu" to "shū", "會|hui" to "huì", "有|you" to "yǒu", "朋|peng" to "péng",
        "服|fu" to "fú", "期|qi" to "qī", "末|mo" to "mò", "本|ben" to "běn",
        "李|li" to "lǐ", "杯|bei" to "bēi", "東|dong" to "dōng", "果|guo" to "guǒ",
        "校|xiao" to "xiào", "梯|ti" to "tī", "棒|bang" to "bàng", "楚|chu" to "chǔ",
        "樂|yue" to "yuè", "樓|lou" to "lóu", "機|ji" to "jī", "次|ci" to "cì",
        "歡|huan" to "huān", "氣|qi" to "qì", "水|shui" to "shuǐ", "汁|zhi" to "zhī",
        "決|jue" to "jué", "沒|mei" to "méi", "治|zhi" to "zhì", "洗|xi" to "xǐ",
        "活|huo" to "huó", "消|xiao" to "xiāo", "淇|qi" to "qí", "淋|lin" to "lín",
        "清|qing" to "qīng", "湯|tang" to "tāng", "準|zhun" to "zhǔn", "滷|lu" to "lǔ",
        "漂|piao" to "piào", "漿|jiang" to "jiāng", "火|huo" to "huǒ", "為|wei" to "wèi",
        "無|wu" to "wú", "照|zhao" to "zhào", "煩|fan" to "fán", "熱|re" to "rè",
        "片|pian" to "piàn", "牛|niu" to "niú", "物|wu" to "wù", "特|te" to "tè",
        "珍|zhen" to "zhēn", "珠|zhu" to "zhū", "班|ban" to "bān", "現|xian" to "xiàn",
        "環|huan" to "huán", "甜|tian" to "tián", "生|sheng" to "shēng", "用|yong" to "yòng",
        "畫|hua" to "huà", "當|dang" to "dāng", "發|fa" to "fā", "的|de" to "de",
        "看|kan" to "kàn", "眼|yan" to "yǎn", "睡|shui" to "shuì", "知|zhi" to "zhī",
        "票|piao" to "piào", "禮|li" to "lǐ", "程|cheng" to "chéng", "空|kong" to "kòng",
        "站|zhan" to "zhàn", "筆|bi" to "bǐ", "等|deng" to "děng", "簡|jian" to "jiǎn",
        "籠|long" to "lóng", "糕|gao" to "gāo", "糖|tang" to "táng", "糰|tuan" to "tuán",
        "累|lei" to "lèi", "結|jie" to "jié", "經|jing" to "jīng", "緊|jin" to "jǐn",
        "練|lian" to "liàn", "罩|zhao" to "zhào", "習|xi" to "xí", "老|lao" to "lǎo",
        "聽|ting" to "tīng", "肉|rou" to "ròu", "背|bei" to "bēi", "腦|nao" to "nǎo",
        "腳|jiao" to "jiǎo", "興|xing" to "xìng", "舊|jiu" to "jiù", "舒|shu" to "shū",
        "茶|cha" to "chá", "菜|cai" to "cài", "藥|yao" to "yào", "蛋|dan" to "dàn",
        "行|hang" to "háng", "行|xing" to "xíng", "衣|yi" to "yī", "袋|dai" to "dài",
        "裡|li" to "lǐ", "西|xi" to "xī", "要|yao" to "yào", "見|jian" to "jiàn",
        "覺|jiao" to "jiào", "解|jie" to "jiě", "訂|ding" to "dìng", "計|ji" to "jì",
        "記|ji" to "jì", "診|zhen" to "zhěn", "話|hua" to "huà", "說|shuo" to "shuō",
        "請|qing" to "qǐng", "謝|xie" to "xiè", "豆|dou" to "dòu", "貴|gui" to "guì",
        "買|mai" to "mǎi", "貼|tie" to "tiē", "起|qi" to "qǐ", "超|chao" to "chāo",
        "踏|ta" to "tà", "車|che" to "chē", "辣|la" to "là", "辦|ban" to "bàn",
        "近|jin" to "jìn", "這|zhe" to "zhè", "通|tong" to "tōng", "週|zhou" to "zhōu",
        "遊|you" to "yóu", "運|yun" to "yùn", "道|dao" to "dào", "遠|yuan" to "yuǎn",
        "邊|bian" to "biān", "郵|you" to "yóu", "鄰|lin" to "lín", "醫|yi" to "yī",
        "重|zhong" to "zhòng", "銀|yin" to "yín", "錢|qian" to "qián", "錯|cuo" to "cuò",
        "酥|su" to "sū", "鍋|guo" to "guō", "鏡|jing" to "jìng", "鐵|tie" to "tiě", "鑰|yao" to "yào",
        "開|kai" to "kāi", "間|jian" to "jiān", "闆|ban" to "bǎn", "關|guan" to "guān",
        "附|fu" to "fù", "院|yuan" to "yuàn", "隊|dui" to "duì", "雞|ji" to "jī",
        "難|nan" to "nán", "雨|yu" to "yǔ", "電|dian" to "diàn", "需|xu" to "xū",
        "青|qing" to "qīng", "靜|jing" to "jìng", "面|mian" to "miàn", "鞋|xie" to "xié",
        "音|yin" to "yīn", "題|ti" to "tí", "飯|fan" to "fàn", "飲|yin" to "yǐn",
        "餃|jiao" to "jiǎo", "餅|bing" to "bǐng", "餐|can" to "cān", "館|guan" to "guǎn",
        "鹽|yan" to "yán", "驗|yan" to "yàn", "高|gao" to "gāo", "麵|mian" to "miàn", "麻|ma" to "má",
        "麼|me" to "me", "點|dian" to "diǎn"
    )

    private val TONE_MARKS = setOf(
        'ā', 'á', 'ǎ', 'à', 'ē', 'é', 'ě', 'è', 'ī', 'í', 'ǐ', 'ì',
        'ō', 'ó', 'ǒ', 'ò', 'ū', 'ú', 'ǔ', 'ù', 'ǖ', 'ǘ', 'ǚ', 'ǜ',
    )

    private val CJK_RANGE = 0x4E00..0x9FFF
}
