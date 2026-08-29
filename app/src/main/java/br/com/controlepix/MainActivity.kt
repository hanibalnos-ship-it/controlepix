package br.com.controlepix

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.controlepix.data.PixDatabase
import br.com.controlepix.notification.PixNotificationListenerService
import br.com.controlepix.notification.PixNotificationParser
import br.com.controlepix.ui.PixControlApp
import br.com.controlepix.ui.PixUiState
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val database by lazy { PixDatabase(applicationContext) }
    private val executor = Executors.newSingleThreadExecutor()
    private var uiState by mutableStateOf(PixUiState())

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PixNotificationListenerService.ACTION_PIX_UPDATED) {
                loadDashboard()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PixControlApp(
                state = uiState,
                onOpenNotificationSettings = {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                onRefresh = { loadDashboard() },
                onAddManual = { amountText, bank -> addManual(amountText, bank) },
                onDelete = { receipt ->
                    executor.execute {
                        database.deleteReceipt(receipt.id)
                        runOnUiThread { loadDashboard() }
                    }
                }
            )
        }

        registerUpdateReceiver()
        loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    override fun onDestroy() {
        unregisterReceiver(updateReceiver)
        executor.shutdown()
        database.close()
        super.onDestroy()
    }

    private fun registerUpdateReceiver() {
        val filter = IntentFilter(PixNotificationListenerService.ACTION_PIX_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(updateReceiver, filter)
        }
    }

    private fun loadDashboard() {
        val enabled = isNotificationAccessEnabled()
        executor.execute {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val tomorrow = today.plusDays(1)
            val monthStart = today.with(TemporalAdjusters.firstDayOfMonth())
            val nextMonthStart = monthStart.plusMonths(1)

            val todaySummary = database.getSummary(
                today.atStartOfDay(zone).toInstant().toEpochMilli(),
                tomorrow.atStartOfDay(zone).toInstant().toEpochMilli()
            )
            val monthSummary = database.getSummary(
                monthStart.atStartOfDay(zone).toInstant().toEpochMilli(),
                nextMonthStart.atStartOfDay(zone).toInstant().toEpochMilli()
            )
            val receipts = database.getReceipts()

            runOnUiThread {
                uiState = PixUiState(
                    today = todaySummary,
                    month = monthSummary,
                    receipts = receipts,
                    notificationAccessEnabled = enabled,
                    loading = false
                )
            }
        }
    }

    private fun addManual(amountText: String, bank: String): Boolean {
        val cents = PixNotificationParser.parseBrazilianMoneyToCents(amountText) ?: return false
        if (cents <= 0) return false

        executor.execute {
            database.insertReceipt(
                amountCents = cents,
                bank = bank.trim().ifBlank { "Manual" },
                receivedAt = System.currentTimeMillis(),
                rawText = null,
                sourcePackage = null,
                manual = true,
                eventKey = "manual:${UUID.randomUUID()}"
            )
            runOnUiThread { loadDashboard() }
        }
        return true
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val listeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return listeners.split(":").any { flattened ->
            val component = ComponentName.unflattenFromString(flattened)
            component?.packageName == packageName
        }
    }
}
