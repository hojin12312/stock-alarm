package com.example.playground.domain

import com.example.playground.data.model.AlgorithmType
import com.example.playground.data.model.MaStatus

// 선택된 단일 알고리즘 기준으로 표시할 상태 결정. 차트·대시보드 공용.
fun resolveDisplayStatus(
    algorithm: AlgorithmType,
    maStatus: MaStatus?,
    quantStatus: MaStatus?,
): MaStatus? = when (algorithm) {
    AlgorithmType.MA_CROSS -> maStatus
    AlgorithmType.RSI_SMA200 -> quantStatus
}
