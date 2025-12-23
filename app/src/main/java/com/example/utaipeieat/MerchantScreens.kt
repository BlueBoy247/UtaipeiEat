package com.example.utaipeieat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MerchantOrder(
    val id: String = "",
    val shopName: String = "",
    val items: String = "", // 例如: "燒肉叉燒飯 x1, 湯 x1"
    val price: Int = 0,
    val status: String = "未付款",
    val mealStatus: String = "準備中",
    val date: Long = 0
)

// --- 1. 商家首頁 (菜單管理) ---
@Composable
fun MerchantHomeScreen(navController: NavController) {
    val shopName = "佳香燒臘"
    val database = Firebase.database
    val menuRef = database.getReference("menus").child(shopName)

    var menuItems by remember { mutableStateOf(listOf<Pair<String, Int>>()) }

    LaunchedEffect(Unit) {
        // 初始化預設菜單 (如果資料庫是空的)
        menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    menuRef.child("燒肉叉燒飯").setValue(100)
                    menuRef.child("燒肉油雞飯").setValue(100)
                    menuRef.child("燒肉香腸飯").setValue(100)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 即時監聽菜單變化
        menuRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Pair<String, Int>>()
                snapshot.children.forEach {
                    val name = it.key ?: ""
                    val price = it.getValue(Int::class.java) ?: 0
                    list.add(name to price)
                }
                menuItems = list
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = { AppHeader() },
        bottomBar = { MerchantBottomNav(navController) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text("哈囉～開始營業囉！", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("目前菜單：", fontSize = 18.sp, color = Color.Gray)

            LazyColumn {
                items(menuItems) { (name, price) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, fontSize = 18.sp)
                            Text("$$price", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

// --- 2. 商家訂單列表 (接單畫面) ---
@Composable
fun MerchantOrderListScreen(navController: NavController) {
    val shopName = "佳香燒臘"
    var orders by remember { mutableStateOf(listOf<MerchantOrder>()) }
    val database = Firebase.database.getReference("orders")

    LaunchedEffect(Unit) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<MerchantOrder>()
                snapshot.children.forEach { data ->
                    // 這裡會自動把資料庫的 JSON 轉成 MerchantOrder 物件
                    // 只要欄位名稱 (items, price, status...) 對得上就會成功
                    val order = data.getValue(MerchantOrder::class.java)
                    if (order != null && order.shopName == shopName) {
                        list.add(order.copy(id = data.key ?: ""))
                    }
                }
                // 依照時間排序 (新的在上面)
                orders = list.sortedByDescending { it.date }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = { AppHeader() },
        bottomBar = { MerchantBottomNav(navController) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text("接單紀錄", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))

            if (orders.isEmpty()) {
                Text("目前還沒有訂單喔...", modifier = Modifier.padding(16.dp), color = Color.Gray)
            }

            LazyColumn {
                items(orders) { order ->
                    val dateStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(order.date))

                    // 列表項目
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (orders.indexOf(order) % 2 == 0) Color(0xFFEEEEEE) else Color.White),
                        shape = androidx.compose.ui.graphics.RectangleShape, // 方形背景
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("merchant_order_detail/${order.id}") }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("訂單時間: $dateStr", fontSize = 12.sp, color = Color.Gray)
                                Text(order.items, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$${order.price}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                // 顯示目前狀態的小字
                                Text(order.mealStatus, fontSize = 12.sp, color = if(order.mealStatus=="可取餐") Color(0xFF00A67E) else Color(0xFFD4C100))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 3. 商家訂單詳細資訊 ---
@Composable
fun MerchantOrderDetailScreen(navController: NavController, orderId: String) {
    val context = LocalContext.current
    val database = Firebase.database.getReference("orders").child(orderId)
    var order by remember { mutableStateOf<MerchantOrder?>(null) }

    LaunchedEffect(orderId) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                order = snapshot.getValue(MerchantOrder::class.java)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(topBar = { AppHeader() }) { innerPadding ->
        order?.let { currentOrder ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("訂單詳情", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 顯示訂單編號
                        Text("單號: $orderId", fontSize = 14.sp, color = Color.Gray)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // 顯示真正的訂單內容 (從 DB 讀出來的)
                        Text("內容：", fontWeight = FontWeight.Bold)
                        Text(currentOrder.items, fontSize = 18.sp)

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("總金額", fontWeight = FontWeight.Bold)
                            Text("$${currentOrder.price}", fontWeight = FontWeight.Bold, color = Color.Red)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // --- 狀態控制區 ---

                        // 1. 付款狀態
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("付款狀態")
                            Button(
                                onClick = {
                                    val newStatus = if (currentOrder.status == "未付款") "已付款" else "未付款"
                                    database.child("status").setValue(newStatus)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentOrder.status == "未付款") Color.Red else Color(0xFF00A67E)
                                )
                            ) {
                                Text(currentOrder.status)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. 餐點狀態
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("餐點狀態")
                            Button(
                                onClick = {
                                    // 切換狀態
                                    val newStatus = if (currentOrder.mealStatus == "準備中") "可取餐" else "準備中"

                                    // ⚠更新資料庫
                                    // 當這裡被寫入 "可取餐" 時，User 端的監聽器就會觸發，並跳出通知
                                    database.child("mealStatus").setValue(newStatus)

                                    if (newStatus == "可取餐") {
                                        Toast.makeText(context, "已更新狀態，客戶將收到通知", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentOrder.mealStatus == "準備中") Color(0xFFD4C100) else Color(0xFF00A67E)
                                )
                            ) {
                                Text(currentOrder.mealStatus)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 商家導覽列 (不變) ---
@Composable
fun MerchantBottomNav(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFD6E2FF)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Text("首頁", Modifier.clickable { navController.navigate("merchant_home") })
        Text("紀錄", Modifier.clickable { navController.navigate("merchant_orders") })
        /*Text("設定", Modifier.clickable { })*/
    }
}