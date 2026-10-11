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

### Предкомпиляция — после каждой установки

`pm install` не компилирует код заранее, а фоновая компиляция (bg-dexopt) запускается только
в простое устройства — на ГУ её может не быть вовсе. Тогда Compose работает в интерпретаторе/JIT,
и интерфейс заметно тормозит при переключении вкладок и скролле. Компилируем сразу:

```text
adb shell cmd package compile -m speed -f com.geely.ex2.range
adb shell am force-stop com.geely.ex2.range
adb shell am start -n com.geely.ex2.range/.ui.MainActivity
```

`force-stop` нужен потому, что сразу после установки `MY_PACKAGE_REPLACED` уже запустил сервис
со старым (некомпилированным) кодом — новый код подхватится только новым процессом.

При установке в `/system/priv-app/` дополнительно:

`install/privapp-permissions-com.geely.ex2.range.xml` → `/system/etc/permissions/`

## Обновление по сети

Сборка `system` умеет обновлять сама себя: «Настройки → Обновление». Источник — файлы **последнего
релиза** этого репозитория на GitHub (репозиторий публичный, ключ доступа не нужен):

```text
https://github.com/ArtsiomDziominski/geely_ex2_live_range/releases/latest/download/update.json
https://github.com/ArtsiomDziominski/geely_ex2_live_range/releases/latest/download/<apk из update.json>
```

### Как выпустить обновление

1. `.\scripts\release-system-apk.ps1 -Notes "Что изменилось"` — как обычно собирает и подписывает
   APK, а рядом кладёт манифест `install\out\update.json`: `versionCode`, `versionName`, имя APK,
   его `sha256` и размер, текст `notes` (показывается в настройках рядом с новой версией).
2. Создать на GitHub релиз и приложить **оба** файла: `geely-ex2-range-system-platform-signed.apk`
   и `update.json`. Имена не менять — приложение ищет их по этим именам. Команду для `gh` скрипт
   печатает в конце.
3. Релиз должен быть обычным, не draft и не pre-release: адрес `releases/latest` на них не указывает.

### Что делает приложение

- Раз в сутки при запуске сервиса (выключается в настройках) и по кнопке «Проверить обновления»
  читает `update.json` и сравнивает `versionCode` с установленным. Нашлась версия новее — на пункте
  «Настройки» появляется точка.
- Само ничего не ставит. По кнопке «Установить» после подтверждения качает APK, сверяет размер и
  SHA-256 с манифестом и отдаёт его системе (`PackageInstaller`). Под uid `system` установка идёт
  без системного диалога; приложение при этом перезапускается, сервис поднимает `BootReceiver`.
- Первый запуск после такой установки сам выполняет предкомпиляцию (`cmd package compile -m speed -f`)
  — то же, что выше делается вручную через adb. Если приложение не на экране и машина на P, процесс
  после этого перезапускается, чтобы подхватить скомпилированный код; иначе он подхватится при
  следующем запуске.
- Сборка `user` обновления только показывает: она подписана debug-ключом, релизный APK поверх неё
  не встанет.

Код: `data/update/` (`UpdateManager`, `UpdateClient`, `ApkInstaller`).

### О доверии

APK подписан публичным AOSP testkey, поэтому подпись сама по себе ничего не доказывает: подписать
APK этим ключом может кто угодно. Подлинность обновления держится на HTTPS и на том, что релиз
лежит в этом репозитории, — тот, кто может публиковать в нём релизы, может поставить на ГУ код с
правами `system`. Поэтому обновление не ставится само, а на аккаунте GitHub стоит держать
двухфакторную защиту.

Проверено на эмуляторе: проверка и загрузка с настоящего GitHub, установка через `PackageInstaller`
с перезапуском сервиса. На ГУ (тихая установка под uid `system` и предкомпиляция) — не проверено.

## Flavors

| Flavor | sharedUserId | Подпись | CAR_* на ГУ |
|--------|--------------|---------|-------------|
| `user` (default) | нет | debug | часто нет |
| `system` | `android.uid.system` | platform testkey (скрипт) | да, как Tools |
