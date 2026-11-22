package edu.androidsec.notes.reminder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import edu.androidsec.notes.R

/**
 * Возвращает действие «запросить разрешение на уведомления и, если оно получено, выполнить
 * [onGranted]». Запрашивать нужно из обработчика кнопки, а не при запуске приложения.
 *
 * У разрешения четыре состояния, и два из них (`ещё не спрашивали` и `отказано навсегда`)
 * неразличимы по `shouldShowRequestPermissionRationale`, поэтому запоминаем факт запроса в
 * [rememberSaveable]. Иначе после второго отказа кнопка молча перестала бы работать.
 */
@Composable
fun rememberNotificationPermission(onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return { onGranted() }

    var rationale by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var asked by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onGranted()
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.POST_NOTIFICATIONS,
            )
        ) {
            // Отказ, после которого система больше не покажет диалог: остаются настройки.
            settings = true
        } else {
            Toast.makeText(context, R.string.permission_denied_toast, Toast.LENGTH_LONG).show()
        }
    }

    if (rationale) {
        AlertDialog(
            onDismissRequest = { rationale = false },
            title = { Text(stringResource(R.string.permission_rationale_title)) },
            text = { Text(stringResource(R.string.permission_rationale_text)) },
            confirmButton = {
                TextButton(onClick = {
                    rationale = false
                    asked = true
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.permission_rationale_continue)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    rationale = false
                    Toast.makeText(context, R.string.permission_denied_toast, Toast.LENGTH_LONG).show()
                }) { Text(stringResource(R.string.permission_rationale_cancel)) }
            },
        )
    }

    if (settings) {
        AlertDialog(
            onDismissRequest = { settings = false },
            title = { Text(stringResource(R.string.permission_settings_title)) },
            text = { Text(stringResource(R.string.permission_settings_text)) },
            confirmButton = {
                TextButton(onClick = {
                    settings = false
                    context.openAppNotificationSettings()
                }) { Text(stringResource(R.string.permission_settings_open)) }
            },
            dismissButton = {
                TextButton(onClick = { settings = false }) {
                    Text(stringResource(R.string.permission_settings_cancel))
                }
            },
        )
    }

    return {
        when {
            // До API 33 разрешения не существует — hasNotificationPermission() вернёт true.
            context.hasNotificationPermission() -> onGranted()
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.POST_NOTIFICATIONS,
            ) -> rationale = true
            !asked -> {
                asked = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> settings = true
        }
    }
}

/** Есть ли у приложения право показывать уведомления (до API 33 — всегда). */
fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** `shouldShowRequestPermissionRationale` есть только у Activity, а `LocalContext` — обёртка. */
private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Экран «Уведомления» приложения; на прошивках без него — карточка приложения. */
private fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    runCatching { startActivity(intent) }
        .onFailure {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null),
                    ),
                )
            }
        }
}
