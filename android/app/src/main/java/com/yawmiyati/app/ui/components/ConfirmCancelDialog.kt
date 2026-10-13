package com.yawmiyati.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ConfirmCancelDialog(
    title: String,
    body: String,
    onConfirm: (reason: String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
) {
    var reason by remember { mutableStateOf("") }
    val canConfirm = reason.isNotBlank() && !isLoading

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Text(body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب الإلغاء (مطلوب)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(reason.trim()) },
                enabled = canConfirm
            ) {
                Text("تأكيد الإلغاء", color = if (canConfirm) Color(0xFFD32F2F) else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("تراجع")
            }
        }
    )
}
