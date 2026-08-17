package ir.khoshkshooyi.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.khoshkshooyi.assistant.Page
import ir.khoshkshooyi.assistant.data.Shop
import ir.khoshkshooyi.assistant.ui.theme.Brass
import ir.khoshkshooyi.assistant.ui.theme.Ink
import ir.khoshkshooyi.assistant.ui.theme.InkSoft
import ir.khoshkshooyi.assistant.ui.theme.LineColor
import ir.khoshkshooyi.assistant.ui.theme.PanelColor
import ir.khoshkshooyi.assistant.ui.theme.Red
import ir.khoshkshooyi.assistant.ui.theme.ShadowColor
import ir.khoshkshooyi.assistant.ui.theme.Teal

@Composable
fun TopBar(shop: Shop?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp), ambientColor = ShadowColor.copy(alpha = 0.25f))
            .background(PanelColor, RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(Ink, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = PanelColor) }
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(shop?.name?.ifBlank { "خشکشویی" } ?: "خشکشویی", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
            Text("دستیار صوتی سفارش", fontSize = 11.sp, color = InkSoft)
        }
    }
}

private data class NavItem(val page: Page, val label: String, val icon: ImageVector)

@Composable
fun BottomNav(page: Page, onSelect: (Page) -> Unit) {
    val items = listOf(
        NavItem(Page.DASHBOARD, "خانه", Icons.Filled.Home),
        NavItem(Page.ORDER, "ثبت سفارش", Icons.Filled.Mic),
        NavItem(Page.INVOICES, "فاکتورها", Icons.Filled.Receipt),
        NavItem(Page.CUSTOMERS, "مشتریان", Icons.Filled.People),
        NavItem(Page.SETTINGS, "تنظیمات", Icons.Filled.Settings),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), ambientColor = ShadowColor.copy(alpha = 0.3f))
            .background(PanelColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { item ->
            val active = item.page == page
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .background(if (active) Brass.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onSelect(item.page) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(item.icon, contentDescription = item.label, tint = if (active) Brass else InkSoft, modifier = Modifier.size(20.dp))
                Text(item.label, fontSize = 10.5.sp, color = if (active) Brass else InkSoft, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), ambientColor = ShadowColor.copy(alpha = 0.12f))
            .background(PanelColor, RoundedCornerShape(16.dp))
            .border(Dp.Hairline, LineColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink, modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}

@Composable
fun StatCard(icon: ImageVector, label: String, value: String, small: Boolean = false, isRed: Boolean = false, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), ambientColor = ShadowColor.copy(alpha = 0.12f))
            .background(PanelColor, RoundedCornerShape(16.dp))
            .border(Dp.Hairline, LineColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = if (isRed) Red else Teal, modifier = Modifier.size(17.dp).padding(bottom = 6.dp))
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = if (small) 12.sp else 16.sp, color = if (isRed) Red else Ink)
        Text(label, fontSize = 10.5.sp, color = InkSoft, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun EmptyNote(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
        Text(text, color = InkSoft, fontSize = 13.sp)
    }
}
