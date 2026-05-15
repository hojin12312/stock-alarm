# CONTINUE — android-playground (Stock Alarm)

## 상태 요약

v0.5.8 릴리스 완료 · origin/main push 완료 · GitHub Release v0.5.8 등록 완료.
다음 작업은 새 주제(알림 후속 / 차트 UX / 다른 기능) — **새 세션 권장**.

---

## v0.5.8 결과 (2026-05-15)

### 수정 내용

**5MA 극점 알림 오탐 수정** (`YahooFinanceDataSource.fetchCloses`)

- **원인**: Yahoo Finance API는 장중 호출 시 오늘의 미확정 봉(현재가)을 closes 리스트 마지막에 포함해서 반환함. 이로 인해 MA5 기울기가 실제 확정 종가 기준과 달리 계산되어, 5일선이 실제로 꺾이지 않았는데도 `prevSlope × currSlope ≤ 0` 조건이 만족되어 저점/고점 알림이 잘못 발송됨.
- **수정**: `result.timestamp`와 close를 인덱스로 매핑 후, 마켓 시간대(KR=Asia/Seoul, US=America/New_York) 기준 오늘 날짜 이후 타임스탬프를 가진 봉은 `filterNotNull()` 전에 제외. 확정 종가만 MA5 계산에 사용.
- **KIS DataSource**: 일봉 전용 API라 미확정 봉 문제가 없음 — 수정 불필요.

### 변경 파일

| 파일 | 변경 내용 |
|---|---|
| `data/source/YahooFinanceDataSource.kt` | `fetchCloses`: timestamp 기반 오늘 봉 제외 로직 추가 |
| `app/build.gradle.kts` | versionCode 26 → 27, versionName 0.5.7 → 0.5.8 |
| `dist/stock-alarm-debug.apk` | v0.5.8 (versionCode 27) |
| `dist/version.json` | versionCode 27, 노트 갱신 |

---

## 배포 상태

| 항목 | 상태 |
|---|---|
| 로컬 커밋 | `5ed53f1` (HEAD) |
| origin/main push | 완료 |
| GitHub Release v0.5.8 | 등록 (APK 첨부) — https://github.com/hojin12312/stock-alarm/releases/tag/v0.5.8 |
| `dist/stock-alarm-debug.apk` | v0.5.8 (versionCode 27) |
| `dist/version.json` | versionCode 27, 노트 갱신 |

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
- v0.5.8 수정 후 오탐이 없어졌는지 실제 알림 발송 여부 확인

### 3. 차트 UX 후속
- 핀치줌 후 range 칩 자동 동기화 (v0.5.5 부터 보류 중)
- 5MA 극점 발생 시점을 차트에 마커로 오버레이?

### 4. 알림 탭 정렬·검색
- 종목별 그룹핑? 검색바?

---

**새 세션에서 `@CONTINUE.md` 로 이어가면 돼!**
