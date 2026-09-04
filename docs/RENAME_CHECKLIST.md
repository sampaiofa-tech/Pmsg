# Checklist de Renomeação do Produto (Transição do Nome Provisório)

Este checklist consolida as ações técnicas, regulatórias e operacionais obrigatórias que devem ser executadas no momento em que o **nome comercial definitivo** do aplicativo for aprovado pela gestão.

> [!WARNING]
> **Atenção aos Prazos e Pontos de Não Retorno**: As decisões de `applicationId` e `deep link scheme` **PRECISAM** ser tomadas e implementadas **ANTES** do primeiro lançamento público na Google Play Store / Apple App Store. Alterá-los posteriormente quebra a base instalada, requer nova listagem de app e invalida todos os links/QR codes distribuídos fisicamente.

<!-- CI Status: paths-ignore ativo para arquivos de documentação (economia de minutos macOS) -->

---

## 1. Repositório e Infraestrutura de Código (GitHub)
- [ ] **Renomear Repositório no GitHub**:
  - Acessar `Settings > General > Repository name` na organização/conta do GitHub.
  - Atualizar para o novo nome (ex: de `Pmsg` para `<NovoNome>`).
  - *Nota*: O GitHub configura automaticamente redirecionamentos permanentes (HTTP 301) para clone URLs e links web existentes.
- [ ] **Atualizar Remoto Local nos Ambientes de Desenvolvimento**:
  ```bash
  git remote set-url origin https://github.com/sampaiofa-tech/<NovoNome>.git
  ```
- [ ] **Atualizar Badges e Documentação do CI**:
  - Verificar referências ao repositório nos workflows do GitHub Actions (`.github/workflows/ios-build.yml`, etc.).

---

## 2. Identidade Visual e Interface do Usuário (UI/UX)
- [ ] **Display Name do Aplicativo**:
  - **Android**: `composeApp/src/androidMain/res/values/strings.xml` (`app_name`).
  - **iOS**: `iosApp/iosApp/Info.plist` (`CFBundleDisplayName` e `CFBundleName`).
  - **Desktop**: Título da janela em `composeApp/src/desktopMain/kotlin/com/example/Main.kt` (`Window(title = "<NovoNome>")`).
  - **Web**: Tag `<title>` em `composeApp/src/wasmJsMain/resources/index.html`.
- [ ] **Assets e Ícones do Produto**:
  - Ícone adaptativo Android (`mipmap-hdpi`, `mipmap-xhdpi`, etc.).
  - Conjunto de ícones iOS `AppIcon.appiconset`.
  - Ícone de barra de tarefas Desktop (`.ico` / `.png`).
  - Favicon e manifest Web.
- [ ] **Listagens de Loja (Store Listings)**:
  - Textos de apresentação, screenshots promocionais e descrições na Google Play Console e Apple App Store Connect.

---

## 3. Decisões Estruturais CRÍTICAS (Executar ANTES da Publicação em Loja)

### a. `applicationId` / `bundleIdentifier`
- [ ] **Definir o `applicationId` definitivo para Android**:
  - Arquivo: `composeApp/build.gradle.kts` (`applicationId = "com.<empresa>.<novonome>"`).
  - *Risco de Não Retorno*: Na Google Play Store, o `applicationId` é **eterno e imutável**. Se publicado como `com.aistudio.vanishchat.zr7k` ou `com.example.pmsg`, **nunca mais poderá ser alterado** sem criar um aplicativo completamente novo do zero, perdendo avaliações, downloads e usuários.
- [ ] **Definir o `bundleIdentifier` definitivo para iOS**:
  - Arquivo: `iosApp/Configuration/Config.xcconfig` ou `composeApp/build.gradle.kts`.
- [ ] **Atualizar o registro no Firebase Console**:
  - Cadastrar o novo `applicationId` no projeto Firebase (`gen-lang-client-...` ou projeto definitivo de produção).
  - Gerar e posicionar o novo `google-services.json` (Android) e `GoogleService-Info.plist` (iOS).

### b. Deep Link Custom Scheme (`URI Protocol`)
- [ ] **Definir o Schema de Protocolo Definitivo**:
  - Atualmente configurado como `pmsg://` (ex: `pmsg://contact?...` e `pmsg://invite?i=...`).
  - Arquivos a atualizar:
    - Android Manifest (`AndroidManifest.xml` intent-filter `<data android:scheme="<novoschema>"/>`).
    - iOS Info.plist (`CFBundleURLSchemes`).
    - Parse e geração no código Kotlin: `IdentityManager.kt` (`CONTACT_SCHEME = "<novoschema>://contact"` e `INVITE_SCHEME = "<novoschema>://invite"`).
    - Cloud Functions: `createInvite.ts` (`inviteLink = "<novoschema>://invite?i=" + token`).
  - *Risco de Não Retorno*: Qualquer alteração de esquema após distribuição física **invalida instantaneamente** todos os QR codes impressos, cartões de visita e links de convite compartilhados previamente entre usuários.

---

## 4. Registro de Propriedade Intelectual e Marca (INPI)
- [ ] **Busca de Anterioridade no INPI**:
  - Pesquisar colidências fonéticas e gráficas no banco de marcas do INPI para o nome definitivo.
- [ ] **Depósito de Pedido de Registro de Marca**:
  - **Classe 9**: Softwares de computador gravados ou baixáveis, aplicativos para dispositivos móveis, programas de comunicação cifrada e mensagens instantâneas.
  - **Classe 42**: Serviços científicos e tecnológicos, serviços de software como serviço (SaaS), segurança da informação e auditoria de sistemas de software.
- [ ] **Monitoramento da Revista da Propriedade Industrial (RPI)**:
  - Acompanhar publicação, eventuais oposições de terceiros e deferimento do registro.

---

## 5. Documentos Legais e de Licenciamento do Repositório
- [ ] **Atualizar `LICENSE-COMMERCIAL.md`**:
  - Substituir `Pmsg (nome provisório)` pela marca definitiva homologada.
  - Preencher e ativar o endereço de contato comercial oficial (`[CONTATO_A_PREENCHER_POR_MIM]`).
- [ ] **Atualizar `CLA.md`**:
  - Atualizar para o nome comercial definitivo e razão social / nome civil do titular do copyright.
- [ ] **Atualizar `README.md`**:
  - Remover as notas e disclaimers de "nome provisório".
  - Se a marca for depositada ou registrada, incluir a menção padrão de marca registrada (`<Nome>® é marca registrada sob o processo INPI nº...`).
