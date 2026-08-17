package ir.khoshkshooyi.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.khoshkshooyi.assistant.data.Customer
import ir.khoshkshooyi.assistant.data.Order
import ir.khoshkshooyi.assistant.toman
import ir.khoshkshooyi.assistant.ui.components.EmptyNote
import ir.khoshkshooyi.assistant.ui.theme.Ink
import ir.khoshkshooyi.assistant.ui.theme.InkSoft
import ir.khoshkshooyi.assistant.ui.theme.LineColor
import ir.khoshkshooyi.assistant.ui.theme.PanelColor
import ir.khoshkshooyi.assistant.ui.theme.Red
import ir.khoshkshooyi.assistant.ui.theme.Teal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomersScreen(customers: List<Customer>, orders: List<Order>, onSettle: (String, Long) -> Unit, onPrint: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    var openId by remember { mutableStateOf<String?>(null) }
    val filtered = customers.filter { it.name.contains(q, ignoreCase = true) || it.nickname.contains(q, ignoreCase = true) }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Text("مشتریان", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Ink)
            OutlinedTextField(
                value = q, onValueChange = { q = it },
                placeholder = { Text("جستجو...") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), singleLine = true
            )
        }
        if (filtered.isEmpty()) {
            item { EmptyNote("مشتری‌ای یافت نشد.") }
        } else {
            items(filtered) { c ->
                val history = orders.filter { it.customerId == c.id }
                val open = openId == c.id
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .background(PanelColor, RoundedCornerShape(14.dp))
                        .border(Dp.Hairline, LineColor, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { openId = if (open) null else c.id },
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                c.name + if (c.nickname.isNotBlank()) " (${c.nickname})" else "",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink
                            )
                            Text(
                                "${history.size} سفارش" + if (c.phone.isNotBlank()) " · ${c.phone}" else "",
                                fontSize = 11.5.sp, color = InkSoft
                            )
                        }
                        Text(
                            if (c.debt > 0) toman(c.debt) else "بدون بدهی",
                            fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = if (c.debt > 0) Red else Teal
                        )
                    }
                    if (open) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            if (c.debt > 0) {
                                OutlinedButton(onClick = { onSettle(c.id, c.debt) }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Teal)
                                    Text("  ثبت تسویه کامل بدهی", color = Teal)
                                }
                            }
                            if (history.isEmpty()) {
                                Text("سفارشی ثبت نشده.", fontSize = 12.sp, color = InkSoft)
                            } else {
                                history.forEach { o ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(o.date))} · فاکتور #${o.invoiceNo}",
                                            fontSize = 12.5.sp, color = InkSoft
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(toman(o.total), fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                            IconButton(onClick = { onPrint(o.id) }) { Icon(Icons.Filled.Print, contentDescription = "چاپ", modifier = Modifier.padding(start = 4.dp)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
