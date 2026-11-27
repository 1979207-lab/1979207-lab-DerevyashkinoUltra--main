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

private data class OrderRow22(
    val id: String, val client: String, val service: String,
    val amount: Double, val status: String, val payment: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(canEdit: Boolean) {
    val db = remember { FirebaseFirestore.getInstance() }
    var rows by remember { mutableStateOf<List<OrderRow22>>(emptyList()) }
    var client by remember { mutableStateOf("") }
    var service by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Все") }
    var paymentFilter by remember { mutableStateOf("Все") }
    var search by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val registration: ListenerRegistration = db.collection("orders").addSnapshotListener { snapshot, error ->
            if (error != null) message = error.localizedMessage ?: "Ошибка загрузки заказов"
            rows = snapshot?.documents?.map { d ->
                OrderRow22(
                    d.id,
                    d.getString("clientName") ?: d.getString("client") ?: "Без клиента",
                    d.getString("service") ?: "",
                    (d.get("amount") as? Number)?.toDouble() ?: 0.0,
                    d.getString("status") ?: "новый",
                    d.getString("payment") ?: if (d.getBoolean("paid") == true) "оплачено" else "не оплачено"
                )
            } ?: emptyList()
        }
        onDispose { registration.remove() }
    }

    val visible = rows.filter { order ->
        (statusFilter == "Все" || order.status.equals(statusFilter, true)) &&
        (paymentFilter == "Все" || (paymentFilter == "Оплачено" && order.payment.equals("оплачено", true)) ||
            (paymentFilter == "Не оплачено" && !order.payment.equals("оплачено", true))) &&
        (search.isBlank() || order.client.contains(search, true) || order.service.contains(search, true))
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Заказы", style = MaterialTheme.typography.headlineSmall)
        if (canEdit) {
            Text("Новый заказ", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(client, { client = it }, label = { Text("Клиент") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(service, { service = it }, label = { Text("Услуга / изделие") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(amount, { amount = it }, label = { Text("Сумма, ₽") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(
                enabled = client.isNotBlank() && service.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                onClick = {
                    val data = mapOf(
                        "clientName" to client.trim(), "service" to service.trim(),
                        "amount" to amount.toDouble(), "status" to "новый",
                        "payment" to "не оплачено", "createdAt" to Date()
                    )
                    db.collection("orders").add(data)
                        .addOnSuccessListener { client = ""; service = ""; amount = ""; message = "Заказ добавлен" }
                        .addOnFailureListener { message = it.localizedMessage ?: "Не удалось сохранить заказ" }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Добавить заказ") }
        }
        OutlinedTextField(search, { search = it }, label = { Text("Поиск по клиенту или изделию") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Статус", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Все", "новый", "в работе", "выполнен", "отменён").forEach { f ->
                FilterChip(selected = statusFilter == f, onClick = { statusFilter = f }, label = { Text(f) })
            }
        }
        Text("Оплата", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Все", "Оплачено", "Не оплачено").forEach { f ->
                FilterChip(selected = paymentFilter == f, onClick = { paymentFilter = f }, label = { Text(f) })
            }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
        Text("Найдено: ${visible.size}", style = MaterialTheme.typography.labelMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(visible, key = { it.id }) { order ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(order.client, style = MaterialTheme.typography.titleMedium)
                        Text(order.service)
                        Text("${"%.2f".format(order.amount)} ₽ · ${order.status}")
                        Text(
                            if (order.payment.equals("оплачено", true)) "Оплата: Оплачено" else "Оплата: Не оплачено",
                            color = if (order.payment.equals("оплачено", true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        if (canEdit) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    val next = if (order.payment.equals("оплачено", true)) "не оплачено" else "оплачено"
                                    db.collection("orders").document(order.id).update("payment", next)
                                        .addOnFailureListener { message = it.localizedMessage ?: "Не удалось изменить оплату" }
                                }) { Text(if (order.payment.equals("оплачено", true)) "Снять оплату" else "Отметить оплаченным") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                listOf("в работе", "выполнен", "отменён").forEach { state ->
                                    TextButton(onClick = {
                                        db.collection("orders").document(order.id).update("status", state)
                                            .addOnFailureListener { message = it.localizedMessage ?: "Не удалось изменить статус" }
                                    }) { Text(state) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
