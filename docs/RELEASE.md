# RELEASE.md — 버전 관리 · 데이터 유지 업데이트 · GitHub 레포

빌드 산출물을 어떻게 배포하고, 실기기에서 **기존 관심목록을 유지한 채** 앱을 업데이트하는 방법.

## 공식 레포

**https://github.com/hojin12312/stock-alarm** (public)

| 경로 | 설명 |
|---|---|
| `dist/stock-alarm-debug.apk` | 최신 debug APK (raw URL로 바로 다운로드 가능) |
| `dist/version.json` | 자동 업데이트 메타 (앱이 시작 시 raw URL로 폴링) |
| `docs/app-icon.png` | 아이콘 원본 (1024x1024) |
| `docs/01-search.png … 06-notification.png` | 스크린샷 |
| `README.md` | 사용자용 설치·업데이트 가이드 |

## 버전 히스토리

| 버전 | versionCode | 주요 내용 |
|---|---|---|
| `v0.1.0` | 1 | 초기: 검색·관심목록·대시보드·알림 |
| `v0.2.0` | 2 | 차트 디테일 + 앱 아이콘 + versionCode 관리 |
| `v0.3.0` | 3 | 데이터 소스 선택(Yahoo / KIS) + Settings 탭 + 키 암호화 저장 + 네트워크 하드닝 |
| `v0.3.1` | 4 | 검색 query1 핫픽스 + 자동 업데이트 (앱 시작 시 dist/version.json 폴링) |
| `v0.3.2` | 5 | 대시보드 필터 UI 개선: 매수/매도 드롭다운 + 시장별(전체/미국/한국) 탭 |
| `v0.3.3` | 6 | 설정에 수동 업데이트 확인 버튼 + 현재 버전 표시 |
| `v0.3.4` | 7 | 대시보드 필터 배치 교체 + 검색 placeholder 수정 |
| `v0.3.5` | 8 | 업데이트 체크 시 HTTP 캐시 무시 (최신 version.json 확실히 읽기) |
| `v0.4.0` | 9 | RSI(2)+SMA(200) 듀얼 알고리즘 전략 추가 |
| `v0.4.1` | 10 | 앱 내 알림 센터 추가 |
| `v0.4.2` | 11 | 알림 필터 + RSI 탭 차트 연동 버그 수정 |
| `v0.4.3` | 12 | RSI 탭 차트 초기 range 1y 자동 설정 (대기 버그 수정) |
| `v0.4.4` | 13 | 알고리즘 체크리스트 필터 + 교집합 모드 |
| `v0.4.5` | 14 | 리팩토링 (사용자 동작 불변) |
| `v0.4.6` | 15 | UX 개선 세트: 다크 모드 지원, 차트 range 통일(+2y/5y), 검색 에러 재시도, KIS 키 보이기 토글, 알림 탭 날짜 그룹핑 |
| `v0.4.7` | 16 | 앱 내 업데이트 다이얼로그에 "전체 업데이트 히스토리" 링크 추가 (외부 브라우저) + GitHub Releases 도입 |
| `v0.4.8` | 17 | 장외 시간 알림 차단 (한국/미국 정규장 시간에만 푸시), 필터 체크리스트에서 알고리즘 토글 시 매수/매도 선택 유지, 차트 상세에 과거 매수/매도 구간 + 전환 마커(MA) / 매수 시그널 점(RSI) 오버레이 추가 |
| `v0.4.9` | 18 | 차트 상세에 알고리즘(MA/RSI) 토글 추가. 과거 매수/매도 구간은 차트 본체 대신 아래쪽 알고리즘별 한 줄짜리 타임라인 바로 분리 표시 (두 알고리즘 선택 시 나란히 두 줄) |
| `v0.5.0` | 19 | 차트 진입 알고리즘 초기화 버그 3종 수정 (알고리즘 상태 유지, RSI 타임라인 회색 처리, 차트/타임라인 x축 정렬) |
| `v0.5.1` | 20 | RSI 타임라인 범례 "RSI 매수 신호"로 분기 |
| `v0.5.2` | 21 | RSI 전략 매도 조건 정식 구현 (BUY `RSI<10` / SELL `RSI>70` / 중립 3상태) |
| `v0.5.3` | 22 | 차트 범위·신호 계산 분리 (항상 5y fetch, range는 표시 윈도우만 조정) |
| `v0.5.4` | 23 | 지수 탭 신설 (코스피·코스닥·나스닥·다우·S&P 500·금·USD/KRW 7개) |
| `v0.5.5` | 24 | 지수 탭 Pull-to-Refresh + 차트 핀치줌/드래그 스크롤 (`detectTransformGestures`) |
| `v0.5.6` | 25 | 배율 높은 폰에서 차트 하단 매수/매도 타임라인 바가 잘려 보이지 않던 문제 수정 (ChartContent `verticalScroll`) + 대시보드 상단 UI 스크롤 가능 (Column → LazyColumn 통합) |
| `v0.5.7` | **26** | 알림 옵션 추가 (MA 5/20 교차 알림 ON 기본, 5MA 극점 알림 OFF 기본, 최소 하나 가드). 5MA 극점 = 워커 호출 간 5MA 기울기 부호 변경 시점, 시장 시간대 기준 하루 최대 1회 |

