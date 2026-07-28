# Modo Noturno (Dark Mode)

Este documento descreve como o **Modo Noturno** foi implementado no CasaPreta.
A implementação é 100% em Jetpack Compose + Material 3, com a preferência do
usuário persistida via **DataStore Preferences** (assíncrono, type-safe e
substituto moderno do `SharedPreferences`).

## 1. Modelo de tema

O usuário pode escolher entre três modos, definidos em
[`ui/theme/Theme.kt`](../app/src/main/java/com/casapreta/app/ui/theme/Theme.kt):

```kotlin
enum class ThemeMode {
    SYSTEM,  // segue o dark mode do sistema operacional
    LIGHT,   // sempre claro
    DARK     // sempre escuro (Modo Noturno forçado)
}
```

A escolha é exposta pelo `SettingsRepository` como um `Flow<ThemeMode>` e
consumida pela `MainActivity` para recompor o `CasaPretaTheme` automaticamente.

## 2. Persistência com DataStore

Arquivo: [`data/SettingsRepository.kt`](../app/src/main/java/com/casapreta/app/data/SettingsRepository.kt)

```kotlin
private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "casapreta_settings")

val themeMode: Flow<ThemeMode> = context.settingsDataStore.data
    .map { prefs -> ThemeMode.fromName(prefs[Keys.THEME_MODE]) }

suspend fun setThemeMode(mode: ThemeMode) {
    context.settingsDataStore.edit { prefs ->
        prefs[Keys.THEME_MODE] = mode.name
    }
}
```

**Por que DataStore e não SharedPreferences?**
- API assíncrona baseada em coroutines (não bloqueia a main thread).
- Type-safe via `Preferences.Key<T>`.
- Transações atômicas e tratamento consistente de erros de I/O.
- Substituto oficial recomendado pela Google desde 2020.

## 3. Aplicação do tema

O `CasaPretaTheme` em `ui/theme/Theme.kt` decide se usa o color scheme claro
ou escuro com base no `ThemeMode` corrente:

```kotlin
@Composable
fun CasaPretaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= S -> { /* dynamic */ }
        isDark -> DarkColors
        else    -> LightColors
    }
    // Ajusta também a cor da status bar
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
```

As cores claras e escuras estão definidas em
[`ui/theme/Color.kt`](../app/src/main/java/com/casapreta/app/ui/theme/Color.kt),
seguindo o token system do Material 3 (primary, secondary, tertiary, error,
background, surface, surfaceVariant, outline, etc.).

## 4. Como o usuário ativa

Na tela de **Configurações** (`ui/screens/SettingsScreen.kt`):

1. Um **Switch** "Modo Noturno" com label dinâmico:
   - Desligado → `ThemeMode.LIGHT`
   - Ligado     → `ThemeMode.DARK`

2. Três **FilterChip**s abaixo permitem escolher explicitamente:
   - Sistema / Claro / Escuro

3. Toda mudança chama `SettingsViewModel.setThemeMode(mode)`, que:
   - Chama `repo.setThemeMode(mode)` em uma coroutine
   - O `Flow<ThemeMode>` emite o novo valor
   - A `MainActivity` recomputa `CasaPretaTheme(themeMode = ...)` e toda a UI
     transita instantaneamente entre os color schemes.

## 5. Estado inicial

Na primeira execução o `Flow` retorna `ThemeMode.SYSTEM` (default do
`ThemeMode.fromName(null)`), ou seja, o app respeita a configuração global
do Android até o usuário alterar manualmente.

## 6. Resumo do fluxo

```
[Switch / Chip em SettingsScreen]
        ↓  onCheckedChange / onClick
[SettingsViewModel.setThemeMode(mode)]
        ↓  viewModelScope.launch
[SettingsRepository.setThemeMode(mode)]
        ↓  DataStore.edit { prefs[THEME_MODE] = mode.name }
[Flow<Preferences> emite novo valor]
        ↓  .map { ThemeMode.fromName(it[THEME_MODE]) }
[StateFlow<ThemeMode> no ViewModel]
        ↓  collectAsState() na MainActivity
[CasaPretaTheme(themeMode = ...)]
        ↓
[MaterialTheme(colorScheme = DarkColors ou LightColors)]
        ↓
[Recomposição de todas as telas com as novas cores]
```
