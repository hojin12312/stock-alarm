package com.example.playground.domain

import com.example.playground.data.model.MaStatus

data class MaSnapshot(
    val ma5: Double,
    val ma20: Double,
    val status: MaStatus,
)

object MaCalculator {
    fun compute(closes: List<Double>): MaSnapshot? {
        if (closes.size < 20) return null
        val recent20 = closes.takeLast(20)
        val recent5 = closes.takeLast(5)
        val ma20 = recent20.average()
        val ma5 = recent5.average()
        val status = if (ma5 < ma20) MaStatus.BUY else MaStatus.SELL
        return MaSnapshot(ma5 = ma5, ma20 = ma20, status = status)
    }
}
