package com.example.utaipeieat

// 定義訂單狀態常量
object OrderStatus {
    const val UNPAID = "未付款" // 紅色標籤
    const val PREPARING = "準備中" // 黃色標籤
    const val READY = "可取餐" // 綠色標籤
}

data class Order(
    val orderId: String = "No. ${System.currentTimeMillis()}", // 訂單編號格式
    val shopName: String = "",
    val userName: String = "",
    val items: List<CartItem> = listOf(),
    val totalAmount: Int = 0,
    val paymentStatus: String = OrderStatus.UNPAID,
    val orderStatus: String = OrderStatus.PREPARING,
    val timestamp: Long = System.currentTimeMillis()
)

// ⚠️ 記得更新 Data Class
data class HistoryItem(val id: Int, val firebaseId: String, val shopName: String, val items: String, val price: Int, val date: Long)
// CartItem 維持不變
data class CartItem(val id: Int, val shopName: String, val itemName: String, val price: Int)