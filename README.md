# 주식 알리미 (Stock Alarm) ✨

한국·미국 주식의 5일/20일 이동평균 교차를 감지해 푸시 알림을 보내주는 안드로이드 네이티브 앱.
Android Studio GUI 없이 **터미널만으로** 편집·빌드·설치·검증한 Jetpack Compose 프로젝트.

## 기능

- 🔍 **주식 검색** — 한국(KOSPI/KOSDAQ)·미국(NASDAQ/NYSE) 종목 동시 검색
- ⭐ **관심목록** — 검색한 종목을 추가/삭제, 시장(KR/US) 칩 표시
- 📈 **이평선 교차 감지** — 15분 주기로 종가 갱신 후 5MA / 20MA 대소 관계 변화 감지
- 🔔 **푸시 알림** — 매수/매도 전환 순간에 알림 발송
- 🗂 **대시보드** — 관심 종목 현재 상태 + 매수/매도/종목명 필터

### 매수/매도 정의

> 5MA < 20MA → **매수**  ·  5MA > 20MA → **매도**
>
> (역추세 / 평균회귀 관점)

## 빠른 설치 (APK 사이드로딩)

1. [`dist/stock-alarm-debug.apk`](dist/stock-alarm-debug.apk) 다운로드
2. 안드로이드 기기 설정 → **알 수 없는 출처 허용**
3. APK 파일을 탭하면 설치
4. 첫 실행 시 **알림 권한** 허용

> 디버그 키로 서명된 APK라 처음 설치 시 보안 경고가 나올 수 있어. 정식 배포용은
> 별도 keystore로 release 빌드가 필요해.

## 기술 스택

| 분류 | 사용 |
|---|---|
| 언어·UI | Kotlin 1.9.23 + Jetpack Compose (BOM 2024.05.00) + Material3 |
| 빌드 | Gradle 8.7 (wrapper) + AGP 8.3.2, JDK 17 |
| 아키텍처 | MVVM + 수동 DI(ServiceLocator) — Hilt 사용 안 함 |
| 네트워킹 | Retrofit 2.11 + OkHttp 4.12 + kotlinx.serialization |
| 로컬 DB | Room 2.6.1 (KSP) |
| 백그라운드 | WorkManager 2.9 (Periodic 15분) |
| 데이터 소스 | **Yahoo Finance 비공식 API** (`query1/query2.finance.yahoo.com`) |
| 최소 SDK | 26 (Android 8.0) / target 34 (Android 14) |

### 패키지 구조

```
com.example.playground
├── PlaygroundApp                   Application, 알림 채널 + WorkManager 15분 enqueue
├── MainActivity                    setContent { PlaygroundApp() } + 알림 권한 요청
├── di/ServiceLocator               OkHttp/Retrofit/Room/Repository/Notifier 싱글톤
├── data/
│   ├── model/                       MaStatus, Market, StockSearchResult, WatchedStock
│   ├── local/                       Room (AppDatabase, WatchlistDao, Converters)
│   ├── remote/                      YahooFinanceApi + NetworkModule + DTO
│   └── repo/StockRepository         검색·차트·이평선 갱신·교차 판정
├── domain/MaCalculator             5/20일 이평선 순수 함수
├── notification/Notifier           알림 채널 + 교차 전환 알림
├── worker/MaCrossoverWorker        CoroutineWorker, 주기 갱신·알림
└── ui/
    ├── nav/                         3탭 BottomBar Navigation
    ├── search/                      검색 화면 + ViewModel
    ├── watchlist/                   관심목록 화면 + ViewModel
    └── dashboard/                   대시보드 + 필터 + [지금 새로고침]
```

## 빌드 (소스에서)

```bash
git clone https://github.com/hojin12312/stock-alarm.git
cd stock-alarm

# 환경 변수: JAVA_HOME=JDK17, ANDROID_HOME=Android SDK
./gradlew installDebug
adb shell am start -n com.example.playground/.MainActivity
```

산출물: `app/build/outputs/apk/debug/app-debug.apk`

## 백그라운드 동작에 대한 솔직한 노트 ⚠️

WorkManager `Periodic 15분`은 **최소 주기**이고 정확한 보장이 아니다.

- **Doze 모드**, **App Standby**, 제조사별 **절전 정책**(샤오미·삼성 등)으로 실제 실행은 더 늦어질 수 있다.
- 사용자가 앱을 **강제 종료**하면 WorkManager 작업도 같이 죽는다 (특히 일부 OEM).
- 분 단위 정확한 알림이 필요하면 `AlarmManager.setExactAndAllowWhileIdle` + `SCHEDULE_EXACT_ALARM` 권한, 또는 Foreground Service / 서버 FCM 푸시로 격상해야 한다.
- Yahoo Finance는 **비공식 API**라 스키마가 변할 수 있다. 안정성이 중요하면 한국투자증권 KIS OpenAPI 등 공식 소스로 교체 고려.

## 라이선스

본 저장소의 코드는 학습·개인 사용 목적으로 자유롭게 활용 가능. Yahoo Finance API 이용 약관은 별도.
