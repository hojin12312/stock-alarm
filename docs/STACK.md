# STACK.md — 스택 · 패키지 구조 · 데이터 소스

`android-playground/`에 들어 있는 **주식 알리미** 앱의 기술 스택과 내부 구조 상세.
빠른 개요는 `CLAUDE.md`, 배포/업데이트는 `docs/RELEASE.md`, 검증 방법은 `docs/VERIFICATION.md` 참고.

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
- 시스템 `gradle`(brew 9.4.1) 금지. 항상 `./gradlew` wrapper.
- Apple Silicon이라 system image는 **arm64-v8a** 필수 (x86은 매우 느림).

환경변수는 `~/.zshrc`에 등록돼 있어 새 세션에 자동 적용:
```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

## 주요 의존성

| 분류 | 라이브러리 |
|---|---|
| 언어·UI | Kotlin 1.9.23 + Jetpack Compose (BOM 2024.05.00) + Material3 + material-icons-extended |
| Navigation | androidx.navigation:navigation-compose:2.7.7 |
| Lifecycle | lifecycle-viewmodel-compose / runtime-compose 2.7.0 |
| 네트워킹 | Retrofit 2.11 + OkHttp 4.12 + logging-interceptor + kotlinx.serialization 1.6.3 + retrofit2-kotlinx-serialization-converter 1.0.0 |
| 로컬 DB | androidx.room 2.6.1 (runtime/ktx/compiler via KSP 1.9.23-1.0.20) |
| 백그라운드 | androidx.work:work-runtime-ktx 2.9.0 |
| 보안·설정 | androidx.security:security-crypto 1.1.0-alpha06 (EncryptedSharedPreferences) + androidx.datastore:datastore-preferences 1.1.1 |
| 차트 | **외부 라이브러리 0** — Compose `Canvas` + `Path`/`PathEffect` 직접 렌더링 |

## 패키지 구조 (app/src/main/java/com/example/playground)

```
com.example.playground
├── PlaygroundApp.kt                 Application, 알림 채널 + WorkManager 15분 enqueue
├── MainActivity.kt                  setContent { PlaygroundApp() } + 알림 권한 요청
├── di/ServiceLocator.kt             싱글톤 그래프 (DB·Repository·Notifier·데이터 소스·KIS 스택·AppSettings)
├── data/
│   ├── model/                       MaStatus, Market, StockSearchResult, WatchedStock, ChartData
│   ├── local/                       Room (AppDatabase, WatchlistDao, WatchlistEntity, Converters)
│   ├── remote/
│   │   ├── YahooFinanceApi.kt       Retrofit 인터페이스 (search, chart)
│   │   ├── NetworkModule.kt         OkHttp(User-Agent, redact 헤더, logging) + Retrofit + Json
│   │   └── dto/                     SearchDto, ChartDto (kotlinx.serialization)
│   ├── prefs/AppSettings.kt         DataStore 기반 설정 (dataSourceId, kisTokenExpiresAt)
│   ├── source/
│   │   ├── StockDataSource.kt       추상 인터페이스 (fetchCloses, fetchChart) + DataSourceId
│   │   ├── YahooFinanceDataSource.kt  Yahoo 구현체 (검색도 여기서 담당)
│   │   └── kis/
│   │       ├── KisApi.kt            Retrofit 인터페이스 (토큰·국내·해외)
│   │       ├── KisNetworkModule.kt  KIS 전용 OkHttp/Retrofit, 민감 헤더 redact
│   │       ├── KisDataSource.kt     심볼 분기 + 토큰 캐시 경유 호출
│   │       ├── KisCredentialStore.kt EncryptedSharedPreferences로 AppKey/Secret
│   │       ├── KisTokenStore.kt     메모리 전용 토큰 + DataStore에 만료 epoch만
│   │       ├── KisSymbolMapper.kt   005930.KS → J/코드, AAPL → NAS/NYS/AMS
│   │       └── dto/                 KisTokenDto, KisDailyPriceDto, KisOverseaDailyDto
│   └── repo/StockRepository.kt      검색(Yahoo 고정) + 활성 소스 라우팅
├── domain/MaCalculator.kt           5/20일 이평선 + movingAverageSeries (순수 함수)
├── notification/Notifier.kt         알림 채널 + notifyCrossover()
├── worker/MaCrossoverWorker.kt      CoroutineWorker, 주기 갱신·알림 (활성 소스 자동 반영)
└── ui/
    ├── theme/                       Color.kt, Theme.kt
    ├── nav/
    │   ├── Destinations.kt          BottomBar 탭 enum (Search/Watchlist/Dashboard/Settings)
    │   └── PlaygroundNavHost.kt     NavHost + BottomBar + chart/{symbol} 라우트 + NavRoutes 객체
    ├── search/                      SearchScreen + SearchViewModel
    ├── watchlist/                   WatchlistScreen + WatchlistViewModel (카드 탭 → 차트)
    ├── dashboard/                   DashboardScreen + DashboardViewModel + [지금 새로고침]
    ├── settings/                    SettingsScreen + SettingsViewModel (데이터 소스·KIS 키 관리)
    └── chart/                       ChartScreen + ChartViewModel (Compose Canvas 라인 차트)
