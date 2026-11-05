# 외장 SSD 개발 환경 (개발용 Mac)

이 문서는 팀의 개발용 Mac 한 대에 맞춘 메모입니다. 이 Mac은 Android 도구, 가상 기기, 캐시를 모두 외장 SSD 이미지 `MacAPFS`에 두고 씁니다. 일반적인 위치에 Android Studio를 설치한 팀원은 [개발 환경 설치 안내](development-setup.md)만 따르면 되고, 이 문서는 같은 구성을 다시 만들거나 이 Mac에서 작업할 때 참고합니다.

> 기준: 2026-11-05 Apple Silicon Mac에서 확인한 값입니다. 버전과 용량은 이후 업데이트로 바뀔 수 있습니다.

## 폴더 구조

```
/Volumes/MacAPFS/
├── PleosDev/                              Android 개발 도구 일체
│   ├── env.sh                             터미널 환경 변수 (아래 표)
│   ├── Open Android Studio.command        env.sh를 적용한 뒤 Android Studio 실행
│   ├── Start Pleos Emulator.command       Pleos 차량 에뮬레이터를 창으로 실행
│   ├── Open Virtual Console.command       포트 8088 연결 후 브라우저로 콘솔 열기
│   ├── studio.properties                  Android Studio 설정·캐시·플러그인·로그 위치
│   ├── tools/                             Android Studio 2026.2, JDK 17.0.21+9 (약 3.6GB)
│   ├── sdk/                               Android SDK (약 24GB)
│   ├── avd/                               가상 기기 (약 4.6GB)
│   ├── gradle/                            Gradle 배포판과 의존성 캐시 (약 2.3GB)
│   ├── android-user/                      adb 키, 에뮬레이터 설정
│   ├── studio-config/ studio-system/ studio-plugins/ studio-log/
│   ├── tmp/                               임시 파일
│   ├── downloads/                         설치 파일 원본 (약 6.1GB)
│   └── samples/pleos-sample-vehicle-app/  공식 Vehicle SDK 샘플 (SDK 2.3.2로 수정한 복사본)
├── hyundai-pleos/                         이 저장소
└── Simulators/                            iOS 시뮬레이터 데이터 (이 프로젝트에서는 쓰지 않음)
```

SDK에 설치된 구성 요소는 다음과 같습니다.

| 구성 요소 | 버전 |
| --- | --- |
| SDK Platforms | Android 34, Android 35 |
| Build-Tools | 35.0.0 |
| Platform-Tools (adb) | 37.0.1 |
| Emulator | 37.2.12 |
| Command-line Tools | 22.0 |
| 시스템 이미지 | `android-34/pleos_car_emulator/arm64-v8a`(Pleos Connect 2.1.0), `android-35/google_apis/arm64-v8a`(휴대폰) |

`sdk/system-images/android-37.0/google_apis_playstore/`에는 설치 도중 멈춘 흔적(`.installer`)만 있고 실제 이미지는 없습니다. 쓸 수 없는 폴더이니 필요하면 SDK Manager에서 정리합니다.

## 환경 변수 (`env.sh`)

터미널에서 Gradle, adb, 에뮬레이터를 쓰기 전에 먼저 적용합니다. 현재 터미널에만 적용되며 셸 설정 파일은 바꾸지 않습니다. SSD 이미지가 마운트되어 있지 않으면 아무것도 설정하지 않고 멈춥니다.

```zsh
source /Volumes/MacAPFS/PleosDev/env.sh
```

| 변수 | 값 | 용도 |
| --- | --- | --- |
| `PLEOS_DEV_ROOT` | `/Volumes/MacAPFS/PleosDev` | 아래 경로의 기준 |
| `JAVA_HOME` | `$PLEOS_DEV_ROOT/tools/jdk-17.0.21+9/Contents/Home` | Gradle이 쓰는 JDK 17 |
| `ANDROID_HOME` | `$PLEOS_DEV_ROOT/sdk` | Android SDK |
| `ANDROID_USER_HOME`, `ANDROID_EMULATOR_HOME` | `$PLEOS_DEV_ROOT/android-user` | adb 키와 에뮬레이터 설정 |
| `ANDROID_AVD_HOME` | `$PLEOS_DEV_ROOT/avd` | 가상 기기 |
| `GRADLE_USER_HOME` | `$PLEOS_DEV_ROOT/gradle` | Gradle 배포판·의존성 캐시 |
| `STUDIO_PROPERTIES` | `$PLEOS_DEV_ROOT/studio.properties` | Android Studio 설정 위치 |
| `TMPDIR` | `$PLEOS_DEV_ROOT/tmp/` | 임시 파일 |
| `PATH` 앞부분 | JDK `bin`, `platform-tools`, `emulator`, `cmdline-tools/latest/bin` | `java`, `adb`, `emulator`, `sdkmanager`, `avdmanager` |

Android Studio는 Finder에서 직접 열지 말고 `Open Android Studio.command`로 엽니다. 그래야 위 경로가 적용됩니다. 처음 실행할 때 SDK 경로는 `/Volumes/MacAPFS/PleosDev/sdk`, Gradle JDK는 `/Volumes/MacAPFS/PleosDev/tools/jdk-17.0.21+9/Contents/Home`으로 지정합니다.

문서의 Gradle 명령은 `gradlew`의 실행 권한과 관계없이 동작하도록 `sh gradlew ...` 형태로 적었습니다.

## 가상 기기

