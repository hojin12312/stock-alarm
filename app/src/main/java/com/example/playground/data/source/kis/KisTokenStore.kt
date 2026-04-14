package com.example.playground.data.source.kis

import com.example.playground.data.prefs.AppSettings
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * KIS access_token 보관소.
 *
 * 보안 원칙:
 * - 토큰 원문은 **디스크에 절대 저장하지 않는다**. 프로세스 생존 기간 동안만 in-memory 유지.
 * - 만료 시각(epoch ms)만 일반 DataStore에 기록해서 재시작 후 "즉시 만료 상태"로 판단.
 * - 앱 프로세스가 재시작되면 항상 재발급한다.
 */
class KisTokenStore(
    private val settings: AppSettings,
) {
    @Volatile private var cachedToken: String? = null
    @Volatile private var cachedExpiresAt: Long = 0L
    private val mutex = Mutex()

    /** 유효한 토큰 반환. 없으면 [issue]로 발급. */
    suspend fun getValidToken(issue: suspend () -> IssuedToken): String = mutex.withLock {
        val now = System.currentTimeMillis()
        val current = cachedToken
        if (current != null && now < cachedExpiresAt - SAFETY_MARGIN_MS) {
            return current
        }
        val issued = issue()
        cachedToken = issued.token
        cachedExpiresAt = issued.expiresAtEpochMs
        settings.setKisTokenExpiresAt(issued.expiresAtEpochMs)
        issued.token
    }

    suspend fun invalidate() = mutex.withLock {
        cachedToken = null
        cachedExpiresAt = 0L
        settings.setKisTokenExpiresAt(0L)
    }

    /** 이미 발급받은 토큰을 캐시에 심는다(예: 검증 단계에서 받은 토큰 재활용). */
    suspend fun seed(issued: IssuedToken) = mutex.withLock {
        cachedToken = issued.token
        cachedExpiresAt = issued.expiresAtEpochMs
        settings.setKisTokenExpiresAt(issued.expiresAtEpochMs)
    }

    data class IssuedToken(val token: String, val expiresAtEpochMs: Long)

    companion object {
        private const val SAFETY_MARGIN_MS = 60_000L
    }
}
