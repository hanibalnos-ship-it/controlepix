package br.com.controlepix.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PixDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE receipts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount_cents INTEGER NOT NULL,
                bank TEXT NOT NULL,
                received_at INTEGER NOT NULL,
                raw_text TEXT,
                source_package TEXT,
                manual INTEGER NOT NULL DEFAULT 0,
                event_key TEXT NOT NULL UNIQUE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_receipts_received_at ON receipts(received_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Primeira versão do banco. Migrações futuras entram aqui.
    }

    fun insertReceipt(
        amountCents: Long,
        bank: String,
        receivedAt: Long,
        rawText: String?,
        sourcePackage: String?,
        manual: Boolean,
        eventKey: String
    ): Boolean {
        if (amountCents <= 0) return false

        val values = ContentValues().apply {
            put("amount_cents", amountCents)
            put("bank", bank)
            put("received_at", receivedAt)
            put("raw_text", rawText)
            put("source_package", sourcePackage)
            put("manual", if (manual) 1 else 0)
            put("event_key", eventKey)
        }

        val result = writableDatabase.insertWithOnConflict(
            "receipts",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
        return result != -1L
    }

    fun deleteReceipt(id: Long) {
        writableDatabase.delete("receipts", "id = ?", arrayOf(id.toString()))
    }

    fun getReceipts(limit: Int = 250): List<PixReceipt> {
        val result = mutableListOf<PixReceipt>()
        readableDatabase.query(
            "receipts",
            arrayOf(
                "id", "amount_cents", "bank", "received_at", "raw_text",
                "source_package", "manual", "event_key"
            ),
            null,
            null,
            null,
            null,
            "received_at DESC",
            limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += PixReceipt(
                    id = cursor.getLong(0),
                    amountCents = cursor.getLong(1),
                    bank = cursor.getString(2),
                    receivedAt = cursor.getLong(3),
                    rawText = cursor.getString(4),
                    sourcePackage = cursor.getString(5),
                    manual = cursor.getInt(6) == 1,
                    eventKey = cursor.getString(7)
                )
            }
        }
        return result
    }

    fun getSummary(startMillis: Long, endMillis: Long): PixSummary {
        readableDatabase.rawQuery(
            """
            SELECT COALESCE(SUM(amount_cents), 0), COUNT(*)
            FROM receipts
            WHERE received_at >= ? AND received_at < ?
            """.trimIndent(),
            arrayOf(startMillis.toString(), endMillis.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) {
                PixSummary(
                    totalCents = cursor.getLong(0),
                    count = cursor.getInt(1)
                )
            } else {
                PixSummary()
            }
        }
    }

    companion object {
        private const val DB_NAME = "controle_pix.db"
        private const val DB_VERSION = 1
    }
}
