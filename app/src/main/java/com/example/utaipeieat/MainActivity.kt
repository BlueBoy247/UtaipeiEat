package com.example.utaipeieat

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.ChildEventListener

class MainActivity : ComponentActivity() {
    private val appStartTime = System.currentTimeMillis()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // 初始化 Firebase

        startOrderListener()

        startMerchantListener()

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current

            CheckNotificationPermission(context)

            NavHost(navController = navController, startDestination = "login") {
                composable("login") { LoginScreen(navController) }

                composable("home") { HomeScreen(navController) }
                composable("menu/{shopName}") { backStack ->
                    val shopName = backStack.arguments?.getString("shopName") ?: ""
                    MenuScreen(navController, shopName)
                }
                composable("cart") { CartScreen(navController) }
                composable("history") { HistoryScreen(navController) }
                composable("history_detail/{id}") { backStack ->
                    val id = backStack.arguments?.getString("id")?.toIntOrNull() ?: 0
                    UserOrderDetailScreen(navController, id)
                }

                composable("merchant_home") { MerchantHomeScreen(navController) }
                composable("merchant_orders") { MerchantOrderListScreen(navController) }
                composable("merchant_order_detail/{orderId}") { backStack ->
                    val orderId = backStack.arguments?.getString("orderId") ?: ""
                    MerchantOrderDetailScreen(navController, orderId)
                }
            }
        }
    }

    private fun startOrderListener() {
        val database = Firebase.database.getReference("orders")
        val prefs = getSharedPreferences("my_orders", Context.MODE_PRIVATE)

        database.addChildEventListener(object : ChildEventListener {
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val orderId = snapshot.key
                val myOrderIds = prefs.getStringSet("ids", setOf()) ?: setOf()

                // 檢查：這張變動的單是不是我的？
                if (myOrderIds.contains(orderId)) {
                    val status = snapshot.child("mealStatus").getValue(String::class.java)
                    val shopName = snapshot.child("shopName").getValue(String::class.java) ?: "UtaipeiEat"

                    // 如果狀態變成「可取餐」，就發通知
                    if (status == "可取餐") {
                        showLocalNotification(shopName, "您的餐點已經準備好囉，請前往取餐！")
                    }
                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 商家接單監聽器
    private fun startMerchantListener() {
        val database = Firebase.database.getReference("orders")
        val myShopName = "佳香燒臘" // 目前寫死

        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val shopName = snapshot.child("shopName").getValue(String::class.java)
                val date = snapshot.child("date").getValue(Long::class.java) ?: 0L

                // 是我的店 且 訂單時間晚於 App 啟動時間 (新進來的單)
                if (shopName == myShopName && date > appStartTime) {
                    val price = snapshot.child("price").getValue(Int::class.java) ?: 0
                    showLocalNotification("新訂單通知", "收到一筆新訂單！金額：$$price")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showLocalNotification(title: String, content: String) {
        val channelId = "order_ready_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0+ 需要建立頻道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "取餐通知", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 設定小圖示
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

// --- 共用元件 ---

@Composable
fun AppHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF5C79D4))
            .padding(16.dp)
    ) {
        Text("UtaipeiEat", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BottomNav(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD6E2FF))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Text("首頁", Modifier.clickable { navController.navigate("home") })
        Text("紀錄", Modifier.clickable { navController.navigate("history") })
        /*Text("設定", Modifier.clickable { /* 導向設定 */ })*/
    }
}

// --- UI 實作 ---

@Composable
fun LoginScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("UtaipeiEat", fontSize = 32.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF5C79D4))
            .padding(20.dp))

        Spacer(modifier = Modifier.height(50.dp))

        Button(
            onClick = { navController.navigate("home") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
            modifier = Modifier.width(200.dp)
        ) {
            Text("教職員生")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { navController.navigate("merchant_home") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
            modifier = Modifier.width(200.dp)
        ) {
            Text("合作商家")
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    // 取得資料庫參照：指向 "menus" 節點
    val database = Firebase.database.getReference("menus")

    // 用 State 來存商家清單
    var shopList by remember { mutableStateOf(listOf<String>()) }

    // 監聽資料庫變化
    LaunchedEffect(Unit) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<String>()
                // 遍歷 menus 底下的所有子節點 (即商家名稱)
                snapshot.children.forEach { shopSnapshot ->
                    shopSnapshot.key?.let { list.add(it) }
                }
                shopList = list
            }

            override fun onCancelled(error: DatabaseError) {
                // 處理錯誤，例如網路連線失敗
            }
        })
    }

    Scaffold(
        topBar = { AppHeader() },
        bottomBar = { BottomNav(navController) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text("哈囉～想吃什麼？", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // 如果資料庫是空的，顯示提示
            if (shopList.isEmpty()) {
                Text("目前沒有營業中的商家...", color = Color.Gray)
            }

            // 動態產生商家卡片
            LazyColumn {
                items(shopList) { shopName ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)), //
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(100.dp)
                            .clickable { navController.navigate("menu/$shopName") } // 點擊後傳遞商家名稱
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(shopName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuScreen(navController: NavController, shopName: String) {
    val context = LocalContext.current
    val dbHelper = remember { OrderDbHelper(context) } // 記得用 remember

    // 1. 菜單資料 (從 Firebase 抓)
    var menuItems by remember { mutableStateOf(listOf<Pair<String, Int>>()) }

    // 2. 購物車數量狀態 Map (Key: 品名, Value: 數量)
    // 這樣畫面才知道哪張卡片要顯示數字 1, 2, 3...
    var cartCounts by remember { mutableStateOf(mapOf<String, Int>()) }

    // 更新數量的 Helper function
    fun refreshCounts() {
        // 直接讀所有 items 並重新計算 map
        val allCartItems = dbHelper.getCartItems()
        // 篩選出目前這家店的，並統計數量
        cartCounts = allCartItems
            .filter { it.shopName == shopName }
            .groupingBy { it.itemName }
            .eachCount()
    }

    // 初始化：抓菜單 + 抓購物車數量
    LaunchedEffect(shopName) {
        refreshCounts() // 進來先更新一次數字

        val database = Firebase.database.getReference("menus").child(shopName)
        database.addValueEventListener(object : ValueEventListener {
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
        bottomBar = {
            // 底部購物車預覽 (可以顯示總金額)
            val totalQty = cartCounts.values.sum()
            val totalPrice = dbHelper.getCartItems().sumOf { it.price } // 簡單計算總價

            if (totalQty > 0) { // 有點餐才顯示底部紅條
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFF8A8A))
                        .clickable { navController.navigate("cart") }
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("購物車", color = Color.White)
                        Text("$totalQty 項", color = Color.White)
                        Text("$$totalPrice", color = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(shopName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn {
                items(menuItems) { (name, price) ->
                    // 取得目前這道菜點了幾個
                    val count = cartCounts[name] ?: 0

                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, end = 8.dp) // 留一點空間給 Badge
                                .clickable {
                                    dbHelper.addToCart(shopName, name, price)
                                    refreshCounts() // 點擊後重新計算數量，UI 會自動更新 Badge
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, fontSize = 18.sp)
                                Text("$$price", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 紅色圓圈標記 (Badge)
                        if (count > 0) {
                            Surface(
                                color = Color.Red,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd) // 對齊右上角
                                    .size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = count.toString(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartScreen(navController: NavController) {
    val context = LocalContext.current
    val dbHelper = remember { OrderDbHelper(context) }

    // 讀取原始資料列表
    var rawCartItems by remember { mutableStateOf(dbHelper.getCartItems()) }

    // 將原始列表轉成 Group，方便顯示數量
    // Map<品項名稱, List<CartItem>>
    val groupedItems = rawCartItems.groupBy { it.itemName }
    val totalAmount = rawCartItems.sumOf { it.price }

    fun refreshCart() {
        rawCartItems = dbHelper.getCartItems()
    }

    Scaffold(
        topBar = { AppHeader() },
        bottomBar = { BottomNav(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("訂單資訊", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))

            if (rawCartItems.isEmpty()) {
                Text("購物車是空的", modifier = Modifier.padding(20.dp), color = Color.Gray)
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
                    modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp).weight(1f)
                ) {
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        // 標題 (顯示第一家店名)
                        item {
                            Text(rawCartItems.first().shopName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }

                        // 顯示合併後的品項
                        items(groupedItems.keys.toList()) { itemName ->
                            val items = groupedItems[itemName]!! // 該品項的所有資料
                            val count = items.size
                            val price = items.first().price // 單價
                            val shopName = items.first().shopName

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左邊：品名
                                Text(itemName, modifier = Modifier.weight(1f))

                                // 中間：數量控制區 (- 1 +)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // ➖ 按鈕
                                    IconButton(
                                        onClick = {
                                            // 呼叫 DB 刪除一筆
                                            dbHelper.removeOneItem(shopName, itemName)
                                            refreshCart() // 刷新畫面
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 數量
                                    Text("x$count", modifier = Modifier.padding(horizontal = 8.dp))

                                    // ➕ 按鈕
                                    IconButton(
                                        onClick = {
                                            // 呼叫 DB 新增一筆
                                            dbHelper.addToCart(shopName, itemName, price)
                                            refreshCart() // 刷新畫面
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 右邊：小計
                                Text("$${price * count}", modifier = Modifier.width(60.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            }
                        }

                        // 總計區
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("合計", fontWeight = FontWeight.Bold)
                                Text("$$totalAmount", fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                        }
                    }
                }
            }

            // 送出按鈕
            Button(
                onClick = {
                    if (rawCartItems.isEmpty()) return@Button
                    val database = Firebase.database
                    val ordersRef = database.getReference("orders")

                    // 這裡改用 "燒肉飯 x2, 湯 x1" 這種格式上傳，商家看比較清楚
                    val itemsDesc = groupedItems.map { "${it.key} x${it.value.size}" }.joinToString(", ")
                    val shopName = rawCartItems.first().shopName

                    val orderData = mapOf(
                        "shopName" to shopName,
                        "items" to itemsDesc, // 例如: "燒肉叉燒飯 x2"
                        "price" to totalAmount,
                        "status" to "未付款",
                        "mealStatus" to "準備中",
                        "date" to System.currentTimeMillis()
                    )

                    val newOrderRef = ordersRef.push()
                    val orderId = newOrderRef.key ?: "" // 取得 Firebase 產生的 ID

                    newOrderRef.setValue(orderData).addOnSuccessListener {
                        val itemsDesc = groupedItems.map { "${it.key} x${it.value.size}" }.joinToString(", ")
                        // 1. 寫入 SQLite (歷史紀錄)
                        dbHelper.addHistory(orderId, shopName, itemsDesc, totalAmount)
                        dbHelper.clearCart()

                        // 2. 把 orderId 存到 SharedPreferences，標記為「我的訂單」
                        val prefs = context.getSharedPreferences("my_orders", Context.MODE_PRIVATE)
                        val currentList = prefs.getStringSet("ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                        currentList.add(orderId)
                        prefs.edit().putStringSet("ids", currentList).apply()

                        // 3. 跳轉
                        navController.navigate("history")
                        Toast.makeText(context, "訂單已送出，ID: $orderId", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                modifier = Modifier.width(200.dp).padding(bottom = 20.dp),
                enabled = rawCartItems.isNotEmpty()
            ) {
                Text("送出")
            }
        }
    }
}

@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    // 使用 remember 取得資料庫內容
    val historyList = remember {
        OrderDbHelper(context).getHistory()
    }

    // 訂單紀錄
    Scaffold(
        topBar = { AppHeader() },
        bottomBar = { BottomNav(navController) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text("訂單紀錄", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))

            LazyColumn {
                items(historyList) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (historyList.indexOf(item) % 2 == 0) Color(0xFFEEEEEE) else Color.White)
                            .clickable {
                                // ✅ 點擊導航到詳情頁，並傳遞 ID
                                navController.navigate("history_detail/${item.id}")
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(item.shopName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            // 可以顯示簡短內容
                            Text(item.items, fontSize = 14.sp, color = Color.Gray, maxLines = 1)
                        }
                        Text("$${item.price}", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun UserOrderDetailScreen(navController: NavController, historyId: Int) {
    val context = LocalContext.current
    val dbHelper = remember { OrderDbHelper(context) }

    // 1. 先從 SQLite 拿到基本資料 (包含 firebaseId)
    val historyItem = remember {
        dbHelper.getHistory().find { it.id == historyId }
    }

    // 2. 用 State 存即時狀態
    var currentMealStatus by remember { mutableStateOf("讀取中...") }
    var currentPaymentStatus by remember { mutableStateOf("讀取中...") }

    // 3. 監聽 Firebase
    LaunchedEffect(historyItem) {
        if (historyItem != null && historyItem.firebaseId.isNotEmpty()) {
            val ref = Firebase.database.getReference("orders").child(historyItem.firebaseId)
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        currentMealStatus = snapshot.child("mealStatus").getValue(String::class.java) ?: "未知"
                        currentPaymentStatus = snapshot.child("status").getValue(String::class.java) ?: "未知"
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    Scaffold(topBar = { AppHeader() }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (historyItem != null) {
                Text("訂單詳情", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0))) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(historyItem.shopName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("單號: ${historyItem.firebaseId}", color = Color.Gray, fontSize = 12.sp)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text("內容：", fontWeight = FontWeight.Bold)
                        Text(historyItem.items, fontSize = 18.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        // 顯示即時狀態 (使用有顏色的按鈕或文字)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("付款狀態：")
                            Text(
                                text = currentPaymentStatus,
                                color = if(currentPaymentStatus == "已付款") Color(0xFF00A67E) else Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("製作進度：")
                            // 用背景色凸顯狀態
                            Surface(
                                color = if(currentMealStatus == "可取餐") Color(0xFF00A67E) else Color(0xFFD4C100),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = currentMealStatus,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("總金額", fontWeight = FontWeight.Bold)
                            Text("$${historyItem.price}", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text("找不到此訂單資料")
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("返回")
            }
        }
    }
}

@Composable
fun CheckNotificationPermission(context: Context) {
    // 只有 Android 13 (API 33) 以上才需要動態請求
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = Manifest.permission.POST_NOTIFICATIONS

        // 建立一個請求權限的啟動器
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    // 使用者允許了
                } else {
                    // 使用者拒絕了，可以考慮跳出 Toast 提醒他去設定開啟
                    Toast.makeText(context, "請開啟通知權限以免漏接餐點訊息", Toast.LENGTH_LONG).show()
                }
            }
        )

        // 當畫面啟動時，檢查是否有權限
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                // 如果沒權限，就跳出詢問視窗
                launcher.launch(permission)
            }
        }
    }
}