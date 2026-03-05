package com.mkz.bingocard.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class AppDialogType {
    INFO,
    WARNING,
    DESTRUCTIVE
}

@Composable
fun AppActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String? = "Cancel",
    type: AppDialogType = AppDialogType.INFO
) {
    val confirmContainerColor = when (type) {
        AppDialogType.INFO -> MaterialTheme.colorScheme.primary
        AppDialogType.WARNING -> MaterialTheme.colorScheme.tertiary
        AppDialogType.DESTRUCTIVE -> MaterialTheme.colorScheme.error
    }
    val confirmContentColor = when (type) {
        AppDialogType.INFO -> MaterialTheme.colorScheme.onPrimary
        AppDialogType.WARNING -> MaterialTheme.colorScheme.onTertiary
        AppDialogType.DESTRUCTIVE -> MaterialTheme.colorScheme.onError
    }
    val titleColor = when (type) {
        AppDialogType.DESTRUCTIVE -> MaterialTheme.colorScheme.error
        AppDialogType.WARNING -> MaterialTheme.colorScheme.tertiary
        AppDialogType.INFO -> MaterialTheme.colorScheme.onSurface
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = titleColor,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmContainerColor,
                    contentColor = confirmContentColor
                )
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            if (dismissLabel != null) {
                OutlinedButton(onClick = onDismiss) {
                    Text(dismissLabel)
                }
            }
        }
    )
}
