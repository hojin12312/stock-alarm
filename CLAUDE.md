# android-playground

터미널만으로 운영하는 Android 네이티브(Kotlin + Jetpack Compose) 플레이그라운드.
Android Studio GUI 없이 편집→빌드→설치→실행→스크린샷까지 전부 CLI로 처리한다.

**현재 들어 있는 앱**: 주식 알리미 (Stock Alarm) — 한·미 주식 검색 + 관심목록 + 5/20MA 교차 알림 + 차트 디테일.

## 현재 상태 (2026-04-24 기준)

- **버전**: `v0.5.6` (versionCode 25)
- **v0.5.6 배율 높은 폰 스크롤 이슈 수정**:
  - **ChartContent 세로 스크롤**: `ChartContent.kt` 최상위 `Column` 에 `.verticalScroll(rememberScrollState())` 추가. 배율 높은 폰(font_scale 1.3 + density 540 등)에서 차트 카드 아래 매수/매도 타임라인 바·범례·데이터 범위 텍스트가 화면 밖으로 잘리던 문제 해결. 차트 카드 내부의 `detectTransformGestures` (pan.x/zoom) 는 그대로 두고 카드 **바깥** 영역(StatusHeader·range 칩·타임라인 바 등)에서 세로 스와이프로 스크롤. 실기 검증: 배율 올린 에뮬레이터에서 MA+RSI 동시 선택 상태 기준 범례 3줄 + "YYYY-MM-DD ~ YYYY-MM-DD · 데이터 N개" 모두 노출 확인.
  - **DashboardScreen Column → LazyColumn 통합**: 최상위 `Column` 을 `LazyColumn` 하나로 교체하고 상단 헤더/알고리즘 칩/필터 Row/텍스트필드를 `item {}` 블록으로 이동. 기존 내부에 중첩돼 있던 `LazyColumn` 흡수. 배율 높을 때 상단 UI가 고정돼 검색창에 접근 못 하던 문제 해결. Empty 상태는 `Box(Modifier.fillParentMaxSize())` 로 전환.
- **v0.5.5 Pull-to-Refresh + 차트 핀치줌/스크롤**:
  - **지수 탭 Pull-to-Refresh**: `MarketViewModel`에 `isRefreshing: StateFlow<Boolean>` 추가, `loadAll()` → `jobs.joinAll()` 구조로 완료 감지. `MarketScreen`에 `PullToRefreshContainer` + `nestedScrollConnection` 적용 (Material3 1.2.1 호환 방식).
  - **차트 핀치줌/드래그 스크롤**: `ChartData`에 `displayOffset: Int` 파라미터 추가, `display*` 슬라이스를 `subList(startIdx, startIdx + displayLen)` 기반으로 변경. `ChartContent`에 `zoomedCount` / `scrollOffset` 로컬 state + `detectTransformGestures` 제스처 처리 추가. range 칩 변경 시 줌·스크롤 초기화 (`LaunchedEffect(data.displayCount)`).
  - **에뮬레이터 드래그 스크롤 실측 통과**: `adb shell input swipe 200 1300 900 1300 1200` (오른쪽 드래그) 으로 1y 차트 범위가 8개월 과거로 이동 확인. 콜백 로그(`pan=Offset(9.9,0.0) zoom=1.0 totalSize=1255 zoomedCount=251`) 정상. 왼쪽 드래그는 초기 상태(최신 윈도우)에서 `coerceIn(0,maxOffset)` 에 의해 0으로 clamp되어 움직이지 않는 게 **정상 동작** (일반 차트 UX와 동일: 오른쪽=과거, 왼쪽=최근).
  - **핀치줌은 adb에서 검증 불가**: `input` 명령이 single-touch만 지원. `detectTransformGestures` 가 pan/zoom 을 같은 콜백에서 처리하므로 pan 통과 = zoom 동작 가능성 매우 높음. 실기기 또는 에뮬레이터 GUI(Ctrl+드래그) 로 최종 확인 권장.
