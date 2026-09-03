<div align="center">
  <h1>🔒 Pmsg - Zero-Trace Ephemeral Secure Messaging</h1>
  <p><strong>Aplicativo Android de Mensagens Ultrasseguras com Autodestruição em 24h, Criptografia AES-256-GCM em Hardware e Zero Rastro.</strong></p>
</div>

---

## 🌟 Visão Geral

O **Pmsg** foi desenvolvido com um objetivo claro: **garantir privacidade absoluta e zero rastros** no dispositivo e em trânsito.

### 🛡️ Pilares de Segurança

1. **Criptografia em Hardware (TEE / StrongBox)**:
   - Criptografia autenticada **AES-256-GCM** com IV aleatório de 12 bytes gerado via `SecureRandom` para cada payload.
   - Chave mestre gerenciada com segurança no **AndroidKeyStore** com isolamento de hardware (TEE / StrongBox).
   - **Fail-Closed Security**: Se houver qualquer inconsistência criptográfica, os dados em texto plano nunca são gravados no disco.

2. **Zero Rastro e Autodestruição (TTL Estrito de 24h)**:
   - Todas as mensagens possuem expiração máxima estrita de **24 horas** (ou presets personalizados: 30s, 1m, 5m, 1h, 6h, 12h, 24h).
   - Limpeza em tempo real no banco Room + **WorkManager** em segundo plano a cada 15 minutos para expurgação automática definitiva.
   - **Multi-pass Shredding**: Sobrescrita de payload com ruído criptográfico antes da exclusão de registros.

3. **Incineração de Pânico (Panic Wipe & Crypto-Shredding)**:
   - Destruição instantânea de todas as salas, mensagens e mídias ao acionar o botão de Pânico ou via **Shake-to-Clear** (chacoalhar o celular).
   - **Crypto-Shredding**: A chave mestre do KeyStore é invalidada e purgada, tornando impossível qualquer recuperação de dados residuais na memória flash.

4. **Proteção Anti-Captura & Notificações Restritas**:
   - `FLAG_SECURE` dinâmico para impedir prints e gravações de tela.
   - Detecção em tempo real de capturas com bloqueio preventivo de visualização única.
   - Proteção de clipboard (`ClipDescription.EXTRA_IS_SENSITIVE`) no Android 13+ para impedir que teclados e históricos capturem mensagens copiadas.
   - Notificações enviadas **estritamente para novas conversas recebidas**, mantendo sigilo total em mensagens recorrentes.

5. **Bloqueio Biométrico e Rate Limiting**:
   - Bloqueio por Impressão Digital / Reconhecimento Facial com fallback estrito para PIN criptografado com Salt individual e SHA-256.
   - **Anti-Brute Force**: Bloqueio progressivo temporário (cooldown lockout) após tentativas incorretas consecutivas.
   - **Auto-Bloqueio por Inatividade**: Bloqueio automático configurável (1 min a 30 min, padrão 5 min).

6. **Backup e Extração Desativados**:
   - `android:allowBackup="false"` e regras de exclusão completas para prevenir cópia de banco via ADB ou nuvem.

---

## 🌪️ Arquitetura Zero-Trace Server-Side (Expiração Autoritativa & Crypto-Shredding)

Para mitigar ameaças em que clientes modificados ou offline tentam burlar o TTL local, o **Pmsg** implementa exclusão e destruição criptográfica autoritativa no servidor:

```mermaid
graph TD
```mermaid
graph TD
    Sender[Remetente Pmsg] -->|1. Envia Mensagem Cifrada| MessagesColl[(Coleção messages)]
    Sender -->|2. storeMessageKey| StoreKeyFn[Callable storeMessageKey]
    StoreKeyFn -->|3. Persiste DEK isolada| KeysColl[(Coleção messageKeys)]
    Recipient[Destinatário Pmsg] -->|4. Baixa Ciphertext| MessagesColl
    Recipient -->|5. getMessageKey com Bearer| GetKeyFn[Callable getMessageKey]
    GetKeyFn -->|6. Valida auth.uid == recipientId| KeysColl
    Recipient -->|7. Decripta em RAM e deleta doc| MessagesColl
    MessagesColl -.->|8. onDeleteMessage Trigger| KeysColl
    Scheduler[Cloud Scheduler - A cada 1h] -->|9. expiresAt vencido| Shredder[Crypto-Shredder Function]
    Shredder -->|10. Hard-Delete DEK| KeysColl
    Shredder -->|11. Hard-Delete Ciphertext| MessagesColl
    FirestoreTTL[Firestore Native TTL] -.->|Expurgo assíncrono| MessagesColl
