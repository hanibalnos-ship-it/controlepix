package br.com.controlepix.notification

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import br.com.controlepix.data.PixDatabase
import java.util.concurrent.Executors

class PixNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(" ") { it.toString() }

        val rawText = listOfNotNull(title, text, bigText, lines)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" | ")

        val parsed = PixNotificationParser.parse(sbn.packageName, rawText) ?: return

        executor.execute {
            val inserted = PixDatabase(applicationContext).use { db ->
                db.insertReceipt(
                    amountCents = parsed.amountCents,
                    bank = parsed.bank,
                    receivedAt = sbn.postTime,
                    rawText = rawText,
                    sourcePackage = sbn.packageName,
                    manual = false,
                    eventKey = "notification:${sbn.key}"
                )
            }

            if (inserted) {
                sendBroadcast(
                    Intent(ACTION_PIX_UPDATED)
                        .setPackage(packageName)
                )
            }
        }
    }

    companion object {
        const val ACTION_PIX_UPDATED = "br.com.controlepix.PIX_UPDATED"
        private val executor = Executors.newSingleThreadExecutor()
    }
}
