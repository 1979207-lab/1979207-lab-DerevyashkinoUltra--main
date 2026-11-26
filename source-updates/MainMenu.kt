package com.derevyashkino.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenu(user: AppUser, darkTheme: Boolean, onToggleTheme: () -> Unit, onSignOut: () -> Unit) {
    var selected by remember { mutableStateOf("Заказы") }
    val owner = user.role == "Владелец"
    val admin = user.role == "Админ"
    val tabs = if (owner || admin) listOf("Заказы", "Клиенты", "Финансы", "Чат") else listOf("Заказы", "Клиенты", "Чат")
    Scaffold(topBar = {
        TopAppBar(title = { Column { Text("Деревяшкино 22"); Text("${user.email} · ${user.role}", style = MaterialTheme.typography.labelSmall) } },
            actions = {
                TextButton(onClick = onToggleTheme) { Text(if (darkTheme) "Светлая" else "Тёмная") }
                TextButton(onClick = onSignOut) { Text("Выйти") }
            })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tabs.indexOf(selected).coerceAtLeast(0)) {
                tabs.forEach { tab -> Tab(selected == tab, onClick = { selected = tab }, text = { Text(tab) }) }
            }
            when (selected) {
                "Заказы" -> OrdersScreen(canEdit = owner || admin)
                "Клиенты" -> ClientsScreen(canEdit = owner || admin)
                "Финансы" -> FinanceScreen()
                "Чат" -> ChatScreen(user)
            }
        }
    })
}
