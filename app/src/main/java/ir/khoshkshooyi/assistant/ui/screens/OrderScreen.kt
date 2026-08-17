package ir.khoshkshooyi.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.khoshkshooyi.assistant.AppViewModel
import ir.khoshkshooyi.assistant.VoiceStatus
import ir.khoshkshooyi.assistant.data.ChatMessage
import ir.khoshkshooyi.assistant.data.ITEM_TYPES
import ir.khoshkshooyi.assistant.data.OrderItem
import ir.khoshkshooyi.assistant.data.SERVICES
import ir.khoshkshooyi.assistant.data.newId
import ir.khoshkshooyi.assistant.toman
import ir.khoshkshooyi.assistant.ui.components.SectionCard
import ir.khoshkshooyi.assistant.ui.theme.Brass
import ir.khoshkshooyi.assistant.ui.theme.Gold
import ir.khoshkshooyi.assistant.ui.theme.Ink
import ir.khoshkshooyi.assistant.ui.theme.InkSoft
import ir.khoshkshooyi.assistant.ui.theme.LineColor
import ir.khoshkshooyi.assistant.ui.theme.Red
import ir.khoshkshooyi.assistant.ui.theme.Teal

@Composable
fun OrderScreen(vm: AppViewModel, hasMicPermission: Boolean, onRequestMic: () -> Unit, goSettings: () -> Unit) {
    var error by remember { mutableStateOf("") }

    val matched = if (vm.draftCustomerName.isNotBlank()) vm.findCustomerByNameOrNickname(vm.draftCustomerName, vm.draftCustomerNickname) else null
    val baseDebt = matched?.debt ?: 0L

    fun handleSubmit() {
        val err = vm.confirmDraftOrder()
        error = err ?: ""
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Text("ثبت سفارش جدید", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Ink)
            Text("با صدا توضیح بده یا مستقیم فرم رو پر کن.", fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))

            VoiceCaptureCard(vm, hasMicPermission, onRequestMic, goSettings)

            SectionCard("اطلاعات مشتری") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = vm.draftCustomerName, onValueChange = { vm.updateDraftCustomerName(it) },
                        label = { Text("نام مشتری") }, placeholder = { Text("مثلاً آقای ابراهیمی") },
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = vm.draftCustomerNickname, onValueChange = { vm.updateDraftCustomerNickname(it) },
                        label = { Text("نام مستعار") }, placeholder = { Text("مثلاً قصاب محل") },
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                OutlinedTextField(
                    value = vm.draftCustomerPhone, onValueChange = { vm.updateDraftCustomerPhone(it) },
                    label = { Text("شماره تماس (اختیاری)") }, placeholder = { Text("09xxxxxxxxx") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp), singleLine = true
                )
                if (matched != null && baseDebt > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(Red.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("بدهی قبلی: ${toman(baseDebt)}", color = Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = vm.draftIncludeDebt, onCheckedChange = { vm.updateDraftIncludeDebt(it) })
                            Text("لحاظ در فاکتور", fontSize = 12.5.sp)
                        }
                    }
                }
            }

            SectionCard("اقلام (${vm.draftItems.size})") {
                vm.draftItems.forEach { it2 ->
                    ItemRow(
                        item = it2,
                        onChange = { patch -> vm.updateDraftItems(vm.draftItems.map { if (it.id == it2.id) patch else it }) },
                        onRemove = { vm.updateDraftItems(vm.draftItems.filter { it.id != it2.id }) }
                    )
                }
                OutlinedButton(
                    onClick = { vm.updateDraftItems(vm.draftItems + OrderItem(id = newId("i"), type = ITEM_TYPES[0])) },
                    modifier = Modifier.fillMaxWidth().padding(top = if (vm.draftItems.isNotEmpty()) 8.dp else 0.dp)
                ) { Text("+ افزودن قلم") }
            }

            SectionCard("وضعیت پرداخت این فاکتور") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("پرداخت شده", vm.draftPaymentStatus == "paid") { vm.updateDraftPaymentStatus("paid") }
                    ChoiceChip("بدهکار", vm.draftPaymentStatus == "debt") { vm.updateDraftPaymentStatus("debt") }
                }
                OutlinedTextField(
                    value = vm.draftNotes, onValueChange = { vm.updateDraftNotes(it) },
                    label = { Text("یادداشت (اختیاری)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 70.dp)
                )
            }

            if (error.isNotBlank()) {
                Text(error, color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
            }
            Button(
                onClick = { handleSubmit() },
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 90.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brass)
            ) {
                Icon(Icons.Filled.Receipt, contentDescription = null)
                Text("  ثبت سفارش و صدور فاکتور", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (active) Teal.copy(alpha = 0.12f) else Color.White, RoundedCornerShape(999.dp))
            .border(Dp.Hairline, if (active) Teal else LineColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .then(Modifier.clickableSafe(onClick))
    ) {
        Text(label, fontSize = 12.5.sp, color = if (active) Teal else InkSoft, fontWeight = FontWeight.SemiBold)
    }
}

private fun Modifier.clickableSafe(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

@Composable
private fun ItemRow(item: OrderItem, onChange: (OrderItem) -> Unit, onRemove: () -> Unit) {
    var typeMenuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(Color(0xFFFCFBF8), RoundedCornerShape(12.dp))
            .border(Dp.Hairline, LineColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(2f)) {
                OutlinedButton(onClick = { typeMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(item.type) }
                DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    ITEM_TYPES.forEach { t ->
                        DropdownMenuItem(text = { Text(t) }, onClick = { onChange(item.copy(type = t)); typeMenuOpen = false })
                    }
                }
            }
            OutlinedTextField(
                value = item.count.toString(),
                onValueChange = { v -> onChange(item.copy(count = v.toIntOrNull() ?: 1)) },
                modifier = Modifier.weight(1f), singleLine = true
            )
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = Red) }
        }
        if (item.type == "سایر") {
            OutlinedTextField(
                value = item.customType, onValueChange = { onChange(item.copy(customType = it)) },
                placeholder = { Text("نوع قلم را بنویسید") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true
            )
        }
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SERVICES.forEach { s ->
                val active = item.services.contains(s)
                Box(
                    modifier = Modifier
                        .background(if (active) Teal.copy(alpha = 0.12f) else Color.White, RoundedCornerShape(999.dp))
                        .border(Dp.Hairline, if (active) Teal else LineColor, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .then(Modifier.clickableSafe {
                            val newServices = if (active) item.services - s else item.services + s
                            onChange(item.copy(services = newServices))
                        })
                ) { Text(s, fontSize = 11.5.sp, color = if (active) Teal else InkSoft) }
            }
        }
        OutlinedTextField(
            value = item.description, onValueChange = { onChange(item.copy(description = it)) },
            placeholder = { Text("توضیحات (رنگ، طرح، نکات...)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true
        )
        OutlinedTextField(
            value = if (item.price == 0L) "" else item.price.toString(),
            onValueChange = { v -> onChange(item.copy(price = v.toLongOrNull() ?: 0)) },
            label = { Text("قیمت واحد (تومان)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true
        )
    }
}

@Composable
private fun VoiceCaptureCard(vm: AppViewModel, hasMicPermission: Boolean, onRequestMic: () -> Unit, goSettings: () -> Unit) {
    val statusLabel = when (vm.voiceStatus) {
        VoiceStatus.LISTENING -> "\uD83C\uDF99\uFE0F در حال گوش دادن..."
        VoiceStatus.PROCESSING -> "\u23F3 در حال فکر کردن..."
        VoiceStatus.SPEAKING -> "\uD83D\uDD0A در حال صحبت..."
        VoiceStatus.IDLE -> ""
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp), ambientColor = Ink.copy(alpha = 0.35f))
            .background(Ink, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(9.dp).background(if (vm.sessionActive) Gold else Color.White.copy(alpha = 0.3f), CircleShape)
                )
                Text("  دستیار صوتی", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
            IconButton(onClick = goSettings) { Icon(Icons.Filled.Settings, contentDescription = "تنظیمات", tint = Color.White) }
        }

        if (vm.apiKeyMissingHint) {
            Text(
                "برای گفتگوی هوشمند، کلید API ${vm.provider.displayName} را در «تنظیمات» وارد کنید.",
                fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }

        if (!vm.sessionActive) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text(
                    "دکمه رو بزن و مثل یه مکالمه‌ی طبیعی باهاش حرف بزن — سفارش ثبت کن، بگو «ثبتش کن» تا خودش فاکتور بزنه، یا وضعیت یه مشتری رو بپرس.",
                    fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 14.dp)
                )
                Button(
                    onClick = {
                        if (!hasMicPermission) { onRequestMic(); return@Button }
                        vm.startVoiceSession()
                    },
                    enabled = !vm.validatingKey,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
                ) {
                    if (vm.validatingKey) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Ink)
                        Text("  در حال بررسی کلید API...")
                    } else {
                        Icon(Icons.Filled.Mic, contentDescription = null)
                        Text("  شروع گفتگو", fontWeight = FontWeight.Bold)
                    }
                }
                if (vm.voiceError.isNotBlank()) {
                    Text(vm.voiceError, color = Color(0xFFF2A9A9), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        } else {
            val visibleMessages = vm.chatMessages + if (vm.liveInterim.isNotBlank()) listOf(ChatMessage("interim", vm.liveInterim)) else emptyList()
            val listState = rememberLazyListState()
            LaunchedEffect(visibleMessages.size, vm.liveInterim) {
                if (visibleMessages.isNotEmpty()) listState.animateScrollToItem(visibleMessages.size - 1)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).padding(vertical = 8.dp)
            ) {
                items(visibleMessages) { m ->
                    val isUser = m.role == "user"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .padding(vertical = 3.dp)
                                .background(
                                    if (m.role == "interim") Color.White.copy(alpha = 0.06f)
                                    else if (isUser) Color.White.copy(alpha = 0.1f) else Gold.copy(alpha = 0.18f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(
                                m.text, color = Color.White, fontSize = 13.sp,
                                fontStyle = if (m.role == "interim") FontStyle.Italic else FontStyle.Normal
                            )
                        }
                    }
                }
            }
            if (statusLabel.isNotBlank()) {
                Text(statusLabel, fontSize = 12.sp, color = Gold, modifier = Modifier.padding(bottom = 10.dp))
            }
            Button(
                onClick = { vm.endVoiceSession() },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("  پایان گفتگو", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
            if (vm.voiceError.isNotBlank()) {
                Text(vm.voiceError, color = Color(0xFFF2A9A9), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
