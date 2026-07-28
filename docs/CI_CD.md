# CI/CD — GitHub Actions

Este documento detalha o workflow `.github/workflows/build.yml` do CasaPreta.

## Arquivo

Localização: [`.github/workflows/build.yml`](../.github/workflows/build.yml)

## Triggers

```yaml
on:
  push:
    branches: [ main, master ]
    tags:     [ 'v*' ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:        # disparo manual pela UI do GitHub
```

- **Push para main/master**: build de validação
- **Push de tag `v*`**: build + criação de GitHub Release com o APK
- **Pull request**: build para validar contribuições
- **workflow_dispatch**: disparo manual para testar o pipeline a qualquer momento

## Jobs

O workflow tem um único job `build` com os seguintes passos:

### 1. Checkout
```yaml
- uses: actions/checkout@v4
```

### 2. Setup JDK 17
```yaml
- uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: 17
```

### 3. Cache do Gradle
Acelera builds subsequentes cacheando `~/.gradle/caches` e `.gradle` do projeto.

### 4. Configurar signing
Os 4 secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`)
são expostos como variáveis de ambiente. O `build.gradle.kts` detecta
`KEYSTORE_BASE64`, decodifica para um arquivo temporário e configura o
`signingConfig` de release:

```kotlin
val decoded = android.util.Base64.decode(keystoreBase64, android.util.Base64.DEFAULT)
val tmpFile = File(rootProject.buildDir, "tmp_keystore.jks")
tmpFile.writeBytes(decoded)
storeFile = tmpFile
```

### 5. Build do APK
```yaml
- name: Build release APK
  run: ./gradlew assembleRelease --no-daemon
```

### 6. Upload do artifact
```yaml
- uses: actions/upload-artifact@v4
  with:
    name: casapreta-apk
    path: app/build/outputs/apk/release/*.apk
    retention-days: 30
```

O APK fica disponível para download na página da run em
`https://github.com/<user>/<repo>/actions/runs/<run_id>`.

### 7. GitHub Release (somente em tags `v*`)
```yaml
- uses: softprops/action-gh-release@v2
  if: startsWith(github.ref, 'refs/tags/v')
  with:
    files: app/build/outputs/apk/release/*.apk
    generate_release_notes: true
```

Quando você cria uma tag `v1.0.0`, o workflow automaticamente cria um Release
no GitHub e anexa o APK.

## Secrets necessários

Configure em **Settings → Secrets and variables → Actions → New repository secret**:

| Nome                | Valor                                                                  |
|---------------------|------------------------------------------------------------------------|
| `KEYSTORE_BASE64`   | `base64 -w 0 casapreta.jks` (uma linha longa sem quebras)             |
| `KEYSTORE_PASSWORD` | Senha da keystore (texto plano)                                       |
| `KEY_ALIAS`         | `casapreta`                                                            |
| `KEY_PASSWORD`      | Senha da chave (texto plano)                                          |

> **Importante:** Nunca commite o arquivo `.jks` ou o `keystore.properties`.
> Ambos estão no `.gitignore`. A keystore só existe na sua máquina e no
> GitHub (codificada como secret).

## Como monitorar uma run

1. Abra `https://github.com/deivid22srk/CasaPreta/actions`
2. Clique na run mais recente
3. Acompanhe o progresso em tempo real
4. Ao final, baixe o APK em **Artifacts → casapreta-apk**

## Como disparar manualmente

1. Vá em **Actions** → **Build APK** (workflow na barra lateral)
2. Clique em **Run workflow** → selecione o branch `main` → **Run workflow**
3. A run aparece imediatamente na lista

## Status badge

Para incluir um badge no README:

```markdown
![Build APK](https://github.com/deivid22srk/CasaPreta/actions/workflows/build.yml/badge.svg)
```
