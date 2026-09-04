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
sequenceDiagram
    autonumber
    participant Sender as Alice (Remetente)
    participant MessagesColl as Firestore (messages)
    participant StoreKeyFn as Callable storeMessageKey
    participant KeysColl as Firestore (messageKeys)
    participant Recipient as Bob (Destinatário)

    Note over Sender: 1. Gera DEK aleatória (AES-256)<br/>2. Cifra payload: AES-256-GCM(msg, DEK, IV)
    Sender->>MessagesColl: 3. Grava doc messages {ciphertext, iv, senderId, recipientId, expiresAt}
    Note over Sender: 4. Par efêmero X25519<br/>5. sharedSecret = X25519(ephemPriv, BobPubKey)<br/>6. KEK = HKDF-SHA256(sharedSecret)<br/>7. wrappedDek = AES-GCM(DEK, KEK, nonce)
    Sender->>StoreKeyFn: 8. storeMessageKey {messageId, ephemeralPubKey, wrappedDek, expiresAt}
    Note over StoreKeyFn: 9. Valida senderId == auth.uid<br/>10. Servidor vê SOMENTE bytes opacos (zero-knowledge)
    StoreKeyFn->>KeysColl: 11. Grava doc messageKeys {ephemeralPubKey, wrappedDek, expiresAt}

    Recipient->>MessagesColl: 12. Escuta/baixa mensagem cifrada
    Recipient->>StoreKeyFn: 13. getMessageKey(messageId) com Bearer token
    StoreKeyFn-->>Recipient: 14. Retorna {ephemeralPubKey, wrappedDek}
    Note over Recipient: 15. sharedSecret = X25519(BobPrivKey, ephemPub)<br/>16. Mesma KEK -> unwrap -> DEK<br/>17. Decifra mensagem em RAM volátil
    Recipient->>MessagesColl: 18. Vanish-After-Read (Deleta doc em messages)
    MessagesColl-.->KeysColl: 19. Trigger onDeleteMessage incinera messageKeys imediatamente
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

## 🆔 Arquitetura de Identidade Criptográfica (v1.1)

A versão v1.1 do **Pmsg** introduz uma camada completa de identidade descentralizada, determinística e autodestrutiva, eliminando a dependência de identidades voláteis ou centralizadas.

### 1. Identidade Descentralizada & Não-Enumerável
- **Par de Chaves X25519 (Curve25519)**: Cada dispositivo possui um par de chaves assimétricas de alta performance e segurança de 128 bits.
- **Derivação Determinística via Argon2id (RFC 9106)**:
  - Seed mnemônica **BIP-39 PT-BR (12 palavras)** com dicionário de 2048 palavras em português.
  - Derivação de chave via Argon2id: **3 iterações**, **32 MB de memória RAM** (32.768 KiB), **paralelismo 1**, e salt de domínio fixo `pmsg-v1-identity-seed`.
  - O material derivado gera a semente privada para a curva X25519.
- **Número de Segurança Comutativo (Padrão Signal)**:
  - Fingerprint de 256 bits: $\text{SHA-256}(\text{chave pública})$.
  - Exibição em **60 dígitos decimais** agrupados em **12 blocos de 5 dígitos**.
  - A ordenação lexicográfica garante comutatividade: ambas as pontas da conversa visualizam exatamente o mesmo número de segurança, permitindo conferência visual presencial ou out-of-band contra ataques MITM.

### 2. Provisionamento Seguro & Envelope Criptográfico
- **Exibição Única**: O mnemônico de 12 palavras é exibido estritamente uma única vez durante o onboarding de criação.
- **Desafio de Confirmação**: O usuário deve confirmar 3 palavras aleatórias antes de inicializar o cofre.
- **Envelope Criptográfico (`IdentityEnvelope`)**: O mnemônico e a chave privada nunca são persistidos em claro no disco nem transmitidos pela rede. São selados com AES-256-GCM utilizando chaves gerenciadas por hardware (**AndroidKeyStore StrongBox/TEE**, **Apple Keychain Secure Enclave**, **Windows DPAPI**).
- **Revisão com Barreira Biométrica**: Qualquer consulta futura ao mnemônico exige autorização biométrica do sistema operacional (ou PIN do dispositivo).
- **Panic Shredding**: O acionamento do Panic Wipe destrói imediatamente o cofre local de identidade e todas as chaves associadas.

