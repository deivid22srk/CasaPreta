# CasaPreta — Documentação

Bem-vindo à documentação do projeto **CasaPreta**. Este diretório reúne todos os
arquivos `.md` que detalham o processo de desenvolvimento, arquitetura,
integração com Shizuku, implementação do Modo Noturno e o pipeline de build
no GitHub Actions.

## Índice

| Arquivo              | Conteúdo                                                                 |
|----------------------|--------------------------------------------------------------------------|
| [DARK_MODE.md](./DARK_MODE.md)       | Como o Modo Noturno foi implementado (DataStore + Compose + ThemeMode)   |
| [SHIZUKU.md](./SHIZUKU.md)          | Integração com a API v13.1.5 do Shizuku (permissão + UserService)        |
| [ARCHITECTURE.md](./ARCHITECTURE.md)   | Visão geral de camadas, fluxo de dados e responsabilidades              |
| [BUILD.md](./BUILD.md)              | Como gerar o APK localmente e via GitHub Actions (APK assinado)          |
| [CI_CD.md](./CI_CD.md)              | Detalha o workflow `build.yml` e os secrets necessários                 |
| [CHANGELOG.md](./CHANGELOG.md)        | Histórico de versões                                                     |

## Resumo executivo

O CasaPreta é um aplicativo Android nativo (Kotlin + Jetpack Compose) que
substitui a versão anterior distribuída apenas como APK binário (baseada no
`web1n.stopapp` v1.9.16). A nova versão foi reescrita do zero com:

- **Jetpack Compose** + **Material 3** para a UI
- **Kotlin 1.9.24** + **AGP 8.5.2** + **Gradle 8.9**
- **DataStore Preferences** para persistir configurações do usuário
- **Shizuku API v13.1.5** para executar operações privilegiadas (esconder apps)
  sem root, usando o servidor Shizuku iniciado via ADB
- **GitHub Actions** com workflow `build.yml` que produz um APK **assinado e
  pronto para distribuição** a cada push ou disparo manual

## Estrutura do projeto

```
CasaPreta/
├── app/
│   ├── build.gradle.kts              # Módulo do app (Compose, Shizuku, signing)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml       # Declara ShizukuProvider
│       ├── aidl/com/casapreta/app/aidl/
│       │   └── IAppManagerService.aidl
│       ├── java/com/casapreta/app/
│       │   ├── CasaPretaApp.kt       # Application
│       │   ├── MainActivity.kt       # Activity única + NavHost
│       │   ├── data/SettingsRepository.kt
│       │   ├── shizuku/ShizukuManager.kt
│       │   ├── viewmodel/SettingsViewModel.kt
│       │   └── ui/
│       │       ├── theme/             # CasaPretaTheme, ThemeMode, Color, Type
│       │       ├── components/         # SwitchSettingItem, SectionHeaderCard
│       │       └── screens/           # HomeScreen, SettingsScreen
│       └── res/                       # strings (PT-BR), themes, drawables
├── docs/                              # ← esta pasta
├── .github/workflows/build.yml       # CI que assina o APK
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/                    # Wrapper 8.9
├── gradlew, gradlew.bat
└── README.md
```

## Pré-requisitos para rodar localmente

- Android Studio Hedgehog ou superior
- JDK 17
- Android SDK com `platform-34` e `build-tools 34.0.0`
- Dispositivo/emulador com Shizuku instalado e servidor iniciado (opcional,
  só para funcionalidades privilegiadas)
