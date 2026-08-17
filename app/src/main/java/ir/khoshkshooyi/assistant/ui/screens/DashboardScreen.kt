package ir.khoshkshooyi.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.khoshkshooyi.assistant.data.Customer
import ir.khoshkshooyi.assistant.data.Order
import ir.khoshkshooyi.assistant.data.Shop
import ir.khoshkshooyi.assistant.toman
import ir.khoshkshooyi.assistant.ui.components.EmptyNote
import ir.khoshkshooyi.assistant.ui.components.StatCard
import ir.khoshkshooyi.assistant.ui.theme.Brass
import ir.khoshkshooyi.assistant.ui.theme.BrassDark
import ir.khoshkshooyi.assistant.ui.theme.Ink
import ir.khoshkshooyi.assistant.ui.theme.InkSoft
import ir.khoshkshooyi.assistant.ui.theme.LineColor
import ir.khoshkshooyi.assistant.ui.theme.PanelColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(shop: Shop?, customers: List<Customer>, orders: List<Order>, goOrder: () -> Unit) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val ordersToday = orders.count { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date)) == today }
    val totalDebt = customers.sumOf { it.debt }

    LazyColumn(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        item {
            Text("سلام ${shop?.owner ?: ""} \uD83D\uDC4B", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Ink)
            Text(
                SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()),
                fontSize = 13.5.sp, color = InkSoft, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
            )
            Button(
                onClick = goOrder,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brass)
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null)
                Text("  شروع سفارش جدید با صدا", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Icons.Filled.People, "مشتریان", customers.size.toString(), modifier = Modifier.weight(1f))
                StatCard(Icons.Filled.Assignment, "سفارش امروز", ordersToday.toString(), modifier = Modifier.weight(1f))
                StatCard(Icons.Filled.AccountBalanceWallet, "بدهی کل", toman(totalDebt), small = true, isRed = totalDebt > 0, modifier = Modifier.weight(1f))
            }
            Text("آخرین فاکتورها", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
        }
        if (orders.isEmpty()) {
            item { EmptyNote("هنوز سفارشی ثبت نشده.") }
        } else {
            items(orders.take(4)) { o ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(PanelColor, RoundedCornerShape(14.dp))
                        .border(Dp.Hairline, LineColor, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(o.customerName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Ink)
                        Text("${o.items.size} قلم · ${o.status}", fontSize = 11.5.sp, color = InkSoft)
                    }
                    Text(toman(o.total), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrassDark)
                }
            }
        }
    }
}
