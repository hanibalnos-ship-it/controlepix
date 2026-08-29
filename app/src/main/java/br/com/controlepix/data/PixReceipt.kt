package br.com.controlepix.data

data class PixReceipt(
    val id: Long,
    val amountCents: Long,
    val bank: String,
    val receivedAt: Long,
    val rawText: String?,
    val sourcePackage: String?,
    val manual: Boolean,
    val eventKey: String
)

data class PixSummary(
    val totalCents: Long = 0,
    val count: Int = 0
)
