package ir.khoshkshooyi.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.khoshkshooyi.assistant.data.Shop
import ir.khoshkshooyi.assistant.ui.theme.Brass
import ir.khoshkshooyi.assistant.ui.theme.Ink
import ir.khoshkshooyi.assistant.ui.theme.InkSoft
import ir.khoshkshooyi.assistant.ui.theme.LineColor
import ir.khoshkshooyi.assistant.ui.theme.PanelColor

@Composable
fun LoginScreen(onLogin: (Shop) -> Unit) {
    var name by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(340.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(62.dp).background(Ink, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = PanelColor, modifier = Modifier.size(28.dp)) }
                Text("دستیار خشکشویی", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink, modifier = Modifier.padding(top = 12.dp))
                Text("ثبت سفارش با صدا یا فرم — فاکتور و پرینت آماده", fontSize = 13.5.sp, color = InkSoft, modifier = Modifier.padding(top = 6.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PanelColor, RoundedCornerShape(14.dp))
                    .border(Dp.Hairline, LineColor, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text("نام مغازه", fontSize = 12.5.sp, color = InkSoft, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    placeholder = { Text("مثلاً خشکشویی ستاره") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    singleLine = true
                )
                Text("نام مسئول (اختیاری)", fontSize = 12.5.sp, color = InkSoft, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                OutlinedTextField(
                    value = owner, onValueChange = { owner = it },
                    placeholder = { Text("نام شما") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    singleLine = true
                )
                Button(
                    onClick = { onLogin(Shop(name.trim(), owner.trim())) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Brass)
                ) { Text("ورود", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
