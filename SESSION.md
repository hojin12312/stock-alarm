# 다음 세션 작업 메모 (2026-04-16 오후 1시 기준)

## 현재 버전
`v0.4.2` (versionCode 11) — 오늘 릴리스 완료

## 해결할 버그

### RSI 전략 탭 → 카드 탭 → 차트에서 "대기" 표시 (오후 1:10 확인)

**증상**: 대시보드 RSI 전략 탭에서 매수/매도로 표시되는 주식 카드를 탭하면, 차트 화면 상단에 상태가 "대기"로 나옴. 새로고침해도 동일.

**원인 분석 (이미 파악됨)**:
- `ChartViewModel` 기본 range = `"3mo"` (약 61개 데이터)
- `QuantCalculator.compute()`는 **최소 201개** 종가 필요 — 부족하면 `null` 반환
- 3개월 데이터로 SMA200 계산 불가 → `quantSnapshot = null` → 상태 뱃지 "대기"
- `1y` (약 250개)로 수동 전환하면 RSI(2)/SMA200/매수·매도 정상 표시됨

**에뮬레이터 검증 결과**: 3mo="대기", 1y="RSI(2) 100.0 / SMA200 398.01 / 매도" 확인

**수정 방향 (권장)**:
`ChartViewModel`에 `algorithmType` 파라미터 추가 → RSI 모드면 초기 range를 `"1y"`로 설정

```kotlin
// ChartViewModel.kt
class ChartViewModel(
    private val repo: StockRepository,
    private val symbol: String,
    private val algorithmType: AlgorithmType = AlgorithmType.MA_CROSS,
) : ViewModel() {
    private val initialRange = if (algorithmType == AlgorithmType.RSI_SMA200) "1y" else "3mo"
    private val _state = MutableStateFlow(ChartUiState(loading = true, range = initialRange))
    ...
    class Factory(
        private val repo: StockRepository,
        private val symbol: String,
        private val algorithmType: AlgorithmType = AlgorithmType.MA_CROSS,
    ) : ViewModelProvider.Factory { ... }
}
```

`PlaygroundNavHost.kt`에서 ChartViewModel.Factory 생성 시 `algo` 파라미터 그대로 전달하면 됨.

## 관련 파일

| 파일 | 내용 |
|---|---|
| `ui/chart/ChartViewModel.kt` | initialRange 수정 대상 |
| `ui/nav/PlaygroundNavHost.kt` | Factory에 algo 전달 (이미 algo 변수 있음, line ~198) |
| `domain/QuantCalculator.kt` | 최소 201개 조건 (건드리지 않음) |

## 참고: 이번 세션(v0.4.1~v0.4.2) 작업 이력

### v0.4.1
- 앱 내 알림 센터 추가 (종 아이콘 + ModalBottomSheet)
- 개별 스와이프 삭제 / 전체 삭제 버튼
- Room DB v2→v3: notifications 테이블 추가

### v0.4.2
- 알림 필터 추가 (MA/RSI, 매수/매도, 미국/한국 조합 가능)
- Room DB v3→v4: notifications.market 컬럼 추가
- RSI 탭 카드 → 차트 진입 시 헤더가 RSI(2)/SMA200으로 표시되도록 수정 (기존엔 항상 MA 헤더)
