package ir.khoshkshooyi.assistant.data

import android.content.Context
import ir.khoshkshooyi.assistant.ai.AiProviderType
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "khoshkshooyi_prefs"
private const val KEY_SHOP = "shop"
private const val KEY_CUSTOMERS = "customers"
private const val KEY_ORDERS = "orders"
private const val KEY_API_KEY_LEGACY = "apiKey" // pre-multi-provider: single OpenAI key
private const val KEY_API_KEYS = "apiKeysByProvider" // JSON object: {"openai": "...", "gemini": "..."}
private const val KEY_AI_PROVIDER = "aiProvider"
private const val KEY_CHAT_MESSAGES = "chatMessages"
private const val MAX_SAVED_CHAT_MESSAGES = 100

class Repository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyApiKey()
    }

    /** Users upgrading from the single-provider version keep their existing OpenAI key. */
    private fun migrateLegacyApiKey() {
        val legacy = prefs.getString(KEY_API_KEY_LEGACY, null)
        if (!legacy.isNullOrBlank() && prefs.getString(KEY_API_KEYS, null) == null) {
            val obj = JSONObject().put(AiProviderType.OPENAI.id, legacy)
            prefs.edit().putString(KEY_API_KEYS, obj.toString()).remove(KEY_API_KEY_LEGACY).apply()
        }
    }

    fun loadShop(): Shop? {
        val raw = prefs.getString(KEY_SHOP, null) ?: return null
        return try { Shop.fromJson(JSONObject(raw)) } catch (e: Exception) { null }
    }

    fun saveShop(shop: Shop) {
        prefs.edit().putString(KEY_SHOP, shop.toJson().toString()).apply()
    }

    fun loadCustomers(): List<Customer> {
        val raw = prefs.getString(KEY_CUSTOMERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Customer.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    fun saveCustomers(list: List<Customer>) {
        prefs.edit().putString(KEY_CUSTOMERS, JSONArray(list.map { it.toJson() }).toString()).apply()
    }

    fun loadOrders(): List<Order> {
        val raw = prefs.getString(KEY_ORDERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Order.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    fun saveOrders(list: List<Order>) {
        prefs.edit().putString(KEY_ORDERS, JSONArray(list.map { it.toJson() }).toString()).apply()
    }

    fun loadApiKey(provider: AiProviderType): String {
        val raw = prefs.getString(KEY_API_KEYS, null) ?: return ""
        return try { JSONObject(raw).optString(provider.id, "") } catch (e: Exception) { "" }
    }

    fun saveApiKey(provider: AiProviderType, key: String) {
        val raw = prefs.getString(KEY_API_KEYS, null)
        val obj = try { if (raw != null) JSONObject(raw) else JSONObject() } catch (e: Exception) { JSONObject() }
        obj.put(provider.id, key)
        prefs.edit().putString(KEY_API_KEYS, obj.toString()).apply()
    }

    fun loadProvider(): AiProviderType = AiProviderType.fromId(prefs.getString(KEY_AI_PROVIDER, null))

    fun saveProvider(provider: AiProviderType) {
        prefs.edit().putString(KEY_AI_PROVIDER, provider.id).apply()
    }

    fun loadChatMessages(): List<ChatMessage> {
        val raw = prefs.getString(KEY_CHAT_MESSAGES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { ChatMessage.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    /** Only ever keeps the most recent [MAX_SAVED_CHAT_MESSAGES] messages on disk. */
    fun saveChatMessages(list: List<ChatMessage>) {
        val capped = list.takeLast(MAX_SAVED_CHAT_MESSAGES)
        prefs.edit().putString(KEY_CHAT_MESSAGES, JSONArray(capped.map { it.toJson() }).toString()).apply()
    }
}

private fun normalize(s: String?): String = (s ?: "").trim().lowercase()

/** Same fuzzy matching rule as the web app's findCustomer(). */
fun findCustomer(customers: List<Customer>, name: String?, nickname: String?): Customer? {
    val n = normalize(name)
    val nn = normalize(nickname)
    return customers.find { c ->
        val cn = normalize(c.name)
        val cnn = normalize(c.nickname)
        (n.isNotEmpty() && cn.isNotEmpty() && (cn == n || cn.contains(n) || n.contains(cn))) ||
            (nn.isNotEmpty() && cnn.isNotEmpty() && cnn == nn)
    }
}

data class CreateOrderResult(
    val customers: List<Customer>,
    val orders: List<Order>,
    val newOrder: Order
)

/**
 * Mirrors createOrder() from the original app: computes item totals, folds in previous
 * debt when requested, updates (or creates) the customer record, and appends the order.
 */
fun createOrder(
    customers: List<Customer>,
    orders: List<Order>,
    draft: OrderDraft
): CreateOrderResult {
    val matched = findCustomer(customers, draft.customerName, draft.customerNickname)
    val baseDebt = matched?.debt ?: 0L
    val itemsTotal = draft.items.sumOf { it.price * it.count }
    val previousDebtLine = if (draft.includeDebt) baseDebt else 0L
    val total = itemsTotal + previousDebtLine

    val newDebt = if (draft.paymentStatus == "paid") {
        if (draft.includeDebt) 0L else baseDebt
    } else {
        if (draft.includeDebt) total else baseDebt + itemsTotal
    }

    val updatedCustomers: List<Customer>
    val customerId: String
    if (matched != null) {
        updatedCustomers = customers.map {
            if (it.id == matched.id) it.copy(
                name = draft.customerName.ifBlank { it.name },
                nickname = draft.customerNickname.ifBlank { it.nickname },
                phone = draft.customerPhone.ifBlank { it.phone },
                debt = newDebt
            ) else it
        }
        customerId = matched.id
    } else {
        val newCustomer = Customer(
            id = newId("c"),
            name = draft.customerName,
            nickname = draft.customerNickname,
            phone = draft.customerPhone,
            debt = newDebt
        )
        updatedCustomers = customers + newCustomer
        customerId = newCustomer.id
    }

    val order = Order(
        id = newId("o"),
        invoiceNo = orders.size + 1,
        date = System.currentTimeMillis(),
        customerId = customerId,
        customerName = draft.customerName,
        customerNickname = draft.customerNickname,
        items = draft.items,
        previousDebtLine = previousDebtLine,
        itemsTotal = itemsTotal,
        total = total,
        paymentStatus = draft.paymentStatus,
        status = "در حال آماده‌سازی",
        notes = draft.notes
    )
    val updatedOrders = listOf(order) + orders
    return CreateOrderResult(updatedCustomers, updatedOrders, order)
}

fun settlePayment(customers: List<Customer>, customerId: String, amount: Long): List<Customer> =
    customers.map {
        if (it.id == customerId) it.copy(debt = maxOf(0L, it.debt - amount)) else it
    }

fun updateOrderStatus(orders: List<Order>, orderId: String, status: String): List<Order> =
    orders.map { if (it.id == orderId) it.copy(status = status) else it }
