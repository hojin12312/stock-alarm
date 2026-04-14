# android-playground

터미널만으로 운영하는 Android 네이티브(Kotlin + Jetpack Compose) 플레이그라운드. Android Studio GUI 없이 클로디가 편집→빌드→설치→실행→스크린샷까지 전부 CLI로 처리한다.

현재 들어 있는 앱: **주식 알리미 (StockAlarm)** — 한·미 주식 검색 + 관심목록 + 5/20일 이동평균 교차 알림.

## 환경 요구사항

| 항목 | 버전 | 설치 경로 |
|---|---|---|
| JDK | OpenJDK 17.0.18 (Homebrew) | `/opt/homebrew/opt/openjdk@17` |
| Android SDK | API 34 | `~/Library/Android/sdk` → `/opt/homebrew/share/android-commandlinetools` (심볼릭 링크) |
| Gradle | 8.7 (wrapper) | 프로젝트 내장 |
| AGP | 8.3.2 | `build.gradle.kts` |
| Kotlin | 1.9.23 | `build.gradle.kts` |
| Compose BOM | 2024.05.00 | `app/build.gradle.kts` |
| Compose Compiler | 1.5.11 | `app/build.gradle.kts` |

**중요**:
- JDK 21/25 쓰지 말 것. AGP 8.3.2는 **JDK 17 고정**.
- 시스템 `gradle`(brew 9.4.1)은 쓰지 않는다. 항상 `./gradlew`로 wrapper 사용.
- Apple Silicon이라 system image는 **arm64-v8a** 필수 (x86은 매우 느림).

## 환경변수

`~/.zshrc`에 등록되어 있음. 새 세션에선 자동 적용. 기존 세션에서는 `source ~/.zshrc`.

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

## 프로젝트 구조

```
android-playground/
├── settings.gradle.kts
├── build.gradle.kts             # AGP/Kotlin/Serialization/KSP 플러그인
├── gradle.properties
├── gradlew / gradlew.bat        # Gradle wrapper (8.7)
└── app/
    ├── build.gradle.kts         # 의존성: Compose + Navigation + Retrofit + Room + WorkManager
    └── src/main/
        ├── AndroidManifest.xml  # INTERNET, POST_NOTIFICATIONS, .PlaygroundApp
        └── java/com/example/playground/
            ├── MainActivity.kt              # 알림 권한 요청 + setContent { PlaygroundApp() }
            ├── PlaygroundApp.kt             # Application, WorkManager 15분 주기 enqueue
            ├── di/ServiceLocator.kt         # Room/Retrofit/Repository/Notifier 싱글톤
            ├── data/
            │   ├── model/                    # MaStatus, Market, StockSearchResult, WatchedStock
            │   ├── local/                    # Room: AppDatabase, WatchlistDao, Entity, Converters
            │   ├── remote/                   # YahooFinanceApi(Retrofit), NetworkModule, dto/
            │   └── repo/StockRepository.kt   # 검색·차트·이평선 갱신·교차 판정
            ├── domain/MaCalculator.kt       # 5/20일 이평선 순수 함수
            ├── notification/Notifier.kt     # 알림 채널 + 교차 전환 알림
            ├── worker/MaCrossoverWorker.kt  # CoroutineWorker, 15분 주기 갱신·알림
            └── ui/
                ├── nav/                      # Destinations, PlaygroundNavHost (3탭 BottomBar)
                ├── search/                   # 검색 탭 + ViewModel
                ├── watchlist/                # 관심목록 탭 + ViewModel
                └── dashboard/                # 대시보드 탭 + ViewModel + [지금 새로고침]
```

- **package/applicationId**: `com.example.playground`
- **앱 이름**: `주식 알리미`
- **minSdk**: 26, **compileSdk/targetSdk**: 34
- UI 방식: **Jetpack Compose + Material3** (XML 레이아웃 사용 안 함)
- 데이터: **Yahoo Finance 비공식 API** (`query2.finance.yahoo.com/v1/finance/search`, `query1.finance.yahoo.com/v8/finance/chart`)
- 매수/매도 라벨: **5MA < 20MA → 매수**, **5MA > 20MA → 매도** (역추세/평균회귀 관점, company.txt 정의 그대로)

## 개발 루프 (표준 워크플로우)

```bash
cd ~/home_apps/android-playground

# 1. 에뮬레이터 기동 (한 번만, 이미 떠 있으면 생략)
nohup emulator -avd pixel_api34 -no-snapshot-save -no-audio > /tmp/emulator.log 2>&1 &
adb wait-for-device && adb shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 2; done; echo ok'

# 2. 편집 → 빌드 + 설치 한방
./gradlew installDebug

# 3. 앱 실행
adb shell am start -n com.example.playground/.MainActivity

# 4. 스크린샷 확인 (클로디가 Read로 볼 수 있음)
adb exec-out screencap -p > /tmp/playground-screen.png

# 5. 로그 보기
adb logcat *:W com.example.playground:V
```

## 유용한 명령

```bash
# 클린 빌드
./gradlew clean assembleDebug

# 의존성 트리
./gradlew :app:dependencies

# AVD 목록
avdmanager list avd

# 실행 중 디바이스
adb devices

# 에뮬레이터 종료
adb emu kill

# APK 위치
ls app/build/outputs/apk/debug/app-debug.apk

# 앱 제거
adb uninstall com.example.playground
```

## AVD 정보

| 속성 | 값 |
|---|---|
| 이름 | `pixel_api34` |
| 디바이스 | Pixel 6 |
| API | 34 (Android 14 "UpsideDownCake") |
| ABI | arm64-v8a, google_apis |
| 경로 | `~/.android/avd/pixel_api34.avd` |

