# Архитектура PulseDesk Android

Пакет `com.kirdev.pulsedeckmobile`, один Gradle-модуль. Kotlin, Jetpack Compose, Material 3, MVVM, StateFlow, корутины, Navigation Compose. DI-фреймворка нет: граф маленький, единственные экземпляры живут в `PulseDeskApp`.

## Пакеты

| Пакет | Что внутри |
|---|---|
| `core/theme` | `Palette` (AMOLED/Graphite, акцент как Compose-состояние), `PulseDeskTheme` |
| `core/ui` | Карточки, строки, диалоги подтверждения, `ActionRunner` (snackbar), `PollWhileVisible` |
| `core/utils` | `Format` — проценты, байты, скорость, время, аптайм |
| `data/local` | `PairingStore` (ПК и токены, AES-GCM в Android Keystore), `AppSettings` |
| `data/remote` | `PcConnection` (WebSocket), `PcApi` (HTTP v1), `PinnedTls`, `Discovery`, `PairingClient`, `Protocol` |
| `media` | `PlayerState`, `MediaNotificationService` |
| `monitoring` | `MetricsHistory` — ряды для графиков из живого потока |
| `navigation` | `AppNavigation`: 5 вкладок, мини-плеер, маршруты |
| `feature/*` | `connection`, `dashboard`, `monitoring`, `media`, `remote`, `files`, `applications`, `devices`, `notifications`, `settings`, `menu`, `pulse` (волна KiroX) |

## Связь с ПК

```
PcConnection ── wss://host:8767/api/v1/display ── PulseDeck (Windows)
   │  hello / metrics / media / error            ▲
   │  subscribe / command (playback, volume)     │
   └─ endpoint (host, token, pinned OkHttp) ──► PcApi ── https://host:8767/api/v1/...
```

- **Один канал.** WebSocket даёт метрики и музыку событиями. HTTP — только для действий и списков: файлы, процессы, окна, питание.
- **Хост.** Сначала USB (`127.0.0.1` через `adb reverse`), иначе Wi-Fi. Пока связь идёт по Wi-Fi, каждые 5 с проверяется кабель, и при его появлении связь переходит на USB.
- **Безопасность.** TLS с пиннингом SHA-256 сертификата ПК, без открытого HTTP (`usesCleartextTraffic=false`). Токен на каждом запросе.
- **Держатели.** Соединение живёт, пока его держат:
  - `ui` — приложение на экране;
  - `media` — медиакарточка при наличии трека.
  Нет держателей — сокет закрыт, трафика нет.
- **Переподключение.** Паузы 1→2→5→10→15→30 с. Каждую третью неудачу ПК ищется заново по имени — на случай, если сменился IP.

## Музыкальный модуль

| Состояние | Когда | Мини-плеер | Медиакарточка / сессия | Тики прогресса | Волна | Кнопки громкости телефона |
|---|---|---|---|---|---|---|
| PLAYING | `media.session.status=playing` | да | да | 2/с (полный), 1/с (мини) | да | → громкость ПК |
| PAUSED | `paused` | да | да («пауза») | нет | нет | на телефоне |
| IDLE | `session=null` (в т.ч. «остановлено») | нет | снимается через 4 с, служба останавливается | нет | нет | на телефоне |
| DISCONNECTED | нет связи | нет | снимается сразу | нет | нет | на телефоне |

- Состояние — чистая функция `playerState(connection, session)`, покрыта тестами.
- **Опроса нет.** ПК сам присылает `media` при изменении. Позиция трека считается на телефоне.
- **Служба `MediaNotificationService`:**
  - запускается, только когда приложение на экране и есть трек. Android 12+ запрещает foreground-службу из фона;
  - `START_NOT_STICKY`;
  - уведомление и метаданные обновляются только при видимых изменениях;
  - при остановке: `stopForeground(REMOVE)`, `session.release()`, `release("media")`.
- **Обложка** скачивается один раз на ключ и декодируется уменьшенной, до ~256 px.
- **Команды** уходят только по живому каналу: без связи они отбрасываются, а не копятся.

## Фоновая работа

| Работа | Когда | Остановка |
|---|---|---|
| WebSocket | приложение на экране **или** есть трек | `release` последнего держателя |
| Метрики | только при видимом приложении (`subscribe metrics=false` в фоне) | автоматически |
| События `/events` | каждые 10 с, только на главной и в «Событиях» | `repeatOnLifecycle(STARTED)` |
| Таймер выключения `/status` | каждые 5 с, только на «Управлении» | то же |
| Процессы `/processes` | каждые 5 с, только на экране процессов | то же |

## Опасные действия

Только через диалог подтверждения:
- выключение и перезагрузка — с выбором задержки и отменой;
- сон;
- блокировка;
- закрытие окна;
- завершение процесса;
- удаление файла — в корзину ПК.

Произвольных команд и терминала нет.
