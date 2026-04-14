# VERIFICATION.md — 검증 방법 · 완료 기록

앱 기능이 실제로 동작하는지 에뮬레이터에서 end-to-end로 확인하는 방법과 지금까지 통과한 시나리오.

## AVD 정보

| 속성 | 값 |
|---|---|
| 이름 | `pixel_api34` |
| 디바이스 | Pixel 6 |
| API | 34 (Android 14 "UpsideDownCake") |
| ABI | arm64-v8a, google_apis |
| 경로 | `~/.android/avd/pixel_api34.avd` |
| 해상도 | 1080 × 2400, density 420 |

## 표준 검증 루프

```bash
cd ~/home_apps/android-playground

# 0. 에뮬레이터 기동 (이미 떠 있으면 생략)
nohup emulator -avd pixel_api34 -no-snapshot-save -no-audio > /tmp/emulator.log 2>&1 &
adb wait-for-device && adb shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 2; done; echo ok'

# 1. 빌드 + 설치
./gradlew installDebug

# 2. 앱 실행
adb shell am start -n com.example.playground/.MainActivity

# 3. screencap
adb exec-out screencap -p > /tmp/playground-screen.png
```

## UI 자동 조작 팁

Compose는 View ID가 없어서 `uiautomator dump`로 bounds 좌표를 먼저 찾아야 정확한 탭이 가능:

```bash
# 1. 현재 화면 dump
adb shell uiautomator dump /sdcard/win.xml
adb pull /sdcard/win.xml /tmp/win.xml

# 2. 특정 텍스트의 bounds 추출
grep -o 'text="검색"[^/]*bounds="[^"]*"' /tmp/win.xml

# 3. 중심 좌표 계산 후 탭
adb shell input tap <x> <y>

# 텍스트 입력
adb shell input text "samsung"
adb shell input keyevent KEYCODE_ENTER

# 뒤로 / 홈
adb shell input keyevent KEYCODE_BACK
adb shell input keyevent KEYCODE_HOME
```

시각적 추정으로 좌표 찍으면 자주 빗나감. 항상 dump→좌표 순서 유지.

## DB 직접 조작 (테스트용)

Room DB를 직접 열어 관심목록이나 `lastStatus`를 조작해 알림 트리거를 검증할 때:

```bash
# 관심목록 조회
adb shell "run-as com.example.playground sqlite3 databases/playground.db 'SELECT symbol, lastStatus FROM watchlist;'"

# lastStatus를 강제로 뒤집어서 알림 트리거 (교차 발생 시뮬레이션)
adb shell "run-as com.example.playground sqlite3 databases/playground.db \"UPDATE watchlist SET lastStatus='BUY' WHERE symbol='005930.KS';\""
adb shell "run-as com.example.playground sqlite3 databases/playground.db \"UPDATE watchlist SET lastStatus='SELL' WHERE symbol='207940.KS';\""

# 그 후 앱 force-stop → 재실행 → 대시보드 [지금 새로고침] 누르면
# 실제 계산된 상태가 다르므로 교차로 인식 → 알림 발송
```

## 알림 셰이드 펼치기

알림 발송 검증 후 실제로 떴는지 확인:

```bash
adb shell cmd statusbar expand-notifications
adb exec-out screencap -p > /tmp/notifications.png
```

## 로그

```bash
# 앱 + 네트워크만
adb logcat -s StockRepository MaCrossoverWorker OkHttp

# 전체
adb logcat *:W com.example.playground:V
```

## 2026-04-14 v0.2.0 검증 완료 시나리오

에뮬레이터(`pixel_api34`)에서 아래 전부 통과:

### 기본 플로우
1. `./gradlew installDebug` → BUILD SUCCESSFUL ✅
2. 앱 콜드 스타트 → 검색 탭 정상 렌더 ✅
3. "samsung" 검색 → Yahoo Finance API에서 `005930.KS Samsung Electronics`, `207940.KS Samsung Biologics`, `489250.KS ETF` 등 응답 정상 파싱 ✅
4. [+관심] 버튼 → "등록됨"으로 전환, 관심목록 탭에서 KR 칩과 함께 표시 ✅
5. 대시보드 탭 → "대기" 상태 카드 표시 + 우상단 [지금 새로고침] 버튼 ✅
6. [지금 새로고침] → Yahoo 차트 호출 → 5/20MA 계산:
   - Samsung Electronics: 5MA 205,750 / 20MA 192,485 / 종가 207,250 → **매도** 배지 (5MA > 20MA) ✅
   - Samsung Biologics: 5MA 1,567,200 / 20MA 1,569,000 / 종가 1,537,000 → **매수** 배지 ✅

