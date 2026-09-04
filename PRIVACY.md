# Política de Privacidade — Pmsg (nome provisório)

**Última atualização:** 4 de setembro de 2026  
**Versão:** 1.0 (Conforme v1.2 da arquitetura do aplicativo)

Esta Política de Privacidade descreve, com transparência e clareza, como o aplicativo **Pmsg (nome provisório)** trata dados e preserva a privacidade e a segurança dos usuários, em estrita conformidade com a **Lei Geral de Proteção de Dados Pessoais do Brasil (Lei nº 13.709/2018 - LGPD)**.

---

## 1. Identificação do Controlador e do Encarregado (DPO)

- **Controlador dos Dados**: Filippe Andrade Sampaio, desenvolvedor independente.
- **Encarregado pelo Tratamento de Dados Pessoais (DPO)**: Filippe Andrade Sampaio.
- **Canal de Contato e Exercício de Direitos**: `azfstick00@gmail.com`.

---

## 2. Dados Tratados pelo Servidor (Inventário Exato)

O Pmsg foi projetado sob o princípio da **minimização extrema de dados** (*Privacy by Design* e *Privacy by Default*). O servidor em nuvem (Google Cloud Platform / Firebase) processa estritamente os seguintes dados técnicos e transitórios:

1. **Identificador Técnico Anônimo (UID)**: Identificador alfanumérico gerado pelo serviço de autenticação anônima do Firebase Auth. **Não vinculado** a nome civil, e-mail, telefone, CPF, redes sociais ou identificadores permanentes de hardware.
2. **Identidade Pública de Roteamento (`identities`)**:
   - `fingerprint`: Hash criptográfico SHA-256 da chave pública de identidade.
   - `pubKey`: Chave pública crua X25519 (usada por contatos para envelopar mensagens para você).
   - `signingPubKey`: Chave pública crua Ed25519 (usada exclusivamente para validar a prova de posse do roteamento).
   - `currentAuthUid`: UID anônimo da sessão ativa associada ao fingerprint para roteamento de notificações.
3. **Timestamps de Ciclo de Vida**:
   - `createdAt` e `expiresAt`: Horários de criação e expiração da mensagem e da chave temporária, utilizados pelo motor de autodestruição (*Crypto-Shredding*).
4. **Ciphertext Efêmero da Mensagem**:
   - Texto cifrado através de **AES-256-GCM**. O servidor **não possui** a chave necessária para decifrar este conteúdo, tratando-o unicamente como sequência opaca de bytes.
5. **DEK Envelopada (*Sealed-Box*)**:
   - A Chave de Criptografia de Dados (DEK) é envelopada no dispositivo do remetente através de **Sealed-Box X25519 + HKDF-SHA256 + AES-256-GCM**. O servidor armazena exclusivamente bytes opacos (`wrappedDek`). O servidor **não aprende, não vê e não armazena a DEK em claro**.
6. **Chave Efêmera por Mensagem (`ephemeralPubKey`)**:
   - Chave pública temporária X25519 gerada pelo remetente exclusivamente para aquela mensagem específica, garantindo anonimato criptográfico do remetente perante o servidor.

---

## 3. Dados que NUNCA são Coletados ou Tratados pelo Servidor

Em razão da arquitetura **Zero-Knowledge** e **Zero-Trace** do Pmsg, o servidor **NUNCA** tem acesso aos seguintes dados:

- ❌ **Conteúdo Legível das Mensagens**: O texto decifrado existe unicamente na memória volátil dos dispositivos dos interlocutores.
- ❌ **Livro de Contatos**: Seus contatos, nomes locais e notas são salvos **exclusivamente no armazenamento local cifrado do seu dispositivo**. O servidor desconhece quem são seus contatos.
- ❌ **Chaves Privadas e Mnemônicos**: O par de chaves privadas (X25519 e Ed25519) e a frase mnemônica de 12 palavras permanecem restritos ao cofre seguro do seu dispositivo (KeyStore/KeyChain/DPAPI) e **jamais são transmitidos pela rede**.
- ❌ **Frames ou Imagens da Câmera**: O leitor de QR Code opera 100% offline dentro do dispositivo. Nenhuma foto ou fluxo de vídeo é transmitido para a nuvem.
- ❌ **Dados Pessoais Identificáveis (PII)**: Não solicitamos nome, e-mail, telefone, documentos ou dados de pagamento para o uso básico do mensageiro.

---

## 4. Bases Legais para o Tratamento (Art. 7º da LGPD)

O tratamento restrito dos dados técnicos acima fundamenta-se nas seguintes bases legais da LGPD:

- **Execução de Contrato e Termos de Uso (Art. 7º, V da LGPD)**: Tratamento estritamente necessário para viabilizar a entrega e roteamento técnico das mensagens solicitadas pelo usuário.
- **Legítimo Interesse do Controlador (Art. 7º, IX da LGPD)**: Aplicação de limites de requisição (*rate limiting*), auditoria técnica de integridade, prevenção a abusos e segurança cibernética da infraestrutura.

---

## 5. Compartilhamento de Dados com Terceiros

**Não comercializamos, não compartilhamos e não monetizamos dados pessoais sob nenhuma hipótese.**

O único compartilhamento existente ocorre na qualidade de **operador de infraestrutura tecnológica**, estritamente vinculado à execução técnica do aplicativo:

- **Google Cloud Platform / Firebase (Google LLC)**: Provedor de computação em nuvem, banco de dados (Firestore), execução sem servidor (Cloud Functions) e autenticação anônima (Firebase Auth). A Google atua exclusivamente na condição de operadora dos serviços contratados.