### 3. Estabelecimento de Contatos: Modelos A e C
```mermaid
sequenceDiagram
    autonumber
    participant Alice as Alice (Criadora)
    participant Cloud as Cloud Functions / Firestore
    participant Bob as Bob (Destinatário)

    Note over Alice,Bob: MODELO A (Presencial / Sem Servidor)
    Alice->>Bob: Exibe QR Code (Fingerprint + PubKey)
    Bob-->>Alice: Escaneia e armazena no SQLite cifrado (VanishDatabase)
    Note over Alice,Bob: Número de Segurança comutativo validado visualmente

    Note over Alice,Bob: MODELO C (Remoto / Efêmero com TTL 24h)
    Alice->>Cloud: createInvite(creatorFingerprint, creatorPubKey)
    Cloud-->>Alice: Retorna link pmsg://invite?token=...&fp=... (TTL 24h)
    Alice->>Bob: Envia link via canal seguro (Clipboard com auto-clear 30s)
    Bob->>Cloud: acceptInvite(inviteToken)
    Note over Cloud: Transação atômica: valida uso único e TTL
    Cloud->>Cloud: Vanish-After-Accept (Hard Delete do convite em invites)
    Cloud-->>Bob: Retorna AlicePubKey e AliceFingerprint
    Bob->>Bob: Armazena Alice em contatos locais e calcula Número de Segurança
```

- **Modelo A (Presencial / Local)**: Troca direta de chaves via QR Code ou string Base64. Totalmente offline, zero metadados em rede.
- **Modelo C (Remoto / Efêmero)**:
  - Links efêmeros com esquema `pmsg://invite?token=<64-hex>&fp=<64-hex>`.
  - Token aleatório com **256 bits de entropia** (`crypto.randomBytes(32)`).
  - Persistido na coleção `invites` com **acesso direto negado a clientes SDK** (`allow read, write: if false` em `firestore.rules`).
  - **Vanish-After-Accept**: Transação atômica no Cloud Functions valida o token, checa o TTL de 24 horas, impede auto-aceite e **exclui imediatamente** o registro do Firestore.
  - Tentativas adicionais com o mesmo token falham com `404 Not Found` ou `400 Failed Precondition`.
  - Área de transferência protegida com auto-limpeza após 30 segundos.

### 4. Rejeição Formal do Modelo B (Diretório Centralizado de Handles)
O Pmsg **rejeitou expressamente** a implementação de um diretório centralizado de usernames, números de telefone ou handles pesquisáveis.
- **Motivo de Segurança & Privacidade**: Diretórios de handles viabilizam raspagem em massa (*scraping*), correlação de metadados, ataques de enumeração, sequestro de contas e intimidação dirigida por adversários estatais ou cibercriminosos.
- **Design Pmsg**: Identidades no Pmsg utilizam hashes criptográficos de 256 bits ($2^{256}$ chaves possíveis). É matematicamente impossível varrer ou enumerar usuários sem que o contato forneça seu identificador explicitamente.

### 5. Restauração & Recuperação de Identidade com Prova de Posse Ed25519 (Fase 5 & F0 Fix)
- **Restauração em Novo Dispositivo**: O operador insere seu mnemônico BIP-39 de 12 palavras em português em um novo dispositivo.
- **Constantes Imutáveis de Protocolo (Argon2id — RFC 9106)**:
  Para garantir interoperabilidade matemática absoluta entre todas as plataformas suportadas (Android, Desktop, iOS, Web), os parâmetros do Argon2id são fixados como **constantes de protocolo imutáveis**:
  | Parâmetro | Valor de Protocolo | Justificativa |
  |---|:---:|---|
  | **Modo** | `Argon2id` | Resistência combinada contra ataques baseados em canal lateral (Argon2i) e aceleração por GPU/ASIC (Argon2d). |
  | **Iterações ($t$)** | `3` | Custo computacional adequado para mitigar ataques de dicionário sem degradar a UX mobile. |
  | **Memória ($m$)** | `32.768 KiB` (32 MB) | Impõe barreira severa de hardware contra ataques de força bruta paralela. |
  | **Paralelismo ($p$)** | `1` | Consistência de execução determinística monothread em runtimes mobile/Wasm. |
  | **Tag Length** | `32 bytes` (256 bits) | Tamanho exato para semente privada de curvas elípticas Curve25519 / Ed25519. |
  | **Domain Salt (X25519)** | `"pmsg-v1-identity-seed"` | Isolamento criptográfico estrito do par de cifragem Diffie-Hellman. |
  | **Domain Salt (Ed25519)** | `"pmsg-v1-identity-signing"` | Isolamento criptográfico estrito do par de assinatura digital de prova de posse. |