```

1. **Ciclo de Vida da DEK e Semântica Vanish-After-Read (`storeMessageKey` & `getMessageKey`)**:
   - **Isolamento Total**: Clientes possuem **zero acesso direto** de leitura ou escrita à coleção `messageKeys` (`allow read, write: if false`).
   - **Registro da DEK (`storeMessageKey`)**: O remetente autenticado registra a DEK via callable HTTPS. O backend valida `request.auth.uid == data.senderId` e aplica clamping de expiração (mínimo 10s, máximo 24h).
   - **Entrega Autorizada (`getMessageKey`)**: O destinatário autenticado solicita a DEK via callable HTTPS. O backend valida estritamente se `request.auth.uid == keyData.recipientId` (ou senderId) e rejeita chaves vencidas.
   - **Semântica Vanish-After-Read**: A DEK sobrevive no Firestore exclusivamente até que a mensagem seja confirmada como lida **OU** até que seu TTL expire. Ao ler, o cliente deleta o documento da mensagem em `messages`, disparando o trigger `onDeleteMessage` que destrói imediatamente a DEK.
   - **Garantia Criptográfica**: Uma vez destruída a DEK, qualquer tentativa de chamada a `getMessageKey` falha com `404 Not Found`, tornando o ciphertext matematicamente irrecuperável mesmo que persistam réplicas residuais.
2. **Purge Local (Client)**:
   - Banco Room local + `WorkManager` (Android) e `ExpiredMessageCleanupScheduler` (Desktop/Web/iOS) executam sanitização contínua em memória e SQLite.
3. **Trigger Reativo de Deleção (`onDeleteMessage`)**:
   - Disparado imediatamente quando qualquer documento em `messages` é excluído (leitura confirmada ou exclusão manual), incinerando a DEK vinculada em `messageKeys`.
4. **Crypto-Shredding Server-Side (`scheduledMessageShredder`)**:
   - Função agendada no Cloud Scheduler a cada 1 hora para varredura e hard-delete atômico em lote de chaves e mensagens expiradas que não foram lidas a tempo.
5. **TTL Nativo do Firestore**:
   - Política de infraestrutura sobre `expiresAt` na coleção `messages` como linha de defesa secundária assíncrona.
6. **Proxy Backend de IA (`geminiProxy`) & Rate Limiting**:
   - Chamadas dos clientes Desktop e Web para geração de notas efêmeras são autenticadas criptograficamente por Firebase ID Token (`verifyIdToken`) e intermediadas por Cloud Function HTTPS.
   - **Rate Limiting por Usuário**: Implementado via transação atômica no Firestore (`userRateLimits/{uid}`) com janela deslizante de 1 minuto (limite padrão: 5 requisições/minuto), retornando HTTP 429 em caso de abuso.
   - A chave `GEMINI_API_KEY` reside exclusivamente no Google Cloud Secret Manager, eliminando qualquer risco de extração em binários ou tráfego de rede do cliente.
7. **Identidade Anônima por Dispositivo (Zero-Trace Device Auth)**:
   - Em conformidade com o princípio de rastreabilidade zero, o Pmsg **não solicita PII** (sem cadastro de e-mail, telefone ou dados pessoais).
   - O cliente Desktop autentica-se diretamente via REST API do Firebase Auth (Google Identity Toolkit), estabelecendo uma identidade criptográfica anônima (`localId`).
   - As credenciais de sessão (`idToken`, `refreshToken`) são persistidas localmente protegidas por **Windows DPAPI** (`Crypt32Util.cryptProtectData`).
   - O `localId` é assumido como o `senderId` das mensagens: isso garante que na chamada à função `storeMessageKey`, a validação de segurança `request.auth.uid == data.senderId` seja satisfeita sem expor a identidade real do operador.
8. **Endpoints Multiplataforma & Política Anti-Spoofing (`AppEndpoints`)**:
   - **Compilação de Release**: URLs e identificador de projeto são constantes imutáveis de compilação. Variáveis de ambiente são **estritamente ignoradas** para impedir que atores com acesso ao ambiente do usuário redirecionem o tráfego ou forjem endpoints (anti-spoofing / anti-MITM).
   - **Compilação de Debug**: Suporta override via variáveis de ambiente (`PMSG_PROXY_URL`, `PMSG_STORE_KEY_URL`) e execução integrada contra a suíte local de emuladores do Firebase (Auth, Firestore e Functions).
   - **Protocolo Callable Oficial**: O cliente `KeyStoreClient` implementa a especificação de envelopes do Firebase Functions v2 (`{"data": {...}}` e resposta `{"result": {...}}`), com header `Authorization: Bearer <idToken>`.

---

## 🛡️ Matriz Honesta de Garantias de Segurança por Plataforma

| Recurso / Garantia | Android (Nativo) | Desktop Windows (JVM) | iOS (CMP) | Web (WasmJS) |
|---|---|---|---|---|
| **Hardware KeyStore / TEE** | ✅ Sim (`AndroidKeyStore` StrongBox/TEE) | ⚠️ Parcial (Windows DPAPI / CNG em repouso) | ✅ Sim (`Apple Keychain` Secure Enclave) | ❌ Não (Memória volátil da aba) |
| **Bloqueio de Screenshot** | ✅ Sim (`FLAG_SECURE` impede print e gravação) | ❌ Não (Sem API de SO para bloquear terceiros) | ❌ Não (iOS **NÃO** bloqueia hardware prints) | ❌ Não (Inviável no navegador) |
| **Detecção de Screenshot** | ✅ Sim (`ScreenCaptureCallback`) | ❌ Não | ✅ Sim (`UserDidTakeScreenshotNotification`) | ❌ Não |
| **Proteção de Clipboard** | ✅ Sim (`EXTRA_IS_SENSITIVE` + Clear TTL) | ⚠️ Sim (Auto-limpeza com timer na JVM) | ⚠️ Sim (Auto-limpeza com timer local) | ⚠️ Sim (Auto-limpeza volátil) |
| **Incineração (Panic Shredding)** | ✅ Sim (Invalidação KeyStore + Overwrite) | ✅ Sim (Deleção DPAPI + Overwrite de memória) | ✅ Sim (Deleção Keychain + Overwrite) | ⚠️ Sim (Limpeza de memória volátil) |
| **Integridade (App Check)** | ✅ Sim (Play Integrity API nativo) | ⚠️ Sim (Token de Sessão via Proxy Backend) | ✅ Sim (DeviceCheck / App Attest nativo) | ⚠️ Sim (reCAPTCHA Enterprise / Proxy) |
| **Origem da API Key Gemini** | 🛡️ Server-Side (Firebase Secret Manager) | 🛡️ Server-Side (Proxy Backend HTTPS) | 🛡️ Server-Side (Firebase Secret Manager) | 🛡️ Server-Side (Proxy Backend HTTPS) |

---

## 🚀 Como Executar Localmente

### Pré-requisitos
- [Android Studio Ladybug / Meerkat ou superior](https://developer.android.com/studio)
- Android SDK (API 24+ / Compile SDK 36)
- JDK 17 ou 21

### Passo a Passo

1. Abra o **Android Studio**.
2. Clique em **Open** e selecione o diretório deste repositório (`Pmsg`).
3. Aguarde o Gradle sincronizar as dependências e o KSP processar os DAOs do Room.
4. (Opcional) Copie `.env.example` para `.env` caso utilize serviços externos.
5. Execute em um emulador ou dispositivo físico com Android 7.0+ (API 24+).

---

## 🧪 Testes Unitários

O projeto inclui suíte de testes unitários para verificação criptográfica:
- `CryptoManagerTest`: Testes de integridade de encriptação, fail-safe, detecção de corrupção e Crypto-Shredding.
- `EphemeralMessageCommonTest`: Testes de cálculo de TTL, auto-desaparecimento após leitura e trituração de mensagens.
- `ExampleRobolectricTest`: Testes de integração de serviços em segundo plano e workers de limpeza.

---

## ⚡ Desenvolvimento & Compose Hot Reload (Antigravity IDE / Desktop)

### Fluxo de Trabalho Diário
1. No terminal integrado ou através das tarefas do VS Code (`Ctrl+Shift+P` > `Tasks: Run Task`), execute:
   ```powershell
   .\gradlew.bat :composeApp:hotRunDesktop --autoReload
   ```
2. A janela do aplicativo abrirá com `alwaysOnTop = true` e o título `Pmsg [desktop-dev]`, mantendo-se sempre visível durante o desenvolvimento.
3. Edite o código da interface em `commonMain` ou `desktopMain` e salve (`Ctrl+S`): as alterações serão refletidas instantaneamente no aplicativo em execução, **sem reiniciar o processo**.

### Servidor MCP (`compose-hot-reload`)
O servidor Model Context Protocol integrado permite a automação e inspeção da interface via AI Agent. Ferramentas disponíveis:
- `status`: Verifica o status atual da aplicação e do servidor de hot reload.
- `reload`: Dispara o hot reload forçado dos componentes atualizados.
- `await_reload`: Aguarda a finalização do ciclo de recompilação e injeção do reload.
- `take_screenshot`: Captura a tela atual da janela do aplicativo desktop.
- `get_semantic_tree`: Inspeciona a árvore de nós semânticos e acessibilidade da UI Compose.
- `get_logs`: Obtém os logs de runtime do aplicativo Compose Desktop.
- `click`: Simula cliques em elementos interativos da interface.
- `type_text`: Simula digitação de texto em campos selecionados.
- `scroll`: Realiza rolagem programática em listas e containers.
- `list_windows`: Lista as janelas ativas da aplicação.
- `get_ui_error`: Retorna eventuais exceções e erros de renderização em tempo real.
- `restart`: Reinicia o processo da aplicação desktop quando necessário.

---

## 🚀 Status de Produção (v1.0-mvp)

Ambiente de produção implantado e homologado no Google Cloud Platform / Firebase sob o projeto **`gen-lang-client-0858445711`** (Região: `us-central1`).

### 1. Endpoints e Recursos Ativos

| Componente | Tipo | Identificador / URL | Estado |
|---|---|---|---|
| **`geminiProxy`** | Cloud Function v2 (HTTPS) | `https://us-central1-gen-lang-client-0858445711.cloudfunctions.net/geminiProxy` | ✅ Ativo |
| **`storeMessageKey`** | Cloud Function v2 (Callable) | `https://us-central1-gen-lang-client-0858445711.cloudfunctions.net/storeMessageKey` | ✅ Ativo |
| **`getMessageKey`** | Cloud Function v2 (Callable) | `https://us-central1-gen-lang-client-0858445711.cloudfunctions.net/getMessageKey` | ✅ Ativo |
| **`onDeleteMessage`** | Eventarc Trigger (Firestore) | Trigger em `messages/{messageId}` (Vanish-After-Read) | ✅ Ativo |
| **`scheduledMessageShredder`** | Cloud Scheduler | Job horário (`0 * * * *`) para purga de mensagens expiradas | ✅ Ativo |
| **Cloud Firestore** | Banco NoSQL Multi-region | `(default)` em `us-central1` com regras de isolamento total | ✅ Ativo |
| **Política TTL** | Firestore Time-To-Live | Collection group `messages`, campo `expiresAt` | ✅ Ativo |
| **Identity Toolkit** | Firebase Auth REST | Provedor Anônimo habilitado para identidade efêmera | ✅ Ativo |