> **Importante**: O Pmsg **NÃO** inclui SDKs de analytics (Google Analytics, Firebase Analytics), SDKs de publicidade (AdMob), redes sociais ou rastreadores de terceiros.

---

## 6. Transferência Internacional de Dados (Art. 33 da LGPD)

A infraestrutura de servidores do Pmsg está localizada na região **`us-central1` (Iowa, Estados Unidos)**, mantida pela Google Cloud Platform.

A transferência internacional de dados técnicos cumpre os requisitos do Art. 33 da LGPD, respaldada por:
- Cláusulas contratuais padrão de proteção de dados firmadas com a Google Cloud.
- Aderência do operador internacional a padrões globais de segurança e privacidade da informação certificados (incluindo **ISO/IEC 27001**, **ISO/IEC 27017**, **ISO/IEC 27018** e relatórios **SOC 1/2/3**).

---

## 7. Prazos de Retenção e Descarte de Dados

Os dados possuem ciclos de retenção estritamente delimitados e processos automatizados de expurgo:

| Tipo de Dado | Local de Armazenamento | Prazo de Retenção | Mecanismo de Expurgo |
|---|---|---|---|
| **Mensagens e DEKs Envelopadas** | Firestore (`messages`, `messageKeys`) | **Máximo de 24 horas** (ou menos, conforme TTL) | Destruição no consumo (*Vanish-After-Read*) ou purga horária pelo *Crypto-Shredder*. |
| **Identidade de Roteamento** | Firestore (`identities`) | Enquanto a identidade existir | Exclusão voluntária pelo usuário ou solicitação formal ao Encarregado. |
| **Logs Técnicos de Auditoria** | Cloud Logging | **30 dias** (retenção padrão) | Sobrescrita automática do Cloud Logging após 30 dias. |
| **Contatos e Chaves Privadas** | Dispositivo Local do Usuário | Permanente até remoção local | Controle exclusivo do usuário pelo aplicativo ou botão de Pânico (*Panic Wipe*). |

---

## 8. Medidas de Segurança da Informação (Art. 46 da LGPD)

Adotamos salvaguardas técnicas avançadas para proteger os dados contra acessos não autorizados:

1. **Criptografia Ponta a Ponta com Servidor Cego (*Zero-Knowledge*)**: Envelopamento de DEK via Sealed-Box X25519 por mensagem. Mesmo em cenário de invasão física ou comprometimento integral dos servidores, o atacante obtém apenas cifras indecifráveis sem a chave privada do aparelho de destino.
2. **Prova de Posse Criptográfica (Ed25519)**: Atualizações de roteamento de sessão exigem assinatura digital baseada em curva elíptica Ed25519 com tolerância temporal estrita anti-replay (janela de 5 minutos), impedindo sequestros de identidade.
3. **Crypto-Shredding Imediato**: Deleção atômica e irreversível da chave DEK ao término da leitura (*Vanish-on-Delete*), impossibilitando reconstituições forenses retrospectivas.
4. **Isolamento de Chaves em Hardware**: Suporte a hardware seguro local (`AndroidKeyStore` StrongBox/TEE, `Apple Keychain` Secure Enclave e Windows DPAPI).

---

## 9. Uso da Câmera do Dispositivo

O aplicativo solicita permissão de acesso à câmera **exclusivamente** para a funcionalidade de escaneamento presencial de códigos QR (adicionar contatos ou aceitar convites).
- O processamento visual dos frames da câmera ocorre **100% de forma local e offline** no hardware do aparelho (via Google ML Kit Barcode Scanning offline no Android e AVFoundation no iOS).
- **Nenhuma imagem, foto ou vídeo é capturado, persistido em disco ou transmitido através da rede.**

---

## 10. Direitos dos Titulares de Dados (Art. 18 da LGPD)

Em cumprimento ao Art. 18 da LGPD, você possui os seguintes direitos em relação aos seus dados:

1. **Confirmação e Acesso**: Confirmação da existência de tratamento e acesso aos dados técnicos associados à sua identidade.
2. **Correção**: Atualização do vínculo de sessão técnica da sua chave pública.
3. **Eliminação e Descarte**: Exclusão definitiva do documento de roteamento de sua identidade (`identities/{fingerprint}`) de nossos servidores.
4. **Informações sobre Compartilhamento**: Informações claras sobre as entidades com as quais os dados foram compartilhados (Google Cloud, conforme Seção 5).
5. **Revogação do Consentimento**: Eliminação da identidade técnica a qualquer momento.

### Como Exercer seus Direitos
Para exercer quaisquer desses direitos, envie uma mensagem para o Encarregado de Dados no endereço:
- **E-mail**: `azfstick00@gmail.com`
- **Assunto**: `[LGPD] Solicitação de Direitos do Titular`
- **Prazo de Resposta**: Em conformidade com o Art. 19 da LGPD, responderemos em formato simplificado ou declaração detalhada em até **15 (quinze) dias**.

Você também possui o direito de peticionar perante a **Autoridade Nacional de Proteção de Dados (ANPD)** caso entenda que o tratamento de seus dados violou preceitos da LGPD.

---

## 11. Alterações e Versionamento desta Política

Esta Política de Privacidade poderá ser atualizada periodicamente para refletir aprimoramentos técnicos de privacidade ou adequações regulatórias. Notificações sobre alterações materiais serão disponibilizadas nesta página e na tela "Sobre seus dados" do aplicativo.
