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

private data class ArchivedOrder(val id: String, val client: String, val service: String, val amount: Double, val status: String)

@Composable
fun ArchiveScreen() {
    val db = remember { FirebaseFirestore.getInstance() }
    var rows by remember { mutableStateOf<List<ArchivedOrder>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Все") }
    DisposableEffect(Unit) {
        val listener: ListenerRegistration = db.collection("orders").addSnapshotListener { snapshot, e ->
            if (e != null) error = e.localizedMessage ?: "Не удалось загрузить архив"
            rows = snapshot?.documents?.mapNotNull { d ->
                val state = d.getString("status") ?: "новый"
                if (state.equals("выполнен", true) || state.equals("отменён", true))
                    ArchivedOrder(d.id, d.getString("clientName") ?: d.getString("client") ?: "Без клиента", d.getString("service") ?: "", (d.get("amount") as? Number)?.toDouble() ?: 0.0, state)
                else null
            } ?: emptyList()
        }
        onDispose { listener.remove() }
    }
    val visibleRows = rows.filter { order ->
        (statusFilter == "Все" || order.status.equals(statusFilter, true)) &&
            (search.isBlank() || order.client.contains(search, true) || order.service.contains(search, true))
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Архив заказов", style = MaterialTheme.typography.headlineSmall)
        Text("Выполненные и отменённые заказы", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(search, { search = it }, label = { Text("Поиск по клиенту или изделию") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Все", "выполнен", "отменён").forEach { state ->
                FilterChip(selected = statusFilter == state, onClick = { statusFilter = state }, label = { Text(state) })
            }
        }
        Text("Найдено: ${visibleRows.size}", style = MaterialTheme.typography.labelMedium)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (visibleRows.isEmpty()) Text("Заказы не найдены", modifier = Modifier.padding(top = 24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visibleRows, key = { it.id }) { order ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(order.client, style = MaterialTheme.typography.titleMedium)
                        Text(order.service)
                        Text("${order.amount} ₽ · ${order.status}")
                    }
                }
            }
        }
    }
}