- **v0.5.4 지수/시장 탭 추가**: 검색 탭 오른쪽에 **지수** 탭 신설. 코스피·코스닥·나스닥·다우존스·S&P 500·금 선물·USD/KRW 7개 지수를 카드로 표시 (실시간 현재가 + 전일 대비 등락률). 카드 탭 시 기존 `ChartScreen`(MA/RSI 알고리즘 동일 적용)으로 진입. `MarketIndex` enum + `fetchChartDirect()` (관심목록 없이 Yahoo 직접 호출) + `ChartViewModel.displayName` 파라미터로 지수 차트 지원. USD/KRW는 소수점 2자리(`%,.2f`) 적용.
- **v0.5.3 차트 범위·신호 계산 분리**: 항상 5y 데이터를 fetch해 RSI/SMA200 전체 계산 후, 선택한 range(1mo~5y)는 표시 윈도우(`displayCount`)만 조정. 단기 범위에서도 RSI 타임라인 바 정상 표시. range 전환 시 캐시된 데이터 재사용 → 네트워크 재요청 없이 즉각 반응. `ChartData.displayCount` + `display*` 슬라이스 프로퍼티 추가. `ChartViewModel.fullData` 캐시 + `calcDisplayCount()` 추가 (초 단위 epoch 기준).
- **v0.5.2 RSI 전략 매도 조건 구현**: `QuantSnapshot.status: MaStatus?` (null=중립). `QuantCalculator.compute()` BUY(`RSI<10`) / SELL(`RSI>70`) / null(그 외) 3상태. `ChartSignals.rsiSellIndices()` 추가. RSI 타임라인 바에 빨간 매도 신호 추가. 범례 "매수 신호 / 매도 신호"로 통일.
- **v0.5.1 RSI 범례 개선**: RSI만 선택 시 범례 "매도 구간" → "RSI 매수 신호"로 분기. (v0.5.2에서 정식 교체됨)
- **v0.5.0 차트 버그 수정 3종**:
  - **차트 진입 알고리즘 초기화**: `NavRoutes.chartFor()` → `Set<AlgorithmType>` 인자로 변경. Dashboard의 `selectedAlgorithms`를 comma-separated route param으로 그대로 전달. MA+RSI 둘 다 선택 후 카드 클릭 시 차트도 둘 다 선택된 상태로 진입 (기존: 2개면 MA로 강제 초기화).
  - **RSI 타임라인 바 회색**: `sma200Series.any { it != null }` 로 데이터 충분 여부 체크. 부족 시(3mo 이하 range) "SMA(200) 계산에 1y 이상 필요" 안내 문구 표시.
  - **차트/타임라인 x축 불일치**: `TimelineBarRow` (Row 구조) → `TimelineBarBox` (Box 오버레이) 변경. label(MA/RSI 텍스트)이 Canvas 위에 오버레이되어 Canvas 폭이 차트와 동일해짐 (기존 36dp 차이 해소).
- **v0.4.9 차트 알고리즘 토글 + 타임라인 바 UI**:
  - `ChartScreen` 에 `selectedAlgorithms: Set<AlgorithmType>` rememberSaveable state 추가 (초기값은 nav 파라미터 하나, `AlgorithmSetSaver` 로 회전 시 유지). 차트 내에서 MA/RSI 각각 토글, 최소 1개 강제.
  - 과거 매수/매도 구간 표시 방식 변경: 차트 본체 배경 음영 + ▲/▼ 마커 + RSI 점 마커 **전부 제거**. 대신 차트 Card 아래에 알고리즘별 한 줄짜리 타임라인 바 (`ui/chart/SignalTimelineBar.kt`). `MaSignalTimelineBar` 는 BUY/SELL 색띠, `RsiSignalTimelineBar` 는 시그널일마다 녹색 세로 바. 두 알고리즘 선택 시 나란히 두 줄 (캘린더 다중 일정 비유).
  - `StatusHeader` 는 선택된 알고리즘에 맞춰 조건부 표시. 둘 다 선택 시 `MA [뱃지]` / `RSI [뱃지]` 각각 라벨 달아 세로 나열.
  - `LineChartCanvas` 는 다시 가격/5MA/20MA 라인만 그리는 원래 형태로 복원.