```

### 리소스 (app/src/main/res)

```
res/
├── drawable/
│   └── ic_launcher_background.xml   단색 흰 배경 (Adaptive Icon용)
├── mipmap-mdpi/ …/mipmap-xxxhdpi/
│   ├── ic_launcher.png              48~192 px legacy 아이콘
│   ├── ic_launcher_round.png        legacy round (동일 이미지)
│   └── ic_launcher_foreground.png   108~432 px Adaptive foreground
├── mipmap-anydpi-v26/
│   ├── ic_launcher.xml              <adaptive-icon> background + foreground
│   └── ic_launcher_round.xml        동일
└── xml/
    ├── network_security_config.xml  cleartext 차단 + Yahoo·KIS 도메인만 허용
    └── data_extraction_rules.xml    cloud-backup / device-transfer 전체 exclude
```

## 기본 정보

- **package / applicationId**: `com.example.playground`
- **앱 이름 (라벨)**: `주식 알리미`
- **minSdk**: 26, **compileSdk / targetSdk**: 34
- **UI 방식**: Jetpack Compose + Material3 (XML 레이아웃 사용 안 함)
- **DI**: 수동 ServiceLocator (Hilt 도입 안 함)

## 데이터 소스 (선택 가능)

`Settings` 탭에서 사용자가 고른 소스로 시세·차트·15분 워커가 라우팅된다. **종목 검색은 항상 Yahoo 고정** — 증권사 Open API 대부분이 종목 검색을 제공하지 않기 때문. 선택 결과는 `DataStore`의 `dataSourceId`에 저장되고, `StockRepository`가 provider 람다(`ServiceLocator.provideActiveDataSource`)로 매 호출마다 반영한다.

### 1. Yahoo Finance (기본, 비공식 API)

무료·API 키 불필요, 한·미 주식 동시 지원.

- **검색**: `GET https://query2.finance.yahoo.com/v1/finance/search?q={query}&quotesCount=10&lang=ko-KR`
- **일봉 차트**: `GET https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?range=3mo&interval=1d`
- **공통 헤더**: `User-Agent: Mozilla/5.0 ...` (OkHttp Interceptor에서 주입)
- **심볼 예시**: 삼성전자 `005930.KS`, Apple `AAPL`
- 4xx/네트워크 실패는 Repository가 `Result` 래핑으로 삼킴 → Worker는 다음 주기 재시도.
- 비공식 API라 스키마가 언제든 변할 수 있고 일부 통신사/국가에서 차단 가능.

### 2. 한국투자증권 KIS Open API (v0.3.0 신규)

공식 REST API. 사용자가 AppKey/AppSecret을 발급받아 Settings에서 입력해야 동작한다.

- **Base URL**: `https://openapi.koreainvestment.com:9443/`
- **토큰**: `POST /oauth2/tokenP` → `access_token` (기본 24시간 유효)
- **국내 일봉**: `GET /uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice`
  - `tr_id=FHKST03010100`, `FID_COND_MRKT_DIV_CODE=J`, 기간은 `FID_INPUT_DATE_1/2`
- **해외 일봉**: `GET /uapi/overseas-price/v1/quotations/dailyprice`
  - `tr_id=HHDFS76240000`, `EXCD=NAS/NYS/AMS`
