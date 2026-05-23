# 打卡助手

打卡助手是一個 Android 原生應用，用來記錄彈性工時下的上班、下班時間，並計算安全下班時間。專案目前面向個人自用與小範圍試用，不包含服務端。

## 功能

- 一鍵記錄上班與下班
- 自訂每日工時
- 可選擇是否扣除午休，並自訂午休時長
- 安全下班時間計算：工時要求 + 午休扣除 + 2 分鐘安全緩衝
- 手動補錄與修改日期、上班時間、下班時間
- 日歷與最近一週、最近一月圖表
- 中國大陸 2026 年法定節假日與調休日規則
- 通知欄常駐下班倒計時
- 桌面小組件
- 圓形輪盤設定工時，支持震動回饋
- 所有資料只保存在本機，不會上傳到服務器

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

## 隱私

打卡記錄與設定保存在 Android 本機 SharedPreferences 中。應用不包含帳號系統，不上傳資料，也不接入第三方分析 SDK。
