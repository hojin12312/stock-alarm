# CONTINUE — android-playground (Stock Alarm)

## 목표

차트 핀치줌/스크롤 실기기 검증. 코드는 완성 상태, 에뮬레이터 adb swipe 미반응 이슈만 남음.

---

## 현재 상태 (2026-04-21 기준)

### ✅ 완료

| 기능 | 상태 |
|---|---|
| 지수 탭 Pull-to-Refresh | **완전 동작** (에뮬레이터 검증 완료) |
| `ChartData.displayOffset` + `subList` 기반 윈도우 슬라이싱 | 완료, 컴파일 성공 |
| `ChartContent` 핀치줌/스크롤 state + `detectTransformGestures` | 코드 완성, 컴파일 성공 |
| v0.5.5 빌드 + dist/ 업데이트 + 커밋 | **완료** (commit `d35b9ab`) |

### ⚠️ 미확인

- **차트 제스처 에뮬레이터 미반응**: `adb shell input swipe` 명령에 `detectTransformGestures` 콜백이 호출되지 않음.
  - logcat에서 터치 이벤트(ACTION_DOWN/UP) 자체는 생성됨 확인
  - Chart Card bounds `[42,912][1038,1752]` 안으로 좌표 보냄 → 그래도 미반응
  - 시도한 것: `detectTransformGestures`, `detectDragGestures`, `awaitPointerEventScope(PointerEventPass.Initial)` — 모두 미반응
  - **가설**: `adb shell input swipe`가 Compose의 `ACTION_MOVE` 연속 스트림을 제대로 생성 못할 수 있음. 실기기에서는 정상 동작 가능성 높음.

---

## 다음 세션 작업

### 우선순위 1: 실기기 테스트
실기기에 v0.5.5 APK 설치 후:
1. 차트 화면 진입 → 두 손가락 핀치 → 줌인/아웃 동작 확인
2. 차트 화면 → 한 손가락 좌우 드래그 → 과거/미래 이동 확인
3. range 칩 변경 후 줌·스크롤 초기화 확인
4. 지수 탭 당겨서 새로고침 확인

### 우선순위 2: 에뮬레이터에서 안 되면
- `Modifier.transformable` + `TransformableState` 대안 시도 (detectTransformGestures와 다른 API)
- 또는 `@Preview` 없이 직접 컴포저블에 로그 찍어 state 변화 추적

### 우선순위 3: 다음 기능 논의 (이전 CONTINUE 2번 항목)
> 사용자 알림 커스터마이징 (알림 임계값, 종목별 ON/OFF) — 별도 논의 후 착수

---

## 변경된 파일 목록 (v0.5.5)

| 파일 | 변경 내용 |
|---|---|
| `data/model/ChartData.kt` | `displayOffset: Int = 0` 추가, `display*` subList 기반 슬라이싱 |
| `ui/chart/ChartContent.kt` | `zoomedCount`/`scrollOffset` state, `detectTransformGestures`, `displayData` 계산 |
| `ui/market/MarketViewModel.kt` | `_isRefreshing` StateFlow, `loadAll()` joinAll 구조 |
| `ui/market/MarketScreen.kt` | `PullToRefreshContainer` + `nestedScrollConnection` |
| `app/build.gradle.kts` | versionCode 24, versionName 0.5.5 |
| `dist/stock-alarm-debug.apk` | v0.5.5 APK |
| `dist/version.json` | versionCode 24 |

## 커밋 상태

- 로컬: `main` 브랜치, 최신 커밋 `d35b9ab` (origin보다 4개 앞섬)
- push: **미완료** — 호진님 확인 후 push
