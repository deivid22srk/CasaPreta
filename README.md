# CasaPreta

App Android (Kotlin + Jetpack Compose) com **Modo Noturno** nas configurações
e **integração Shizuku** para operações privilegiadas sem root.

![Build APK](https://github.com/deivid22srk/CasaPreta/actions/workflows/build.yml/badge.svg)

## Funcionalidades

- **Modo Noturno** com três modos: Sistema / Claro / Escuro (persistido via DataStore)
- **Integração Shizuku v13.1.5** (permissão por app + UserService para esconder apps)
- UI 100% Jetpack Compose + Material 3
- Strings em PT-BR
- Build de APK assinado via GitHub Actions

## Como começar

1. Clone o repositório
2. Abra no Android Studio (Hedgehog ou superior)
3. Aguarde a sincronização do Gradle
4. Rode em um dispositivo/emulador com Android 6.0+ (API 23+)

Para usar as funcionalidades privilegiadas (hide/unhide apps):

1. Instale o app [Shizuku](https://shizuku.rikka.app/download/)
2. Inicie o servidor (via ADB ou depuração sem fio no Android 11+)
3. Abra o CasaPreta → Configurações → **Autorizar Shizuku**

## Documentação

Toda a documentação técnica está em [`docs/`](./docs/):

- [DARK_MODE.md](./docs/DARK_MODE.md) — implementação do Modo Noturno
- [SHIZUKU.md](./docs/SHIZUKU.md) — integração com Shizuku
- [ARCHITECTURE.md](./docs/ARCHITECTURE.md) — visão arquitetural
- [BUILD.md](./docs/BUILD.md) — build local e assinatura
- [CI_CD.md](./docs/CI_CD.md) — pipeline do GitHub Actions
- [CHANGELOG.md](./docs/CHANGELOG.md) — histórico de versões

## Build

### Debug (local)

```bash
./gradlew assembleDebug
```

### Release assinado (CI)

O workflow `.github/workflows/build.yml` gera o APK assinado a cada push ou
disparo manual. Veja [docs/BUILD.md](./docs/BUILD.md) para configurar os
secrets de assinatura.

## Stack

| Camada            | Tecnologia                          |
|-------------------|-------------------------------------|
| Linguagem         | Kotlin 1.9.24                       |
| UI                | Jetpack Compose + Material 3        |
| Build             | Gradle 8.9 + AGP 8.5.2 (Kotlin DSL) |
| Persistência      | DataStore Preferences               |
| Navegação         | Compose Navigation 2.7.7            |
| Permissões priv.  | Shizuku API v13.1.5                 |
| CI/CD             | GitHub Actions                      |
| minSdk / targetSdk | 23 / 34                            |

## Licença

Uso interno — todos os direitos reservados.
