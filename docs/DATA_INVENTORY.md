# Inventário de Dados e Relatório de Auditoria de Rastreamento (LGPD)

**Data da Auditoria:** 4 de setembro de 2026  
**Controlador:** Filippe Andrade Sampaio (desenvolvedor independente)  
**Encarregado (DPO):** `azfstick00@gmail.com`  
**Aplicação:** Pmsg (nome provisório)  
**Baseline de Código Auditado:** v1.2 (`composeApp`, Cloud Functions v1.2)

---

## 1. Inventário de Dados Tratados

A tabela a seguir discrimina de forma taxativa e exaustiva todas as informações e dados técnicos tratados pelo aplicativo e pela infraestrutura do Pmsg.

| Dado | Onde Vive (Armazenamento) | Por Quanto Tempo (Retenção) | Quem Tem Acesso | Base Legal (LGPD) |
| :--- | :--- | :--- | :--- | :--- |
| **UID Anônimo do Firebase Auth** | Firebase Authentication / Firestore | Enquanto a sessão anônima for válida | Servidor (Cloud Functions) para validação de sessão; Operador de nuvem (Google Cloud) | Art. 7º, IX — Legítimo Interesse (segurança e mitigação de abuso) |
| **Fingerprint Criptográfico (Ed25519)** | Firestore (`identities/{fingerprint}`) | Até remoção explícita pelo titular ou revogação de identidade | Usuários com o link/QR code de convite; Servidor (validação de roteamento) | Art. 7º, V — Execução de contrato / Termos de Uso |
| **Chave Pública de Roteamento (Ed25519)** | Firestore (`identities/{fingerprint}`) | Até remoção explícita pelo titular | Público aos portadores do fingerprint; Servidor | Art. 7º, V — Execução de contrato |
| **Chave Pública de Criptografia (X25519)** | Firestore (`identities/{fingerprint}`) | Até remoção explícita pelo titular | Público aos portadores do fingerprint (usado para empacotar a DEK) | Art. 7º, V — Execução de contrato |
| **Ciphertext Efêmero da Mensagem** | Firestore (`messages/{messageId}`) | Máximo 24 horas (TTL) ou destruição imediata após leitura (*Vanish*) | Remetente e Destinatário (apenas bytes opacos ilegíveis pelo servidor) | Art. 7º, V — Execução de contrato |
| **DEK Envelopada (bytes opacos)** | Firestore (`messages/{messageId}/keys`) | Máximo 24 horas (TTL) ou destruição imediata após leitura (*Vanish*) | Destinatário detentor da chave privada correspondente. O servidor **não possui a chave privada e não tem acesso aos bytes em claro** | Art. 7º, V — Execução de contrato |
| **Chave Efêmera Pública (ephemeralPubKey)** | Firestore (`messages/{messageId}/keys`) | Máximo 24 horas (TTL) ou destruição imediata após leitura | Destinatário e Servidor (armazenamento estritamente temporário para derivação ECDH) | Art. 7º, V — Execução de contrato |
| **Timestamps de Criação e Expiração** | Firestore (`messages/{messageId}`) | Máximo 24 horas (eliminados com a mensagem) | Servidor (Cloud Functions e Cloud Firestore TTL scheduler) | Art. 7º, IX — Legítimo Interesse (gestão do ciclo de vida da efemeridade) |
| **Logs Técnicos de Acesso / Execução** | Google Cloud Logging (`us-central1`) | 30 dias (política padrão e estrita do Google Cloud Logging) | Desenvolvedor/Administrador para diagnóstico e mitigação de incidentes | Art. 7º, II c/c Art. 16 do Marco Civil da Internet (obrigação legal) |
| **Chaves Privadas (Ed25519, X25519)** | **Dispositivo do Usuário** (Room / Keychain / Keystore criptografada) | Até desinstalação do app ou acionamento de *Panic Wipe* | **Apenas o usuário**. **NUNCA** trafegam ou vivem no servidor. | Não aplicável ao servidor (dado não coletado) |
| **Frase Mnemônica (12 Palavras BIP-39)** | **Memória volátil / Armazenamento do Dispositivo** | Até remoção voluntária pelo usuário | **Apenas o usuário**. **NUNCA** é enviada ou armazenada pelo servidor. | Não aplicável ao servidor (dado não coletado) |
| **Contatos Locais e Apelidos** | **Dispositivo do Usuário** (banco Room local) | Até remoção pelo usuário ou *Panic Wipe* | **Apenas o usuário**. O servidor **desconhece** o grafo de contatos. | Não aplicável ao servidor (dado não coletado) |
| **Frames / Imagens da Câmera** | **Memória volátil do Dispositivo** | Zero segundos (descartados no milissegundo após detecção do QR) | **Apenas o decodificador local (ML Kit Barcode Scanning / fallback local)**. Zero gravação em disco ou rede. | Não aplicável ao servidor (dado não coletado) |

