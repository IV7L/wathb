package com.yawmiyati.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yawmiyati.app.BuildConfig
import com.yawmiyati.app.YawmiyatiApp
import com.yawmiyati.app.data.api.AppConfigResponse
import com.yawmiyati.app.data.api.ConfigApi
import com.yawmiyati.app.ui.theme.WathbDeepTeal

private sealed interface ConfigState {
    object Loading : ConfigState
    data class Ready(val config: AppConfigResponse) : ConfigState
    object Failed : ConfigState
}

@Composable
fun AppUpdateGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<ConfigState>(ConfigState.Loading) }
    var updateBannerDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        state = try {
            val config = ConfigApi.service.getConfig()
            YawmiyatiApp.instance.supportWhatsapp = config.supportWhatsapp
            ConfigState.Ready(config)
        } catch (_: Exception) {
            ConfigState.Failed
        }
    }

    when (val s = state) {
        is ConfigState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WathbDeepTeal)
            }
        }

        is ConfigState.Failed -> {
            // Network unavailable — allow through rather than blocking.
            content()
        }

        is ConfigState.Ready -> {
            val versionCode = BuildConfig.VERSION_CODE
            val config = s.config

            if (versionCode < config.minSupportedVersionCode) {
                // Blocking screen — cannot dismiss.
                BlockingUpdateScreen(
                    apkUrl = config.apkDownloadUrl,
                    onOpenStore = {
                        if (config.apkDownloadUrl.isNotBlank()) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(config.apkDownloadUrl))
                            )
                        }
                    }
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    // Dismissible update banner.
                    if (!updateBannerDismissed && versionCode < config.latestVersionCode) {
                        UpdateBanner(
                            apkUrl = config.apkDownloadUrl,
                            onUpdate = {
                                if (config.apkDownloadUrl.isNotBlank()) {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(config.apkDownloadUrl))
                                    )
                                }
                            },
                            onDismiss = { updateBannerDismissed = true }
                        )
                    }
                    Box(Modifier.weight(1f)) { content() }
                }
            }
        }
    }
}

@Composable
private fun BlockingUpdateScreen(apkUrl: String, onOpenStore: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "يجب تحديث التطبيق",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "هذا الإصدار لم يعد مدعوماً. يرجى تحديث التطبيق للاستمرار.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onOpenStore,
                enabled = apkUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WathbDeepTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تحديث التطبيق", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun UpdateBanner(apkUrl: String, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = WathbDeepTeal)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "يتوفر إصدار جديد",
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onUpdate) {
                Text("تحديث", color = androidx.compose.ui.graphics.Color.White)
            }
            TextButton(onClick = onDismiss) {
                Text("لاحقاً", color = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}
