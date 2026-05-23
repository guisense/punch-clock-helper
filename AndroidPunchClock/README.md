# 打卡助手 Android

Android 原生版本。應用不需要服務器，資料只保存在手機本機。

## 功能

- 一鍵記錄今天上班時間
- 一鍵記錄今天下班時間
- 自訂每日工時
- 可選擇是否扣除午休，並自訂午休時長
- 自動計算安全下班時間
- 手動補錄
- 日歷圖表查看最近一週與最近一月
- 通知欄常駐下班倒計時
- 桌面小組件
- 圓形輪盤設定工時，支持震動回饋
- 所有資料只保存在本機，不會上傳

## 開發工具

需要安裝 Android Studio。安裝後打開這個資料夾：

```text
AndroidPunchClock
```

也可以使用命令列：

```sh
./gradlew assembleDebug
```

如果同步時提示 `SSL peer shut down incorrectly`，通常是 Gradle 官網下載中斷。專案已固定使用 Gradle 8.5，並把 wrapper 下載源設為騰訊雲鏡像：

```text
gradle/wrapper/gradle-wrapper.properties
```

在 Android Studio 裡點 `File > Sync Project with Gradle Files` 重新同步即可。

## 跑到 OPPO 手機

1. 手機打開「開發者選項」
2. 開啟「USB 調試」
3. 用 USB 連接 Mac 和手機
4. 手機上允許 USB 調試授權
5. Android Studio 左上角選擇你的 OPPO 手機
6. 點 Run

## ColorOS 提醒設定

為了讓下班提醒更可靠，建議安裝後到系統設定裡檢查：

- 允許通知
- 允許鬧鐘與提醒
- 電池管理中不要限制「打卡助手」
- 如有「自啟動」或「後台運行」限制，給予允許
