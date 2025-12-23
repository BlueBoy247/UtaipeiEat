# UtaipeiEat

此為臺北市立大學資訊科學系「Android應用程式專案開發與社群媒體應用」課程之成果。<br>
This is the outcome of the course Developing Android App and Social Media Applications.

## 簡介
UtaipeiEat 是一個校園學餐訂餐平台，提供學生、教職員與學生餐廳商家一個便利的訂餐服務管道。系統分為兩大角色：客戶（學生/教職員）與商家，客戶可透過此 APP 進行下單及追蹤訂單狀態，商家則可管理訂單。

## 動機
目前校內多數餐廳皆透過 LINE 社群或官方帳號提供點餐服務，然而商家有時會更替，學生在就學期間可能會因此需要加入很多不同的官方帳號或社群。此外，LINE 社群成員眾多，訊息流量大，一天可能累積近百則訊息，容易淹沒點餐相關資訊，對學生而言相當不便。因此，本專題旨在開發一款整合式點餐 APP，將校內餐廳的點餐管道集中管理，以提升使用便利性與整體點餐效率。

## 版本
### 應用程式版本資訊
* 版本號
* 版本代碼：1
* 支援平台：Android
* 最低支援版本：Android 12 (API Level 31)
* 目標版本：Android 16 (API Level 36)
### 開發環境
* 整合開發環境 (IDE)：Android Studio 2025.1.3.7
* 測試裝置：Pixel 9a 模擬器 (API 36) / 實體 Android 手機

## 使用技術
* 程式語言：Kotlin
* 建置工具：Gradle
* 前端介面
  * 框架：Jetpack Compose
  * 設計系統：Material Design 3
* 後端與雲端服務
  * 雲端平台：Google Firebase.
  * 即時資料庫：Firebase Realtime Database
* 本地資料儲存
  * 資料庫技術：Android Native SQLite
* 通知系統
  * 通知類型：本地推播通知
  * 權限管理：支援 Android 13+ (API 33) 動態請求 POST_NOTIFICATIONS 權限
