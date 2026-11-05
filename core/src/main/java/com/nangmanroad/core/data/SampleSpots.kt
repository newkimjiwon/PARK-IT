package com.nangmanroad.core.data

/**
 * Sample posts until the shared backend exists. Places are named only down to the city/county
 * level and every tip is phrased as a visitor report, so nothing here reads as official
 * confirmation that camping or parking is allowed.
 */
object SampleSpots {

    private fun unsplash(id: String, width: Int = 1400) =
        "https://images.unsplash.com/photo-$id?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=$width"

    private val coast = unsplash("1775406866232-2b5047fb93c0", 1600)
    private val sunset = unsplash("1702038852944-ac3c27498313")
    private val lake = unsplash("1551760223-58c78db2984e")
    private val night = unsplash("1596344502400-7951346410e5")
    private val tent = unsplash("1513781419235-2988ecacab83")
    private val village = unsplash("1542086043-08aee8fd3c21")
    private val road = unsplash("1696226549788-1a60cba184f1")

    private val romance = Author("u-romance", "낭만을찾아서", "강원도를 여행 중이에요")
    private val nightSky = Author("u-nightsky", "밤하늘수집가", "별 보이는 곳만 골라 다녀요")
    private val weekend = Author("u-weekend", "주말의기록", "주말마다 짧게 떠나요")
    private val fivePm = Author("u-fivepm", "오후다섯시", "해 질 무렵을 좋아해요")
    private val yunseul = Author("u-yunseul", "윤슬을따라", "주말마다 새로운 바다를 찾아 떠나요")

    private val lantern = Product(
        id = "p-lantern",
        name = "휴대용 무드 랜턴",
        reason = "조명이 없는 곳에서 밤 풍경을 오래 즐기기 좋아요",
        recommender = "낭만을찾아서",
        priceWon = 39000,
        isSponsored = false,
        imageUrl = tent,
    )
    private val chair = Product(
        id = "p-chair",
        name = "로우 캠핑 체어",
        reason = "노을을 볼 때 차 옆에 두기 편한 높이예요",
        recommender = "낭만을찾아서",
        priceWon = null,
        isSponsored = false,
        imageUrl = sunset,
    )
    private val table = Product(
        id = "p-table",
        name = "우드 차량용 테이블",
        reason = "트렁크에 펼쳐서 간단한 티타임에 썼어요",
        recommender = "낭만을찾아서",
        priceWon = 59000,
        isSponsored = true,
        imageUrl = coast,
    )
    private val blanket = Product(
        id = "p-blanket",
        name = "방수 피크닉 담요",
        reason = "새벽 바닷바람이 생각보다 차가워요",
        recommender = "윤슬을따라",
        priceWon = 27000,
        isSponsored = false,
        imageUrl = road,
    )
    private val starMap = Product(
        id = "p-starmap",
        name = "적색광 헤드랜턴",
        reason = "눈이 어둠에 적응한 상태를 지켜줘요",
        recommender = "밤하늘수집가",
        priceWon = 18000,
        isSponsored = false,
        imageUrl = night,
    )

