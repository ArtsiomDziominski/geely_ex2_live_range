# EX2 Расход

Android-приложение для ГУ Geely EX2: расход за период и поездку, прогноз остатка по окнам 5 / 15 / 30 км.

Package: `com.geely.ex2.range`

Пишется с нуля. Из [Geely EX2 Tools](../geely_ex2_tools) взяты только проверенные VHAL id, формулы декодирования и reflection к Car API. Код Tools целиком не копируется. `SpeedNormalizer` (+1 км/ч) не используется. SOC нигде не режется до целых.

## Стек

Kotlin, Jetpack Compose, Material 3, ViewModel + StateFlow, foreground service, JSON в `filesDir`.

## Сборка для ГУ (platform-подпись)

```powershell
.\scripts\release-system-apk.ps1
```

APK: `install\out\geely-ex2-range-system-platform-signed.apk`. Установка: [docs/system-install.md](docs/system-install.md).

## Первый прогон на авто

1. Поставить system APK (скрипт выше), не обычный debug.
2. Открыть **Справка** — блок «Сырые значения VHAL».
3. Подтвердить единицы: SOC float 0–100, скорость км/ч без +1, передача P=4/R=2/N=1/D=8, температура `(raw-80)/2`, одометр если читается.
4. Задать полезную ёмкость C, кВт·ч.
5. Формулы на Главной имеют смысл после этого подтверждения.

Документация: [docs/architecture.md](docs/architecture.md), [docs/features/range/overview.md](docs/features/range/overview.md).
