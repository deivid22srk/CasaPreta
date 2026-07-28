# Build & Assinatura do APK

Este documento explica como gerar o APK do CasaPreta de duas formas:

1. **Localmente** (para desenvolvimento/debug)
2. **Via GitHub Actions** (CI — APK release assinado pronto para distribuição)

## 1. Pré-requisitos

- JDK 17 (openjdk-17-jre ou superior)
- Android SDK com:
  - `platforms;android-34`
  - `build-tools;34.0.0`
  - `platform-tools`
- Variável de ambiente `ANDROID_HOME` apontando para o SDK

## 2. Build local (debug)

```bash
cd CasaPreta
./gradlew assembleDebug
```

Saída: `app/build/outputs/apk/debug/app-debug.apk`

## 3. Build local (release assinado)

Para gerar um APK release assinado localmente você precisa de uma keystore.
Crie uma (apenas uma vez) com:

```bash
keytool -genkeypair -v \
  -keystore app/keystore/casapreta.jks \
  -keyalg RSA -keysize 2048 -validity 36500 \
  -alias casapreta \
  -storepass <sua_senha> \
  -keypass <sua_senha> \
  -dname "CN=CasaPreta, OU=Dev, O=CasaPreta, L=SaoPaulo, ST=SP, C=BR"
```

Crie o arquivo `app/keystore/keystore.properties` (NÃO commite):

```properties
KEYSTORE_PATH=/caminho/absoluto/para/app/keystore/casapreta.jks
KEYSTORE_PASSWORD=<sua_senha>
KEY_ALIAS=casapreta
KEY_PASSWORD=<sua_senha>
```

Esse arquivo está no `.gitignore`. O `app/build.gradle.kts` lê essas
propriedades e configura o `signingConfig` de release automaticamente.

Agora rode:

```bash
./gradlew assembleRelease
```

Saída: `app/build/outputs/apk/release/app-release.apk` (assinado).

## 4. Build via GitHub Actions

O workflow `.github/workflows/build.yml` faz tudo automaticamente. Para que
ele funcione, você precisa configurar **4 secrets** no repositório GitHub:

| Secret name          | Descrição                                                    |
|----------------------|--------------------------------------------------------------|
| `KEYSTORE_BASE64`    | Keystore `.jks` codificada em base64 (veja comando abaixo)   |
| `KEYSTORE_PASSWORD`  | Senha da keystore                                            |
| `KEY_ALIAS`          | Alias da chave (ex: `casapreta`)                             |
| `KEY_PASSWORD`       | Senha da chave (normalmente igual à da keystore)             |

### Como gerar o `KEYSTORE_BASE64`

```bash
base64 -w 0 app/keystore/casapreta.jks > keystore.b64
# copie o conteúdo de keystore.b64 e cole no secret KEYSTORE_BASE64
```

Em macOS use `base64 -i app/keystore/casapreta.jks | tr -d '\n'`.

### Disparando o workflow

O workflow roda automaticamente em:
- Push para `main` ou `master`
- Pull Request para `main`
- Tag `v*` (ex: `v1.0.0`) — dispara release com o APK anexado

Você também pode disparar manualmente na aba **Actions → Build APK → Run workflow**
(workflow_dispatch).

### Artefatos

Ao final de cada run bem-sucedida:
- **Artifact** `casapreta-apk` contendo `app-release.apk` (assinado)
- Em builds de tag `v*`, um **GitHub Release** é criado com o APK anexado

## 5. Verificando a assinatura

Depois de baixar o APK, confirme que ele está assinado:

```bash
keytool -printcert -jarfile app-release.apk | head -20
```

Você deve ver o DN que configurou no `keytool -genkeypair`.

## 6. Troubleshooting

| Problema                                    | Solução                                                       |
|---------------------------------------------|---------------------------------------------------------------|
| `Keystore file not set for signing config`  | Verifique se os 4 secrets estão configurados no GitHub       |
| APK gerado mas não instala                  | Habilitar "fontes desconhecidas" no Android                   |
| `Shizuku is not installed` em runtime       | Instale o app Shizuku e inicie o servidor                     |
| `IllegalStateException: binder is dead`     | Reinicie o servidor Shizuku (servidor morre a cada reboot)    |
