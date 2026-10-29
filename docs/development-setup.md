# Pleos Connect 개발 환경 설치 및 샘플 실행

이 문서는 macOS와 Windows 팀원이 Pleos Connect Vehicle SDK 샘플을 빌드하고 차량 에뮬레이터에서 실행하기 위한 안내입니다. 메뉴 이름은 Android Studio 버전에 따라 조금 다를 수 있습니다.

## 준비물과 기준 버전

- macOS 또는 Windows PC, 인터넷 연결, Git, 충분한 저장 공간. Pleos 시스템 이미지는 다운로드만 약 4.7GB이고 압축을 풀면 약 20GB입니다. Android SDK·IDE·가상 기기·빌드 캐시를 위한 공간도 추가로 필요합니다. 개발한 Mac에서는 전체 약 34GB를 사용했습니다.
- [Android Studio 최신 안정판](https://developer.android.com/studio)과 [JDK 17](https://adoptium.net/temurin/releases/?version=17). Mac은 자신의 CPU에 맞는 설치 파일을 선택합니다.
- Android SDK Platform 34와 35, Android SDK Build-Tools 35.0.0, Android SDK Platform-Tools, Android Emulator.
- Pleos Connect Emulator 2.1.0: Android 14 / API 34. 앱을 만들 때는 **minSdk 28 이상**을 설정합니다.
- 아래에서 확인하는 공식 샘플 기준 AGP 8.8.2, Kotlin 2.0.0, Gradle Wrapper 8.10.2. 별도로 Gradle을 설치할 필요는 없습니다. AGP 8.8의 최소 Gradle 버전은 8.10.2입니다.

공모전에서는 Gradle 8.0 이상을 안내하지만 실제 버전은 사용하는 AGP의 요구사항도 충족해야 합니다. [Pleos 환경 요구사항](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator) · [AGP 8.8 호환성](https://developer.android.com/build/releases/agp-8-8-0-release-notes)

> 확인 범위: 2026-10-29 Apple Silicon Mac에서 CLI 빌드, 화면 없는 에뮬레이터 부팅과 샘플 실행을 확인했습니다. Windows와 Intel Mac에서는 이 저장소의 절차를 아직 직접 검증하지 않았습니다. Pleos의 Mac 이미지 주소는 ARM64이므로 Intel Mac 지원은 공식 문서에서 따로 확인해 주세요.

## 1. Android Studio와 JDK 설치

1. 위 링크에서 Android Studio와 JDK 17을 내려받아 각 PC에 설치합니다. 약관이 표시되면 내용을 확인하고 동의합니다.
2. Android Studio를 처음 열어 설치 마법사를 진행합니다. Android SDK 설치 위치를 기억해 둡니다. 외장 드라이브에 저장하고 싶다면 **처음부터 SDK 위치를 외장 드라이브로 선택**합니다.
3. Android Studio의 **Settings / Preferences → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**에서 설치한 JDK 17의 홈 디렉터리를 선택합니다. 프로젝트를 연 뒤 설정이 보이는 경우 해당 단계에서 지정합니다.
4. 터미널에서도 JDK 17을 사용할 경우 `JAVA_HOME`을 JDK 17 홈으로 설정합니다. Android Studio의 Gradle JDK와 터미널의 `JAVA_HOME`은 별도 설정입니다.

macOS 터미널에서 시스템에 등록된 JDK 17을 선택하는 예시:

```zsh
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
"$JAVA_HOME/bin/java" -version
```

위 명령은 시스템에 등록된 JDK에만 적용됩니다. 외장 SSD에 압축을 풀어 둔 JDK처럼 등록되지 않은 경우에는 직접 경로를 지정합니다. 이 Mac에 이미 구성된 환경은 `/Volumes/MacAPFS/PleosDev/env.sh`를 사용하면 됩니다.

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

터미널 빌드도 가능합니다. 먼저 해당 터미널의 `JAVA_HOME`이 JDK 17인지 확인하세요. SDK를 찾지 못하면 Android Studio에서 샘플을 열어 SDK 경로를 지정하거나 `ANDROID_HOME`을 실제 SDK 위치로 설정합니다.

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

## 함께 작업할 때

- SDK, JDK, Android Studio, 가상 기기 이미지, `.gradle` 캐시와 빌드 산출물은 각자의 PC에 둡니다. 저장소에 올리지 않습니다.
- `local.properties`, 개인별 SDK 경로와 Pleos 클라이언트 시크릿 등 인증 정보도 저장소에 올리지 않습니다.
- Vehicle State와 NaviHelper의 사용 가능 상태 및 다른 SDK의 제공 여부는 [Pleos SDK 사용 안내](https://document.pleos.ai/docs/pleos-only/create-my-project)에서 확인합니다. 공모전 기획 전에 실제 API 제공 상태를 확인하세요.
- 외장 SSD 환경은 실행 중 드라이브를 분리하지 않습니다. Android Studio와 에뮬레이터를 종료한 뒤 운영체제에서 안전하게 추출합니다. 이 Mac은 CoreSimulator 데이터도 외장 이미지에 있으므로 기존 시뮬레이터 데이터를 초기화하거나 경로를 임의로 교체하지 않습니다.
