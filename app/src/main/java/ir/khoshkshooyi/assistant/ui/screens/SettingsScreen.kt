package ir.khoshkshooyi.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.khoshkshooyi.assistant.ai.AiProviderType
import ir.khoshkshooyi.assistant.ai.clientFor
import ir.khoshkshooyi.assistant.data.Shop
import ir.khoshkshooyi.assistant.ui.components.SectionCard
import ir.khoshkshooyi.assistant.ui.theme.Brass
import ir.khoshkshooyi.assistant.ui.theme.Ink
import ir.khoshkshooyi.assistant.ui.theme.InkSoft
import ir.khoshkshooyi.assistant.ui.theme.Red
import ir.khoshkshooyi.assistant.ui.theme.Teal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    shop: Shop?,
    provider: AiProviderType,
    apiKeyFor: (AiProviderType) -> String,
    onSaveShop: (Shop) -> Unit,
    onSelectProvider: (AiProviderType) -> Unit,
    onUpdateApiKey: (AiProviderType, String) -> Unit
) {
    var name by remember { mutableStateOf(shop?.name ?: "") }
    var owner by remember { mutableStateOf(shop?.owner ?: "") }
    var shopSaved by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf(provider) }
    var key by remember(selectedProvider) { mutableStateOf(apiKeyFor(selectedProvider)) }
    var testState by remember(selectedProvider) { mutableStateOf("idle") } // idle | testing | ok | error
    var testMsg by remember(selectedProvider) { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Text("تنظیمات", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Ink, modifier = Modifier.padding(bottom = 14.dp))

            SectionCard("اطلاعات مغازه") {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام مغازه") }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), singleLine = true)
                OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("نام مسئول (اختیاری)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), singleLine = true)
                Button(
                    onClick = {
                        onSaveShop(Shop(name.trim(), owner.trim()))
                        shopSaved = true
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Brass)
                ) { Text(if (shopSaved) "ذخیره شد ✓" else "ذخیره تغییرات") }
            }

            SectionCard("دستیار صوتی هوش مصنوعی") {
                Text(
                    "موتور هوش مصنوعی را انتخاب کنید — می‌توانید کلید هر دو را ذخیره کنید و هروقت خواستید جابه‌جا شوید.",
                    fontSize = 12.5.sp, color = InkSoft, modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiProviderType.values().forEach { p ->
                        val selected = p == selectedProvider
                        Button(
                            onClick = {
                                selectedProvider = p
                                onSelectProvider(p)
                            },
                            modifier = Modifier.weight(1f),
                            colors = if (selected) ButtonDefaults.buttonColors(containerColor = Brass)
                            else ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFEDE7DC), contentColor = Ink)
                        ) { Text(p.displayName, fontSize = 12.5.sp) }
                    }
                }

                Text(
                    "${selectedProvider.keyLabel} — فقط روی همین دستگاه ذخیره می‌شود و برای گفتگوی صوتی هوشمند لازم است.",
                    fontSize = 12.5.sp, color = InkSoft, modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it; onUpdateApiKey(selectedProvider, it); testState = "idle"; testMsg = "" },
                    placeholder = { Text(selectedProvider.keyPlaceholder) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true
                )
                OutlinedButton(
                    onClick = {
                        if (key.isBlank()) { testState = "error"; testMsg = "اول یک کلید وارد کنید."; return@OutlinedButton }
                        testState = "testing"; testMsg = ""
                        val providerToTest = selectedProvider
                        val keyToTest = key
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { clientFor(providerToTest).validateApiKey(keyToTest) }
                                testState = "ok"; testMsg = "کلید معتبر است و متصل می‌شود ✓"
                            } catch (e: Exception) {
                                testState = "error"; testMsg = e.message ?: "کلید نامعتبر است."
                            }
                        }
                    },
                    enabled = testState != "testing",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (testState == "testing") {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 6.dp))
                        Text("در حال بررسی...")
                    } else Text("تست کلید API")
                }
                if (testMsg.isNotBlank()) {
                    Text(testMsg, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (testState == "ok") Teal else Red, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
