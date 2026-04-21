# CONTINUE.md — 다음 세션 이어받기

## 목표
RSI(2)+SMA(200) 전략의 **정확한 매도 조건**을 조사하고, 앱 전체에 반영한다.

## 현재 상태

### 완료
- v0.5.1 릴리스됨 (versionCode 20)
- 버그 수정 3종 완료 (v0.5.0): 차트 진입 알고리즘 초기화, RSI 타임라인 회색, x축 불일치
- RSI 범례 "RSI 매수 신호" 분기 (v0.5.1) — 임시 조치. 다음 세션에서 교체 예정.

### 미완료 (다음 세션 작업)
RSI 전략의 매도 조건이 잘못 구현되어 있음.

**현재 코드** (`QuantCalculator.compute()`):
```kotlin
val status = if (lastClose > sma200 && rsi2 < 10) MaStatus.BUY else MaStatus.SELL
```
→ "매수 조건 아닌 것 = 전부 SELL"로 단순화됨. 실제 알고리즘과 다름.

**호진님 확인 사항**: 카드에 매도/매수 시그널 모두 잡힌다고 함 → 현재 폴백 로직으로도 SELL 감지는 되지만, 원래 전략의 진짜 매도 조건이 아님.

## 다음 세션 작업 목록

### 1. RSI(2)+SMA(200) 전략 매도 조건 조사
- Larry Connors 원저 또는 신뢰할 수 있는 출처에서 정확한 매도 조건 확인
- 유력한 후보: `RSI(2) > 90` 또는 `RSI(2) > 95`
- 호진님이 "카드에 매도 시그널 잡힌다"고 했으므로 현재 else SELL 동작이 어떻게 트리거되는지도 같이 파악

### 2. `QuantCalculator.kt` 수정
```
app/src/main/java/com/example/playground/domain/QuantCalculator.kt
```
- `compute()` 매도 조건을 정확한 조건으로 교체
- 매수/매도/중립 3가지 상태가 필요할 수 있음 (현재 MaStatus는 BUY/SELL 2가지뿐)
  → MaStatus에 NEUTRAL 추가 검토 or null로 처리

### 3. `ChartSignals.kt` 매도 인덱스 추가
```
app/src/main/java/com/example/playground/domain/ChartSignals.kt
```
- `rsiSellIndices(closes, sma200Series, rsi2Series): List<Int>` 추가
- 조건: 조사된 정확한 매도 조건 반영

### 4. `SignalTimelineBar.kt` RSI 타임라인 빨간 바 추가
```
app/src/main/java/com/example/playground/ui/chart/SignalTimelineBar.kt
```
- `RsiSignalTimelineBar`에 `rsiSellIndices` 기반 빨간 세로 바 추가
- 현재 녹색(매수 신호)만 있음 → 녹색(매수) + 빨간(매도) 세로 바

### 5. `ChartContent.kt` 범례 원복
```
app/src/main/java/com/example/playground/ui/chart/ChartContent.kt
```
- v0.5.1에서 임시로 분기한 "RSI 매수 신호" 범례 → 적절한 범례로 교체
- RSI도 매수/매도 구간이 생기므로 "매수 구간 / 매도 구간"으로 통일 가능

## 변경된 파일 (v0.5.0~v0.5.1)
- `app/build.gradle.kts` — versionCode 20, versionName 0.5.1
- `ui/nav/PlaygroundNavHost.kt` — NavRoutes comma-separated algos
- `ui/dashboard/DashboardScreen.kt` — onStockClick Set<AlgorithmType>
- `ui/dashboard/DashboardViewModel.kt` — chartAlgorithmType 제거
- `ui/chart/ChartScreen.kt` — initialAlgorithms: Set<AlgorithmType>
- `ui/chart/ChartViewModel.kt` — algorithmType 파라미터 제거
- `ui/chart/SignalTimelineBar.kt` — TimelineBarBox 오버레이 구조 + RSI 데이터부족 안내
- `ui/chart/ChartContent.kt` — RSI 범례 분기 (임시)
- `dist/stock-alarm-debug.apk`, `dist/version.json`

## 커밋 상태
`main` 브랜치, push 완료. 최신 커밋: `v0.5.1: RSI 범례 'RSI 매수 신호'로 분기`
