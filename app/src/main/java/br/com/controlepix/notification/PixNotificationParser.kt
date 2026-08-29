package br.com.controlepix.notification

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer

object PixNotificationParser {

    data class ParsedPix(
        val amountCents: Long,
        val bank: String
    )

    private val amountPatterns = listOf(
        Regex("""R\$\s*([0-9]{1,3}(?:\.[0-9]{3})*,[0-9]{2}|[0-9]+,[0-9]{2})""", RegexOption.IGNORE_CASE),
        Regex("""BRL\s*([0-9]{1,3}(?:\.[0-9]{3})*,[0-9]{2}|[0-9]+,[0-9]{2})""", RegexOption.IGNORE_CASE)
    )

    private val positiveSignals = listOf(
        "recebeu",
        "recebido",
        "recebemos",
        "pix recebido",
        "pix que voce recebeu",
        "entrada pix",
        "transferencia pix recebida",
        "credito pix",
        "creditado"
    )

    private val negativeSignals = listOf(
        "pix enviado",
        "voce enviou",
        "enviado com sucesso",
        "pagamento realizado",
        "pix agendado",
        "agendamento",
        "debitado"
    )

    fun parse(packageName: String, rawText: String): ParsedPix? {
        if (rawText.isBlank()) return null

        val normalized = normalize(rawText)
        if (!normalized.contains("pix")) return null
        if (negativeSignals.any(normalized::contains)) return null
        if (positiveSignals.none(normalized::contains)) return null

        val amountText = amountPatterns.firstNotNullOfOrNull { regex ->
            regex.find(rawText)?.groupValues?.getOrNull(1)
        } ?: return null

        val cents = parseBrazilianMoneyToCents(amountText) ?: return null
        if (cents <= 0) return null

        return ParsedPix(
            amountCents = cents,
            bank = bankNameFromPackage(packageName)
        )
    }

    fun parseBrazilianMoneyToCents(value: String): Long? {
        return try {
            val cleaned = value.trim()
                .replace(Regex("[^0-9,.]"), "")

            if (cleaned.isBlank()) return null

            val normalized = when {
                // Formato brasileiro: 1.234,56 ou 150,00
                cleaned.contains(',') -> cleaned.replace(".", "").replace(",", ".")
                // Também aceita digitação manual com ponto decimal: 150.00
                cleaned.matches(Regex("[0-9]+\\.[0-9]{1,2}")) -> cleaned
                // Ponto sem casas decimais claras é tratado como separador de milhar.
                cleaned.contains('.') -> cleaned.replace(".", "")
                else -> cleaned
            }

            BigDecimal(normalized)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    fun bankNameFromPackage(packageName: String): String {
        val p = packageName.lowercase()
        return when {
            "nu" in p && ("nubank" in p || "production" in p) -> "Nubank"
            "mercadopago" in p -> "Mercado Pago"
            "intermedium" in p || "bancointer" in p -> "Inter"
            "itau" in p -> "Itaú"
            "santander" in p -> "Santander"
            "bb.android" in p || "bancodobrasil" in p -> "Banco do Brasil"
            "caixa" in p -> "Caixa"
            "picpay" in p -> "PicPay"
            "pagbank" in p || "pagseguro" in p -> "PagBank"
            "sicredi" in p -> "Sicredi"
            "sicoob" in p -> "Sicoob"
            "bradesco" in p -> "Bradesco"
            "stone" in p -> "Stone"
            else -> "Banco/App"
        }
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
