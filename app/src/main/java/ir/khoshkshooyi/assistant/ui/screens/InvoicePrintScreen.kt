package ir.khoshkshooyi.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.khoshkshooyi.assistant.data.Order
import ir.khoshkshooyi.assistant.data.Shop
import ir.khoshkshooyi.assistant.toman
import ir.khoshkshooyi.assistant.ui.theme.BgColor
import ir.khoshkshooyi.assistant.ui.theme.Brass
import ir.khoshkshooyi.assistant.ui.theme.BrassDark
import ir.khoshkshooyi.assistant.ui.theme.Ink
import ir.khoshkshooyi.assistant.ui.theme.InkSoft
import ir.khoshkshooyi.assistant.ui.theme.LineColor
import ir.khoshkshooyi.assistant.ui.theme.Red
import ir.khoshkshooyi.assistant.ui.theme.Teal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicePrintScreen(shop: Shop?, order: Order?, onClose: () -> Unit) {
    if (order == null) return
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.ArrowForward, contentDescription = null)
                Text("  بازگشت")
            }
            Button(
                onClick = { InvoicePrinter.print(context, shop, order) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Brass)
            ) {
                Icon(Icons.Filled.Print, contentDescription = null)
                Text("  چاپ فاکتور")
            }
        }

        Column(
            modifier = Modifier
                .width(380.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 30.dp)
                .verticalScroll(rememberScrollState())
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(Dp.Hairline, LineColor, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().background(Ink).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(shop?.name?.ifBlank { "خشکشویی" } ?: "خشکشویی", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("فاکتور رسمی", color = Color.White.copy(alpha = 0.75f), fontSize = 11.5.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                InvoiceLine("شماره فاکتور", "#${order.invoiceNo}")
                InvoiceLine("تاریخ", SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(order.date)))
                InvoiceLine("مشتری", order.customerName + if (order.customerNickname.isNotBlank()) " (${order.customerNickname})" else "")
            }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                order.items.forEachIndexed { i, it2 ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${it2.type} × ${it2.count}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(toman(it2.price * it2.count), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        if (it2.services.isNotEmpty()) {
                            Text(it2.services.joinToString(" · "), fontSize = 11.sp, color = Teal, modifier = Modifier.padding(top = 3.dp))
                        }
                        if (it2.description.isNotBlank()) {
                            Text(it2.description, fontSize = 11.sp, color = InkSoft, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                InvoiceLine("جمع اقلام", toman(order.itemsTotal))
                if (order.previousDebtLine > 0) InvoiceLine("بدهی قبلی", toman(order.previousDebtLine), red = true)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("قابل پرداخت", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(toman(order.total), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = BrassDark)
                }
                Text(
                    if (order.paymentStatus == "paid") "پرداخت شده" else "بدهکار",
                    fontWeight = FontWeight.Bold, fontSize = 11.5.sp,
                    color = if (order.paymentStatus == "paid") Teal else Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (order.notes.isNotBlank()) {
                Text("یادداشت: ${order.notes}", fontSize = 11.5.sp, color = InkSoft, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }
            Text(
                "با تشکر از اعتماد شما", fontSize = 10.5.sp, color = InkSoft,
                modifier = Modifier.fillMaxWidth().padding(18.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun InvoiceLine(k: String, v: String, red: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, color = InkSoft, fontSize = 12.5.sp)
        Text(v, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = if (red) Red else Ink)
    }
}