- **v0.4.8 장외 알림 차단 + 필터 상태 유지 + 차트 과거 신호 오버레이**:
  - **장외 알림 차단**: `data/model/MarketHours.kt::Market.isOpenNow()` 추가. KR 은 평일 KST 09:00-15:30, US 는 평일 ET 09:30-16:00 만 `true`. `MaCrossoverWorker` 와 `DashboardViewModel.refreshNow` 양쪽에서 `notifier.notify*` 호출 직전에 게이팅. DB 상태는 그대로 갱신되므로 개장 직후 첫 워커가 밀린 교차를 정상 감지. 휴장일은 반영 X, DST 는 `ZoneId.of("America/New_York")` 가 자동 처리. **과거 "장 시각 게이팅 도입 안 함" 결정 역전 — 새벽 알림 재현 케이스 대응**.
  - **알고리즘 필터 리셋 버그**: `DashboardViewModel.toggleAlgorithm()` 이 알고리즘 토글마다 `statusFilter = null` 을 덮어써서 매수/매도 선택이 "전체"로 돌아가던 문제. 해당 라인 제거만으로 해결.
  - **차트 과거 신호 오버레이**: `QuantCalculator.rsiSeries`/`smaSeries` 신규 (각 i 에 대한 RSI(2)/SMA(200) 시계열), `ChartData.rsi2Series`/`sma200Series` lazy 파생, `domain/ChartSignals.kt` 에 MA 신호 구간/전환점 + RSI BUY 인덱스 추출. `LineChartCanvas` 에 `algorithmType` 받아 오버레이: **MA 교차** → BUY/SELL 구간 배경 음영(`buy/sell.copy(alpha=0.1)`) + ▲ 골든 / ▼ 데드 삼각형 마커. **RSI 전략** → `close>SMA200 && RSI2<10` 만족한 날짜에 녹색 점 마커. 데이터 부족 구간(첫 200일)은 마커 없음. 범례 `LegendRow(algorithmType)` 로 동적화.
- **v0.4.7 GitHub Releases 도입 + 업데이트 다이얼로그 히스토리 링크**:
  - `UpdateDialog.kt` — "📜 전체 업데이트 히스토리 보기" 텍스트 링크 추가. `Intent.ACTION_VIEW`로 외부 웹 브라우저에서 `github.com/hojin12312/stock-alarm/releases` 오픈. 긴 notes 대비 `heightIn(max=360.dp)` + `verticalScroll` 적용.
  - 이번 버전부터 `gh release create`로 정식 GitHub Release 등록 (태그 `v0.4.7`, APK 자산 첨부). 첫 릴리스 노트 끝에 "이전 버전(v0.4.6 이하) 변경 이력 → `docs/RELEASE.md` 버전 히스토리 표" 링크 포함.
  - `docs/RELEASE.md` "현재 버전" 섹션 → "버전 히스토리"로 이름 변경 + v0.3.4~v0.4.7 표 채움. 릴리스 절차에 `gh release create` 단계 8번으로 추가.
  - **다이얼로그 실물 UI 확인 지연**: v0.4.7 설치본이 띄우는 다이얼로그(링크 포함)는 **다음 릴리스 v0.4.8 배포 시점에** v0.4.7 사용자에게 처음으로 노출됨 (자동 업데이트 메커니즘의 1버전 지연 특성).
- **v0.4.6 UX 개선**:
  - **다크 모드 정식 지원**: `ui/theme/Theme.kt` + `Color.kt`. `AppTheme { }` 래퍼 + `ExtendedColors`(buy/sell/ma5Line/ma20Line) 라이트/다크 변종. 하드코딩 9개 Color는 전부 `AppColors.extended` 경유.
  - **차트 range 통일 + 확장**: `AppSettings.chartRange` (DataStore) 로 마지막 선택을 탭 간 공유. 칩 `1mo/3mo/6mo/1y/2y/5y` (기존 4개 → 6개). 초기값 `3mo`.
  - **검색 에러 피드백**: `SearchErrorType`(NETWORK/RATE_LIMIT/OTHER) 분류 + 상황별 안내 문구 + 다시 시도 버튼.
  - **KIS 키 입력 UX**: AppKey/Secret 보이기·숨기기 토글 아이콘, 저장 후 `"저장된 AppKey: ****_****_{뒤3글자}"` 노출.
  - **알림 탭 날짜 그룹핑**: 오늘/어제/이번 주/지난 주/그 이전 헤더. 필터 적용 후 0건일 때 "해당 조건의 알림이 없어" 분기.