| 항목 | `Pleos_Connect_v2_1` (차량) | `NangmanRoad_Phone` (휴대폰) |
| --- | --- | --- |
| 기기 프로필 | Pleos Connect v2.0 (Automotive) | Pixel 7 |
| 시스템 이미지 | Android 14 (API 34) Pleos Connect 2.1.0 | Android 15 (API 35) Google APIs |
| 화면 | 2560×1440, 160dpi | 1080×2400, 420dpi |
| RAM | 4GB | 2GB |
| 데이터 파티션 | 10GB | 10GB |
| 부팅 | 항상 Cold boot (Pleos 차량 기능에 필요) | 일반 |
| 하드웨어 키보드 | 켜짐 | 켜짐 (`hw.keyboard=yes`) |
| 쓰는 앱 | `vehicle` | `phone` |

휴대폰 가상 기기는 다음 명령으로 만들었습니다. 시스템 이미지는 압축 파일 기준 1.78GB를 받습니다.

```zsh
sdkmanager --install "system-images;android-35;google_apis;arm64-v8a"
avdmanager create avd -n NangmanRoad_Phone -k "system-images;android-35;google_apis;arm64-v8a" -d pixel_7
```

`avdmanager`가 `devices.xml`을 읽지 못했다는 오류를 출력해도 가상 기기는 만들어집니다. 만든 뒤 `avd/NangmanRoad_Phone.avd/config.ini`에서 `hw.keyboard=yes`로 바꾸면 Mac 키보드로 입력할 수 있습니다.

## 에뮬레이터 실행과 종료

창으로 실행합니다.

```zsh
emulator -avd Pleos_Connect_v2_1 -no-snapshot-load -no-snapshot-save -no-audio -no-boot-anim
```

```zsh
emulator -avd NangmanRoad_Phone -no-snapshot-save -no-audio -no-boot-anim
```

화면 없이 실행하려면 `-no-window`를 붙입니다. 스크린샷은 `adb exec-out screencap -p > shot.png`로 받습니다.

두 에뮬레이터를 함께 켜면 먼저 켠 기기가 `emulator-5554`, 다음 기기가 `emulator-5556`이 됩니다. 순서에 따라 번호가 바뀌므로 매번 확인하고, 모든 adb 명령에 `-s`로 대상을 지정합니다.

```zsh
adb devices
adb -s emulator-5554 emu avd name
adb -s emulator-5556 emu kill
```

`emu avd name`은 그 번호가 어느 가상 기기인지 알려 주고, `emu kill`은 해당 에뮬레이터를 종료합니다.

## Virtual Console

`Open Virtual Console.command`는 `emulator-5554`에만 포트를 연결합니다. 휴대폰 에뮬레이터를 먼저 켰다면 이 스크립트가 휴대폰에 연결되므로, Pleos 에뮬레이터 번호로 직접 연결합니다.

```zsh
adb -s emulator-5556 forward tcp:8088 tcp:8088
open http://127.0.0.1:8088/
```

터미널에서 기어를 바꾸는 방법은 [차량용 앱 문서](vehicle-app.md#virtual-console로-주행-상태-바꾸기)에 있습니다. 기어를 D로 바꾸면 Pleos 시스템이 화면을 내비게이션 앱으로 돌릴 수 있습니다. 그럴 때는 앱을 다시 앞으로 가져옵니다.

```zsh
adb -s emulator-5556 shell am start -n com.nangmanroad.vehicle/.MainActivity
```

## 앱 설치

저장소 루트에서 빌드한 뒤 각 기기에 설치합니다. 기기 번호는 위에서 확인한 값으로 바꿉니다.

```zsh
sh gradlew :vehicle:assembleDebug :phone:assembleDebug
adb -s emulator-5556 install -r -g vehicle/build/outputs/apk/debug/vehicle-debug.apk
adb -s emulator-5554 install -r phone/build/outputs/apk/debug/phone-debug.apk
```

차량 앱은 Pleos 권한을 설치할 때 함께 부여하도록 `-g`를 붙입니다. 휴대폰 앱의 테스트 데이터를 지우려면 `adb -s emulator-5554 shell pm clear com.nangmanroad.phone`을 실행합니다.

## 알아둘 점

- **SSD 분리:** Android Studio와 에뮬레이터를 모두 종료한 뒤 macOS에서 이미지를 안전하게 추출합니다. 실행 중에 분리하면 가상 기기와 Gradle 캐시가 손상될 수 있습니다.
- **iOS 시뮬레이터 데이터:** CoreSimulator 데이터도 같은 외장 이미지의 `Simulators/`에 있습니다. 이 프로젝트와는 관계없지만, 기존 시뮬레이터 데이터를 초기화하거나 경로를 임의로 바꾸지 않습니다.
- **휴대폰 에뮬레이터 입력:** `adb shell input text`로는 영문만 입력할 수 있습니다. 처음 입력하면 키보드의 스타일러스 안내 창이 뜰 수 있는데, 'Cancel'로 닫으면 됩니다.
- **디스크 공간:** `downloads/`에는 Android Studio, JDK, 명령줄 도구, Pleos 이미지 설치 파일이 남아 있습니다(약 6.1GB). 다시 설치할 일이 없다면 지워서 공간을 확보할 수 있습니다.
- **공식 샘플:** `samples/pleos-sample-vehicle-app/`은 `vehicleSdk`를 2.3.2로 바꾼 상태입니다. 바뀐 내용은 그 폴더에서 `git diff`로 확인합니다.
