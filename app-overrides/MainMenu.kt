package com.derevyashkino.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private val Forest = Color(0xFF174B35)
private val ForestLight = Color(0xFF28734D)
private val Leaf = Color(0xFF4B965B)
private val Gold = Color(0xFFC18A32)
private val Page = Color(0xFFF1F5F0)
private val Ink = Color(0xFF203B2C)

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
                .addOnFailureListener { error = it.localizedMessage ?: "Не удалось загрузить профиль" }
        }
    }

    val colors = lightColorScheme(
        primary = Forest, onPrimary = Color.White,
        secondary = Leaf, onSecondary = Color.White,
        tertiary = Gold, background = Page, surface = Color.White,
        surfaceVariant = Color(0xFFE7EEE8), onSurface = Ink,
        onSurfaceVariant = Color(0xFF65766B), error = Color(0xFFB4483E)
    )
    MaterialTheme(colorScheme = colors) {
        when {
            firebaseUser == null -> LoginScreen(onError = { error = it }, externalError = error)
            appUser != null -> MainMenu(user = appUser!!, onSignOut = {
                auth.signOut()
                error = ""
            })
            else -> Box(Modifier.fillMaxSize().background(Page), contentAlignment = Alignment.Center) {
                if (error.isNotBlank()) Text(error) else CircularProgressIndicator(color = Forest)
            }
        }
    }
}

private data class MenuItem(val title: String, val symbol: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenu(user: AppUser, onSignOut: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("derevyashkino_ui", android.content.Context.MODE_PRIVATE) }
    var selected by remember { mutableStateOf("Главная") }
    var dark by remember { mutableStateOf(preferences.getBoolean("dark_theme", false)) }
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
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFF83C99A), secondary = Color(0xFFA4D5A5),
        background = Color(0xFF111D16), surface = Color(0xFF1C2B22),
        surfaceVariant = Color(0xFF293A30), onSurface = Color(0xFFE6F0E8),
        onSurfaceVariant = Color(0xFFB7C9BC)
    ) else lightColorScheme(
        primary = Forest, onPrimary = Color.White, secondary = Leaf,
        tertiary = Gold, background = Page, surface = Color.White,
        surfaceVariant = Color(0xFFE7EEE8), onSurface = Ink,
        onSurfaceVariant = Color(0xFF65766B), error = Color(0xFFB4483E)
    )

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (selected != "Главная") {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Forest, titleContentColor = Color.White,
                            navigationIconContentColor = Color.White, actionIconContentColor = Color.White
                        ),
                        title = {
                            Column {
                                Text(if (selected == "Ещё") "Настройки" else selected, fontWeight = FontWeight.Bold)
                                Text(user.email + " · " + user.role, style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = .78f))
                            }
                        },
                        navigationIcon = {
                            TextButton(onClick = { selected = "Главная" }) {
                                Text("⌂", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            }
                        },
                        actions = {
                            if (selected == "Ещё") TextButton(onClick = onSignOut) {
                                Text("Выйти", color = Color.White)
                            }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = Forest
                ) {
                    menu.forEach { item ->
                        NavigationBarItem(
                            selected = selected == item.title,
                            onClick = { selected = item.title },
                            icon = { Text(item.symbol, style = MaterialTheme.typography.titleLarge) },
                            label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Forest, selectedTextColor = Forest,
                                indicatorColor = Color(0xFFDDEBE0),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selected) {
                    "Главная" -> HomeDashboard(
                        userEmail = user.email, userRole = user.role,
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
                        onDarkChange = { dark = it; preferences.edit().putBoolean("dark_theme", it).apply() },
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
    userEmail: String,
    userRole: String,
    canSeeFinance: Boolean,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFEAF1E9), MaterialTheme.colorScheme.background))
        ).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Forest),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Color(0xFF123B2B), Color(0xFF28734D)))
                ).padding(horizontal = 22.dp, vertical = 25.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = .15f)) {
                        Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                            Text("Д22", color = Color.White, fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Деревяшкино 22", color = Color.White,
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("Управление мастерской", color = Color.White.copy(alpha = .78f))
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("ВАША МАСТЕРСКАЯ", color = Color(0xFFE2C48B),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Все важное —\\nв одном месте.", color = Color.White,
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("$userEmail  ·  $userRole", color = Color.White.copy(alpha = .78f),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Рабочий стол", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardTile("▣", "Заказы", "Создание и контроль", ForestLight,
                    Modifier.weight(1f)) { onOpen("Заказы") }
                DashboardTile("♟", "Клиенты", "Контакты и история", Color(0xFF4B874C),
                    Modifier.weight(1f)) { onOpen("Клиенты") }
            }
            if (canSeeFinance) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardTile("▥", "Финансы", "Доходы и расходы", Color(0xFF79603A),
                        Modifier.weight(1f)) { onOpen("Финансы") }
                    DashboardTile("▰", "Чат", "Связь команды", Color(0xFF287C83),
                        Modifier.weight(1f)) { onOpen("Чат") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardTile("▰", "Чат", "Связь команды", Color(0xFF287C83),
                        Modifier.weight(1f)) { onOpen("Чат") }
                    DashboardTile("▤", "Архив", "Готовые заказы", Color(0xFF526B61),
                        Modifier.weight(1f)) { onOpen("Архив") }
                }
            }
            if (canSeeFinance) {
                DashboardTile("▤", "Архив заказов", "Выполненные и отменённые", Color(0xFF526B61),
                    Modifier.fillMaxWidth()) { onOpen("Архив") }
            }
        }
        Spacer(Modifier.height(18.dp))
        OutlinedButton(
            onClick = onSettings,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Forest)
        ) { Text("⚙   Настройки", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(12.dp))
        Text("Деревяшкино 22 · рабочее пространство",
            modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DashboardTile(
    symbol: String, title: String, subtitle: String, color: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 118.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = color) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Text(symbol, color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Настройки приложения", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        SettingsSwitchRow("Тёмная тема", dark, onDarkChange)
        SettingsSwitchRow("Уведомления", notifications, onNotificationsChange)
        SettingsSwitchRow("Звук уведомлений", sound, onSoundChange)
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp)) {
                Text("Деревяшкино 22", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("Версия 1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp)) { Text("Выйти из аккаунта") }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
