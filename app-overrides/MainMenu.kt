package com.derevyashkino.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class MenuItem(val title: String, val symbol: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenu(user: AppUser, onSignOut: () -> Unit) {
    var selected by remember { mutableStateOf("Заказы") }
    var dark by remember { mutableStateOf(false) }
    val owner = user.role == "Владелец"
    val admin = user.role == "Админ"
    val menu = buildList {
        add(MenuItem("Заказы", "📦"))
        add(MenuItem("Архив", "🗄️"))
        add(MenuItem("Клиенты", "👥"))
        if (owner || admin) add(MenuItem("Финансы", "💰"))
        add(MenuItem("Чат", "💬"))
    }
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Деревяшкино 22")
                            Text("${user.email} · ${user.role}", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        IconButton(onClick = { dark = !dark }) { Text(if (dark) "☀️" else "🌙") }
                        TextButton(onClick = onSignOut) { Text("Выйти") }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    menu.forEach { item ->
                        NavigationBarItem(
                            selected = selected == item.title,
                            onClick = { selected = item.title },
                            icon = { Text(item.symbol) },
                            label = { Text(item.title) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selected) {
                    "Заказы" -> OrdersScreen(canEdit = owner || admin)
                    "Архив" -> ArchiveScreen()
                    "Клиенты" -> ClientsScreen(canEdit = owner || admin)
                    "Финансы" -> FinanceScreen()
                    "Чат" -> ChatScreen(user)
                }
            }
        }
    }
}