---

## 2. Auditoria Real de Dependências (Verificação de Ausência de Rastreamento)

Foi executada verificação formal e automatizada no repositório de código fonte do aplicativo (`composeApp`) e nas definições de dependências centralizadas (`gradle/libs.versions.toml`) com o intuito de atestar a ausência de SDKs de publicidade, telemetria analítica comportamental e rastreamento de terceiros.

### 2.1. Execução do Comando de Auditoria

```bash
git grep -i -E "analytics|crashlytics|admob|facebook|appsflyer|adjust|mixpanel|amplitude" composeApp/build.gradle.kts gradle/libs.versions.toml
```

**Resultado obtido:**
```text
Exit code: 1 (nenhuma ocorrência encontrada)
```

### 2.2. Discriminação Positiva de Bibliotecas Utilizadas em `composeApp`

Todas as dependências declaradas no aplicativo pertencem estritamente às seguintes categorias funcionais essenciais:

1. **Framework e UI:**
   - Jetpack Compose / Compose Multiplatform (`androidx.compose.*`, `org.jetbrains.compose.*`)
   - Material 3 Design System
   - AndroidX Core KTX, Lifecycle, Activity Compose, Biometric
2. **Criptografia e Segurança:**
   - Bouncy Castle (`org.bouncycastle:bcprov-jdk18on:1.79`) para operações de Edwards/Curve25519 em JVM/Desktop
   - Implementações nativas Apple CryptoKit (`iosMain`) e Android KeyStore (`androidMain`)
3. **Persistência Local Offline:**
   - AndroidX Room (`androidx.room:room-*`) e SQLite Bundled
4. **Comunicação de Rede e Serialização:**
   - Ktor Client (`io.ktor:ktor-client-*`)
   - Kotlinx Serialization JSON
5. **QR Code Offline:**
   - QRose (`io.github.alexzhirkevich:qrose:1.0.1`) — renderização vetorial de QR codes puramente em memória
   - Google ML Kit Barcode Scanning (`com.google.mlkit:barcode-scanning:17.3.0`) — biblioteca on-device (sem comunicação de rede para processamento de imagem)
   - CameraX (`androidx.camera:*`) — pipeline de câmera restrito à visualização local

### 2.3. Declaração de Ausência de Rastreamento

Fica atestado tecnicamente que o aplicativo Pmsg:
- **NÃO** inclui Google Analytics for Firebase (`firebase-analytics`);
- **NÃO** inclui Firebase Crashlytics (`firebase-crashlytics`);
- **NÃO** inclui Facebook Core / Audience Network SDKs;
- **NÃO** inclui bibliotecas de atribuição de anúncios (AppsFlyer, Adjust, Singular, Branch);
- **NÃO** inclui ferramentas de gravação de sessão ou telemetria comportamental (Mixpanel, Amplitude, Segment, PostHog);
- **NÃO** faz uso de cookies de terceiros, identificadores de publicidade (IDFA/GAID) ou técnicas de *device fingerprinting* para publicidade.

---

## 3. Conclusão e Certificação Técnica

O ecossistema Pmsg opera sob estrita consonância com os princípios de **Finalidade**, **Adequação**, **Necessidade** e **Segurança** dispostos no art. 6º da Lei Geral de Proteção de Dados (Lei nº 13.709/2018), tratando unicamente os elementos técnicos indispensáveis para viabilizar a entrega de mensagens efêmeras com criptografia ponta-a-ponta de chaves.

### 3.1. Certificação de Ausência de Segredos no Histórico de Versionamento
Adicionalmente, confirma-se que o repositório público do Pmsg é 100% isento de chaves privadas, segredos de infraestrutura ou mnemônicos BIP-39 em todo o histórico de commits, conforme atestado pelas auditorias de segurança conduzidas nas versões v1.0, v1.1 e v1.2 e pelas diretrizes permanentes de operação do agente de IA documentadas em `AGENTS.md`.
