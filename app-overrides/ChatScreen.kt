package com.derevyashkino.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import java.util.Date

private data class ChatRow22(
    val id: String,
    val text: String,
    val sender: String,
    val mediaUrl: String,
    val mediaType: String
)

@Composable
fun ChatScreen(user: AppUser) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    var text by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<ChatRow22>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    var uploading by remember { mutableStateOf(false) }

    fun sendMedia(uri: Uri, mediaType: String) {
        uploading = true
        error = ""
        val mimeType = context.contentResolver.getType(uri)
            ?: if (mediaType == "video") "video/mp4" else "image/jpeg"
        val fileName = "${System.currentTimeMillis()}.${if (mediaType == "video") "mp4" else "jpg"}"
        val ref = FirebaseStorage.getInstance().reference
            .child("chat/${user.uid}/$fileName")
        val metadata = StorageMetadata.Builder().setContentType(mimeType).build()
        ref.putFile(uri, metadata)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw (task.exception ?: Exception("Не удалось загрузить файл"))
                ref.downloadUrl
            }
            .addOnSuccessListener { download ->
                db.collection("chat").add(
                    mapOf(
                        "text" to "",
                        "type" to mediaType,
                        "mediaUrl" to download.toString(),
                        "senderUid" to user.uid,
                        "senderEmail" to user.email,
                        "createdAt" to Date()
                    )
                ).addOnFailureListener {
                    error = "Файл загружен, но сообщение не сохранено: ${it.localizedMessage}"
                    uploading = false
                }.addOnSuccessListener { uploading = false }
            }
            .addOnFailureListener { exception ->
                val storageError = exception as? StorageException
                error = when (storageError?.errorCode) {
                    -13011 -> "В Firebase не найден Storage bucket. Проверьте, что Firebase Storage включён в проекте."
                    -13021 -> "Firebase Storage запретил загрузку: проверьте правила Storage для авторизованных пользователей."
                    -13020 -> "Сессия входа истекла. Выйдите и войдите снова."
                    -13013 -> "Превышена квота Firebase Storage."
                    else -> "Ошибка загрузки (${storageError?.errorCode ?: "нет кода"}): ${exception.localizedMessage ?: "неизвестная ошибка"}"
                }
                uploading = false
            }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) sendMedia(uri, "image")
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) sendMedia(uri, "video")
    }

    DisposableEffect(Unit) {
        val listener: ListenerRegistration = db.collection("chat").addSnapshotListener { snap, e ->
            if (e != null) error = e.localizedMessage ?: "Ошибка чата"
            rows = snap?.documents
                ?.sortedBy { it.getTimestamp("createdAt")?.toDate()?.time ?: 0L }
                ?.map {
                    ChatRow22(
                        it.id,
                        it.getString("text") ?: "",
                        it.getString("senderEmail") ?: "Сотрудник",
                        it.getString("mediaUrl") ?: "",
                        it.getString("type") ?: "text"
                    )
                } ?: emptyList()
        }
        onDispose { listener.remove() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Общий чат", style = MaterialTheme.typography.titleLarge)
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(rows, key = { it.id }) { message ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(message.sender, style = MaterialTheme.typography.labelMedium)
                        if (message.text.isNotBlank()) Text(message.text)
                        if (message.mediaUrl.isNotBlank()) {
                            Text(if (message.mediaType == "video") "Видеофайл" else "Фотография")
                            TextButton(onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.mediaUrl)))
                                }.onFailure { error = it.localizedMessage ?: "Не удалось открыть вложение" }
                            }) { Text("Открыть вложение") }
                        }
                    }
                }
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        OutlinedTextField(
            text,
            { text = it },
            label = { Text("Сообщение") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = text.isNotBlank() && !uploading,
                modifier = Modifier.weight(1f),
                onClick = {
                    val message = text.trim()
                    db.collection("chat").add(
                        mapOf(
                            "text" to message,
                            "type" to "text",
                            "senderUid" to user.uid,
                            "senderEmail" to user.email,
                            "createdAt" to Date()
                        )
                    ).addOnFailureListener {
                        error = it.localizedMessage ?: "Не удалось отправить сообщение"
                    }
                    text = ""
                }
            ) { Text("Отправить") }
            OutlinedButton(enabled = !uploading, onClick = { imagePicker.launch("image/*") }) {
                Text("Фото")
            }
            OutlinedButton(enabled = !uploading, onClick = { videoPicker.launch("video/*") }) {
                Text("Видео")
            }
        }
        if (uploading) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}
