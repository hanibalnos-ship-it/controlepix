package br.com.controlepix.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.controlepix.data.PixReceipt
import br.com.controlepix.data.PixSummary
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


data class PixUiState(
    val today: PixSummary = PixSummary(),
    val month: PixSummary = PixSummary(),
    val receipts: List<PixReceipt> = emptyList(),
    val notificationAccessEnabled: Boolean = false,
    val loading: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixControlApp(
    state: PixUiState,
    onOpenNotificationSettings: () -> Unit,
    onRefresh: () -> Unit,
    onAddManual: (amountText: String, bank: String) -> Boolean,
    onDelete: (PixReceipt) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Controle Pix", fontWeight = FontWeight.Bold)
                            Text(
                                "Recebimentos no aparelho",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors()
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(2.dp)) }

                item {
                    NotificationAccessCard(
                        enabled = state.notificationAccessEnabled,
                        onOpenSettings = onOpenNotificationSettings
                    )
                }

                item {
                    SummaryCard(
                        title = "Hoje",
                        summary = state.today,
                        highlight = true
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SmallMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Este mês",
                            value = money(state.month.totalCents)
                        )
                        SmallMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Ticket médio hoje",
                            value = if (state.today.count == 0) {
                                money(0)
                            } else {
                                money(state.today.totalCents / state.today.count)
                            }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Últimos recebimentos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onRefresh) {
                            Text("Atualizar")
                        }
                    }
                }

                if (!state.loading && state.receipts.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                Text("Ainda não há recebimentos.", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Libere o acesso às notificações e, quando chegar um Pix compatível, ele aparecerá aqui. Você também pode usar o botão + para lançar manualmente."
                                )
                            }
                        }
                    }
                }

                items(state.receipts, key = { it.id }) { receipt ->
                    ReceiptCard(receipt = receipt, onDelete = { onDelete(receipt) })
                }

                item { Spacer(Modifier.height(88.dp)) }
            }
        }

        if (showAddDialog) {
            AddManualDialog(
                onDismiss = { showAddDialog = false },
                onSave = { amount, bank ->
                    if (onAddManual(amount, bank)) {
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@Composable
private fun NotificationAccessCard(enabled: Boolean, onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (enabled) "Leitura automática ativa" else "Leitura automática desativada",
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (enabled) {
                    "O app está autorizado a analisar notificações e registrar apenas as que parecem ser Pix recebidos."
                } else {
                    "Para somar Pix automaticamente, libere o acesso às notificações para Controle Pix."
                }
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onOpenSettings) {
                Text(if (enabled) "Revisar permissão" else "Liberar acesso")
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, summary: PixSummary, highlight: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(5.dp))
            Text(
                money(summary.totalCents),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${summary.count} recebimento${if (summary.count == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SmallMetricCard(modifier: Modifier, title: String, value: String) {
    Card(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(5.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReceiptCard(receipt: PixReceipt, onDelete: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(receipt.receivedAt).atZone(zone)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm")

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    money(receipt.amountCents),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(receipt.bank, style = MaterialTheme.typography.bodyMedium)
                Text(
                    date.format(formatter) + if (receipt.manual) "  • manual" else "",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            TextButton(onClick = onDelete) {
                Text("Excluir")
            }
        }
    }
}

@Composable
private fun AddManualDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("Manual") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar recebimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = false
                    },
                    label = { Text("Valor (ex.: 150,00)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error,
                    singleLine = true
                )
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("Banco/observação") },
                    singleLine = true
                )
                if (error) {
                    Text("Digite um valor válido maior que zero.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val normalized = amount.trim().replace("R$", "", ignoreCase = true).trim()
                val valid = normalized.isNotBlank()
                if (!valid) {
                    error = true
                } else {
                    onSave(normalized, bank.ifBlank { "Manual" })
                }
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun money(cents: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(cents / 100.0)
}