- **Regeneração Determinística Dupla**:
  1. **Par X25519 de cifragem**: derivado via Argon2id com salt `"pmsg-v1-identity-seed"`, gerando o mesmo par e o mesmo fingerprint imutável ($\text{SHA-256}(\text{pubKey})$).
  2. **Par Ed25519 de assinatura**: derivado via Argon2id com salt `"pmsg-v1-identity-signing"`, gerando a chave de assinatura vinculada deterministicamente à identidade.
  - *Provedores Criptográficos por Plataforma*:
    - **Android & Desktop (JVM)**: Bouncy Castle RFC 8032 (`org.bouncycastle.math.ec.rfc8032.Ed25519`).
    - **iOS (CMP) & Web (WasmJS)**: Engine Multiplataforma Kotlin puro (com implementação nativa de SHA-512).
    - **Backend (Node.js 20)**: Módulo nativo `crypto.verify` com reconstituição de prefixo SPKI RFC 8410 (`302a300506032b6570032100`) para a chave pública crua de 32 bytes (zero dependências externas de npm).

- **Vulnerabilidade Corrigida (F0 Security Fix)**:
  - *Problema Identificado*: Validar apenas $\text{SHA-256}(\text{pubKey}) == \text{fingerprint}$ não prova posse da chave privada — qualquer contato (Eve) conhece a `pubKey` pública de Bob e poderia sequestrar o roteamento técnico (`identities/{fingerprint}`) para receber mensagens e DEKs destinadas a ele.
  - *Princípio Adotado*: **"Roteamento exige prova de posse Ed25519; hash de consistência não é autenticação."**
  - *Solução Criptográfica Implementada*:
    - Na criação da identidade ou convite, a chave pública de assinatura Ed25519 (`signingPubKey`) é registrada de forma imutável em `identities/{fingerprint}`.
    - A callable `updateIdentityRouting` exige assinatura digital Ed25519 válida sobre o payload canônico `"pmsg-routing-v1|<fingerprint>|<newAuthUid>|<timestamp>"`.
    - Janela anti-replay estrita: timestamps com desvio superior a 5 minutos ($|\text{now} - \text{timestamp}| > 300.000\text{ ms}$) são sumariamente rejeitados.
    - A assinatura é verificada contra o `signingPubKey` registrado no documento. Chamadas sem assinatura válida ou originadas por contatos atacantes são rejeitadas com `permission-denied`.
    - No `firestore.rules`, updates diretos à coleção `identities` por clientes SDK são terminantemente bloqueados (`allow update, delete: if false;`), garantindo que o roteamento só possa ser alterado mediante verificação criptográfica no backend.

- **Janela TOFU de Criação de Identidades & Mitigação Futura (Backlog v1.3)**:
  - *Mecanismo Atual de Criação*: Atualmente, o documento `identities/{fingerprint}` pode ser criado diretamente pelo cliente SDK autenticado (onde `firestore.rules` exige `request.auth != null`, `!exists(...)`, `currentAuthUid == request.auth.uid` e presença de todas as chaves obrigatórias) OU indiretamente via Admin SDK ao invocar `createInvite`.
  - *Janela TOFU (Trust On First Use)*: Como o `create` é liberado para novos documentos, se um atacante obtiver antecipadamente o fingerprint público de um usuário antes que este registre seu documento ou emita seu primeiro convite, o atacante poderia tentar pré-criar o registro vinculando sua própria `signingPubKey`.
  - *Avaliação de Risco Atual*: Risco residual nulo/baixo no estágio atual, uma vez que não existem diretórios públicos de enumeração e a base ainda não possui usuários reais.
  - *Mitigação Futura Planejada (Backlog v1.3)*: Bloqueio total de criação direta por clientes em `firestore.rules` (`allow create: if false;`), transferindo a inicialização da identidade exclusivamente para uma Cloud Function Callable que exigirá prova de posse cruzada (assinatura Ed25519 sobre o binding criptográfico canônico `x25519pub || ed25519pub`), impedindo que qualquer entidade pré-registre chaves que não possui.