### 알림
7. 교차 알림 검증: `run-as sqlite3`로 `lastStatus` 강제 반전 후 새로고침 → 알림 셰이드에 `[매수 전환] Samsung Biologics`, `[매도 전환] Samsung Electronics` 두 알림이 "주식 알리미" 채널로 정상 표시 ✅
8. WorkManager: `PlaygroundApp.onCreate()`에서 `enqueueUniquePeriodicWork("ma_crossover_periodic", KEEP, 15분)` 등록 ✅

### 차트 (v0.2.0 신규)
9. 관심목록 카드 탭 → `chart/{symbol}` 라우트 → BottomBar 숨김, TopAppBar(뒤로) + 헤더(현재가·5MA·20MA·상태) + Compose Canvas 3라인 렌더링 ✅
10. 기간 토글 `1mo / 3mo / 6mo / 1y` → 데이터 재페치, 1y 선택 시 데이터 243개로 확장 ✅
11. 대시보드 카드 탭 → 동일 차트 화면으로 이동 (Biologics 매수 배지 + 하락 추세 시각화) ✅

### 아이콘 + 데이터 유지 업데이트 (v0.2.0 신규)
12. 런처 드로어에서 **주식 알리미** 앱이 녹색 차트 + 원화 동전 아이콘으로 표시 ✅
13. 아이콘 추가 포함 `./gradlew installDebug`(= `adb install -r`) 후 기존 Samsung Electronics/Biologics 2개 관심목록 엔트리 **그대로 유지됨** ✅ (데이터 유지 업데이트 원리 검증)

## 2026-04-14 v0.3.0 검증 완료 시나리오

v0.2.0 통과분 + 데이터 소스 선택/보안 추가분. 에뮬레이터(`pixel_api34`)에서 아래 통과:

### 빌드 & 회귀
1. `./gradlew assembleDebug` → BUILD SUCCESSFUL, KSP Room 컴파일 정상 ✅
2. `./gradlew installDebug` → `adb install -r`, 기존 v0.2.0 DB(Samsung Electronics/Biologics 2개 관심목록) **그대로 유지** ✅
3. 앱 콜드 스타트 → 4탭 하단 네비게이션(검색/관심목록/대시보드/**설정**) 정상 렌더 ✅

### Settings 탭 + KIS UI
4. 하단 "설정" 탭 탭 → `SettingsScreen` 진입, "데이터 소스" 섹션에 Yahoo Finance(기본) / 한국투자증권(KIS) 라디오 표시 ✅
5. KIS 라디오 탭 → 확장 카드 표시: "KIS 인증 정보" 헤더 + 저장 상태("저장된 키 없음") + AppKey/AppSecret password 입력란 + [저장 & 테스트] 버튼 + 하단 스낵바 "AppKey/Secret을 입력한 뒤 저장해줘" ✅
6. 다시 Yahoo 라디오 탭 → 확장 카드 사라지고 기본 모드로 복귀 ✅

### 회귀 (Yahoo 경로 유지)
7. 검색 탭 → "AAPL" 입력 → Yahoo `v1/finance/search` 응답에서 Apple Inc./IncomeShares Apple/Apple Inc. 3건 파싱 ✅
8. 대시보드 탭 → 기존 Samsung Electronics/Biologics 카드 + 매수/매도 배지 정상 표시 (DB 보존 증명) ✅

### 보안
9. `adb logcat | grep -Ei "appkey|appsecret|authorization"` → 앱 로그에 평문 노출 없음(keystore2 시스템 로그만) ✅
10. `AndroidManifest.xml` 빌드 산출물 검사: `allowBackup=false`, `usesCleartextTraffic=false`, `networkSecurityConfig=@xml/network_security_config`, `dataExtractionRules=@xml/data_extraction_rules` ✅

### 미검증 (실제 KIS 키 필요)
- KIS `/oauth2/tokenP` 실제 발급 성공 → `[저장 & 테스트]` 성공 토스트
- KIS 국내/해외 일봉 호출로 실제 관심목록 스냅샷 갱신
- 위 둘은 루루가 AppKey/Secret 준비한 뒤 Settings에서 "저장 & 테스트" → 대시보드 새로고침으로 확인

## 스크린샷 보관 위치

검증 시 캡처한 대표 화면은 `docs/01-search.png`, `docs/03-watchlist.png`, `docs/04-dashboard.png`, `docs/05-chart.png`, `docs/06-notification.png` 등. README에도 그대로 embed됨.

재검증 시 기존 파일을 덮어쓰고 README의 마크다운 링크는 그대로 유지 (파일명 변경 금지).
