# CONTINUE.md — 다음 세션 이어받기

## 목표
지수/시장 지표 탭 추가 — 금·환율·코스피·코스닥·나스닥·다우존스 카드 → 차트 진입 (MA/RSI 동일 적용).

## 현재 상태

### 완료 (이번 세션)
- v0.5.2: RSI 전략 매도 조건 정확히 구현
  - `QuantSnapshot.status: MaStatus?` (null = 중립/대기)
  - BUY: `close > SMA(200) && RSI(2) < 10`
  - SELL: `close > SMA(200) && RSI(2) > 70`
  - `ChartSignals.rsiSellIndices()` 추가, RSI 타임라인 빨간 바 추가
  - 범례 "매수 신호 / 매도 신호"로 통일
- v0.5.3: 차트 범위·신호 계산 분리
  - 항상 5y 데이터 fetch → RSI/SMA200 전체 계산
  - `ChartData.displayCount` + `display*` 슬라이스 프로퍼티
  - `ChartViewModel.fullData` 캐시: range 전환 시 네트워크 재요청 없음
  - 1mo 범위에서도 RSI 타임라인 바 정상 표시

### 미완료 (다음 세션 작업)
지수/시장 지표 탭 신규 기능.

## 다음 세션 작업 목록

### 1. 지수 탭 데이터 정의
Yahoo Finance 심볼:
| 항목 | 심볼 |
|---|---|
| 코스피 | `^KS11` |
| 코스닥 | `^KQ11` |
| 나스닥 | `^IXIC` |
| 다우존스 | `^DJI` |
| S&P 500 | `^GSPC` |
| 금 선물 | `GC=F` |
| USD/KRW 환율 | `USDKRW=X` |

- 고정 목록 (관심목록에 추가 불필요)
- `data/model/MarketIndex.kt` 신규: `symbol`, `name`, `market` 정도

### 2. 네비게이션 탭 추가
현재 탭: 검색 / 관심목록 / 대시보드 / 설정 (4개)
추가할 탭: **지수** (검색과 관심목록 사이, 또는 대시보드 오른쪽) — 호진님과 위치 협의

### 3. 지수 목록 화면 (`ui/market/MarketScreen.kt`)
- 각 지수를 카드로 표시: 이름, 현재가, 전일 대비 등락률
- 카드 탭 → 기존 `ChartScreen` 그대로 진입 (MA/RSI 알고리즘 동일 적용)
- 데이터 fetch: `StockRepository.fetchChart(symbol, "5y")` 그대로 재사용 가능

### 4. 현재가 표시용 API
지수 현재가는 차트와 별도로 빠르게 가져와야 함.
- `StockRepository.fetchSnapshot()` 또는 `fetchChart(range="1d")` 로 마지막 값만 추출
- 혹은 차트 진입 시에만 로딩해도 충분 — 카드에 현재가 표시가 필요한지 호진님과 협의

### 5. 단위 처리
- 환율(USDKRW=X): 소수점 2자리 (1,450.23)
- 지수: 정수 또는 소수점 2자리
- 현재 `formatNumber()`가 1000 이상이면 정수 처리 → 그대로 쓰면 됨

## 설계 결정 사항 (다음 세션에서 협의)
1. **지수 탭 위치**: 4번째 탭(설정 왼쪽)? 아니면 기존 탭 구조 재배치?
2. **카드에 현재가 표시 여부**: 진입 전 미리 로딩 vs 차트 진입 시에만
3. **WorkManager 알림 적용 여부**: 지수에도 MA/RSI 알림 줄지 (초기엔 안 해도 될 듯)

## 변경된 파일 (v0.5.2~v0.5.3)
- `app/build.gradle.kts` — versionCode 22, versionName 0.5.3
- `domain/QuantCalculator.kt` — `QuantSnapshot.status: MaStatus?`, 3상태 로직
- `domain/ChartSignals.kt` — `rsiSellIndices()` 추가
- `ui/chart/SignalTimelineBar.kt` — RSI 빨간 바 + display 슬라이스
- `ui/chart/ChartContent.kt` — 범례 "매수 신호 / 매도 신호", displayTimestamps
- `data/model/ChartData.kt` — `displayCount` + `display*` 프로퍼티 추가
- `ui/chart/ChartViewModel.kt` — `fullData` 캐시 + `calcDisplayCount()` (초 단위)
- `ui/chart/LineChartCanvas.kt` — display 슬라이스 사용
- `dist/stock-alarm-debug.apk`, `dist/version.json`

## 커밋 상태
`main` 브랜치, push 완료. 최신 커밋: `v0.5.3: 차트 범위와 신호 계산 분리`
