# Integração com Shizuku

O CasaPreta usa o **Shizuku** (e seu irmão Magisk, o **Sui**) para executar
operações privilegiadas sem precisar de root. Este documento explica como a
integração foi feita, baseada na **API v13.1.5** (a mais recente publicada no
Maven Central em setembro de 2023 e ainda atual em 2026).

> **Referência oficial:** https://github.com/RikkaApps/Shizuku-API

## 1. O que é Shizuku

Shizuku é um app que inicia um processo (`app_process`) com privilégios de
**ADB (uid 2000)** ou **root (uid 0)**. Esse processo expõe um binder que
qualquer app pode adquirir (depois de receber permissão do usuário) e usar
para chamar APIs do sistema que normalmente exigiriam `adb shell`.

Vantagens:
- Não requer root (apenas ADB uma vez por boot, ou wireless debugging no Android 11+)
- Permite usar APIs como `PackageManager.setApplicationEnabledSetting(...)` em
  pacotes de terceiros sem a flag `MANAGE_USERS` reservada ao sistema
- Múltiplos apps podem compartilhar o mesmo servidor Shizuku

## 2. Dependências

No `app/build.gradle.kts`:

```kotlin
implementation("dev.rikka.shizuku:api:13.1.5")
implementation("dev.rikka.shizuku:provider:13.1.5")
```

- `api` — contém a classe `rikka.shizuku.Shizuku` com todos os métodos
- `provider` — contém o `ShizukuProvider` (ContentProvider) que adquiri o
  binder do app Shizuku e o entrega ao nosso processo

## 3. Manifest

```xml
<provider
    android:name="rikka.shizuku.ShizukuProvider"
    android:authorities="${applicationId}.shizuku"
    android:multiprocess="false"
    android:enabled="true"
    android:exported="true"
    android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
```

A partir da v12.1.0 o `ShizukuProvider` também inicializa o **Sui**
automaticamente (para usuários com Magisk). Caso queira desativar esse
comportamento, chame `ShizukuProvider.disableAutomaticSuiInitialization()`
antes de `onCreate()` — não fizemos isso pois queremos suportar os dois.

## 4. Ciclo de vida do binder

A classe [`shizuku/ShizukuManager.kt`](../app/src/main/java/com/casapreta/app/shizuku/ShizukuManager.kt)
registra dois listeners globais:

```kotlin
Shizuku.addBinderReceivedListener { /* Shizuku acabou de ficar disponível */ }
Shizuku.addBinderDeadListener     { /* Shizuku morreu ou foi parado      */ }
```

Esses callbacks atualizam um `MutableStateFlow<Status>` no `SettingsViewModel`,
que a UI observa para mostrar o estado atual:

```kotlin
sealed class Status {
    data object NotInstalled : Status()   // Shizuku/Sui não está presente
    data object NotRunning   : Status()   // Instalado mas servidor parado
    data object Running      : Status()   // Binder vivo, pronto para uso
}
```

A verificação efetiva usa `Shizuku.pingBinder()` envolvida em try/catch —
qualquer exceção significa "não instalado".

## 5. Solicitando permissão ao usuário

Mesmo com o binder vivo, o usuário precisa **conceder permissão por app**.
O fluxo é idêntico ao de uma permissão de runtime:

```kotlin
// 1. Registrar listener antes de pedir
Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
    val granted = grantResult == PackageManager.PERMISSION_GRANTED
    // atualiza StateFlow
}

// 2. Verificar antes de pedir
if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
    // já tem
} else if (Shizuku.shouldShowRequestPermissionRationale()) {
    // usuário marcou "não perguntar de novo" — abrir Shizuku app
} else {
    Shizuku.requestPermission(requestCode)
}
```

A tela de Settings tem um botão **"Autorizar Shizuku"** que só aparece quando
o status é `Running` mas `hasPermission()` é falso.

## 6. UserService (execução privilegiada)

Para operações mais complexas que não cabem em um `Binder` call simples
(usar `PackageManager` diretamente, por exemplo), o Shizuku oferece o
**UserService**: um serviço Android que roda em outro processo, sob o
uid do Shizuku (0 ou 2000).

### AIDL

`app/src/main/aidl/com/casapreta/app/aidl/IAppManagerService.aidl`:

```aidl
interface IAppManagerService {
    boolean hidePackage(String packageName);
    boolean unhidePackage(String packageName);
    boolean isPackageHidden(String packageName);
    int getPrivilegedUid();
}
```

### Implementação

`AppManagerService` (em `ShizukuManager.kt`) estende `IAppManagerService.Stub`
e chama `PackageManager.setApplicationEnabledSetting(...)` — uma API que,
fora do Shizuku, exigiria permissões reservadas ao sistema.

### Binding

```kotlin
val args = Shizuku.UserServiceArgs(
    ComponentName("com.casapreta.app", "com.casapreta.app.shizuku.AppManagerService")
)
    .tag("app_manager_service")   // IMPORTANTE: idempotente entre versões
    .version(1)
    .daemon(false)
    .processNameSuffix("app_manager")
    .debuggable(Build.VERSION.SDK_INT >= P)

Shizuku.bindUserService(args, connection)
```

O `connection` é um `ServiceConnection` padrão. Em `onServiceConnected` você
recebe um `IBinder` que deve ser convertido com
`IAppManagerService.Stub.asInterface(binder)`.

### Importante

- O `tag` no `UserServiceArgs` deve ser estável (não usar nome de classe,
  pois ProGuard/R8 pode renomeá-lo). Por isso usamos a string fixa
  `"app_manager_service"`.
- Se `version` mudar, o Shizuku mata o serviço antigo e inicia um novo.
- O processo do UserService **não é um processo de app válido** — muitas APIs
  de `Context` (registerReceiver, getContentResolver) não funcionam. Por isso
  pegamos o `PackageManager` via `ActivityThread.currentApplication()`.

## 7. Diferenças ADB vs Root

| Identidade        | uid retornado por `Shizuku.getUid()` | Capacidades principais                              |
|-------------------|--------------------------------------|------------------------------------------------------|
| ADB (sem root)    | 2000                                 | Pode desabilitar apps, instalar pacotes, força-stop |
| Root (via Sui)    | 0                                    | Tudo acima + acesso a `/data` de outros apps        |

A função `getPrivilegedUid()` do UserService deixa a UI exibir qual modo
está ativo.

## 8. UX esperada

1. Usuário instala o app Shizuku (Play Store ou site oficial)
2. Inicia o servidor via ADB ou wireless debugging (Android 11+)
3. Abre o CasaPreta → Home mostra status "Servidor parado" ou "Sem permissão"
4. Vai em Configurações → clica em **Autorizar Shizuku**
5. Aparece o dialog do sistema → conceder
6. Status muda para **"Conectado e autorizado"**

A partir desse ponto, o app pode chamar `hidePackage`/`unhidePackage` em
qualquer app instalado, sem root.
