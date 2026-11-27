package com.derevyashkino.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AppRoot() {
    val auth = remember { FirebaseAuth.getInstance() }
    var firebaseUser by remember { mutableStateOf(auth.currentUser) }
    var appUser by remember { mutableStateOf<AppUser?>(null) }
    var error by remember { mutableStateOf("") }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseUser = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(firebaseUser?.uid) {
        val current = firebaseUser
        appUser = null
        if (current != null) {
            FirebaseFirestore.getInstance().collection("users").document(current.uid).get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role") ?: "Сотрудник"
                    appUser = AppUser(current.uid, current.email ?: "", role)
                }
                .addOnFailureListener {
                    error = it.localizedMessage ?: "Не удалось загрузить профиль"
                }
        }
    }

    MaterialTheme {
        when {
            firebaseUser == null -> LoginScreen(onError = { error = it }, externalError = error)
            appUser != null -> MainMenu(user = appUser!!, onSignOut = {
                auth.signOut()
                error = ""
            })
            else -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                if (error.isNotBlank()) Text(error) else CircularProgressIndicator()
            }
        }
    }
}

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
