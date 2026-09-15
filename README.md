# Xuwyx Case

Минимальный нативный Android-прототип игры.

## Параметры

- Название: `Xuwyx Case`
- Android package: `com.xuwyx.app`
- Версия: `2.43.0` (`versionCode 24300`)
- Ориентация: только альбомная
- Минимальная версия Android: 6.0 (API 23)
- Загрузочный экран из предоставленного набора ресурсов
- Главное меню с открываемым профилем
- Выбор из 19 аватаров и 44 рамок
- Рамка `Blue Gem` (`9x`) находится на первой странице выбора рамок

## Локальный запуск

Открой корень репозитория в Android Studio и запусти конфигурацию `app`.

## Сборка APK

```bash
gradle :app:assembleDebug
```

APK появится в `app/build/outputs/apk/debug/Xuwyx-Case-2.43.0-debug.apk`.
GitHub Actions автоматически устанавливает Java, Android SDK и Gradle, собирает
приложение при каждом push в `main` и сохраняет APK как artifact.
