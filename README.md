# 打卡助手

打卡助手是一個 Android 原生應用，用來記錄彈性工時下的上班、下班時間，並計算安全下班時間。專案目前面向個人自用與小範圍試用，不包含服務端。

## 功能

- 一鍵記錄上班與下班
- 首次使用三步引導：每日工時、午休扣除、安全緩衝
- 自訂每日工時、午休時長、安全緩衝時間
- 自動計算安全下班時間：工時要求 + 午休扣除 + 安全緩衝
- 手動補錄與修改日期、上班時間、下班時間
- 最近一步撤銷：上班後可撤銷上班，下班後可撤銷下班
- 日歷與最近一週、最近一月圖表
- 支援所在地區：中國大陸、中國香港、中國臺灣
- 支援從 GitHub 同步各地 2026 節假日資料
- 通知欄常駐下班倒計時
- 小、中、大三種桌面小組件
- 跟隨系統深色模式
- CSV 匯出、JSON 備份與恢復
- 圓形輪盤設定分鐘數，支持震動回饋
- 所有資料只保存在本機，不會上傳到服務器

## 節假日資料

內建 2026 年規則作為 fallback，也可以在 App 設定頁從 GitHub 更新。

目前路徑：

- 中國大陸：`holidays/cn/2026.json`
- 中國香港：`holidays/hk/2026.json`
- 中國臺灣：`holidays/tw/2026.json`

App 會根據設定頁選擇的「所在地區」拉取對應 JSON。

## 專案位置

Android 專案位於：

```text
AndroidPunchClock/
```

## 開發

使用 Android Studio 打開 `AndroidPunchClock` 目錄，或在命令列執行：

```sh
cd AndroidPunchClock
./gradlew assembleDebug
```

專案使用 Gradle Wrapper，distribution URL 設為騰訊雲 Gradle 鏡像，以改善中國大陸網路環境下的同步穩定性。

## 安裝到手機

開啟 Android 手機的 USB 調試後，可以用 Android Studio Run，也可以使用：

```sh
cd AndroidPunchClock
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 隱私

打卡記錄與設定保存在 Android 本機 SharedPreferences 中。應用不包含帳號系統，不上傳資料，也不接入第三方分析 SDK。
