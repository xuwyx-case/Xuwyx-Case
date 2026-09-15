# Xuwyx Case

Минимальный Unity-прототип Android-игры.

## Параметры

- Unity: `2022.3.62f3`
- Название: `Xuwyx Case`
- Android package: `com.xuwyx.app`
- Версия: `2.43.0` (`versionCode 24300`)
- Ориентация: только альбомная
- Стартовая сцена: `Assets/Scenes/MainMenu.unity`

## Локальный запуск

1. Открой корень репозитория через Unity Hub в Unity `2022.3.62f3`.
2. Открой сцену `Assets/Scenes/MainMenu.unity`.
3. Нажми Play.

## Сборка APK

В Unity выбери `Xuwyx Case > Build Android APK`. APK появится в
`build/Android/Xuwyx-Case-2.43.0.apk`.

GitHub Actions запускает ту же сборку при каждом push в `main` и сохраняет APK
как artifact. Для работы Unity Builder в настройках репозитория должны быть
добавлены секреты `UNITY_LICENSE`, `UNITY_EMAIL` и `UNITY_PASSWORD`.