`v0.4.7` 부터는 [GitHub Releases](https://github.com/hojin12312/stock-alarm/releases) 에도 버전별 상세 노트가 등록된다. 이전 버전(`v0.4.6` 이하)은 이 표로만 확인.

버전 값은 `app/build.gradle.kts`의 `defaultConfig`:
```kotlin
versionCode = 26
versionName = "0.5.7"
```

## 릴리스 절차 (새 버전 올릴 때)

1. **versionCode 증가** — `app/build.gradle.kts`에서 반드시 +1. 안 올리면 실기기 업데이트 거부됨.
2. **versionName 갱신** — semver 느낌으로 `0.2.0 → 0.3.0` 등.
3. **빌드 + 검증** — `./gradlew installDebug` → 에뮬레이터에서 주요 플로우 screencap.
4. **APK 복사** — `cp app/build/outputs/apk/debug/app-debug.apk dist/stock-alarm-debug.apk`
5. **`dist/version.json` 갱신** — `versionCode`, `versionName`, `notes`를 새 릴리스 값으로. **APK와 같은 커밋**으로 묶어야 raw URL 캐시 미스매치를 피한다.
6. **README / docs 업데이트** — 새 기능 반영, 스크린샷 교체. `docs/RELEASE.md` 의 버전 히스토리 표에도 한 줄 추가.
7. **커밋 + 푸시** — 커밋 메시지에 버전 명시.
8. **GitHub Release 등록** — 푸시 후 태그 생성 + APK 첨부. 앱 내 "전체 업데이트 히스토리" 링크가 이 페이지(`/releases`)를 가리킨다.

```bash
cd ~/home_apps/android-playground
./gradlew installDebug
cp app/build/outputs/apk/debug/app-debug.apk dist/stock-alarm-debug.apk
# dist/version.json 손으로 수정 (versionCode, versionName, notes)
git add -A
git commit -m "v0.4.7: <changes>"
git push

# GitHub Release 생성 (태그 자동 생성됨)
gh release create v0.4.7 dist/stock-alarm-debug.apk \
  --title "v0.4.7" \
  --notes "$(cat <<'EOF'
## 변경 사항
- <bullet>
- <bullet>

---
이전 버전(v0.4.6 이하)의 변경 이력은 [docs/RELEASE.md 버전 히스토리 표](https://github.com/hojin12312/stock-alarm/blob/main/docs/RELEASE.md#버전-히스토리)에서 확인할 수 있어.
EOF
)"
```

> 자동 업데이트 흐름: 사용자가 앱을 켜면 `MainActivity`가 백그라운드로 `dist/version.json`을 raw URL로 가져와 `BuildConfig.VERSION_CODE`와 비교 → 더 큰 값이면 다이얼로그 → DownloadManager로 APK 다운로드 → 시스템 설치 화면. 사용자가 "설치" 한 번 더 탭해야 함 (Android 보안상 완전 무인 설치는 불가).

## ⭐ 관심목록을 유지하면서 업데이트하기

**핵심**: `adb uninstall` 후 재설치하면 Room DB(관심목록·이평선 상태)가 **전부 날아간다**. 아래 방법을 써야 보존됨.

### 원리

- `adb install -r <apk>` — `-r` 플래그가 "replace existing app, preserve data" 의미. 같은 서명 키면 기존 앱을 덮어쓰고 `/data/data/com.example.playground/databases/playground.db`는 그대로.
- `./gradlew installDebug` — 내부적으로 `adb install -r`을 호출. 개발 머신에서 가장 편한 방법. **검증됨**: 검증 기록(`docs/VERIFICATION.md`) 참고.
- 파일 매니저 APK 탭 설치 — "업데이트" 선택 시 동일 효과. "삭제 후 설치" 옵션은 절대 금지.

### 실기기 전송 시나리오

**A. USB/WiFi adb 연결이 있을 때** (가장 확실):
```bash
adb install -r stock-alarm-debug.apk
```

**B. 사이드로딩 (APK 파일만 전송)**:
1. 폰 브라우저로 `https://raw.githubusercontent.com/hojin12312/stock-alarm/main/dist/stock-alarm-debug.apk` 다운로드
2. 설정 → 알 수 없는 출처 허용
3. 파일 매니저에서 APK 탭 → **"업데이트"** 선택 (삭제 옵션 X)

**C. 개발 머신 → 에뮬레이터**:
```bash
./gradlew installDebug
```

### 업데이트 거부되는 경우

| 에러 | 원인 | 해결 |
|---|---|---|
| 버전이 낮거나 같음 | `versionCode` 안 올라감 | `app/build.gradle.kts`에서 `versionCode` 증가 |
| 서명 불일치 (INSTALL_FAILED_UPDATE_INCOMPATIBLE) | 다른 머신에서 빌드된 APK → 디버그 키 다름 | 같은 머신에서 빌드한 APK만 사용. 정식 배포는 release keystore 필요 |
| 제조사 절전 정책 | 앱은 설치됐지만 WorkManager 주기가 안 돎 | 설정 → 배터리 → 제한 없음 |

### 디버그 키 vs Release 키

- 현재 APK는 `~/.android/debug.keystore`로 서명됨. 같은 머신에서 빌드하면 항상 같은 키.
- 머신이 바뀌거나 keystore가 삭제되면 새 키 → 기존 설치본과 **서명 불일치** → 어쩔 수 없이 uninstall 필요.
- 영구적 배포(여러 머신에서 빌드, 정식 배포)가 필요하면 release keystore를 따로 만들어 git에는 안 올리고 별도 보관 후 `signingConfigs`에 연결.

## dist/ 디렉토리

`.gitignore`에서 `build/`, `.gradle/` 등은 제외하지만 `dist/`는 명시적으로 추적됨:
```
!dist/
!dist/**
```

APK를 커밋하는 이유: 사용자가 raw URL 하나로 바로 받을 수 있게. GitHub Releases 방식도 있지만 현재는 in-repo 커밋 방식 유지.

## 주의

- **배포 전 버전·스크린샷 확인**: 새 기능 화면 screencap을 `docs/`에 덮어쓰고 README에 반영.
- **Yahoo Finance 스키마 변경**: 외부 API라 언젠가 깨질 수 있음. Repository layer에서 모든 네트워크 실패를 `Result` 래핑으로 삼키므로 앱 크래시는 안 나지만 대시보드가 "갱신 안 됨" 상태가 될 수 있음. 디버깅은 `adb logcat -s StockRepository OkHttp`로.