- **v0.4.5 리팩토링 (사용자 동작 불변)**:
  - 포매팅 유틸 통합: `util/Formatters.kt` (`formatNumber`, `formatDecimal1`, `formatDateYmd`, `formatDateTime`, `formatClock`)
  - `StatusBadge` 공통 Composable: `ui/common/StatusBadge.kt`
  - 상태 판별 로직: `domain/AlgoStatusResolver.kt::resolveDisplayStatus()`
  - `ChartScreen.kt` 378줄 → 3분할 (`ChartScreen.kt` 84줄 + `ChartContent.kt` 177줄 + `LineChartCanvas.kt` 105줄)
- **GitHub**: https://github.com/hojin12312/stock-alarm (public)
- **최신 APK**: `dist/stock-alarm-debug.apk` — raw URL로 배포 중
- **앱 아이콘**: 녹색 차트 + 원화 동전 (5 해상도 legacy + Adaptive Icon v26)
- **데이터 소스**: 설정 화면에서 선택 — Yahoo Finance(기본) / 한국투자증권(KIS) Open API
  - 검색은 항상 Yahoo 고정(증권사 API는 종목 검색 미제공)
  - 시세·차트·15분 워커만 활성 소스 경유
  - KIS AppKey/Secret은 `EncryptedSharedPreferences`로 암호화 저장, 토큰 원문은 메모리 전용
- **마지막 검증**: 신규 설치 후 samsung/tesla 검색 정상, query1으로 200 OK 확인 (2026-04-15)
- **v0.3.1 핫픽스**: 검색 엔드포인트를 `query2.finance.yahoo.com` → `query1`로 변경. query2가 모든 검색 요청에 429 반환하던 이슈 해결.
- **자동 업데이트**: 앱 시작 시 `dist/version.json`을 raw URL로 폴링 → 더 큰 versionCode 발견 시 다이얼로그 → DownloadManager로 APK 다운로드 → 시스템 설치 화면. 릴리스마다 `dist/version.json`도 같이 갱신해야 함 (`docs/RELEASE.md` 5단계 참고).
- **루루 최종 확정 사항**:
  - 매수/매도 라벨은 `5MA<20MA=매수` (문서 정의 그대로)
  - WorkManager 15분 주기. 장 시각 게이팅은 v0.4.8 부터 도입 (알림만 스킵, DB 갱신은 그대로 — 새벽 알림 재현 후 결정 역전)
  - `versionCode` 매 릴리스마다 증가, `./gradlew installDebug`로 관심목록 유지 업데이트
  - 미래에셋증권은 모바일 앱용 REST Open API를 공개하지 않아 KIS로 대체

## 문서 인덱스

| 파일 | 내용 |
|---|---|
| [`docs/STACK.md`](docs/STACK.md) | 스택·환경 요구사항·패키지 구조·의존성·데이터 소스·기능 목록 |
| [`docs/RELEASE.md`](docs/RELEASE.md) | 버전 관리·릴리스 절차·**데이터 유지 업데이트**·GitHub 레포 운영 |
| [`docs/VERIFICATION.md`](docs/VERIFICATION.md) | 에뮬레이터 검증 루프·UI 자동 조작·DB 직접 조작·완료 시나리오 기록 |
| [`README.md`](README.md) | 사용자용 (설치·업데이트 가이드·스크린샷·기술 스택 요약) |

다음 세션에서 특정 주제가 필요하면 위 파일들을 먼저 읽어. 이 `CLAUDE.md`는 **인덱스 + 빠른 시작**만 담는다.

## 새 세션 진입 시 첫 체크

```bash
# 1. 환경 확인
./gradlew --version          # Gradle 8.7 + JVM 17 나와야 함
adb devices                  # 에뮬레이터 떠 있는지
./gradlew :app:compileDebugKotlin 2>&1 | tail -5   # 기존 코드가 컴파일 되는지

# 2. 에뮬레이터 없으면 기동
nohup emulator -avd pixel_api34 -no-snapshot-save -no-audio > /tmp/emulator.log 2>&1 &
adb wait-for-device && adb shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 2; done; echo ok'

# 3. 현재 git 상태 확인 (push 안 된 변경 있는지)
cd ~/home_apps/android-playground && git status && git log --oneline -5
```

## 표준 개발 루프

```bash
cd ~/home_apps/android-playground

# 편집 → 빌드 + 설치 한방 (내부적으로 adb install -r → 기존 DB 유지)
./gradlew installDebug

# 앱 실행
adb shell am start -n com.example.playground/.MainActivity

# 스크린샷 확인
adb exec-out screencap -p > /tmp/playground-screen.png

# 로그 보기
adb logcat -s StockRepository MaCrossoverWorker OkHttp
```

