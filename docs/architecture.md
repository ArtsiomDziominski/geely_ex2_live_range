# Архитектура EX2 Расход

Package `com.geely.ex2.range`. Две Compose-страницы, расчёты в чистом domain-слое, VHAL только через тонкий reflection-клиент.

```text
ui/                  Compose: Главная, Справка, ViewModel
service/             Foreground service (specialUse) + BootReceiver
app/                 Application, AppContainer, TimeSource
data/vhal/           CarClient (reflection), VehicleTelemetryReader, VhalIds
data/store/          period.json, settings.json, buffer-checkpoint.json
domain/engine/       RangeEngine — период, поездка, буфер, окна
domain/calculation/  расход, интеграл пути, прогноз окон
domain/tracker/      передача: debounce 500 мс, устойчивый P 2 с
domain/decode/       SOC, скорость, t°, P/R/N/D
```

## Потоки

1. Сервис раз в 1 с читает VHAL (скорость также callback 1 Гц, передача ONCHANGE).
2. `RangeEngine.onTick` обновляет путь, период, поездку и кольцо ≥ 35 км.
3. На устойчивом P пишется `period.json` (идемпотентно на одну парковку).
4. Checkpoint буфера — каждые ~5 с и при P.
5. UI подписан на `StateFlow` контейнера; отказ permission не роняет процесс.

## Сборка на ГУ

Flavor `system` + AOSP platform testkey. Скрипт `scripts/release-system-apk.ps1`, инструкция [system-install.md](system-install.md).

## Границы

- Domain не импортирует Android (кроме тестов на JVM).
- Car API не тянет Tools: нет виджетов, `SpeedNormalizer`, `toInt()` для SOC.
- Штатный remaining range автомобиля не подменяет окна 5/15/30.
