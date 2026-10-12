package com.yawmiyati.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val CONSENT_VERSION = 1

private val CONSENT_TEXT = """
شروط الاستخدام وسياسة الخصوصية — وَثْب

باستخدامك لتطبيق وَثْب، فإنك توافق على ما يلي:

١. جمع البيانات
نجمع رقم هاتفك وبيانات ملفك الشخصي وسجل عملك على المنصة. قد نجمع رقم هويتك الوطنية عند التحقق منك، ويُخزَّن مشفَّراً ولا يُكشف عنه.

٢. استخدام البيانات
تُستخدم بياناتك لمطابقتك بأصحاب العمل، وإدارة المدفوعات، وتحسين الخدمة، وضمان سلامة المنصة.

٣. حماية بياناتك
لا نبيع بياناتك ولا نشاركها مع أطراف خارجية لأغراض تجارية.

٤. حقوقك
يحق لك طلب حذف حسابك وبياناتك في أي وقت عبر التواصل مع الدعم.

٥. التعديلات
قد نُحدِّث هذه الشروط ونُخطرك بأي تغييرات جوهرية.

بالمتابعة، تؤكد أنك قرأت هذه الشروط وتوافق عليها.
""".trimIndent()

@Composable
fun ConsentScreen(
    onAccepted: (consentVersion: Int) -> Unit,
    onBack: () -> Unit,
) {
    var checked by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "الشروط والأحكام",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = CONSENT_TEXT,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it },
                )
                Text(
                    text = "قرأت الشروط وأوافق عليها",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onAccepted(CONSENT_VERSION) },
                enabled = checked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("أكمل التسجيل", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("رجوع", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
