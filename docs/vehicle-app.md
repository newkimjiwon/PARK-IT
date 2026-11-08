# 차량용 앱 — 낭만로드

[Figma 프로토타입](figma-prototype-review.md)의 차량 디스플레이 화면을 Pleos Connect용 Android Automotive 앱으로 옮긴 것입니다. 코드는 `vehicle/` 모듈에 있고, 데이터 모델과 사진 지도는 [스마트폰 앱](phone-app.md)과 함께 쓰는 `core/` 모듈에 있습니다. 계정·서버 동기화는 아직 없습니다.

> 확인 범위: 2026-11-05 Apple Silicon Mac의 Pleos Connect Emulator 2.1.0(2560×1440, 앱 창 2:1)에서 빌드·설치·실행했습니다. 지도, 장소 상세, 앨범 화면을 확인했습니다. NaviHelper 길 안내와 Virtual Console의 P↔D 전환에 따른 주행 화면도 직접 확인했습니다. 세로 창과 1:1 창 배치, 실제 차량은 확인하지 않았습니다. 같은 날 공용 코드를 `core/`로 옮긴 뒤에는 빌드만 확인했고, Pleos 에뮬레이터에서 다시 실행하지는 않았습니다.

## 빌드와 실행

먼저 [개발 환경 설치 안내](development-setup.md)를 따라 JDK 17, Android SDK, Pleos 에뮬레이터를 준비합니다. 그다음 저장소 루트에서 실행합니다.

macOS:

```zsh
sh gradlew :vehicle:assembleDebug :vehicle:testDebugUnitTest
adb install -r -g vehicle/build/outputs/apk/debug/vehicle-debug.apk
adb shell am start -n com.nangmanroad.vehicle/.MainActivity
```

Windows PowerShell:

```powershell
.\gradlew.bat :vehicle:assembleDebug :vehicle:testDebugUnitTest
adb install -r -g vehicle\build\outputs\apk\debug\vehicle-debug.apk
adb shell am start -n com.nangmanroad.vehicle/.MainActivity
```

Android Studio에서는 저장소 폴더를 열고 실행 구성 `vehicle`을 선택한 뒤 Pleos 가상 기기에서 실행합니다. 사진은 Unsplash 주소에서 불러오므로 에뮬레이터가 인터넷에 연결되어 있어야 합니다. 연결되지 않으면 사진 자리에 회색 대체 이미지가 표시됩니다.

## 화면과 기능

| 화면 | 동작 |
| --- | --- |
| 지도 | 장소 사진을 마커로 표시합니다. 가까운 마커는 개수가 적힌 묶음으로 합쳐지고, 묶음을 누르면 확대됩니다. 두 손가락 확대·축소와 이동, 두 번 탭 확대, 확대·축소·선택 위치 버튼을 지원합니다. |
| 검색·필터 | 장소명, 지역, 작성자, 분류로 검색합니다. 분류 7개(전체 포함)로 지도와 피드를 함께 거릅니다. |
| 미리보기 카드 | 선택한 장소의 사진, 분류, 거리, 좋아요, 꿀팁 수를 보여줍니다. 길 안내, 저장, 자세히 버튼이 있습니다. |
| 지역 피드 | 지도에 보이는 장소만 오른쪽 피드에 표시합니다. 인기·최신·팔로잉 정렬을 지원합니다. 창이 좁으면(1100dp 미만) 피드를 숨깁니다. |
| 장소 상세 | 사진을 좌우로 넘겨 봅니다. 팔로우, 좋아요, 저장, 휴대폰으로 보내기, 길 안내를 지원합니다. 탭은 방문 꿀팁, 준비물, 댓글 세 가지입니다. |
| 방문 꿀팁 | '확인 필요', '준비하세요', '괜찮아요' 순으로 정렬합니다. 카드를 누르면 상세 설명과 제보자, 확인 날짜가 펼쳐집니다. '도움이 됐어요'를 누를 수 있고, 차박·주차 허용 여부는 현장에서 확인하라는 안내를 표시합니다. |
| 아이템 | 추천자, 추천 이유, 가격(없으면 '가격 정보 없음'), 광고·제휴 표시가 있습니다. 구매는 '휴대폰에서 보기'로 넘깁니다. |
| 댓글 | 질문 표시와 답글을 읽기 전용으로 보여줍니다. 작성은 휴대폰으로 넘깁니다. |
| 앨범 | 추천, 팔로잉, 최신, 저장한 장소, 휴대폰에서 보냄 탭이 있는 사진 그리드입니다. 사이드 레일의 '저장'은 저장한 장소 탭을 엽니다. |
| 주행 화면 | 기어가 P가 아니면 사진과 피드를 모두 숨깁니다. 목적지, 남은 거리·시간, 도착 전 꿀팁 최대 3개만 크게 보여줍니다. 사이드 레일의 '주행 화면'으로 정차 중에 미리 볼 수 있습니다. |
| 파킷 | 주행하다 기어를 P로 놓으면 지도 위에 "여기, 파킷할까요?" 배너가 뜹니다. '파킷하기'를 누르면 내비게이션이 알려준 현재 위치와 시각이 휴대폰 알림으로 가고, 휴대폰에서 그 위치가 미리 찍힌 장소 공유가 시작됩니다. 알려진 장소에서 300m 안에 세웠거나 길 안내한 곳에 도착하면 "'장소'에 왔어요"로 바뀌고, 파킷하면 방문 완료로 표시한 뒤 휴대폰에서 꿀팁 확인을 요청합니다. 사이드 레일의 '파킷'으로 아무 때나 띄울 수 있습니다. 내비게이션이 연결되지 않아 위치를 모르면 띄우지 않습니다. |