### 2. Matriz de Segurança e Proteções Implementadas

- **Isolamento de DEKs (`messageKeys`)**: Regras do Firestore bloqueiam leitura e escrita direta de qualquer cliente SDK (`allow read, write: if false`). Acesso restrito ao backend Admin SDK.
- **Crypto-Shredding Reativo**: Ao ler uma mensagem, o cliente deleta o documento em `messages`; o trigger `onDeleteMessage` destrói imediatamente a chave em `messageKeys`. Qualquer chamada subsequente retorna `HTTP 404 NOT_FOUND`.
- **Barreira de Expiração**: Mensagens com TTL vencido têm acesso à chave bloqueado pelo backend com `HTTP 400 FAILED_PRECONDITION`.
- **Rate Limiting**: Sliding window de 1 minuto gerenciada via transação atômica em `userRateLimits/{uid}` (máximo 5 chamadas/min). A 6ª chamada retorna `HTTP 429 Too Many Requests`.
- **Anti-Spoofing em Release**: Em compilações de release, endpoints e Project ID são constantes imutáveis de compilação; variáveis de ambiente são estritamente ignoradas.
- **Hardware Protection no Desktop**: Credenciais de sessão anônima do Identity Toolkit são cifradas em repouso no Windows via **Windows DPAPI** (`Crypt32Util.cryptProtectData`).
- **App Check**: Operando sem enforcement estrito no backend para manter a interoperabilidade de clientes Desktop e plataformas multiplataforma.