UI 자동 조작·DB 직접 조작·알림 검증 방법은 `docs/VERIFICATION.md` 참고.

## 유용한 명령

```bash
./gradlew clean assembleDebug                     # 클린 빌드
./gradlew :app:dependencies                        # 의존성 트리
avdmanager list avd                                # AVD 목록
adb emu kill                                       # 에뮬레이터 종료
ls app/build/outputs/apk/debug/app-debug.apk       # APK 위치

# 앱 제거 (데이터 소멸!) — 평상시엔 쓰지 말 것. 업데이트는 installDebug로.
adb uninstall com.example.playground
```

## AVD 정보

| 속성 | 값 |
|---|---|
| 이름 | `pixel_api34` |
| 디바이스 | Pixel 6 |
| API | 34 (Android 14) |
| ABI | arm64-v8a, google_apis |
| 해상도 | 1080 × 2400, density 420 |
| 경로 | `~/.android/avd/pixel_api34.avd` |

## 디렉토리 구조 (루트)

```
android-playground/
├── CLAUDE.md                 ← 이 파일 (인덱스 + 빠른 시작)
├── README.md                 ← 사용자용
├── docs/                     ← 내부 문서 + 스크린샷 + 아이콘 원본
│   ├── STACK.md              기술 스택·패키지 구조
│   ├── RELEASE.md            릴리스·데이터 유지 업데이트
│   ├── VERIFICATION.md       검증 방법·기록
│   ├── app-icon.png          아이콘 원본 1024×1024
│   └── 01-search.png … 06-notification.png
├── dist/stock-alarm-debug.apk   ← 배포용 APK (git 추적)
├── settings.gradle.kts
├── build.gradle.kts          ← AGP/Kotlin/Serialization/KSP 플러그인
├── gradle.properties
├── gradlew / gradlew.bat
└── app/                      ← 자세한 패키지 구조는 docs/STACK.md
```

## 알려진 제약 / 함정

- **에뮬레이터 창은 GUI**라 macOS 데스크탑에 뜸. 헤드리스 원하면 `emulator ... -no-window`.
- **Gradle 9는 AGP 8.3.2와 호환 안 됨**. wrapper `distributionUrl`은 `gradle-8.7-bin.zip` 고정.
- **ANDROID_HOME이 심볼릭 링크** (`~/Library/Android/sdk` → `/opt/homebrew/share/android-commandlinetools`). brew 업그레이드 시 대부분 자동 복원되지만 깨지면 `rm -rf ~/Library/Android/sdk && ln -s /opt/homebrew/share/android-commandlinetools ~/Library/Android/sdk`.
- **첫 빌드는 1-2분**. 이후엔 캐시돼서 빠름.
- sdkmanager 라이선스는 전부 수락돼 있음. 새 컴포넌트 추가 시 `yes | sdkmanager --licenses`.

## 클로디를 위한 작업 메모

- **UI 변경 검증**: 빌드→설치→실행 후 반드시 `screencap`으로 실제 렌더링 확인 (Compose preview 없음).
- **uiautomator dump로 좌표 먼저**: Compose 화면 탭할 땐 `adb shell uiautomator dump /sdcard/win.xml && adb pull /sdcard/win.xml /tmp/win.xml` → grep으로 bounds 확인 → `adb shell input tap`. 시각적 추정은 자주 빗나감.
- **코드 스타일**: Compose 함수 PascalCase, 파일당 하나의 최상위 Composable 권장. 수동 DI(ServiceLocator) 사용, Hilt 도입 안 함.
- **디자인/아키텍처 변경은 루루와 상의 후**. 버그 수정은 즉시 진행 OK.
- **`adb uninstall` 금지 (평상시)**: Room DB 전부 날아감. 업데이트는 `./gradlew installDebug`로.
- **릴리스 전 체크**: `app/build.gradle.kts`의 `versionCode` 증가 필수. 안 올리면 실기기 업데이트 거부됨.
- **수정한 파일 Write 후에도 디스크에 반영 안 될 때가 있었음 (세션 1회)**: Write 직후 Read로 검증하는 습관. 이전 세션에서 `MainActivity.kt` 덮어쓰기가 한 번 실패했던 이력.
