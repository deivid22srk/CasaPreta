# Changelog

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo.
O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e este projeto adere a [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Unreleased]

### Adicionado
- Documentação inicial completa em `docs/`

## [1.0.0] — 2026-07-28

### Adicionado
- Reescrita completa do app CasaPreta em Kotlin + Jetpack Compose
  (substitui a versão anterior empacotada como APK binário baseada em
  `web1n.stopapp` v1.9.16)
- **Modo Noturno** nas Configurações com três modos:
  - Sistema (segue o dark mode do Android)
  - Claro (sempre claro)
  - Escuro (sempre escuro / Modo Noturno forçado)
- Persistência da preferência de tema via **DataStore Preferences**
- **Integração Shizuku API v13.1.5**:
  - `ShizukuProvider` declarado no AndroidManifest
  - `ShizukuManager` com listeners de binder (alive/dead)
  - Fluxo completo de verificação + pedido de permissão por app
  - `IAppManagerService` AIDL + `UserService` para operações privilegiadas
    (hide/unhide de pacotes via `PackageManager.setApplicationEnabledSetting`)
- Tela de **Home** com dashboard de status (Modo Noturno + Shizuku)
- Tela de **Configurações** com switch do Modo Noturno, chips de seleção
  de tema e card de status do Shizuku com botão "Autorizar"
- `CasaPretaTheme` com color schemes Material 3 (light + dark)
- Strings localizadas em **PT-BR**
- Ícone do launcher em todas as densidades (mdpi → xxxhdpi) + adaptive icon
- Workflow **GitHub Actions** (`build.yml`) que produz APK release assinado:
  - Suporte a `push`, `pull_request`, `tag v*` e `workflow_dispatch`
  - Decodifica keystore de secret base64
  - Faz upload do APK como artifact
  - Cria GitHub Release automaticamente em tags `v*`

### Alterado
- Repositório agora contém código-fonte em vez de apenas APK binário

### Removido
- APK binário `casa Preta_1.9.16_PT-BR_unsigned_sign.apk` (mantido apenas
  como referência histórica; pode ser removido em versão futura)
