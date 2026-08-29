# System APK для ГУ Geely EX2

Обычный `userDebug` / `adb install` часто **не получает** `android.car.permission.CAR_*`.
На ГУ это работает у CentralEXAuto / EX2 Tools, потому что APK:

- `android:sharedUserId="android.uid.system"`
- подписан **AOSP platform testkey** (`android@android.com`)

## Сборка

```powershell
.\scripts\release-system-apk.ps1
```

Каждый запуск: `VERSION_CODE += 1` в `app\version.properties`, затем сборка и platform-подпись.

Результат:

- `install\out\geely-ex2-range-v{VERSION_NAME}({VERSION_CODE}).apk`
- `install\out\geely-ex2-range-system-platform-signed.apk` — копия со стабильным именем

Без повышения сборки: `.\scripts\release-system-apk.ps1 -NoBump`  
Сменить marketing-версию: `.\scripts\release-system-apk.ps1 -VersionName 0.1.1`

Ключи: `keys\aosp-platform\` (локально, не в git). Те же `platform.pk8` / `platform.x509.pem`, что у Tools.

## Установка на ГУ

Сначала снимите user-сборку (иначе конфликт UID/подписи):

```text
adb uninstall com.geely.ex2.range
```

На этой ГУ надёжнее push + `pm install`:

```text
adb push install/out/geely-ex2-range-system-platform-signed.apk /data/local/tmp/geely-ex2-range-system.apk
adb shell pm install -r -g /data/local/tmp/geely-ex2-range-system.apk
```

При установке в `/system/priv-app/` дополнительно:

`install/privapp-permissions-com.geely.ex2.range.xml` → `/system/etc/permissions/`

## Flavors

| Flavor | sharedUserId | Подпись | CAR_* на ГУ |
|--------|--------------|---------|-------------|
| `user` (default) | нет | debug | часто нет |
| `system` | `android.uid.system` | platform testkey (скрипт) | да, как Tools |
