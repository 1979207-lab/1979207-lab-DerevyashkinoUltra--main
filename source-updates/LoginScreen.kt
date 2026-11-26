package com.derevyashkino.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun AppRoot(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    var profile by remember { mutableStateOf<AppUser?>(null) }
    var loading by remember { mutableStateOf(user != null) }
    var error by remember { mutableStateOf("") }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(user?.uid) {
        profile = null
        if (user == null) { loading = false; return@LaunchedEffect }
        loading = true
        FirebaseFirestore.getInstance().collection("users").document(user!!.uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "Сотрудник"
                profile = AppUser(user!!.uid, user!!.email ?: "", role)
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                loading = false
            }
            .addOnFailureListener { error = it.localizedMessage ?: "Не удалось загрузить профиль"; loading = false }
    }

    when {
        user == null -> LoginScreen(onError = { error = it }, externalError = error)
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        profile != null -> MainMenu(profile!!, darkTheme = darkTheme, onToggleTheme = onToggleTheme, onSignOut = { auth.signOut() })
        else -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Не удалось загрузить профиль", style = MaterialTheme.typography.titleLarge)
            Text(error)
            Button(onClick = { auth.signOut() }) { Text("Выйти") }
        }
    }
}

@Composable
fun LoginScreen(onError: (String) -> Unit, externalError: String) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Деревяшкино 22", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Вход в рабочее пространство")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
        if (externalError.isNotBlank()) Text(externalError, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(enabled = !busy && email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
            busy = true
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { busy = false }
                .addOnFailureListener { busy = false; onError(it.localizedMessage ?: "Ошибка входа") }
        }) { Text(if (busy) "Входим…" else "Войти") }
    }
}
