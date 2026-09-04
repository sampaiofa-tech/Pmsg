# Pmsg v1.2 — E2E DE DEK (Zero-Knowledge) + QR Presencial

## 🛡️ Resumo da Versão v1.2
A versão **v1.2** eleva a garantia criptográfica do **Pmsg** ao estado de arte da privacidade:
1. **Frente 1 — E2E de DEK (Servidor Zero-Knowledge)**: O servidor nunca mais vê a DEK em claro. A chave simétrica da mensagem passa a ser envelopada usando o esquema **Sealed-Box** (par efêmero X25519 por mensagem + HKDF-SHA256 + AES-256-GCM de 96 bits). O Firestore armazena estritamente **bytes opacos**. Mesmo na hipótese de comprometimento total da nuvem (banco de dados, functions e logs), o conteúdo de todas as mensagens permanece **matematicamente irrecuperável**.
2. **Frente 2 — Troca Presencial via QR Code**: Exibição universal de QR Codes via `qrose` (Android, Desktop, iOS, Web) e leitura por câmera via CameraX + ML Kit offline (Android) e AVFoundation (iOS), com fallback transparente para colar strings no Desktop.

---

### 🔑 Inovações Criptográficas da Frente 1 (E2E DEK)
- **Esquema Sealed-Box por Mensagem**:
  - Remetente (Alice) gera par efêmero X25519 por mensagem (anonimato criptográfico do remetente no envelope).
  - $sharedSecret = \text{X25519}(ephemeralPriv, recipientPubKey)$.
  - $KEK = \text{HKDF-SHA256}(sharedSecret, salt=\text{SHA-256}(ephemeralPub), info=\text{"pmsg-dek-wrap-v1"})$.
  - $wrappedDek = \text{AES-256-GCM}(DEK, KEK, nonce_{96\text{-bit}})$.
- **Payload 100% Opaco**: Documentos na coleção `messageKeys` gravam exclusivamente `{ messageId, senderId, recipientId, ephemeralPubKey, wrappedDek, expiresAt }`. Campo `dek` em claro eliminado.
- **Desembrulho por Bob**: $sharedSecret = \text{X25519}(recipientPrivKey, ephemeralPub) \rightarrow KEK \rightarrow unwrap \rightarrow DEK$.
- **Garantia Evoluída**: Comprometimento total do servidor = ciphertexts + DEKs envelopadas = IRRECUPERÁVEL sem as chaves privadas físicas dos dispositivos.

---

### 📷 Inovações da Frente 2 (QR Presencial)
- **Exibição Universal**: Renderização nativa multiplataforma de QR Codes na aba "Meu Código" (Modelo A) e no "Convite Remoto" (Modelo C) com toggle fluido *QR Code ↔ String URI*.
- **Desktop Interoperável**: O Desktop exibe o QR na tela para que celulares escaneiem sem necessidade de periféricos de webcam.
- **Leitura Offline (Zero-Trace)**:
  - Android: `CameraX` + `ML Kit Barcode Scanning` em modo 100% offline.
  - iOS: `AVFoundation` nativo via Kotlin/Native com preview `UIKitView`.
  - Desktop / Web: Fallback transparente para área de transferência.
- **Pipeline Unificado**: O payload decodificado do QR alimenta exatamente o mesmo validador da string URI ($\text{fp} == \text{SHA-256}(\text{pk})$ e cálculo comutativo do Número de Segurança de 60 dígitos).

---

# Pmsg v1.0-mvp — Release Notes

## 🛡️ Resumo
O **Pmsg** é um aplicativo de mensageria efêmera e ultrassegura construído sobre arquitetura **Zero-Trace** utilizando **Kotlin Multiplatform (KMP)** e **Compose Multiplatform (CMP)**. Projetado para garantir privacidade máxima e ausência de rastros perenes, o sistema assegura que mensagens, chaves e metadados tenham ciclos de vida estritamente controlados e autodestrutivos.

---

