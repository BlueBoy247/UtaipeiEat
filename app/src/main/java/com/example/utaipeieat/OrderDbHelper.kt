package com.example.utaipeieat

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OrderDbHelper(context: Context) : SQLiteOpenHelper(context, "utaipei_eat.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        // ✅ 修改：多加一個 firebaseId 欄位
        db.execSQL("CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT, firebaseId TEXT, shopName TEXT, items TEXT, price INTEGER, date LONG)")

        db.execSQL("CREATE TABLE cart (id INTEGER PRIMARY KEY AUTOINCREMENT, shopName TEXT, itemName TEXT, price INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS history")
        db.execSQL("DROP TABLE IF EXISTS cart")
        onCreate(db)
    }

    // --- 購物車功能 (Cart) ---

    // 加入購物車
    fun addToCart(shopName: String, itemName: String, price: Int) {
        val values = ContentValues().apply {
            put("shopName", shopName)
            put("itemName", itemName)
            put("price", price)
        }
        writableDatabase.insert("cart", null, values)
    }

    // 讀取購物車所有商品
    fun getCartItems(): List<CartItem> {
        val list = mutableListOf<CartItem>()
        val db = readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM cart", null)

        if (cursor.moveToFirst()) {
            do {
                list.add(
                    CartItem(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        shopName = cursor.getString(cursor.getColumnIndexOrThrow("shopName")),
                        itemName = cursor.getString(cursor.getColumnIndexOrThrow("itemName")),
                        price = cursor.getInt(cursor.getColumnIndexOrThrow("price"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // 清空購物車 (送出訂單後使用)
    fun clearCart() {
        writableDatabase.execSQL("DELETE FROM cart")
    }

    // --- 歷史紀錄功能 (History) ---

    // ✅ 修改：新增 firebaseId 參數
    fun addHistory(firebaseId: String, shopName: String, items: String, price: Int) {
        val values = ContentValues().apply {
            put("firebaseId", firebaseId) // 存入雲端 ID
            put("shopName", shopName)
            put("items", items)
            put("price", price)
            put("date", System.currentTimeMillis())
        }
        writableDatabase.insert("history", null, values)
    }

    // ✅ 修改：讀取時也要讀 firebaseId
    fun getHistory(): List<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM history ORDER BY date DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    HistoryItem(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        firebaseId = cursor.getString(cursor.getColumnIndexOrThrow("firebaseId")), // 讀取 ID
                        shopName = cursor.getString(cursor.getColumnIndexOrThrow("shopName")),
                        items = cursor.getString(cursor.getColumnIndexOrThrow("items")),
                        price = cursor.getInt(cursor.getColumnIndexOrThrow("price")),
                        date = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // ✅ 新增 1: 計算某個商品在購物車內的數量 (給菜單的 Badge 用)
    fun getItemCount(shopName: String, itemName: String): Int {
        val db = readableDatabase
        // 計算符合 店家 與 品項名稱 的資料有幾筆
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM cart WHERE shopName = ? AND itemName = ?",
            arrayOf(shopName, itemName)
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    // ✅ 新增 2: 刪除「一筆」特定商品 (給購物車的 -1 按鈕用)
    fun removeOneItem(shopName: String, itemName: String) {
        val db = writableDatabase
        // 子查詢技巧：只找出「第一筆」符合該名稱的 ID，然後刪除它
        // 這樣才不會按一次 -1 就把所有同名商品都刪光
        db.execSQL(
            "DELETE FROM cart WHERE id = (SELECT id FROM cart WHERE shopName = ? AND itemName = ? LIMIT 1)",
            arrayOf(shopName, itemName)
        )
    }
}
