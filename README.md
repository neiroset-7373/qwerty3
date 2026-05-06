# Spidi Кликер v3.1 — Native Android

Нативное Android приложение, полностью переписанное с React на Kotlin.

## Особенности

- 100% Native Kotlin
- Полностью автономная работа (без внешних URL)
- SharedPreferences для сохранений
- Material Design интерфейс

## Структура проекта

```
app/src/main/
├── java/com/wintozo/spidi/
│   ├── GameActivity.kt       # Главный игровой экран
│   ├── OobeActivity.kt       # Экран первоначальной настройки
│   ├── GameState.kt          # Модели данных и логика
│   ├── SettingsManager.kt    # Управление сохранениями
│   ├── UpgradeAdapter.kt     # Адаптер улучшений
│   ├── GiftAdapter.kt        # Адаптер подарков
│   └── WallpaperGridAdapter.kt # Адаптер обоев
└── res/
    ├── layout/               # XML макеты
    ├── drawable/             # Графические ресурсы
    └── raw/                  # Музыкальные файлы
```

## Сборка

```bash
./gradlew assembleRelease
```

## CI/CD

Автоматическая сборка через GitHub Actions при пуше в main.

## Лицензия

© 2024 WintoCraft
