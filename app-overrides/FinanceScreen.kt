package com.derevyashkino.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

private data class FinanceRow22(val id: String, val amount: Double, val type: String, val comment: String)
private data class UnpaidOrder22(val id: String, val client: String, val amount: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen() {
    val db = remember { FirebaseFirestore.getInstance() }
    var amount by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("income") }
    var rows by remember { mutableStateOf<List<FinanceRow22>>(emptyList()) }
    var unpaid by remember { mutableStateOf<List<UnpaidOrder22>>(emptyList()) }
    var error by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val transactionsListener: ListenerRegistration = db.collection("transactions").addSnapshotListener { snap, e ->
            if (e != null) error = e.localizedMessage ?: "Ошибка загрузки финансов"
            rows = snap?.documents?.map {
                FinanceRow22(it.id, (it.get("amount") as? Number)?.toDouble() ?: 0.0,
                    it.getString("type") ?: "expense",
                    it.getString("comment") ?: it.getString("description") ?: "")
            } ?: emptyList()
        }
        val ordersListener: ListenerRegistration = db.collection("orders").addSnapshotListener { snap, e ->
            if (e != null) error = e.localizedMessage ?: "Ошибка загрузки заказов"
            unpaid = snap?.documents?.filter {
                val payment = it.getString("payment") ?: if (it.getBoolean("paid") == true) "оплачено" else "не оплачено"
                !payment.equals("оплачено", true)
            }?.map {
                UnpaidOrder22(it.id, it.getString("clientName") ?: it.getString("client") ?: "Без клиента",
                    (it.get("amount") as? Number)?.toDouble() ?: 0.0)
            } ?: emptyList()
        }
        onDispose { transactionsListener.remove(); ordersListener.remove() }
    }

    val income = rows.filter { it.type == "income" || it.type == "доход" }.sumOf { it.amount }
    val expenses = rows.filter { it.type != "income" && it.type != "доход" }.sumOf { it.amount }
    val unpaidTotal = unpaid.sumOf { it.amount }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Финансы", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Доходы: ${"%.2f".format(income)} ₽")
                Text("Расходы: ${"%.2f".format(expenses)} ₽")
                Text("Баланс: ${"%.2f".format(income - expenses)} ₽", style = MaterialTheme.typography.titleMedium)
                Divider()
                Text("Неоплаченные заказы: ${unpaid.size}")
                Text("К оплате: ${"%.2f".format(unpaidTotal)} ₽", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Новая операция", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(type == "income", { type = "income" }, label = { Text("Доход") })
            FilterChip(type == "expense", { type = "expense" }, label = { Text("Расход") })
        }
        OutlinedTextField(amount, { amount = it }, label = { Text("Сумма") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(comment, { comment = it }, label = { Text("Комментарий") }, modifier = Modifier.fillMaxWidth())
        Button(enabled = (amount.toDoubleOrNull() ?: 0.0) > 0, onClick = {
            db.collection("transactions").add(mapOf("amount" to amount.toDouble(), "type" to type,
                "comment" to comment.trim(), "createdAt" to Date()))
                .addOnSuccessListener { amount = ""; comment = ""; error = "" }
                .addOnFailureListener { error = it.localizedMessage ?: "Ошибка сохранения" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Добавить операцию") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Text("История операций", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rows, key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${r.amount} ₽ · ${if (r.type == "income" || r.type == "доход") "Доход" else "Расход"}")
                        Text(r.comment)
                    }
                }
            }
        }
    }
}