- **Mensagens Anteriores Perdidas por Design**: Mensagens recebidas no antigo dispositivo $\le 24$h antes da recuperação são perdidas por design. O Pmsg **não mantém histórico de conversas nem backlogs persistentes em servidores**, garantindo imunidade contra apreensão física retrospectiva.

### 6. Rate Limiting em Firestore (`userRateLimits`)
Para impedir ataques de força bruta, colheita e negação de serviço, todas as Cloud Functions críticas implementam janelas deslizantes atômicas gravadas na coleção `userRateLimits/{uid}` (inacessível a clientes SDK):
- **`resolveFingerprint`**: 30 requisições / 1 minuto.
- **`createInvite`**: 10 convites / 10 minutos.
- **`acceptInvite`**: 15 tentativas / 1 minuto.
- **`updateIdentityRouting`**: 5 atualizações / 10 minutos.
- **`geminiProxy`**: 5 chamadas / 1 minuto.

### 7. E2E de DEK via Sealed-Box (v1.2 — Servidor Zero-Knowledge)

A versão v1.2 do **Pmsg** implementa o encapsulamento criptográfico ponta-a-ponta das chaves de criptografia de dados (DEK), eliminando por completo qualquer visibilidade de chaves em claro por parte do servidor:

- **Esquema Sealed-Box por Mensagem**:
  - Para cada mensagem enviada, o remetente (Alice) gera um par de chaves efêmero X25519 (`ephemeralPriv`, `ephemeralPub`).
  - **Anonimato Criptográfico do Remetente**: A chave de identidade de Alice NÃO é utilizada no wrapping da DEK. O envelope não vaza metadados sobre a identidade do remetente.
  - $\text{sharedSecret} = \text{X25519}(\text{ephemeralPriv}, \text{recipientPubKey})$
  - $\text{KEK} = \text{HKDF-SHA256}(\text{sharedSecret}, \text{salt}=\text{SHA-256}(\text{ephemeralPub}), \text{info}=\text{"pmsg-dek-wrap-v1"})$
  - $\text{wrappedDek} = \text{AES-256-GCM}(\text{DEK}, \text{KEK}, \text{nonce}_{96\text{-bit}})$
- **Armazenamento de Bytes 100% Opacos no Firestore**:
  - O documento na coleção `messageKeys` armazena exclusivamente: `{ messageId, senderId, recipientId, ephemeralPubKey, wrappedDek, expiresAt }`.
  - **O campo "dek" em claro foi terminantemente eliminado** de toda a infraestrutura e chamadas de rede.
- **Desembrulho pelo Destinatário (Bob)**:
  - Bob obtém `{ ephemeralPubKey, wrappedDek }` via `getMessageKey`.
  - $\text{sharedSecret} = \text{X25519}(\text{recipientPrivKey}, \text{ephemeralPub})$
  - Deriva exatamente a mesma $\text{KEK}$ via HKDF-SHA256 e decifra a $\text{DEK}$ via AES-256-GCM em memória volátil.
- **🛡️ GARANTIA DEFINITIVA EVOLUÍDA (Zero-Trace / Zero-Knowledge)**:
  - **Comprometimento TOTAL do Servidor**: Mesmo na hipótese de comprometimento absoluto do Firestore, Cloud Functions, logs e tráfego de rede, um invasor obtém apenas **ciphertexts + DEKs envelopadas**. O conteúdo é **MATEMATICAMENTE IRRECUPERÁVEL** sem as chaves privadas dos dispositivos participantes.
  - **Histórico Local de Mensagens Enviadas**: A cópia da DEK de Alice é cifrada localmente em seu cofre de hardware (KeyVault). A perda física do dispositivo implica na perda do histórico de enviados — característica inerente ao modelo de privacidade máxima.

### 8. Troca Presencial de Contatos via QR Code (v1.2)

- **Exibição Universal (Display)**:
  - Implementado com a biblioteca pura Kotlin `io.github.alexzhirkevich:qrose` para Compose Multiplatform.
  - Renderiza nativamente em **todas as plataformas** (Android, iOS, Desktop Windows e Web).
  - Disponível na aba **"Meu Código"** (Modelo A) com toggle intuitivo *QR Code ↔ String URI* e no **Convite Remoto** (Modelo C).
  - **Desktop interoperável**: O PC desktop exibe o QR Code em tela cheia para que dispositivos móveis escaneiem sem necessidade de webcam no computador.
