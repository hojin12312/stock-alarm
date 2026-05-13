# CONTINUE — android-playground (Stock Alarm)

## 상태 요약

v0.5.7 릴리스 완료 · origin/main push 완료 · GitHub Release v0.5.7 등록 완료 · 에뮬레이터 UI 검증 통과.
다음 작업은 새 주제(알림 후속 / 차트 UX / 다른 기능) — **새 세션 권장**.

---

## v0.5.7 결과 (2026-05-13)

### ✅ 추가된 기능

- **알림 옵션 토글 2개** (`Settings` 화면 새 섹션)
  - `MA 5/20 교차 알림` — 디폴트 ON
  - `5MA 극점 알림` — 디폴트 OFF
  - 마지막 하나 끄려고 하면 Snackbar `"알림 옵션은 최소 하나는 켜둬야 해"` + 상태 유지

- **5MA 극점 감지 로직** (B 방식: 워커 호출 간 기울기)
  - 워커가 종목별 SMA5 를 매 호출 시 샘플링
  - `prevSlope = lastMa5 − prevPrevMa5`, `currentSlope = newMa5 − lastMa5`
  - `prevSlope * currentSlope ≤ 0` 이면 극점 판정
  - 새 기울기 부호 ≥ 0 → 저점(LOW), < 0 → 고점(HIGH)
  - 종목 시장 시간대(KR=KST, US=ET) 기준 **하루 1회** 가드
  - 장 시각 게이팅(`isOpenNow()`)은 기존 정책 그대로 적용

- **알림 탭**
  - 필터 칩 `5MA 극점` 추가
  - 카드 라벨: `5MA 저점` / `5MA 고점`, LOW=buy 색, HIGH=sell 색
  - 매수/매도 필터는 BUY+LOW / SELL+HIGH 양쪽 포함

### ✅ 에뮬레이터 검증

- DB 마이그레이션 4→5 정상 (`prevPrevMa5`, `lastExtremaNotifyDate` 컬럼 추가 확인)
- 알림 옵션 UI 정상 표시
- Switch 토글 정상, "최소 하나" 가드 Snackbar 정상
- 디폴트 상태(MA ON, 극점 OFF) 복원 가능

### ⚠️ 미검증 (실제 시그널 트리거)

- 5MA 극점 알림 실제 발송은 **실시간 시세 변동 + 장중 워커 2회 이상**이 있어야 트리거됨
- 현재 코드 경로(로직 → DAO 업데이트 → Notifier 호출)는 컴파일·매핑 모두 통과
- 향후 실제 알림 발송 테스트는 장중에 관심목록 종목 변동 관찰 필요

---

## 배포 상태

| 항목 | 상태 |
|---|---|
| 로컬 커밋 | `f47789d` (HEAD) |
| origin/main push | ✅ 완료 |
| GitHub Release v0.5.7 | ✅ 등록 (APK 첨부) — https://github.com/hojin12312/stock-alarm/releases/tag/v0.5.7 |
| `dist/stock-alarm-debug.apk` | v0.5.7 (versionCode 26) |
| `dist/version.json` | versionCode 26, 노트 갱신 |

---

## 다음 후보 작업

### 1. 알림 임계값 커스터마이징 (이전 세션부터 보류 중)
- MA 교차 알림: `5MA<20MA` 비율 임계값 조절 (예: 0.1% 이상 차이일 때만)
- 종목별 알림 ON/OFF 토글 (관심목록 카드에 추가)
- 시장별 알림 강도 (한국만 vs 미국만)
- 별도 설계 논의 후 착수

### 2. 5MA 극점 알림 실측 확인 (선택, 장중)
- 관심목록 종목 1~2개로 장중에 워커 2회 이상 돌아가는 상황 관찰
- `adb logcat -s MaCrossoverWorker:I` 로 `extrema=N` 카운트 확인
- 실제 알림 발송 + 알림 탭 카드 표시 확인

### 3. 차트 UX 후속
- 핀치줌 후 range 칩 자동 동기화 (v0.5.5 부터 보류 중)
- 5MA 극점 발생 시점을 차트에 마커로 오버레이?

### 4. 알림 탭 정렬·검색
- 종목별 그룹핑? 검색바?

---

## v0.5.7 관련 파일 (참고)

| 파일 | 역할 |
|---|---|
| `data/prefs/AppSettings.kt` | `maCrossNotifyEnabled` / `maExtremaNotifyEnabled` 키 + getter/setter |
| `data/local/AppDatabase.kt` | `MIGRATION_4_5` (prevPrevMa5, lastExtremaNotifyDate) |
| `data/local/WatchlistEntity.kt` | 새 컬럼 2개 |
| `data/local/WatchlistDao.kt` | `updateSnapshot` 시그니처 확장 + `markExtremaNotified` |
| `data/model/MarketHours.kt` | `Market.todayLocalDate()` 시간대 헬퍼 |
| `data/repo/StockRepository.kt` | `refreshSnapshot` 극점 감지 + `ExtremaDirection` enum |
| `notification/Notifier.kt` | `notifyMa5Extrema` + `Ma5ExtremaDirection` enum |
| `worker/MaCrossoverWorker.kt` | 설정 분기 + 극점 알림 + 하루 1회 가드 |
| `ui/dashboard/DashboardViewModel.kt` | `refreshNow` 도 동일 분기 (Factory 에 settings 추가) |
| `ui/settings/SettingsViewModel.kt` | 알림 옵션 state + setter 가드 |
| `ui/settings/SettingsScreen.kt` | `NotifyOptionRow` + 카드 섹션 |
| `ui/notification/NotificationSheet.kt` | `5MA 극점` 필터 칩 + 카드 라벨/색 매핑 |
| `ui/notification/NotificationViewModel.kt` | BUY+LOW / SELL+HIGH 필터 결합 |

---

**새 세션에서 `@CONTINUE.md` 로 이어가면 돼!**
