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
