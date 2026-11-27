package com.derevyashkino.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(onError: (String) -> Unit, externalError: String) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxSize().background(colors.surfaceVariant.copy(alpha = 0.45f)).padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.primaryContainer,
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🪵", style = MaterialTheme.typography.headlineLarge)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Деревяшкино 22",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text("Рабочее пространство вашей мастерской", color = colors.onSurfaceVariant)
                Spacer(Modifier.height(28.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Электронная почта") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, shape = RoundedCornerShape(14.dp)
                )
                if (externalError.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(externalError, color = colors.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    onClick = {
                        busy = true
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { busy = false }
                            .addOnFailureListener {
                                busy = false
                                onError(it.localizedMessage ?: "Ошибка входа")
                            }
                    }
                ) {
                    Text(if (busy) "Входим…" else "Войти", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
