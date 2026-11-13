# Bergafin (Android)

Fork do [jellyfin-android](https://github.com/jellyfin/jellyfin-android) que acompanha as versões estáveis oficiais. O ID é `io.github.agelinena.bergafin`, então ele convive com o app oficial no mesmo celular.

## O que muda em relação ao oficial

- **Login pelo Authentik:** o botão do [Bergafin-web](https://github.com/Agelinena/Bergafin-web) abre o Authentik no navegador do sistema, onde passkeys e a sessão aberta funcionam. O [plugin](https://github.com/Agelinena/Bergafin-auth) devolve o login para `bergafin://login?ticket=…`, e o app entrega o ticket ao web. Código em `app/src/main/java/org/jellyfin/mobile/bergafin/BergafinLogin.kt`.
- **Atualizador próprio:** ao abrir o app e a cada 12 horas, ele confere a release *latest* deste repositório. Se houver versão nova, avisa por notificação; tocando, baixa e instala. Código em `bergafin/update/`. Na primeira vez, o Android pede a permissão "Instalar apps desconhecidos".

## Versões e releases

- **Formato:** `v<versão oficial>-berga.<N>`, por exemplo `v2.7.3-berga.1`. O `versionCode` é o código oficial × 100 + N, então sempre cresce (`buildSrc/src/main/kotlin/BergafinVersion.kt`).
- **Release:** cada push no `main` gera uma release assinada (`bergafin-release.yml`).
- **Acompanhamento do oficial:** todo dia, `bergafin-sync.yml` confere a última versão estável oficial. Quando sai uma nova, reaplica os nossos commits por rebase e gera a release. Se der conflito, abre uma issue e nada muda.
- **Workflows do projeto oficial:** ficam desligados neste repositório.

## Assinatura

Todo APK é assinado com a mesma chave (secrets `BERGAFIN_KEYSTORE` e `BERGAFIN_KEYSTORE_PASSWORD`; certificado SHA-256 `72:C1:74:CF:…:58:BC`). Com outra chave, o atualizador não instala por cima. O workflow confere o certificado antes de publicar.
