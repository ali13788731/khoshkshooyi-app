package ir.khoshkshooyi.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.khoshkshooyi.assistant.data.Order
import ir.khoshkshooyi.assistant.toman
import ir.khoshkshooyi.assistant.ui.components.EmptyNote
import ir.khoshkshooyi.assistant.ui.theme.Brass
import ir.khoshkshooyi.assistant.ui.theme.BrassDark
import ir.khoshkshooyi.assistant.ui.theme.Ink
import ir.khoshkshooyi.assistant.ui.theme.InkSoft
import ir.khoshkshooyi.assistant.ui.theme.LineColor
import ir.khoshkshooyi.assistant.ui.theme.PanelColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val STATUSES = listOf("در حال آماده‌سازی", "آماده تحویل", "تحویل شد")

@Composable
fun InvoicesScreen(orders: List<Order>, onPrint: (String) -> Unit, onStatus: (String, String) -> Unit) {
    var q by remember { mutableStateOf("") }
    val filtered = orders.filter { it.customerName.contains(q, ignoreCase = true) }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Text("فاکتورها", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Ink)
            OutlinedTextField(
                value = q, onValueChange = { q = it },
                placeholder = { Text("جستجوی نام مشتری...") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), singleLine = true
            )
        }
        if (filtered.isEmpty()) {
            item { EmptyNote("فاکتوری یافت نشد.") }
        } else {
            items(filtered) { o -> InvoiceCard(o, onPrint, onStatus) }
        }
    }
}

@Composable
private fun InvoiceCard(o: Order, onPrint: (String) -> Unit, onStatus: (String, String) -> Unit) {
    var statusMenuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(PanelColor, RoundedCornerShape(14.dp))
            .border(Dp.Hairline, LineColor, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    o.customerName + if (o.customerNickname.isNotBlank()) " (${o.customerNickname})" else "",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink
                )
                Text(
                    "فاکتور #${o.invoiceNo} · ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(o.date))} · ${o.items.size} قلم",
                    fontSize = 11.5.sp, color = InkSoft
                )
            }
            Text(toman(o.total), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = BrassDark)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { statusMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(o.status, fontSize = 12.5.sp) }
                DropdownMenu(expanded = statusMenuOpen, onDismissRequest = { statusMenuOpen = false }) {
                    STATUSES.forEach { s ->
                        DropdownMenuItem(text = { Text(s) }, onClick = { onStatus(o.id, s); statusMenuOpen = false })
                    }
                }
            }
            IconButton(onClick = { onPrint(o.id) }) { Icon(Icons.Filled.Print, contentDescription = "چاپ", tint = Brass) }
        }
    }
}