- **Leitura via Câmera (Scanner Offline)**:
  - **Android**: `CameraX` + `ML Kit Barcode Scanning` operando **100% offline no dispositivo** (nenhuma imagem ou frame sai do aparelho, em estrita fidelidade ao princípio zero-trace). Permissão `CAMERA` com fallback transparente.
  - **iOS**: Pipeline nativo via `AVFoundation` (`AVCaptureMetadataOutput` tipo `.qr`) com preview acelerado por hardware via `UIKitView`.
  - **Desktop / Web**: Fallback claro e orientado para inserção de string URI via área de transferência (desktops raramente possuem câmera frontal conveniente).
  - **Pipeline Unificado de Parsing**: O payload decodificado do QR entra exatamente no mesmo analisador e validador (`IdentityManager.parseContactUri`), aplicando a checagem criptográfica $\text{fingerprint} == \text{SHA-256}(\text{pubKey})$ e cálculo do Número de Segurança de 60 dígitos.

---

## 🛡️ Matriz Honesta de Garantias de Segurança por Plataforma

| Recurso / Garantia | Android (Nativo) | Desktop Windows (JVM) | iOS (CMP) | Web (WasmJS) |
|---|---|---|---|---|
| **Hardware KeyStore / TEE** | ✅ Sim (`AndroidKeyStore` StrongBox/TEE) | ⚠️ Parcial (Windows DPAPI / CNG em repouso) | ✅ Sim (`Apple Keychain` Secure Enclave) | ❌ Não (Memória volátil da aba) |
| **Proteção de Identidade / Envelope** | ✅ Sim (Chave Mestre StrongBox + Biometria) | ✅ Sim (DPAPI + Chave AES-256 em Cofre) | ✅ Sim (Keychain + LocalAuthentication) | ⚠️ Parcial (Sessão efêmera WebCrypto) |
| **Bloqueio de Screenshot** | ✅ Sim (`FLAG_SECURE` impede print e gravação) | ❌ Não (Sem API de SO para bloquear terceiros) | ❌ Não (iOS **NÃO** bloqueia hardware prints) | ❌ Não (Inviável no navegador) |
| **Detecção de Screenshot** | ✅ Sim (`ScreenCaptureCallback`) | ❌ Não | ✅ Sim (`UserDidTakeScreenshotNotification`) | ❌ Não |
| **Proteção de Clipboard** | ✅ Sim (`EXTRA_IS_SENSITIVE` + Clear 30s) | ⚠️ Sim (Auto-limpeza com timer na JVM) | ⚠️ Sim (Auto-limpeza com timer local) | ⚠️ Sim (Auto-limpeza volátil) |
| **Incineração (Panic Shredding)** | ✅ Sim (Invalidação KeyStore + Overwrite) | ✅ Sim (Deleção DPAPI + Overwrite de memória) | ✅ Sim (Deleção Keychain + Overwrite) | ⚠️ Sim (Limpeza de memória volátil) |
| **Contatos Locais Cifrados** | ✅ Sim (Room + AES-256-GCM em Hardware) | ✅ Sim (SQLite Cifrado com Envelope Local) | ✅ Sim (Keychain + Armazenamento Local Cifrado) | ⚠️ Volátil (IndexedDB efêmero em sessão) |
| **Integridade (App Check)** | ✅ Sim (Play Integrity API nativo) | ⚠️ Sim (Token de Sessão via Proxy Backend) | ✅ Sim (DeviceCheck / App Attest nativo) | ⚠️ Sim (reCAPTCHA Enterprise / Proxy) |
| **Visibilidade da DEK no Servidor** | 🛡️ **Zero-Knowledge** (Sealed-Box X25519; servidor vê apenas bytes opacos) | 🛡️ **Zero-Knowledge** | 🛡️ **Zero-Knowledge** | 🛡️ **Zero-Knowledge** |
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

### 6. Operação: Monitoramento em Produção & Políticas de Alerta (ATIVO — Free Tier)

O monitoramento contínuo da infraestrutura em produção (`gen-lang-client-0858445711`) está **ATIVO** e operacional no **Google Cloud Monitoring** e **Cloud Logging**, funcionando integralmente com custo **US$ 0,00 / mês** no Free Tier.

