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
- `ExampleUnitTest`: Testes de cálculo de TTL, auto-desaparecimento após leitura e trituração de mensagens.

---

## 📄 Licença

Desenvolvido para segurança e privacidade estrita. Zero Trace.