- **심볼 변환**: `KisSymbolMapper`가 Yahoo 심볼(`005930.KS`, `AAPL`)을 KIS 파라미터로 변환. 미국 거래소는 `WatchlistEntity.exchange` 힌트로 판별.
- **한도**: 초당 20건, 일 20만건 (15분 주기 워커 + 소수 관심목록에는 여유).

### 보안 (KIS 관련)

- **AppKey/AppSecret**: `EncryptedSharedPreferences`(`AES256_SIV`(키) / `AES256_GCM`(값), Android Keystore 기반 MasterKey)에 저장. 평문 디스크 기록 없음.
- **access_token**: **디스크에 절대 저장하지 않음**. 프로세스 생존 동안만 `@Volatile` in-memory. 만료 epoch만 DataStore에 기록해서 재시작 직후 "즉시 만료"로 판단, 다음 호출에서 재발급.
- **OkHttp 로깅 redact**: Yahoo·KIS 양쪽 `HttpLoggingInterceptor`가 `Authorization`/`appkey`/`appsecret` 헤더를 `██`로 마스킹.
- **네트워크 하드닝**: `usesCleartextTraffic=false`, `network_security_config.xml`에서 Yahoo·KIS 도메인만 TLS 허용.
- **백업·기기 이전 차단**: `allowBackup=false` + `data_extraction_rules.xml`에서 `cloud-backup`·`device-transfer` 전체 exclude.
- **입력 검증**: "저장 & 테스트" 버튼이 즉시 토큰 발급을 시도 → 성공 시에만 키 저장. 최소 길이(16자) 체크.

## 매수/매도 라벨 정의

> **5MA < 20MA → 매수** · **5MA > 20MA → 매도**

역추세 / 평균회귀 관점이며 company.txt 요구사항 원문 그대로. 일반 골든크로스와 반대 방향인 점 주의.

## 기능 목록 (v0.3.0)

- 🔍 주식 검색 (한·미 동시, 항상 Yahoo 고정)
- ⭐ 관심목록 추가/삭제
- 📈 5/20MA 교차 감지 (15분 주기 WorkManager)
- 🔔 매수/매도 전환 시 푸시 알림
- 🗂 대시보드 + 필터 (전체/매수/매도 + 종목명 검색)
- 📊 차트 디테일 (종가·5MA·20MA 3라인, Compose Canvas, 1mo/3mo/6mo/1y 토글)
- ⚙ **데이터 소스 선택** — Yahoo Finance / 한국투자증권(KIS) Open API (v0.3.0 신규)
- 🔐 KIS AppKey/Secret 암호화 저장 + 저장 전 토큰 발급 검증 (v0.3.0 신규)
- 🎨 앱 아이콘 (녹색 차트 + 원화 동전)

## 백그라운드 동작 제약

WorkManager `Periodic 15분`은 **최소 주기**이고 정확한 보장이 아님.

- **Doze 모드**, **App Standby**, 제조사별 **절전 정책**(샤오미·삼성 등)으로 실제 실행은 더 늦어질 수 있음.
- 사용자가 앱을 **강제 종료**하면 WorkManager 작업도 같이 죽을 수 있음 (특히 일부 OEM).
- 분 단위 정확한 알림이 필요하면 `AlarmManager.setExactAndAllowWhileIdle` + `SCHEDULE_EXACT_ALARM` 권한, 또는 Foreground Service / 서버 FCM 푸시로 격상 필요.
- 종목 10개 기준 하루 약 18 MB 데이터. 배터리·CPU는 무시할 수준.
- 장 외 시간에도 계속 돌지만 종가가 변하지 않으면 DB upsert는 동일 값이라 알림은 0건. 다만 Doze 로 지연된 워커가 장 마감 후에 교차를 뒤늦게 감지해 새벽 알림이 튀는 케이스가 있어, v0.4.8 부터 정규장 시간대에만 푸시를 발송한다 (`data/model/MarketHours.kt::isOpenNow`). DB 상태 자체는 그대로 갱신돼 개장 직후 첫 워커가 밀린 교차를 정상 감지. 휴장일은 반영하지 않고 주말 + 정규장 시간만 체크 — 휴장일엔 알림이 안 뜨는 쪽이라 치명도 낮음. DST 는 `ZoneId.of("America/New_York")` 가 자동 처리.