#### 6.1 Canal de Notificação e Gestão Operacional
- **Canal Ativo**: `Pmsg-Ops-Alerts` (E-mail do operador verificado com notificação de teste recebida e validada).
- **Gestão Operacional**: Manutenção e edição centralizadas manualmente via **Google Cloud Console > Monitoring > Alerting / Uptime checks**. Nenhuma dependência do `gcloud CLI` local na máquina de desenvolvimento, preservando o princípio de menor privilégio e isolamento de credenciais.

#### 6.2 Políticas de Alerta e Uptime Checks Ativos em Produção

| Política / Recurso | Tipo | Severidade | Condição de Disparo | Status em Produção | Ação / Justificativa |
|---|---|:---:|---|:---:|---|
| **`[PMSG-P0] Falha na Execução do Shredder`** | Log-based Alert | **CRÍTICO (P0)** | `severity>=ERROR` no log da Cloud Function / Cloud Run `scheduledMessageShredder`. | ✅ **ATIVO** | Purga de segurança: alerta imediato de falha no expurgo de mensagens expiradas. |
| **`[PMSG-P0] Ausência do Shredder >2h`** | Métrica (Ausência) | **CRÍTICO (P0)** | Ausência de sinal de execução (`shredder_heartbeat`) por janela superior a 120 min (o job roda a cada 1h: `0 * * * *`). | ✅ **ATIVO** | Detecta interrupção silenciosa ou suspensão do Cloud Scheduler. |
| **`[PMSG-P1] Erros 5xx nas Cloud Functions`** | Métrica de Erro | **ALTO (P1)** | `response_code_class="5xx"` (ou status de erro) nas 9 Cloud Functions v2 do projeto: `geminiProxy`, `storeMessageKey`, `getMessageKey`, `onDeleteMessage`, `scheduledMessageShredder`, `resolveFingerprint`, `createInvite`, `acceptInvite` e `updateIdentityRouting`. | ✅ **ATIVO** | Notifica falhas internas de execução, quebras de contrato ou timeouts de backend em toda a infraestrutura serverless. |
| **`[PMSG-P0] Alteração no Secret GEMINI_API_KEY`** | Cloud Audit Log | **CRÍTICO (P0)** | Chamadas de auditoria `DestroySecretVersion`, `DisableSecretVersion` ou `DeleteSecret` no secret `GEMINI_API_KEY`. | ✅ **ATIVO** | Previne e notifica alterações indevidas ou deleção da chave da IA no Secret Manager. |
| **`[PMSG] geminiProxy online`** | Uptime Check HTTPS | **ALTO (P1)** | Sondagem HTTPS a cada 1 min no endpoint `geminiProxy`. Validação de status code: resposta `401 Unauthorized` tratada como sucesso. | ✅ **ATIVO** | Comprova disponibilidade contínua do endpoint e validação da barreira de autenticação. |

#### 6.3 Integração Contínua (CI) e Orçamento de Minutos macOS
- **Política de Execução**: O CI iOS (`.github/workflows/ios-build.yml`) roda no push de tags (`v*`), disparos manuais (`workflow_dispatch`) e em pushes/PRs de código. Não roda em commits de documentação ou scripts de desenvolvimento (economia de minutos macOS do plano Free: ~200 min/mês = ~13 builds). Commits com alterações exclusivas em `**.md`, `docs/**`, `scripts/**`, `pages/**`, `LICENSE`, `LICENSE-COMMERCIAL.md` e `CLA.md` são ignorados via `paths-ignore`.

#### 6.4 Padrão Técnico de Não-Vazamento de Credenciais (Runtime Seguro)
Para qualquer automação ou script que demande credenciais temporárias do Git/GitHub em runtime, é mandatória a captura direta em memória volátil, sem eco no console e sem persistência em disco:
```powershell
$cred = "protocol=https`nhost=github.com`n" | git credential fill
$token = ($cred | Where-Object { $_ -match '^password=' }) -replace '^password=',''
# $token existe estritamente em memória e é injetado diretamente nos headers HTTP
# PROIBIDO: imprimir $token, gravá-lo em logs/arquivos ou deixá-lo ecoar no terminal
```
Para as diretrizes completas e regras permanentes do agente de IA, consulte [`AGENTS.md`](AGENTS.md).