### 3. Estimativa Mensal de Custos (Google Cloud Free Tier)

| Serviço | Quota Mensal Gratuita | Consumo Previsto (Até 50k msgs/mês) | Custo Estimado |
|---|---|---|:---:|
| Cloud Scheduler | 3 jobs gratuitos / mês | 1 job (`scheduledMessageShredder`) | US$ 0,00 |
| Cloud Functions v2 | 2.000.000 invocações / mês | ~100.000 invocações | US$ 0,00 |
| Cloud Firestore | 1.5M leituras, 600k escritas / mês | ~100k leituras, ~50k escritas | US$ 0,00 |
| Firestore TTL | Deleções TTL são isentas de cobrança | Auto-purga contínua | US$ 0,00 |
| Secret Manager | 6 versões ativas gratuitas | 1 secret (`GEMINI_API_KEY`) | US$ 0,00 |
| Identity Toolkit | 50.000 MAUs gratuitas | Sessões anônimas efêmeras | US$ 0,00 |
| **TOTAL MENSAL** | — | — | **US$ 0,00 / mês** |

### 4. Runbook de Rotação do Secret `GEMINI_API_KEY`

Caso seja necessário rotacionar a chave de API do Gemini Studio:

```bash
# 1. Definir o novo valor no Secret Manager (interativo, seguro, sem eco em tela):
npx firebase-tools functions:secrets:set GEMINI_API_KEY

# 2. Re-implantar a Cloud Function proxy para vincular a nova versão do secret:
npx firebase-tools deploy --only functions:geminiProxy

# 3. Validar a nova versão em produção:
npx firebase-tools functions:secrets:get GEMINI_API_KEY
```

### 5. Configuração do Arquivo `google-services.json` (Passo Manual)

Por se tratar de um repositório público voltado à privacidade, o arquivo `google-services.json` está explicitamente ignorado pelo `.gitignore`.
Para compilar o cliente Android localmente:
1. Acesse o **Firebase Console** no projeto `gen-lang-client-0858445711`.
2. Vá em **Configurações do Projeto > Seus aplicativos > Android** (`com.aistudio.vanishchat.zr7k`).
3. Baixe o arquivo `google-services.json` e posicione-o no diretório `composeApp/`.
4. O build Gradle está configurado com `MissingGoogleServicesStrategy.ERROR` para impedir compilações acidentais sem a configuração correta.

---

## 📄 Licença

Desenvolvido para segurança e privacidade estrita. Zero Trace.
