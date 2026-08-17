package ir.khoshkshooyi.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ir.khoshkshooyi.assistant.ui.screens.CustomersScreen
import ir.khoshkshooyi.assistant.ui.screens.DashboardScreen
import ir.khoshkshooyi.assistant.ui.screens.InvoicePrintScreen
import ir.khoshkshooyi.assistant.ui.screens.InvoicesScreen
import ir.khoshkshooyi.assistant.ui.screens.LoginScreen
import ir.khoshkshooyi.assistant.ui.screens.OrderScreen
import ir.khoshkshooyi.assistant.ui.screens.SettingsScreen
import ir.khoshkshooyi.assistant.ui.components.BottomNav
import ir.khoshkshooyi.assistant.ui.components.TopBar
import ir.khoshkshooyi.assistant.ui.theme.BgColor
import ir.khoshkshooyi.assistant.ui.theme.KhoshkshooyiTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    private lateinit var micPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        micPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) vm.startVoiceSession()
        }

        setContent {
            // The app is Persian-only, so force RTL layout regardless of the device's
            // system locale/direction setting.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                KhoshkshooyiTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppRoot(vm = vm, onRequestMic = { requestMicPermission() })
                    }
                }
            }
        }
    }

    private fun requestMicPermission() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            vm.startVoiceSession()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}

@androidx.compose.runtime.Composable
private fun AppRoot(vm: AppViewModel, onRequestMic: () -> Unit) {
    if (vm.loading) {
        Box(modifier = Modifier.fillMaxSize().background(BgColor), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val printOrderId = vm.printOrderId
    if (printOrderId != null) {
        val order = vm.orders.find { it.id == printOrderId }
        InvoicePrintScreen(shop = vm.shop, order = order, onClose = { vm.printOrderId = null })
        return
    }

    if (vm.page == Page.LOGIN) {
        LoginScreen(onLogin = { vm.login(it) })
        return
    }

    val hasMic = androidx.compose.ui.platform.LocalContext.current.let { ctx ->
        androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        TopBar(vm.shop)
        Box(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 16.dp)) {
            when (vm.page) {
                Page.DASHBOARD -> DashboardScreen(vm.shop, vm.customers, vm.orders, goOrder = { vm.page = Page.ORDER })
                Page.ORDER -> OrderScreen(
                    vm = vm,
                    hasMicPermission = hasMic,
                    onRequestMic = onRequestMic,
                    goSettings = { vm.page = Page.SETTINGS }
                )
                Page.INVOICES -> InvoicesScreen(vm.orders, onPrint = { vm.printOrderId = it }, onStatus = { id, s -> vm.setOrderStatus(id, s) })
                Page.CUSTOMERS -> CustomersScreen(vm.customers, vm.orders, onSettle = { id, amount -> vm.settleDebt(id, amount) }, onPrint = { vm.printOrderId = it })
                Page.SETTINGS -> SettingsScreen(
                    shop = vm.shop,
                    provider = vm.provider,
                    apiKeyFor = { vm.apiKeyFor(it) },
                    onSaveShop = { vm.saveShop(it) },
                    onSelectProvider = { vm.updateProvider(it) },
                    onUpdateApiKey = { p, key -> vm.updateApiKey(p, key) }
                )
                Page.LOGIN -> Unit
            }
        }
        BottomNav(page = vm.page, onSelect = { vm.page = it })
    }
}