## 알려진 제약 / 함정

- **에뮬레이터 창은 GUI**라 macOS 데스크탑에 뜸. 완전 헤드리스로 가려면 `emulator ... -no-window` 추가하고 `adb exec-out screencap`으로 화면 확인.
- **Gradle 9 쓰면 AGP 8.3.2와 호환 안 됨**. wrapper의 `distributionUrl`은 반드시 `gradle-8.7-bin.zip`.
- **ANDROID_HOME이 심볼릭 링크**(`~/Library/Android/sdk` → brew 경로). brew 업그레이드 시 `android-commandlinetools` cask가 그 경로를 재생성하므로 대부분 안전. 깨지면 `rm -rf ~/Library/Android/sdk && ln -s /opt/homebrew/share/android-commandlinetools ~/Library/Android/sdk`로 복구.
- **첫 빌드는 오래 걸림** (Gradle 8.7 배포판 다운로드 + Compose 의존성 전부, 약 1~2분). 이후엔 캐시돼서 빠름.
- sdkmanager 라이선스는 이미 전부 수락됨. 새 SDK 컴포넌트 추가 시 `yes | sdkmanager --licenses` 다시 돌려도 됨.

## 검증 완료 기록 (2026-04-14, 주식 알리미)

아래 시나리오가 에뮬레이터(`pixel_api34`)에서 전부 통과:

1. `./gradlew installDebug` → BUILD SUCCESSFUL ✅
2. 앱 콜드 스타트 → 검색 탭(주식 검색) 정상 렌더 ✅
3. "samsung" 검색 → Yahoo Finance API에서 `005930.KS Samsung Electronics`, `207940.KS Samsung Biologics`, `489250.KS ETF` 등 응답 정상 파싱 ✅
4. [+관심] 버튼 → "등록됨"으로 전환, 관심목록 탭에서 KR 칩과 함께 표시 ✅
5. 대시보드 탭 → "대기" 상태 카드 표시 + 우상단 [지금 새로고침] 버튼 ✅
6. [지금 새로고침] → Yahoo 차트 호출 → 5/20MA 계산:
   - Samsung Electronics: 5MA 205,750 / 20MA 192,485 / 종가 207,250 → **매도** 배지 (5MA > 20MA) ✅
   - Samsung Biologics: 5MA 1,567,200 / 20MA 1,569,000 / 종가 1,537,000 → **매수** 배지 ✅
7. 교차 알림 검증: `run-as com.example.playground sqlite3` 로 `lastStatus`를 강제로 뒤집고 새로고침 → 알림 셰이드에 `[매수 전환] Samsung Biologics`, `[매도 전환] Samsung Electronics` 두 알림이 "주식 알리미" 채널로 정상 표시 ✅
8. WorkManager: `PlaygroundApp.onCreate()`에서 `enqueueUniquePeriodicWork("ma_crossover_periodic", KEEP, 15분)` 등록 ✅

## 백그라운드 동작·배포 관련 참고

- WorkManager `Periodic` 15분이 *최소 주기*. Doze·앱 대기·제조사 절전 정책으로 실제 실행은 더 늦어질 수 있음. "분 단위 정확"이 필요하면 `AlarmManager.setExactAndAllowWhileIdle` + `SCHEDULE_EXACT_ALARM` 권한으로 격상 필요.
- 사용자가 강제 종료(특히 샤오미·화웨이)하면 WorkManager 작업도 같이 죽음. Foreground Service나 FCM 서버 푸시가 아니면 100% 보장 불가.
- 앱은 네이티브 라이브러리 없음 → 디버그 APK(`app/build/outputs/apk/debug/app-debug.apk`)는 minSdk 26+ 어떤 안드로이드 기기에도 사이드로딩 가능. 받는 쪽이 "출처를 알 수 없는 앱 허용" 켜야 함. 정식 배포는 `assembleRelease` + 자체 keystore 서명 필요.
- Yahoo Finance 비공식 API라 스키마가 언제든 변할 수 있고 일부 통신사·국가에서 차단될 수 있음. 안정성이 필요하면 KIS OpenAPI 등 공식 소스로 교체.

---

## 클로디를 위한 작업 메모

- **새 세션 진입 시 첫 체크**: `./gradlew --version`이 Gradle 8.7 + JVM 17을 보여주는지, `adb devices`로 에뮬레이터 떠있는지 확인.
- **UI 변경 검증**: 빌드→설치→실행 후 반드시 `screencap`으로 실제 렌더링 확인 (Compose preview는 못 쓰니까).
- **uiautomator dump**: Compose 화면에서 좌표를 정확히 찾으려면 `adb shell uiautomator dump /sdcard/win.xml && adb pull /sdcard/win.xml /tmp/win.xml`로 view bounds 확인 후 `adb shell input tap`. 시각적 추정은 자주 빗나감.
- **DB 직접 조작 (테스트용)**: `adb shell "run-as com.example.playground sqlite3 databases/playground.db 'SELECT * FROM watchlist;'"`. `lastStatus`를 강제로 뒤집어서 알림 트리거 검증할 때 쓴다.
- **알림 셰이드 펼치기**: `adb shell cmd statusbar expand-notifications`로 알림 발송 검증.
- **코드 스타일**: Compose 함수는 PascalCase, 파일당 하나의 최상위 Composable 권장. 수동 DI(ServiceLocator) 사용, Hilt는 도입 안 함.
- **디자인/아키텍처 변경은 루루와 상의 후**. 버그 수정은 즉시 진행 OK.
