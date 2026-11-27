package com.derevyashkino.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (error.isNotBlank()) Text(error) else CircularProgressIndicator()
            }
        }
    }
}

private data class MenuItem(val title: String, val symbol: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenu(user: AppUser, onSignOut: () -> Unit) {
    var selected by remember { mutableStateOf("Главная") }
    var dark by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }
    var notificationSound by remember { mutableStateOf(true) }
    val owner = user.role == "Владелец"
    val admin = user.role == "Админ"
    val canSeeFinance = owner || admin
    val menu = buildList {
        add(MenuItem("Заказы", "▣"))
        add(MenuItem("Клиенты", "♟"))
        if (canSeeFinance) add(MenuItem("Финансы", "▥"))
        add(MenuItem("Чат", "▰"))
        add(MenuItem("Архив", "▤"))
        add(MenuItem("Ещё", "•••"))
    }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            topBar = {
                if (selected != "Главная") {
                    TopAppBar(
                        title = {
                            Column {
                                Text(if (selected == "Ещё") "Настройки" else selected, fontWeight = FontWeight.Bold)
                                Text(user.email + " · " + user.role, style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        navigationIcon = {
                            TextButton(onClick = { selected = "Главная" }) { Text("⌂") }
                        },
                        actions = {
                            if (selected == "Ещё") TextButton(onClick = onSignOut) { Text("Выйти") }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    menu.forEach { item ->
                        NavigationBarItem(
                            selected = selected == item.title,
                            onClick = { selected = item.title },
                            icon = { Text(item.symbol, style = MaterialTheme.typography.titleLarge) },
                            label = { Text(item.title) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selected) {
                    "Главная" -> HomeDashboard(
                        canSeeFinance = canSeeFinance,
                        onOpen = { selected = it },
                        onSettings = { selected = "Ещё" }
                    )
                    "Заказы" -> OrdersScreen(canEdit = owner || admin)
                    "Клиенты" -> ClientsScreen(canEdit = owner || admin)
                    "Финансы" -> if (canSeeFinance) FinanceScreen()
                    "Чат" -> ChatScreen(user)
                    "Архив" -> ArchiveScreen()
                    "Ещё" -> SettingsPanel(
                        dark = dark,
                        onDarkChange = { dark = it },
                        notifications = notifications,
                        onNotificationsChange = { notifications = it },
                        sound = notificationSound,
                        onSoundChange = { notificationSound = it },
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeDashboard(
    canSeeFinance: Boolean,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit
) {
    val blue = Color(0xFF0867E8)
    val green = Color(0xFF35BD43)
    val purple = Color(0xFF7251E8)
    val cyan = Color(0xFF078FE5)
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        Text("⌂", style = MaterialTheme.typography.displayLarge, color = Color(0xFFD58A19))
        Text("Деревяшкино 22", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text("Удобное управление\nвашим бизнесом", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        HomeAction("▣", "Заказы", blue) { onOpen("Заказы") }
        HomeAction("♟", "Клиенты", green) { onOpen("Клиенты") }
        if (canSeeFinance) HomeAction("▥", "Финансы", purple) { onOpen("Финансы") }
        HomeAction("▰", "Чат", cyan) { onOpen("Чат") }
        HomeAction("▤", "Архив", Color(0xFF526B78)) { onOpen("Архив") }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onSettings,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("⚙   Настройки", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun HomeAction(symbol: String, title: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(62.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
    ) {
        Text(symbol, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsPanel(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    notifications: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    sound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Настройки приложения", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        SettingsSwitchRow("Тёмная тема", dark, onDarkChange)
        SettingsSwitchRow("Уведомления", notifications, onNotificationsChange)
        SettingsSwitchRow("Звук уведомлений", sound, onSoundChange)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Деревяшкино 22", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Версия 1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Выйти из аккаунта") }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