#### 6.5 Visibilidade do Repositório e Auditoria Pública
O repositório é PÚBLICO por decisão de arquitetura (AGPL-3.0): todo o código, documentos legais e o histórico de homologação são auditáveis publicamente. Documentos internos de operação (DSR, runbooks) são controlados fora do versionamento público. O inventário de segurança confirma que nenhuma chave privada, segredo de infraestrutura ou mnemônico BIP-39 jamais existiu no histórico de commits (conforme auditorias rigorosas v1.0–v1.2 e regras em [`AGENTS.md`](AGENTS.md)).

---

## 🛡️ Segurança de Usuários & Conformidade Google Play (v1.3)

O ciclo **v1.3** implementa os requisitos técnicos mandatórios da Google Play Store (Políticas de Dados do Usuário e Conteúdo Gerado pelo Usuário - UGC) para mensageria anônima:

### 1. Age-Gate 18+ & Aceite Legal Versionado
- **Barreira Bloqueante de Primeira Execução**: O aplicativo não avança para nenhuma tela operacional sem que o usuário confirme explicitamente ter 18 anos ou mais e assinale o aceite formal da [Política de Privacidade](https://sampaiofa-tech.github.io/Pmsg/privacidade.html) e dos [Termos de Uso](https://sampaiofa-tech.github.io/Pmsg/termos.html).
- **Armazenamento Seguro de Consentimento**: Registrado localmente em cofre isolado com carimbo de data/hora e versão canônica do documento (ex: `1.0`).
- **Invalidação por Atualização de Versão**: Caso uma nova versão dos termos ou política seja publicada, o aplicativo invalida o consentimento anterior e exige novo aceite imediato antes de liberar o acesso.

### 2. Bloqueio de Contatos Client-Side & Auto-Purge de Mensagens
- **Armazenamento Criptografado Local**: Blocklist mantida localmente no banco Room (`blocked_contacts`: `{fingerprint, blockedAt}`) no Android e em arquivos de configuração locais protegidos no Desktop.
- **Interface de Gestão de Bloqueio**: Opção "Bloquear Contato" acessível tanto na lista de contatos quanto na tela de chat efêmero e diretamente em cada mensagem recebida, além de tela dedicada para visualização e desbloqueio.
- **Enforçamento no Fetch (Auto-Purge)**: Mensagens originadas de fingerprints bloqueados são descartadas e incineradas sumariamente no momento do recebimento, sem decifração em memória nem renderização visual na UI, mantendo registro local do total de mensagens expurgadas.
- **Limitação Técnica Honesta (Client-Enforced)**:
  > [!NOTE]
  > O servidor Pmsg opera sob a premissa fundamental de **Zero-Knowledge** e não armazena nem tem visibilidade sobre a lista de contatos bloqueados pelos usuários. Portanto, o bloqueio é **estritamente client-enforced** (executado no dispositivo do destinatário).
  > O remetente bloqueado ainda pode gerar tráfego efêmero e custo de escrita transitório no Firestore até que a mensagem seja purgada na chegada pelo destinatário ou destruída pelo TTL máximo de 24 horas. Esse custo é estritamente contido pelos rate limits por UID e autenticação do Firebase.

---

## 📄 Licença

Este projeto é disponibilizado sob o modelo de **duplo licenciamento**:

1. **Core Open Source (AGPL-3.0)**:
   - Distribuído sob os termos da licença **GNU Affero General Public License v3.0 (AGPL-3.0)** — veja o arquivo [`LICENSE`](LICENSE).
   - O código que você audita é exatamente o código que executa: transparência, auditabilidade pública e garantia de que modificações disponibilizadas como serviço de rede permaneçam com código aberto para a comunidade.
2. **Edição Comercial (Escritório / Private)**:
   - Licenciamento proprietário voltado a empresas e escritórios que demandam isenção das obrigações da AGPL-3.0, customizações de governança corporativa, implantação on-premises / nuvem privada e suporte técnico com SLA.
   - Para detalhes e solicitação de propostas comerciais, consulte [`LICENSE-COMMERCIAL.md`](LICENSE-COMMERCIAL.md).
3. **Contribuições de Código**:
   - Todas as contribuições externas ao repositório são regidas pelo Acordo de Contribuição Individual — consulte [`CLA.md`](CLA.md).

> [!IMPORTANT]
> **Nota sobre a Marca e Denominação do Produto**: O nome "Pmsg" adotado neste repositório e na documentação técnica é **estritamente provisório**. O registro de marca permanece **indefinido** perante os órgãos competentes (como o INPI) até a deliberação e homologação da denominação comercial definitiva do produto.