저장, 좋아요, 팔로우, '도움이 됐어요', 휴대폰으로 보낼 항목은 기기 안(`SharedPreferences`)에 저장되어 앱을 다시 열어도 유지됩니다.

## Pleos SDK 연동

| 기능 | SDK | 권한 | 코드 |
| --- | --- | --- | --- |
| 주행·정차 판단 | Vehicle 2.3.2 `Gear.registerGearSelection` | `pleos.car.permission.CAR_DRIVE` | `pleos/DrivingStateMonitor.kt` |
| 길 안내 | NaviHelper 3.0.5 `requestRoute` | `pleos.car.permission.NAVI_ROUTE_SEARCH` | `pleos/NavigationBridge.kt` |
| 현재 위치, 남은 거리·시간 | NaviHelper `getCurrentLocationInfo`, `getDestinationInfo` | `pleos.car.permission.NAVI_ROUTE` | `pleos/NavigationBridge.kt` |

두 SDK 모두 Pleos Playground에서 별도 신청 없이 사용할 수 있는 상태입니다(2026-11-05 [SDK 사용 안내](https://document.pleos.ai/docs/pleos-only/create-my-project) 기준). 연결되지 않으면 앱은 계속 동작합니다. 이때 거리는 서울시청 기준 직선거리로 표시하고, 길 안내를 누르면 "차량 내비게이션에 연결할 수 없어요"라고 알립니다.

에뮬레이터에서 기어를 D로 바꾸면 Pleos 시스템이 일반 앱 화면을 "주행 중에는 사용할 수 없습니다"로 가립니다. 이 앱은 Activity에 `distractionOptimized` 메타데이터를 선언해 자체 주행 화면을 보여줍니다. 에뮬레이터에서는 이 선언이 적용되는 것을 확인했지만, 앱 심사에서 허용되는지는 확인하지 않았습니다. 제출 전에 [앱 심사 가이드](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide)를 확인해야 합니다.

### Virtual Console로 주행 상태 바꾸기

[Virtual Console](development-setup.md#5-virtual-console-확인)을 브라우저에서 열고 기어 버튼으로 P와 D를 바꿉니다. 브라우저 없이 터미널에서 바꿀 수도 있습니다. 콘솔은 클라이언트 세션이 살아 있을 때만 명령을 받으므로, 세션 요청을 먼저 보냅니다.

```zsh
curl -s -X POST "http://127.0.0.1:8088/browser/session?client=cli"
curl -s -X PUT "http://127.0.0.1:8088/vehicle/gear?gear=Drive"
curl -s -X PUT "http://127.0.0.1:8088/vehicle/gear?gear=Park"
```

세션은 약 3초 동안 요청이 없으면 끊깁니다. 이 엔드포인트는 콘솔 페이지의 스크립트에서 확인한 것으로, 공식 문서에 있는 API는 아닙니다.

## 코드 구조

```
shared/src/main/kotlin/com/nangmanroad/core/   서버·차량·휴대폰 공용 (순수 Kotlin)
├── data/    모델, 샘플 장소 9곳, 목록·추천 로직, 꿀팁 충돌 표시
└── sync/    동기화 API 계약

core/src/main/java/com/nangmanroad/core/        차량·휴대폰 앱 공용 (Android)
├── data/    기기 안 저장소(로그인하지 않았을 때)
├── sync/    서버 클라이언트, 로그인 세션, 서버/기기 저장소를 고르는 SpotRepository
├── map/     사진 지도(투영, 묶음 표시, 기본 지도 그리기)
└── ui/      색상 토큰, 사진·아바타, 꿀팁 아이콘·색

vehicle/src/main/java/com/nangmanroad/vehicle/
├── MainActivity.kt, AppViewModel.kt   화면 상태, 사용자 동작, SDK·서버 연결
├── pleos/                             Vehicle SDK·NaviHelper 래퍼
└── ui/                                지도 홈, 상세, 앨범, 계정 연결, 주행 화면, 차량용 컴포넌트, 테마
```

- 지도는 NAVER Maps SDK(`naver-map-compose`)를 야간 모드로 띄우고, 사진 마커·현재 위치·확대 버튼은 지도의 투영을 이용해 우리 컴포저블로 그 위에 올립니다(`core/map/NaverPhotoMap.kt`). 키 설정은 [네이버 지도 키](development-setup.md#네이버-지도-키)를 참고합니다. 키가 없으면 같은 인터페이스의 Canvas 지도(`core/map/PhotoMap.kt`)로 자동 대체되므로 호출하는 화면은 둘을 구분하지 않습니다.
- 파킷을 보내면 지도 위 현재 위치에 코랄 핀이 떨어지고 링이 퍼집니다(`HereMarker`의 `pulse`). 이름을 동작으로 보여 주는 장면이라 시연 영상에서 그대로 씁니다.
- 추천은 좋아요·저장한 풍경 분류, 팔로우한 작성자, 인기, 최신성, 거리를 더한 규칙 기반 점수입니다(`SpotQueries.recommended`).
- 색과 글꼴은 두 앱이 함께 쓰도록 `core`의 `ui/Palette.kt`, `ui/Fonts.kt`에 모았습니다. 차량용 글자 크기는 Pleos 최소 글자 크기(18px)를 기준으로 `vehicle`의 `ui/theme/Theme.kt`에 정했습니다.
- 화면은 Pleos 시스템 UI에 맞춰 차분하게 둡니다. 배경과 글자는 무채색이고, 코랄은 화면의 주요 동작(차량에서는 '길 안내')과 지도에서 고른 장소에만 씁니다. 선택한 탭과 필터는 흰 글자와 밑줄로, 저장·좋아요 같은 상태는 채워진 아이콘으로 표시합니다. 꿀팁·준비물·댓글은 카드 대신 구분선이 있는 목록입니다. 영어 소제목은 쓰지 않습니다.
- 글꼴은 Pretendard 1.3.9(Regular·Medium·SemiBold·Bold)를 `core/src/main/res/font/`에 넣었습니다. SIL Open Font License 1.1이며 전문은 `core/licenses/Pretendard-OFL.txt`에 있습니다. 거리, 개수, 시간은 고정폭 숫자(`tabular()`)로 표시합니다.

## 아직 없는 것

- 실제 인증과 운영 서버. 휴대폰 계정 연결, 저장 동기화, '휴대폰으로 보내기'는 [동기화 서버](sync-server.md)로 동작하지만 개발용입니다. 계정을 연결하지 않으면 예전처럼 기기 안에만 저장합니다.
- 실제 지도 타일과 위치 검색
- 음성 안내. TextToSpeech, SpeechToText SDK는 2026-11-05 기준 '준비 중' 상태입니다.
- 샘플 사진은 Unsplash 이미지를 장소와 무관하게 배정한 것입니다. 예를 들어 단양 게시물에 김포공항 표지판 사진이 나옵니다.