    val all: List<Spot> = listOf(
        Spot(
            id = "s-gangneung",
            title = "강릉의 조용한 노을 명소",
            region = "강원 강릉시",
            address = "강원특별자치도 강릉시 사천면",
            location = LatLng(37.8395, 128.8770),
            category = Category.SEA_SUNSET,
            author = romance,
            story = "우연히 들른 곳인데, 해가 지는 순간 하늘이 정말 아름다웠어요.",
            photos = listOf(coast, sunset, road),
            likeCount = 324,
            saveCount = 152,
            postedDaysAgo = 2,
            tips = listOf(
                HoneyTip("t-gn-parking", TipKind.PARKING, "공간이 협소해요", "차 5~6대가 들어가면 꽉 차요. 주말 오후에는 일찍 도착하세요.", TipStatus.PREPARE, 41, 3, "낭만을찾아서"),
                HoneyTip("t-gn-toilet", TipKind.TOILET, "확인이 필요해요", "주변에 화장실이 없다는 제보와 임시 화장실이 생겼다는 제보가 함께 있어요.", TipStatus.NEEDS_CHECK, 28, 3, "방문자 2명"),
                HoneyTip("t-gn-signal", TipKind.SIGNAL, "일부 구간 약해요", "해변 끝 쪽에서는 데이터가 자주 끊겨요. 지도를 미리 받아 두세요.", TipStatus.PREPARE, 17, 6, "주말의기록"),
                HoneyTip("t-gn-night", TipKind.NIGHT, "조명이 부족해요", "해가 지면 주차 공간이 거의 보이지 않아요. 랜턴을 챙기세요.", TipStatus.PREPARE, 22, 3, "낭만을찾아서"),
                HoneyTip("t-gn-access", TipKind.ACCESS, "비 오면 미끄러워요", "비 오는 날에는 마지막 진입로가 미끄러울 수 있어요.", TipStatus.PREPARE, 28, 5, "오후다섯시"),
                HoneyTip("t-gn-store", TipKind.STORE, "차로 10분 거리", "가장 가까운 편의점은 사천면 시내에 있어요.", TipStatus.GOOD, 9, 12, "낭만을찾아서"),
                HoneyTip("t-gn-power", TipKind.POWER, "외부 전원 없음", "전기를 쓸 곳이 없어요. 보조 배터리를 충분히 챙기세요.", TipStatus.PREPARE, 11, 12, "낭만을찾아서"),
            ),
            products = listOf(lantern, chair, table, blanket),
            comments = listOf(
                Comment(
                    "c-gn-1", "밤하늘수집가", "승용차도 마지막 진입로까지 들어갈 수 있나요?", "2시간 전", isQuestion = true,
                    replies = listOf(Comment("c-gn-1r", "낭만을찾아서", "네, 들어갈 수 있어요. 다만 비 온 다음 날에는 조심하세요.", "1시간 전")),
                ),
                Comment("c-gn-2", "주말의기록", "덕분에 지난 주말에 다녀왔어요. 해 지기 30분 전이 제일 좋았어요.", "어제"),
                Comment(
                    "c-gn-3", "오후다섯시", "밤에 사람이 많은 편인가요?", "3일 전", isQuestion = true,
                    replies = listOf(Comment("c-gn-3r", "윤슬을따라", "평일 밤에는 두세 팀 정도였어요.", "3일 전")),
                ),
            ),
        ),
        Spot(
            id = "s-jecheon",
            title = "별이 쏟아지는 호숫가",
            region = "충북 제천시",
            address = "충청북도 제천시 청풍면",
            location = LatLng(36.9930, 128.1640),
            category = Category.STARS,
            author = nightSky,
            story = "달이 없는 밤에 가면 호수에 별이 비쳐요. 새벽 두 시쯤이 가장 선명했어요.",
            photos = listOf(night, lake),
            likeCount = 218,
            saveCount = 97,
            postedDaysAgo = 5,
            tips = listOf(
                HoneyTip("t-jc-night", TipKind.NIGHT, "주변이 아주 어두워요", "별 보기에는 좋지만 발밑이 보이지 않아요. 적색광 랜턴을 추천해요.", TipStatus.PREPARE, 33, 4, "밤하늘수집가"),
                HoneyTip("t-jc-toilet", TipKind.TOILET, "전망대에 있어요", "호숫가 전망대 화장실은 밤에도 열려 있었어요.", TipStatus.GOOD, 19, 4, "밤하늘수집가"),
                HoneyTip("t-jc-signal", TipKind.SIGNAL, "잘 터져요", "통화와 데이터 모두 문제없었어요.", TipStatus.GOOD, 6, 10, "주말의기록"),
                HoneyTip("t-jc-caution", TipKind.CAUTION, "밤에 차량 통행", "호숫가 도로로 밤에도 차가 지나가요. 비상등을 켜 두세요.", TipStatus.PREPARE, 14, 4, "밤하늘수집가"),
                HoneyTip("t-jc-store", TipKind.STORE, "미리 사 오세요", "근처 가게는 저녁 8시면 문을 닫아요.", TipStatus.PREPARE, 12, 8, "오후다섯시"),
            ),
            products = listOf(starMap, lantern),
            comments = listOf(
                Comment("c-jc-1", "낭만을찾아서", "망원경 없이도 은하수가 보였나요?", "어제", isQuestion = true,
                    replies = listOf(Comment("c-jc-1r", "밤하늘수집가", "8월 그믐 즈음에는 맨눈으로도 보였어요.", "어제"))),
            ),
        ),
        Spot(
            id = "s-yangpyeong",
            title = "물안개 머무는 아침",
            region = "경기 양평군",
            address = "경기도 양평군 양서면",
            location = LatLng(37.5345, 127.3155),
            category = Category.LAKE_RIVER,
            author = weekend,
            story = "해 뜨기 전 강 위로 물안개가 천천히 올라와요. 서울에서 가까워 새벽에 다녀오기 좋아요.",
            photos = listOf(lake, village),
            likeCount = 186,
            saveCount = 120,
            postedDaysAgo = 1,
            tips = listOf(
                HoneyTip("t-yp-parking", TipKind.PARKING, "공영주차장 이용", "강변 공영주차장을 이용했어요. 이른 새벽에는 여유가 있어요.", TipStatus.GOOD, 21, 2, "주말의기록"),
                HoneyTip("t-yp-toilet", TipKind.TOILET, "주차장 옆에 있어요", "주차장 옆 공중화장실을 쓸 수 있었어요.", TipStatus.GOOD, 15, 2, "주말의기록"),
                HoneyTip("t-yp-caution", TipKind.CAUTION, "주말엔 붐벼요", "해 뜬 뒤에는 사진 찍는 사람이 많아져요.", TipStatus.PREPARE, 18, 2, "윤슬을따라"),
                HoneyTip("t-yp-night", TipKind.NIGHT, "새벽엔 추워요", "강가라 봄가을에도 새벽 기온이 많이 내려가요.", TipStatus.PREPARE, 10, 9, "오후다섯시"),
            ),
            products = listOf(blanket),
            comments = listOf(
                Comment("c-yp-1", "윤슬을따라", "몇 시쯤 가야 물안개를 볼 수 있나요?", "4일 전", isQuestion = true,
                    replies = listOf(Comment("c-yp-1r", "주말의기록", "일출 30분 전쯤이 가장 짙었어요.", "4일 전"))),
            ),
        ),
        Spot(
            id = "s-goseong",
            title = "소나무 숲의 작은 쉼표",
            region = "강원 고성군",
            address = "강원특별자치도 고성군 죽왕면",
            location = LatLng(38.3020, 128.5240),
            category = Category.FOREST,
            author = fivePm,
            story = "바다 바로 뒤 소나무 숲이라 바람 소리와 파도 소리가 같이 들려요.",
            photos = listOf(tent, road),
            likeCount = 142,
            saveCount = 64,
            postedDaysAgo = 9,
            tips = listOf(
                HoneyTip("t-gs-parking", TipKind.PARKING, "숲길 입구에 주차", "숲 안까지는 차가 들어가지 않아요.", TipStatus.PREPARE, 13, 7, "오후다섯시"),
                HoneyTip("t-gs-toilet", TipKind.TOILET, "해변 쪽에 있어요", "해변 공중화장실까지 걸어서 5분이에요.", TipStatus.GOOD, 8, 7, "오후다섯시"),
                HoneyTip("t-gs-power", TipKind.POWER, "외부 전원 없음", "전기를 쓸 곳이 없어요.", TipStatus.PREPARE, 5, 7, "오후다섯시"),
                HoneyTip("t-gs-caution", TipKind.CAUTION, "화기 사용 주의", "솔잎이 많아 불을 쓰면 위험해요. 현장 안내문을 꼭 확인하세요.", TipStatus.PREPARE, 24, 7, "낭만을찾아서"),
                HoneyTip("t-gs-signal", TipKind.SIGNAL, "잘 터져요", "숲 안에서도 데이터가 잘 됐어요.", TipStatus.GOOD, 3, 15, "주말의기록"),
                HoneyTip("t-gs-store", TipKind.STORE, "걸어서 갈 수 있어요", "해변 앞 편의점이 늦게까지 열어요.", TipStatus.GOOD, 7, 15, "오후다섯시"),
            ),
            products = listOf(chair, lantern),
            comments = emptyList(),
        ),
        Spot(
            id = "s-taean",
            title = "서해 끝에서 보는 마지막 빛",
            region = "충남 태안군",
            address = "충청남도 태안군 안면읍",
            location = LatLng(36.4970, 126.3350),
            category = Category.SEA_SUNSET,
            author = yunseul,
            story = "바위 사이로 해가 떨어지는 순간이 정말 짧아요. 놓치지 않게 일찍 자리 잡으세요.",
            photos = listOf(sunset, coast),
            likeCount = 402,
            saveCount = 233,
            postedDaysAgo = 4,
            tips = listOf(
                HoneyTip("t-ta-parking", TipKind.PARKING, "노을 시간엔 만차", "일몰 1시간 전에는 주차장이 거의 차요.", TipStatus.PREPARE, 52, 1, "윤슬을따라"),
                HoneyTip("t-ta-toilet", TipKind.TOILET, "해변 입구에 있어요", "해변 입구 화장실을 이용할 수 있어요.", TipStatus.GOOD, 20, 1, "윤슬을따라"),
                HoneyTip("t-ta-caution", TipKind.CAUTION, "물때를 확인하세요", "밀물 때 바위 쪽으로 들어가면 고립될 수 있어요.", TipStatus.PREPARE, 47, 1, "주말의기록"),
            ),
            products = listOf(blanket, chair),
            comments = listOf(
                Comment("c-ta-1", "오후다섯시", "사진 정말 예뻐요. 다음 주에 가 볼게요!", "2일 전"),
            ),
        ),
        Spot(
            id = "s-yeongwol",
            title = "구름 위 언덕의 별밤",
            region = "강원 영월군",
            address = "강원특별자치도 영월군 영월읍",
            location = LatLng(37.1980, 128.4870),
            category = Category.STARS,
            author = nightSky,
            story = "언덕 위로 올라가면 도시 불빛이 거의 없어요. 별자리 앱 없이도 북두칠성이 또렷해요.",
            photos = listOf(night, road),
            likeCount = 175,
            saveCount = 88,
            postedDaysAgo = 12,
            tips = listOf(
                HoneyTip("t-yw-access", TipKind.ACCESS, "경사가 급해요", "언덕길이 가파르고 굽어요. 밤에는 천천히 올라가세요.", TipStatus.PREPARE, 29, 6, "밤하늘수집가"),
                HoneyTip("t-yw-night", TipKind.NIGHT, "가로등이 없어요", "주차장 외에는 조명이 없어요.", TipStatus.PREPARE, 15, 6, "밤하늘수집가"),
                HoneyTip("t-yw-signal", TipKind.SIGNAL, "확인이 필요해요", "통신이 잘 된다는 제보와 끊긴다는 제보가 엇갈려요.", TipStatus.NEEDS_CHECK, 7, 6, "방문자 3명"),
            ),
            products = listOf(starMap),
            comments = emptyList(),
        ),
        Spot(
            id = "s-danyang",
            title = "강을 따라 달리는 아침 드라이브",
            region = "충북 단양군",
            address = "충청북도 단양군 단양읍",
            location = LatLng(36.9840, 128.3650),
            category = Category.DRIVE,
            author = weekend,
            story = "남한강을 따라 굽이진 길이 이어져요. 창문을 열고 천천히 달리기 좋은 길이에요.",
            photos = listOf(road, village),
            likeCount = 133,
            saveCount = 71,
            postedDaysAgo = 7,
            tips = listOf(
                HoneyTip("t-dy-caution", TipKind.CAUTION, "자전거가 많아요", "주말 오전에는 자전거 동호회가 많이 지나가요.", TipStatus.PREPARE, 16, 5, "주말의기록"),
                HoneyTip("t-dy-parking", TipKind.PARKING, "전망 쉼터에 주차", "중간 전망 쉼터에 잠시 세울 수 있어요.", TipStatus.GOOD, 11, 5, "주말의기록"),
            ),
            products = emptyList(),
            comments = emptyList(),
        ),
        Spot(
            id = "s-gyeongju",
            title = "저수지 옆 고요한 한옥 마을",
            region = "경북 경주시",
            address = "경상북도 경주시 양남면",
            location = LatLng(35.7860, 129.2240),
            category = Category.QUIET,
            author = fivePm,
            story = "관광지에서 조금 떨어져 있어 조용해요. 오후 다섯 시쯤 지붕 위로 빛이 길게 내려와요.",
            photos = listOf(village, lake),
            likeCount = 98,
            saveCount = 45,
            postedDaysAgo = 15,
            tips = listOf(
                HoneyTip("t-gj-caution", TipKind.CAUTION, "주민 거주 지역", "마을 안쪽은 주민이 사는 곳이에요. 조용히 둘러봐 주세요.", TipStatus.PREPARE, 30, 10, "오후다섯시"),
                HoneyTip("t-gj-parking", TipKind.PARKING, "마을 입구 주차", "마을 입구 공터에 주차했어요.", TipStatus.GOOD, 8, 10, "오후다섯시"),
                HoneyTip("t-gj-toilet", TipKind.TOILET, "주변에 없어요", "마을 안에는 공중화장실이 없어요.", TipStatus.PREPARE, 6, 10, "오후다섯시"),
            ),
            products = emptyList(),
            comments = emptyList(),
        ),
        Spot(
            id = "s-hadong",
            title = "섬진강 모래톱의 오후",
            region = "경남 하동군",
            address = "경상남도 하동군 화개면",
            location = LatLng(35.1920, 127.6260),
            category = Category.LAKE_RIVER,
            author = romance,
            story = "넓은 모래톱 옆으로 강물이 천천히 흘러요. 그늘에 의자 하나 두고 오후 내내 쉬었어요.",
            photos = listOf(lake, tent),
            likeCount = 121,
            saveCount = 59,
            postedDaysAgo = 20,
            tips = listOf(
                HoneyTip("t-hd-toilet", TipKind.TOILET, "강변 쉼터에 있어요", "강변 쉼터 화장실을 쓸 수 있었어요.", TipStatus.GOOD, 9, 14, "낭만을찾아서"),
                HoneyTip("t-hd-caution", TipKind.CAUTION, "비 온 뒤 수위 주의", "비가 많이 온 뒤에는 모래톱까지 물이 차요.", TipStatus.PREPARE, 18, 14, "낭만을찾아서"),
            ),
            products = listOf(chair),
            comments = emptyList(),
        ),
    )

    val authors: List<Author> = all.map { it.author }.distinctBy { it.id }

    fun byId(id: String): Spot? = all.firstOrNull { it.id == id }
}
