package ir.khoshkshooyi.assistant

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.khoshkshooyi.assistant.ai.AiProviderType
import ir.khoshkshooyi.assistant.ai.clientFor
import ir.khoshkshooyi.assistant.data.ChatMessage
import ir.khoshkshooyi.assistant.data.Customer
import ir.khoshkshooyi.assistant.data.ITEM_TYPES
import ir.khoshkshooyi.assistant.data.Order
import ir.khoshkshooyi.assistant.data.OrderDraft
import ir.khoshkshooyi.assistant.data.OrderItem
import ir.khoshkshooyi.assistant.data.Repository
import ir.khoshkshooyi.assistant.data.SERVICES
import ir.khoshkshooyi.assistant.data.Shop
import ir.khoshkshooyi.assistant.data.createOrder
import ir.khoshkshooyi.assistant.data.findCustomer
import ir.khoshkshooyi.assistant.data.newId
import ir.khoshkshooyi.assistant.data.settlePayment
import ir.khoshkshooyi.assistant.data.updateOrderStatus
import ir.khoshkshooyi.assistant.voice.TtsPlayer
import ir.khoshkshooyi.assistant.voice.VoiceRecognizer
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class Page { LOGIN, DASHBOARD, ORDER, INVOICES, CUSTOMERS, SETTINGS }
enum class VoiceStatus { IDLE, LISTENING, PROCESSING, SPEAKING }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    val recognizer = VoiceRecognizer(app)
    private val ttsPlayer = TtsPlayer(app)

    var loading by mutableStateOf(true); private set
    var shop by mutableStateOf<Shop?>(null); private set
    var page by mutableStateOf(Page.LOGIN)
    var customers by mutableStateOf<List<Customer>>(emptyList()); private set
    var orders by mutableStateOf<List<Order>>(emptyList()); private set
    var printOrderId by mutableStateOf<String?>(null)
    var provider by mutableStateOf(AiProviderType.DEFAULT); private set
    private var apiKeys by mutableStateOf<Map<AiProviderType, String>>(emptyMap())
    val apiKey: String get() = apiKeys[provider].orEmpty()
    val apiKeyMissingHint: Boolean get() = apiKey.isBlank()

    // Voice session state
    var sessionActive by mutableStateOf(false); private set
    var voiceStatus by mutableStateOf(VoiceStatus.IDLE); private set
    var chatMessages by mutableStateOf<List<ChatMessage>>(emptyList()); private set
    var liveInterim by mutableStateOf(""); private set
    var voiceError by mutableStateOf(""); private set
    var validatingKey by mutableStateOf(false); private set

    // The order the voice assistant is currently building — this is the single source of
    // truth, so the assistant can build and submit an order end-to-end by itself, whether
    // or not the person also touches the on-screen form.
    var draftCustomerName by mutableStateOf(""); private set
    var draftCustomerNickname by mutableStateOf(""); private set
    var draftCustomerPhone by mutableStateOf(""); private set
    var draftItems by mutableStateOf<List<OrderItem>>(emptyList()); private set
    var draftPaymentStatus by mutableStateOf("debt"); private set
    var draftNotes by mutableStateOf(""); private set
    var draftIncludeDebt by mutableStateOf(true); private set
    var draftError by mutableStateOf(""); private set

    private var conversationHistory = JSONArray()
    private var sessionShouldContinue = false
    private var sessionProvider = AiProviderType.DEFAULT
    private var sessionApiKey = ""

    init {
        shop = repo.loadShop()
        customers = repo.loadCustomers()
        orders = repo.loadOrders()
        apiKeys = AiProviderType.values().associateWith { repo.loadApiKey(it) }
        provider = repo.loadProvider()
        chatMessages = repo.loadChatMessages()
        page = if (shop != null) Page.DASHBOARD else Page.LOGIN
        loading = false
    }

    /** آخرین ۱۰۰ پیام گفتگوی صوتی همیشه روی گوشی ذخیره می‌شوند تا با بستن برنامه از بین نروند. */
    private fun persistChatMessages(list: List<ChatMessage>) {
        val capped = list.takeLast(100)
        chatMessages = capped
        repo.saveChatMessages(capped)
    }

    fun login(newShop: Shop) {
        shop = newShop
        repo.saveShop(newShop)
        page = Page.DASHBOARD
    }

    fun saveShop(newShop: Shop) {
        shop = newShop
        repo.saveShop(newShop)
    }

    fun apiKeyFor(p: AiProviderType): String = apiKeys[p].orEmpty()

    fun updateApiKey(p: AiProviderType, key: String) {
        apiKeys = apiKeys + (p to key)
        repo.saveApiKey(p, key)
    }

    fun updateProvider(p: AiProviderType) {
        provider = p
        repo.saveProvider(p)
    }

    fun findCustomerByNameOrNickname(name: String?, nickname: String?): Customer? =
        findCustomer(customers, name, nickname)

    fun submitOrder(draft: OrderDraft): Order {
        val result = createOrder(customers, orders, draft)
        customers = result.customers
        orders = result.orders
        repo.saveCustomers(result.customers)
        repo.saveOrders(result.orders)
        return result.newOrder
    }

    fun setOrderStatus(orderId: String, status: String) {
        orders = updateOrderStatus(orders, orderId, status)
        repo.saveOrders(orders)
    }

    fun settleDebt(customerId: String, amount: Long) {
        customers = settlePayment(customers, customerId, amount)
        repo.saveCustomers(customers)
    }

    // ---------------------------------------------------------------
    // Draft order — shared by the form (OrderScreen) and the voice assistant
    // ---------------------------------------------------------------

    fun updateDraftCustomerName(v: String) { draftCustomerName = v }
    fun updateDraftCustomerNickname(v: String) { draftCustomerNickname = v }
    fun updateDraftCustomerPhone(v: String) { draftCustomerPhone = v }
    fun updateDraftItems(v: List<OrderItem>) { draftItems = v }
    fun updateDraftPaymentStatus(v: String) { draftPaymentStatus = v }
    fun updateDraftNotes(v: String) { draftNotes = v }
    fun updateDraftIncludeDebt(v: Boolean) { draftIncludeDebt = v }
    fun clearDraftError() { draftError = "" }

    fun resetDraft() {
        draftCustomerName = ""
        draftCustomerNickname = ""
        draftCustomerPhone = ""
        draftItems = emptyList()
        draftPaymentStatus = "debt"
        draftNotes = ""
        draftIncludeDebt = true
        draftError = ""
    }

    /** Attempts to finalize the current draft into a real order. Returns an error message, or null on success. */
    fun confirmDraftOrder(): String? {
        if (draftCustomerName.isBlank()) return "نام مشتری را وارد کنید."
        if (draftItems.isEmpty()) return "حداقل یک قلم اضافه کنید."
        val baseDebt = findCustomerByNameOrNickname(draftCustomerName, draftCustomerNickname)?.debt ?: 0L
        val order = submitOrder(
            OrderDraft(
                customerName = draftCustomerName.trim(),
                customerNickname = draftCustomerNickname.trim(),
                customerPhone = draftCustomerPhone.trim(),
                items = draftItems.map { if (it.type == "سایر" && it.customType.isNotBlank()) it.copy(type = it.customType) else it },
                includeDebt = draftIncludeDebt && baseDebt > 0,
                paymentStatus = draftPaymentStatus,
                notes = draftNotes
            )
        )
        resetDraft()
        printOrderId = order.id
        page = Page.INVOICES
        return null
    }

    private fun mapParsedItems(itemsJson: List<JSONObject>): List<OrderItem> = itemsJson.map { it2 ->
        val type = it2.optString("type", "سایر")
        val servicesArr = it2.optJSONArray("services")
        val services = if (servicesArr != null) (0 until servicesArr.length()).map { i -> servicesArr.getString(i) }.filter { SERVICES.contains(it) } else emptyList()
        OrderItem(
            id = newId("i"),
            type = if (ITEM_TYPES.contains(type)) type else "سایر",
            customType = if (ITEM_TYPES.contains(type)) "" else type,
            count = it2.optInt("count", 1),
            services = services,
            description = it2.optString("description", ""),
            price = it2.optLong("unitPrice", 0)
        )
    }

    private fun mergeParsedIntoDraft(parsed: ir.khoshkshooyi.assistant.ai.ParsedAIResult) {
        if (!parsed.customerName.isNullOrBlank()) draftCustomerName = parsed.customerName
        if (!parsed.customerNickname.isNullOrBlank()) draftCustomerNickname = parsed.customerNickname
        if (parsed.paymentStatusMentioned == "paid" || parsed.paymentStatusMentioned == "debt") draftPaymentStatus = parsed.paymentStatusMentioned
        if (!parsed.notes.isNullOrBlank()) draftNotes = if (draftNotes.isBlank()) parsed.notes else "$draftNotes — ${parsed.notes}"
        if (parsed.items.isNotEmpty()) draftItems = draftItems + mapParsedItems(parsed.items)
    }

    // ---------------------------------------------------------------
    // Voice assistant
    // ---------------------------------------------------------------

    private fun customerStatusAnswer(name: String?, nickname: String?): String {
        val c = findCustomer(customers, name, nickname)
            ?: return "مشتری‌ای به اسم ${name ?: "؟"} پیدا نکردم."
        val history = orders.filter { it.customerId == c.id }
        val open = history.filter { it.status != "تحویل شد" }
        var text = "${c.name}${if (c.nickname.isNotBlank()) " (${c.nickname})" else ""}: "
        text += if (c.debt > 0) "${toman(c.debt)} بدهی دارد. " else "بدهی ندارد. "
        text += if (open.isNotEmpty()) {
            "سفارش‌های بازش: " + open.joinToString("، ") { "فاکتور #${it.invoiceNo}، ${it.items.size} قلم (${it.status})" } + "."
        } else "سفارش بازی ندارد."
        return text
    }

    fun startVoiceSession() {
        if (apiKey.isBlank()) {
            voiceError = "برای گفتگوی صوتی، ابتدا کلید API ${provider.displayName} را در تنظیمات وارد کنید."
            return
        }
        voiceError = ""
        validatingKey = true
        voiceStatus = VoiceStatus.PROCESSING
        sessionProvider = provider
        sessionApiKey = apiKey
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    clientFor(sessionProvider).validateApiKey(sessionApiKey)
                }
            } catch (e: Exception) {
                validatingKey = false
                voiceStatus = VoiceStatus.IDLE
                voiceError = "کلید API معتبر نیست: ${e.message ?: "نامشخص"} — آن را در تنظیمات بررسی و اصلاح کنید."
                return@launch
            }
            validatingKey = false
            conversationHistory = JSONArray()
            resetDraft()
            val greeting = "سلام! من دستیار خشکشویی‌تون هستم. سفارش رو براتون توضیح می‌دم بگید بنویسم، و هروقت گفتید «ثبتش کن» خودم برای فاکتور ثبتش می‌کنم."
            persistChatMessages(chatMessages + ChatMessage("assistant", greeting))
            liveInterim = ""
            sessionActive = true
            sessionShouldContinue = true
            voiceStatus = VoiceStatus.SPEAKING
            ttsPlayer.speak(greeting, sessionProvider, sessionApiKey)
            if (!sessionShouldContinue) return@launch
            conversationLoop()
        }
    }

    private suspend fun conversationLoop() {
        while (sessionShouldContinue) {
            voiceStatus = VoiceStatus.LISTENING
            liveInterim = ""
            val text = recognizer.listenOnce(
                onPartial = { partial -> liveInterim = partial },
                onError = { msg -> voiceError = msg }
            )
            if (!sessionShouldContinue) break
            liveInterim = ""
            if (text.isBlank()) continue
            voiceError = ""
            persistChatMessages(chatMessages + ChatMessage("user", text))
            voiceStatus = VoiceStatus.PROCESSING
            var shouldEndAfterReply = false
            val reply = try {
                val (parsed, updatedHistory) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    clientFor(sessionProvider).chat(conversationHistory, text, sessionApiKey)
                }
                conversationHistory = updatedHistory
                when (parsed.intent) {
                    "query" -> customerStatusAnswer(parsed.customerName, parsed.customerNickname)
                    "order" -> {
                        mergeParsedIntoDraft(parsed)
                        parsed.reply.ifBlank { "متوجه نشدم، می‌شه دوباره بگید؟" }
                    }
                    "submitOrder" -> {
                        mergeParsedIntoDraft(parsed)
                        val err = confirmDraftOrder()
                        if (err == null) {
                            shouldEndAfterReply = true
                            val newOrder = orders.firstOrNull()
                            if (newOrder != null) "ثبت شد ✅ فاکتور شماره ${newOrder.invoiceNo} برای ${newOrder.customerName} به مبلغ ${toman(newOrder.total)}."
                            else "سفارش با موفقیت ثبت شد."
                        } else {
                            "هنوز نمی‌تونم ثبتش کنم: $err"
                        }
                    }
                    else -> parsed.reply.ifBlank { "متوجه نشدم، می‌شه دوباره بگید؟" }
                }
            } catch (e: Exception) {
                voiceError = "پردازش با خطا مواجه شد (${e.message ?: "نامشخص"}). کلید API یا اتصال اینترنت را بررسی کنید."
                "الان نتونستم درست پردازش کنم، دوباره بگید لطفاً."
            }
            if (!sessionShouldContinue) break
            persistChatMessages(chatMessages + ChatMessage("assistant", reply))
            voiceStatus = VoiceStatus.SPEAKING
            val warn = ttsPlayer.speak(reply, sessionProvider, sessionApiKey)
            if (!warn.isNullOrBlank()) voiceError = warn
            if (!sessionShouldContinue) break
            if (shouldEndAfterReply) {
                endVoiceSession()
                break
            }
        }
        voiceStatus = VoiceStatus.IDLE
    }

    fun endVoiceSession() {
        sessionShouldContinue = false
        sessionActive = false
        recognizer.stop()
        ttsPlayer.stop()
        voiceStatus = VoiceStatus.IDLE
        liveInterim = ""
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.stop()
        ttsPlayer.destroy()
    }
}

fun toman(n: Long): String {
    val formatted = java.text.NumberFormat.getInstance(java.util.Locale("fa", "IR")).format(n)
    return "$formatted تومان"
}
