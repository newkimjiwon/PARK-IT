# Pleos Connect 개발 환경 설치 및 샘플 실행

이 문서는 macOS와 Windows 팀원이 이 저장소의 앱과 Pleos Connect Vehicle SDK 샘플을 빌드하고 에뮬레이터에서 실행하기 위한 안내입니다. 메뉴 이름은 Android Studio 버전에 따라 조금 다를 수 있습니다.

## 준비물과 기준 버전

- macOS 또는 Windows PC, 인터넷 연결, Git, 충분한 저장 공간. Pleos 시스템 이미지는 다운로드만 약 4.7GB이고 압축을 풀면 약 20GB입니다. Android SDK·IDE·가상 기기·빌드 캐시를 위한 공간도 추가로 필요합니다. 개발한 Mac에서는 전체 약 34GB를 사용했습니다.
- [Android Studio 최신 안정판](https://developer.android.com/studio)과 [JDK 17](https://adoptium.net/temurin/releases/?version=17). Mac은 자신의 CPU에 맞는 설치 파일을 선택합니다.
- Android SDK Platform 34와 35, Android SDK Build-Tools 35.0.0, Android SDK Platform-Tools, Android Emulator.
- 휴대폰 앱을 실행하려면 Android 15 / API 35 Google APIs 시스템 이미지가 추가로 필요합니다. Apple Silicon은 arm64-v8a, Windows는 x86_64를 고릅니다. 압축 파일만 약 1.8GB입니다.
- Pleos Connect Emulator 2.1.0: Android 14 / API 34. 앱을 만들 때는 **minSdk 28 이상**을 설정합니다.
- 공식 샘플과 이 저장소의 앱 모두 AGP 8.8.2, Kotlin 2.0.0, Gradle Wrapper 8.10.2를 씁니다. 별도로 Gradle을 설치할 필요는 없습니다. AGP 8.8의 최소 Gradle 버전은 8.10.2입니다.

공모전에서는 Gradle 8.0 이상을 안내하지만 실제 버전은 사용하는 AGP의 요구사항도 충족해야 합니다. [Pleos 환경 요구사항](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator) · [AGP 8.8 호환성](https://developer.android.com/build/releases/agp-8-8-0-release-notes)

> 확인 범위: 2026-10-29 Apple Silicon Mac에서 CLI 빌드, 화면 없는 에뮬레이터 부팅과 샘플 실행을 확인했습니다. 2026-11-05에는 같은 Mac에서 휴대폰 에뮬레이터(6단계)와 두 에뮬레이터 동시 실행(7단계)을 확인했습니다. Windows와 Intel Mac에서는 이 저장소의 절차를 아직 직접 검증하지 않았습니다. Pleos의 Mac 이미지 주소는 ARM64이므로 Intel Mac 지원은 공식 문서에서 따로 확인해 주세요.

## 1. Android Studio와 JDK 설치

1. 위 링크에서 Android Studio와 JDK 17을 내려받아 각 PC에 설치합니다. 약관이 표시되면 내용을 확인하고 동의합니다.
2. Android Studio를 처음 열어 설치 마법사를 끝까지 따라갑니다. Android SDK 설치 위치를 기억해 둡니다. 외장 드라이브에 저장하고 싶다면 **처음부터 SDK 위치를 외장 드라이브로 선택**합니다.
3. Android Studio의 **Settings / Preferences → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**에서 설치한 JDK 17의 홈 디렉터리를 선택합니다. 프로젝트를 연 뒤 설정이 보이면 그때 지정합니다.
4. 터미널에서도 JDK 17을 사용할 경우 `JAVA_HOME`을 JDK 17 홈으로 설정합니다. Android Studio의 Gradle JDK와 터미널의 `JAVA_HOME`은 별도 설정입니다.

macOS 터미널에서 시스템에 등록된 JDK 17을 선택하는 예시:

```zsh
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
"$JAVA_HOME/bin/java" -version
```

위 명령은 시스템에 등록된 JDK에만 적용됩니다. 외장 SSD에 압축을 풀어 둔 JDK처럼 등록되지 않은 경우에는 직접 경로를 지정합니다.

Windows PowerShell 예시 — 경로는 본인이 설치한 실제 JDK 폴더로 바꿉니다:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17'
& "$env:JAVA_HOME\bin\java.exe" -version
```

이 명령의 환경 변수는 현재 터미널에만 적용됩니다. 새 터미널을 열면 다시 설정하거나 Windows의 사용자 환경 변수에서 영구 설정합니다. 환경 변수를 영구 변경한 경우 Android Studio를 종료한 뒤 다시 열어야 적용됩니다.

## 2. Android SDK 설치

Android Studio의 **Tools → SDK Manager**에서 SDK 위치를 확인하고 다음을 설치합니다.

- **SDK Platforms:** Android API 34, Android API 35
- **SDK Tools:** Android SDK Build-Tools 35.0.0, Android SDK Platform-Tools, Android Emulator
- 버전 선택이 보이지 않으면 **Show Package Details**를 켭니다.

Android API 35는 이 문서에서 기준으로 삼은 샘플을 컴파일하는 데 사용하고, API 34는 Pleos 전용 에뮬레이터 이미지에 사용합니다. 각 SDK 라이선스는 설치 과정에서 확인하고 동의합니다.

## 3. Pleos Connect 에뮬레이터 설치

1. **Tools → SDK Manager → SDK Update Sites**에서 `+`를 누릅니다.
2. 이름에 `Pleos Connect System Image`를 입력하고 운영체제에 맞는 주소를 등록합니다.

   | 운영체제 | SDK Update Site URL |
   | --- | --- |
   | macOS (ARM64) | `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml` |
   | Windows / Linux | `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml` |

3. **SDK Platforms**에서 **Show Package Details**를 켜고 Android API 34 아래의 **Pleos Connect v2.1.0 system image**를 선택해 설치합니다. Pleos SDK 약관을 확인하고 동의합니다.
4. Android Studio를 재시작합니다.
5. **Tools → Device Manager → Create Device**에서 Automotive → **Pleos Connect v2.1.0**을 선택합니다. 시스템 이미지는 방금 설치한 API 34 Pleos 이미지를 선택합니다.
6. 고급 설정에서 RAM을 **4GB**, 부팅을 **Cold boot**로 설정하고 가상 기기를 생성합니다. Cold boot는 Pleos 차량 기능을 사용하는 데 필요합니다.
7. Device Manager에서 기기를 실행하고 부팅이 완료될 때까지 기다립니다. Windows에서 에뮬레이터가 시작하지 않으면 가상화 지원과 Android Emulator 시스템 요구사항을 확인합니다.

정확한 화면과 옵션은 [Pleos 공식 설치 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)를 따릅니다. 일반 휴대전화 이미지에는 Pleos Vehicle Service가 없으므로 차량 SDK 검증에 전용 이미지가 필요합니다.

## 4. 공식 샘플 빌드와 실행

샘플은 **이 저장소 바깥의 별도 폴더**에 복제합니다. 예를 들어 공모전 저장소와 같은 상위 폴더에서 다음을 실행합니다. 이미 같은 이름의 폴더가 있다면 다른 이름을 선택하세요.

```sh
git clone https://github.com/Pleos-Playground/pleos-sample-vehicle-app.git
cd pleos-sample-vehicle-app
git switch --detach 959981d960ec71144accb27af5ae5d341e72a644
```

이 커밋은 검증 당시 샘플 상태를 재현하기 위한 기준입니다. 샘플에서 실험한 변경 사항은 이 저장소에 자동으로 포함되지 않습니다.

**SDK 버전 수정:** 위 커밋의 `gradle/libs.versions.toml`에서 `vehicleSdk = "2.3.1"`을 `vehicleSdk = "2.3.2"`로 바꿉니다. 2026-10-29 확인 당시 공식 Pleos Maven 저장소에서 2.3.1은 404였고 2.3.2는 제공되어, 이 수정으로 Mac에서 APK 빌드가 성공했습니다. 이후 버전이나 접근 상태가 바뀔 수 있으므로 최신 샘플을 사용할 때는 현재 샘플과 공식 Maven 저장소를 다시 확인하세요.

Android Studio에서 샘플 폴더를 열고 Gradle Sync를 마친 다음, 실행 대상으로 Pleos 가상 기기를 선택하고 **Run**을 누릅니다. IDE에서 SDK 경로 또는 Gradle JDK를 묻는 경우 1~2단계의 경로를 지정합니다.

터미널에서도 빌드할 수 있습니다. 먼저 그 터미널의 `JAVA_HOME`이 JDK 17인지 확인하세요. SDK를 찾지 못하면 Android Studio에서 샘플을 열어 SDK 경로를 지정하거나 `ANDROID_HOME`을 실제 SDK 위치로 설정합니다.

macOS:

```zsh
sh gradlew --version
sh gradlew assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat --version
.\gradlew.bat assembleDebug
```

빌드에 성공하면 `app/build/outputs/apk/debug/app-debug.apk`가 생성됩니다. `--version`에서 사용하는 JVM이 17인지 확인합니다. Windows의 `gradlew.bat`은 PowerShell에서 실행하고, Gradle Wrapper는 필요한 Gradle을 처음 실행할 때 내려받습니다.

참고로 이 샘플의 minSdk는 23이지만, **새로 만드는 Pleos 공모전 앱에는 공식 요구사항인 28 이상**을 사용합니다.

## 5. Virtual Console 확인

Pleos 기기가 부팅된 상태에서 터미널을 열고, SDK의 `platform-tools`에 있는 `adb`를 사용합니다. `adb`가 PATH에 없으면 다음 예시의 `ANDROID_HOME`을 2단계에서 확인한 SDK 경로로 먼저 설정하세요.

macOS:

```zsh
export ANDROID_HOME="/실제/Android/sdk/경로"
"$ANDROID_HOME/platform-tools/adb" devices
"$ANDROID_HOME/platform-tools/adb" -s emulator-5554 forward tcp:8088 tcp:8088
```

Windows PowerShell:

```powershell
$env:ANDROID_HOME = 'C:\실제\Android\sdk\경로'
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices
& "$env:ANDROID_HOME\platform-tools\adb.exe" -s emulator-5554 forward tcp:8088 tcp:8088
```

`emulator-5554`는 예시입니다. `adb devices`에 표시된 Pleos 에뮬레이터의 식별자로 바꿉니다. 이후 브라우저에서 [http://127.0.0.1:8088/](http://127.0.0.1:8088/)을 열면 차량 상태를 시뮬레이션할 수 있습니다. 포트 충돌이나 연결 문제가 있으면 [공식 Virtual Console 안내](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator)를 확인합니다.

## 네이버 지도 키

앱은 먼저 서버의 NAVER 지도 Client ID를 조회합니다. 서버에 유효한 설정이 없으면 빌드할 때 `local.properties`에서 읽은 ID를 사용하고, 둘 다 없으면 단순화된 Canvas 지도를 표시합니다. 서버 응답을 기다리는 동안에도 Canvas 지도가 표시됩니다. `local.properties`는 개발 PC의 파일로, 앱 실행 시 직접 읽지 않고 빌드할 때 앱에 포함됩니다.

1. [네이버 클라우드 플랫폼](https://www.ncloud.com)에 가입하고 콘솔에서 **Services → Application Services → Maps**로 들어갑니다.
2. **Application 등록**에서 이름을 정하고, API 선택에서 **Dynamic Map**을 켭니다. 이 항목이 없으면 지도가 429 오류로 열리지 않습니다.
3. Android 앱 패키지 이름으로 `com.nangmanroad.vehicle`과 `com.nangmanroad.phone`을 모두 등록합니다. 패키지 이름이 틀리면 401 오류가 납니다.
4. 등록된 애플리케이션의 인증 정보에서 **Client ID(키 ID)** 를 복사합니다.
5. 서버에 ID를 설정하거나, 서버를 사용할 수 없을 때의 대안으로 저장소 루트의 `local.properties`(git에서 제외됨)에 한 줄을 추가하고 다시 빌드합니다.

```
naver.ncpKeyId=여기에_키_ID
```

명령줄에서 한 번만 넣으려면 `-Pnangmanroad.naverKeyId=키_ID`를 붙입니다. 키는 저장소에 올리지 않습니다. 지도는 내비게이션 지도 스타일의 주간 모드로 표시되며, 설정은 `core/src/main/java/com/nangmanroad/core/map/NaverPhotoMap.kt`에 있습니다.

2026-11-08 Windows에서 확인한 내용입니다.

- 콘솔의 인증 정보에는 키가 **Client ID (X-NCP-APIGW-API-KEY-ID)** 로 표시됩니다. 이 값을 그대로 `naver.ncpKeyId`에 넣으면 됩니다. SDK에는 `NcpKeyClient`로 전달하며(기본값), 예전 AI·NAVER API 방식의 키라면 `local.properties`에 `naver.clientType=legacy`를 추가합니다.
- 앱 로그에 `NAVER Maps auth failed: 401`이 나오면 콘솔의 Android 앱 패키지 이름을 확인한 뒤 **Application 수정 화면에서 그대로 저장**을 한 번 눌러 보세요. 등록 직후에는 패키지 목록이 모바일 인증 서버에 반영되지 않아 401이 나다가, 다시 저장하고 앱을 다시 빌드·설치하니 해결되었습니다. 키가 살아 있는지는 Web 서비스 URL에 `http://localhost:포트`를 잠시 추가하고 `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=키` 를 불러오는 HTML로 확인할 수 있습니다.
- 429가 나오면 Dynamic Map이 선택되지 않았거나 한도를 넘은 것입니다. 한도 설정의 "한도 초과 사용"은 허용안함으로 두면 과금 대신 차단됩니다.
- 키를 처음 넣거나 바꾼 뒤에는 `sh gradlew :core:clean`을 먼저 실행하고 빌드합니다. 키는 빌드할 때 상수로 코드에 들어가는데, 증분 빌드는 그 코드를 다시 컴파일하지 않아 예전 값(키가 없던 때의 Canvas 지도)이 남습니다. 2026-11-10 macOS에서 확인했습니다.
- 콘솔에서 Client ID 옆의 40자짜리 값은 Client Secret입니다. 앱에는 넣지 않습니다.

### 서버에서 지도 Client ID 받기

앱은 기본적으로 `https://34.64.170.97/map-config.json`에서 지도 Client ID와 클라이언트 종류를 읽습니다. 응답은 `{"naverKeyId":"...","clientType":"ncp"}` 형식이며 `clientType`은 `ncp` 또는 `legacy`입니다. 이 주소는 공개되므로 Client Secret이나 다른 비밀 키를 넣지 마세요. 서버에 접근할 수 없거나 설정이 유효하지 않으면 위에 설명한 로컬 빌드 설정을 사용하고, 그것도 없으면 Canvas 지도를 사용합니다. 앱을 다시 열거나 지도 화면에 다시 들어가면 설정을 다시 읽습니다.

다른 설정 서버를 사용하려면 Gradle 옵션 `-Pnangmanroad.configUrl=https://...`로 주소를 지정합니다. HTTPS만 허용합니다. 원격 서버에 ID가 설정되어 있으면 로컬 빌드 ID보다 우선합니다.

## 6. 휴대폰 에뮬레이터 준비

[스마트폰 앱](phone-app.md)은 일반 Android 휴대폰 에뮬레이터에서 실행합니다.

1. **Tools → SDK Manager → SDK Platforms**에서 **Show Package Details**를 켜고 Android API 35 아래의 **Google APIs** 시스템 이미지를 설치합니다. Apple Silicon은 ARM 64 v8a, Windows는 Intel x86_64 이미지를 고릅니다.
2. **Tools → Device Manager → Create Device**에서 Phone → **Pixel 7**을 고르고, 방금 설치한 API 35 이미지로 가상 기기를 만듭니다.
3. 기기를 실행한 뒤 Android Studio에서 실행 구성 `phone`을 선택해 **Run**을 누릅니다.

터미널에서 만들 수도 있습니다. macOS 예시입니다.

```zsh
sdkmanager --install "system-images;android-35;google_apis;arm64-v8a"
avdmanager create avd -n NangmanRoad_Phone -k "system-images;android-35;google_apis;arm64-v8a" -d pixel_7
```

`sdkmanager`와 `avdmanager`는 SDK의 `cmdline-tools/latest/bin`에 있습니다. PATH에 없으면 전체 경로로 실행합니다.

## 7. 두 에뮬레이터를 함께 쓸 때

차량 앱과 휴대폰 앱을 같이 보려면 Pleos 에뮬레이터와 휴대폰 에뮬레이터를 동시에 켭니다. 메모리가 충분해야 하며, 두 기기를 합쳐 RAM 6GB를 씁니다.

- 먼저 켠 기기가 `emulator-5554`, 다음 기기가 `emulator-5556`이 됩니다. `adb devices`로 확인하고, adb 명령마다 `-s 번호`로 대상을 지정합니다.
- `adb -s 번호 emu avd name`을 실행하면 그 번호가 어느 가상 기기인지 알 수 있습니다.
- 5단계의 Virtual Console 포트 연결도 Pleos 에뮬레이터 번호로 해야 합니다. 휴대폰을 먼저 켰다면 `emulator-5556`일 수 있습니다.

## 함께 작업할 때

- SDK, JDK, Android Studio, 가상 기기 이미지, `.gradle` 캐시와 빌드 산출물은 각자의 PC에 둡니다. 저장소에 올리지 않습니다.
- `local.properties`, 개인별 SDK 경로와 Pleos 클라이언트 시크릿 등 인증 정보도 저장소에 올리지 않습니다.
- Vehicle State와 NaviHelper를 쓸 수 있는지, 다른 SDK가 제공되는지는 [Pleos SDK 사용 안내](https://document.pleos.ai/docs/pleos-only/create-my-project)에서 확인합니다. 공모전 기획 전에 실제 API 제공 상태를 확인하세요.
- SDK나 가상 기기를 외장 드라이브에 둔 경우, 실행 중에는 드라이브를 분리하지 않습니다. Android Studio와 에뮬레이터를 종료한 뒤 운영체제에서 안전하게 추출합니다.
