package ir.khoshkshooyi.assistant.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

fun newId(prefix: String): String =
    "${prefix}_${System.currentTimeMillis()}_${Random.nextInt(1000)}"

val ITEM_TYPES = listOf(
    "پرده", "شلوار", "پیراهن", "کت", "کت و شلوار", "پالتو", "مانتو", "روتختی", "رومیزی", "کاور مبل", "سایر"
)
val SERVICES = listOf("خشکشویی", "سفیدشویی", "اتو", "بخارزنی", "لکه‌گیری", "سایر")

data class Shop(
    val name: String = "",
    val owner: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("owner", owner)
    }

    companion object {
        fun fromJson(o: JSONObject): Shop = Shop(
            name = o.optString("name", ""),
            owner = o.optString("owner", "")
        )
    }
}

data class OrderItem(
    val id: String,
    val type: String,
    val customType: String = "",
    val count: Int = 1,
    val services: List<String> = emptyList(),
    val description: String = "",
    val price: Long = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("customType", customType)
        put("count", count)
        put("services", JSONArray(services))
        put("description", description)
        put("price", price)
    }

    companion object {
        fun fromJson(o: JSONObject): OrderItem {
            val servicesArr = o.optJSONArray("services") ?: JSONArray()
            val services = (0 until servicesArr.length()).map { servicesArr.getString(it) }
            return OrderItem(
                id = o.optString("id", newId("i")),
                type = o.optString("type", ITEM_TYPES[0]),
                customType = o.optString("customType", ""),
                count = o.optInt("count", 1),
                services = services,
                description = o.optString("description", ""),
                price = o.optLong("price", 0)
            )
        }
    }
}

data class Customer(
    val id: String,
    val name: String,
    val nickname: String = "",
    val phone: String = "",
    val debt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("nickname", nickname)
        put("phone", phone)
        put("debt", debt)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(o: JSONObject): Customer = Customer(
            id = o.optString("id", newId("c")),
            name = o.optString("name", ""),
            nickname = o.optString("nickname", ""),
            phone = o.optString("phone", ""),
            debt = o.optLong("debt", 0),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

data class Order(
    val id: String,
    val invoiceNo: Int,
    val date: Long,
    val customerId: String,
    val customerName: String,
    val customerNickname: String = "",
    val items: List<OrderItem>,
    val previousDebtLine: Long = 0,
    val itemsTotal: Long,
    val total: Long,
    val paymentStatus: String, // "paid" | "debt"
    val status: String = "در حال آماده‌سازی",
    val notes: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("invoiceNo", invoiceNo)
        put("date", date)
        put("customerId", customerId)
        put("customerName", customerName)
        put("customerNickname", customerNickname)
        put("items", JSONArray(items.map { it.toJson() }))
        put("previousDebtLine", previousDebtLine)
        put("itemsTotal", itemsTotal)
        put("total", total)
        put("paymentStatus", paymentStatus)
        put("status", status)
        put("notes", notes)
    }

    companion object {
        fun fromJson(o: JSONObject): Order {
            val itemsArr = o.optJSONArray("items") ?: JSONArray()
            val items = (0 until itemsArr.length()).map { OrderItem.fromJson(itemsArr.getJSONObject(it)) }
            return Order(
                id = o.optString("id", newId("o")),
                invoiceNo = o.optInt("invoiceNo", 0),
                date = o.optLong("date", System.currentTimeMillis()),
                customerId = o.optString("customerId", ""),
                customerName = o.optString("customerName", ""),
                customerNickname = o.optString("customerNickname", ""),
                items = items,
                previousDebtLine = o.optLong("previousDebtLine", 0),
                itemsTotal = o.optLong("itemsTotal", 0),
                total = o.optLong("total", 0),
                paymentStatus = o.optString("paymentStatus", "debt"),
                status = o.optString("status", "در حال آماده‌سازی"),
                notes = o.optString("notes", "")
            )
        }
    }
}

/** One message bubble in the voice-assistant chat log. role: "user" | "assistant" */
data class ChatMessage(val role: String, val text: String) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("role", role)
        put("text", text)
    }

    companion object {
        fun fromJson(o: JSONObject): ChatMessage = ChatMessage(
            role = o.optString("role", "assistant"),
            text = o.optString("text", "")
        )
    }
}

/** Draft used while building a new order on the OrderScreen, before it's committed. */
data class OrderDraft(
    val customerName: String = "",
    val customerNickname: String = "",
    val customerPhone: String = "",
    val items: List<OrderItem> = emptyList(),
    val includeDebt: Boolean = true,
    val paymentStatus: String = "debt",
    val notes: String = ""
)