## 📱 Suporte a Plataformas
- **Android**: APK de produção com suporte a Biometria (BiometricPrompt), `FLAG_SECURE` contra captura de tela, Room local criptografado com SQLCipher/bundled e hardware KeyStore.
- **Desktop Windows**: Aplicação desktop (JAR / distribuição nativa MSI) com autenticação real via Google Identity Toolkit, isolamento seguro de tokens com criptografia DPAPI (`CryptProtectData`/`CryptUnprotectData`), e compose hot reload operacional.
- **Web (WasmJS)**: Cliente browser compilado em WebAssembly, utilizando WebCrypto API para proteção criptográfica em tempo de execução.
- **iOS**: Target nativo `iosSimulatorArm64` e `iosArm64` com pipeline de CI compilando framework estático no runner macOS (`ios-build.yml` verde). *Nota: Validação interativa física em simulador/dispositivo iOS permanece como item planejado de roadmap.*

---

## 🔐 Arquitetura de Segurança & Zero-Trace
- **Criptografia Simétrica de Ponta**: AES-256-GCM com vetores de inicialização (IV) de 12 bytes gerados por RNG criptográfico.
- **Armazenamento de Chaves por Plataforma**:
  - Android: `AndroidKeyStore` com chaves protegidas por hardware (TEE/StrongBox).
  - Desktop: Windows DPAPI no escopo do usuário corrente (`CurrentUser`).
  - iOS: Secure Enclave / Keychain Services.
  - Web: WebCrypto API (chaves não exportáveis em memória volátil).
- **DEKs Isoladas Server-Side com Crypto-Shredding**:
  - Chaves de criptografia de dados (DEK) são armazenadas em coleção isolada `messageKeys` com regras rígidas de Firestore (`read: if false`, `write: if false`), sendo manipuladas exclusivamente pelas Cloud Functions v2 autorizadas (`storeMessageKey` e `getMessageKey`).
  - **Vanish-After-Read**: O trigger `onDeleteMessage` destrói a DEK imediatamente no exato milissegundo em que a mensagem é deletada ou consumida, tornando o texto cifrado matematicamente irrecuperável.
- **Proxy Gemini Autenticado com Rate Limiting**:
  - Todas as chamadas para geração de notas efêmeras são mediadas pela Cloud Function `geminiProxy` (`gemini-3.6-flash`), exigindo ID Token Firebase válido e verificado.
  - Rate limiting estrito transacional no Firestore (5 req/min por UID).
- **Higiene de Binários**:
  - Zero segredos e zero API keys de backend embutidas em binários (`JAR` com 0 ocorrências de `AIzaSy`; chave do Gemini blindada via Google Secret Manager no backend).

---

## ☁️ Infraestrutura & Backend de Produção
- **Projeto Google Cloud / Firebase**: `gen-lang-client-0858445711` (us-central1, plano Blaze).
- **5 Cloud Functions v2 Ativas**:
  1. `geminiProxy` (HTTPS): Proxy seguro com modelo `gemini-3.6-flash` e rate limit.
  2. `storeMessageKey` (Callable v2): Registro e validação de DEK com clamping de TTL (10s a 24h).
  3. `getMessageKey` (Callable v2): Entrega autorizada de DEK com verificação estrita de expiração e participante.
  4. `onDeleteMessage` (Eventarc Firestore Trigger): Crypto-shredding instantâneo ao apagar documento.
  5. `scheduledMessageShredder` (Cloud Scheduler): Expurgo horário de chaves vencidas (`0 * * * *`).
- **Políticas TTL**: Regra nativa de TTL ativada no Firestore em `messages.expiresAt`.
- **Custos Operacionais**: **~US$ 0,00/mês** (integralmente dentro do free tier do Google Cloud e Firebase).

---

## ⚠️ Limitações Conhecidas & Observações
1. **Validação do iOS**: Compilação de código nativo e frameworks validada 100% no CI (`macos-latest`). A execução e testes instrumentados interativos no iOS Simulator continuam pendentes para o próximo marco.
2. **Cliente Web**: Pela natureza do ambiente de execução do navegador, a Web é considerada um cliente de menor garantia de isolamento comparada a Android/Desktop nativos.
3. **App Check**: Operando em modo de monitoramento permissivo (sem enforcement estrito) para interoperabilidade plena com o cliente Desktop.
4. **Dívida Técnica**: Detalhamento completo de triagem e roadmap de dependências em [docs/TECH-DEBT.md](docs/TECH-DEBT.md).
