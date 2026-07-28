# Arquitetura

O CasaPreta segue o padrão **single-activity + Compose Navigation** com
separação clara de responsabilidades em camadas. Não há Dagger/Hilt para
manter o projeto enxuto — a injeção de dependências é manual via
`AndroidViewModel` + `viewModel()`.

## Camadas

```
┌────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose + Material 3)   │
│  ─ HomeScreen, SettingsScreen               │
│  ─ CasaPretaTheme, Color, Type              │
└────────────┬───────────────────────────────┘
             │ collectAsState()
┌────────────▼───────────────────────────────┐
│  ViewModel Layer                            │
│  ─ SettingsViewModel                        │
│    • themeMode: StateFlow<ThemeMode>        │
│    • shizukuStatus: StateFlow<Status>       │
│    • shizukuPermission: StateFlow<Boolean>  │
└────────────┬───────────────────────────────┘
             │ suspend / Flow
┌────────────▼───────────────────────────────┐
│  Data / Service Layer                       │
│  ─ SettingsRepository  (DataStore)          │
│  ─ ShizukuManager      (Shizuku API v13.1.5)│
└─────────────────────────────────────────────┘
```

## Componentes

### `MainActivity`
Única Activity do app. Habilita edge-to-edge, instancia o `SettingsViewModel`
e observa `themeMode` para recompor o `CasaPretaTheme`. Toda a navegação
interna é feita via `NavHost` (Compose Navigation).

### `CasaPretaApp`
`Application` subclass. Reservada para futuras inicializações (analytics,
crash reporting). Hoje é mínima porque o `ShizukuProvider` cuida da
inicialização do Sui automaticamente.

### `SettingsRepository`
Encapsula o `DataStore<Preferences>` privado do app. Expõe:
- `themeMode: Flow<ThemeMode>` — preferência de tema
- `setThemeMode(mode)` — grava a preferência

### `ShizukuManager`
Fachada sobre a API estática `rikka.shizuku.Shizuku`. Centraliza:
- Registro/remoção dos listeners de binder (`OnBinderReceivedListener`,
  `OnBinderDeadListener`)
- Verificação de status (`pingBinder`, `isPreV11`)
- Verificação/pedido de permissão (`checkSelfPermission`,
  `shouldShowRequestPermissionRationale`, `requestPermission`)
- Binding do `UserService` (`bindUserService`, `unbindUserService`)
- A classe `AppManagerService` (mesmo arquivo) é o AIDL Stub que roda no
  processo do Shizuku

### `SettingsViewModel` (AndroidViewModel)
Mantém o estado observável pela UI:
- `themeMode: StateFlow<ThemeMode>` — backed by DataStore
- `shizukuStatus: StateFlow<ShizukuManager.Status>`
- `shizukuPermission: StateFlow<Boolean>`

Conecta os callbacks do `ShizukuManager` aos `MutableStateFlow`. Quando o
binder morre ou volta, a UI reage automaticamente.

### UI
- `ui/theme/Theme.kt` — `CasaPretaTheme`, `ThemeMode`, color schemes
- `ui/theme/Color.kt` — tokens de cor Material 3 (light + dark)
- `ui/theme/Type.kt` — tipografia
- `ui/components/SettingsComponents.kt` — `SwitchSettingItem` reutilizável
- `ui/screens/HomeScreen.kt` — dashboard inicial com status
- `ui/screens/SettingsScreen.kt` — switch do Modo Noturno + integração Shizuku

## Fluxo de dados (exemplo: alternar Modo Noturno)

```
1. Usuário toca no Switch "Modo Noturno" (ligado)
2. onCheckedChange(true) → SettingsViewModel.setThemeMode(DARK)
3. viewModelScope.launch { repo.setThemeMode(DARK) }
4. DataStore.edit { prefs[THEME_MODE] = "DARK" }   (grava no disco)
5. Flow<Preferences> emite novo snapshot
6. .map { ThemeMode.fromName("DARK") }  →  Flow<ThemeMode> emite DARK
7. StateFlow<ThemeMode> no ViewModel atualiza para DARK
8. MainActivity.collectAsState() lê DARK
9. CasaPretaTheme(themeMode = DARK) → isDark = true → DarkColors
10. MaterialTheme propaga o novo colorScheme → recomposição global
```

Tudo isso acontece em ~10ms, sem travar a main thread, sem necessidade de
recreate da Activity.

## Fluxo de dados (exemplo: pedir permissão Shizuku)

```
1. Usuário clica em "Autorizar Shizuku"
2. SettingsViewModel.requestShizukuPermission()
3. shizuku.addPermissionResultListener { _, granted -> _shizukuPermission.value = granted }
4. shizuku.requestPermission(1001)
5. Shizuku.requestPermission(1001) dispara dialog do sistema
6. Usuário concede → OnRequestPermissionResultListener dispara
7. grantResult == PERMISSION_GRANTED → granted = true
8. _shizukuPermission.value = true → UI reage (botão some, status muda)
```

## Decisões de design

| Decisão                                | Motivo                                                            |
|----------------------------------------|-------------------------------------------------------------------|
| Compose ao invés de View/XML           | Menos código, recomposição reativa, integração nativa com Kotlin |
| DataStore ao invés de SharedPreferences | API assíncrona, type-safe, recomendado pelo Google              |
| single Activity                        | Reduz boilerplate, melhor controle de estado                      |
| Sem Hilt                               | Projeto pequeno, `viewModel()` é suficiente                       |
| AIDL para o UserService                | Único modo de expor APIs para o processo do Shizuku               |
| Keystore fora do git                   | Segurança: a chave privada nunca vai para o repositório           |
