# Política de Privacidade — Raix

**Última atualização:** 05 de setembro de 2026  
**Versão:** 2.0 (Conforme v1.4 da arquitetura do aplicativo)  
**Marca:** Raix (Marca nominativa depositada no INPI sob protocolo nº 945109300)  
**Canal Oficial:** `https://raixtech.com`

Esta Política de Privacidade descreve, com transparência e rigor técnico, como o aplicativo **Raix** trata dados e preserva a privacidade e a segurança absoluta dos usuários, em estrita conformidade com a **Lei Geral de Proteção de Dados Pessoais do Brasil (Lei nº 13.709/2018 - LGPD)** e o **Marco Civil da Internet (Lei nº 12.965/2014 - MCI)**.

---

## 1. Identificação do Controlador e do Encarregado (DPO)

- **Controlador dos Dados Técnicos de Infraestrutura**: Filippe Andrade Sampaio, desenvolvedor independente.
- **Encarregado pelo Tratamento de Dados Pessoais (DPO)**: Filippe Andrade Sampaio (Pessoa Natural).
- **Canal Oficial de Contato e Exercício de Direitos**: `contato@raixtech.com`.

---

## 2. Dados Tratados pelo Servidor (Inventário Exato)

O Raix foi projetado sob o princípio da **minimização extrema de dados** (*Privacy by Design* e *Privacy by Default*). O servidor em nuvem (Google Cloud Platform / Firebase) processa estritamente os seguintes dados técnicos e transitórios:

1. **Identificador Técnico Anônimo (UID)**: Identificador alfanumérico gerado pelo serviço de autenticação anônima do Firebase Auth. **Não vinculado** a nome civil, e-mail, telefone, CPF, redes sociais ou identificadores permanentes de hardware.
2. **Identidade Pública de Roteamento (`identities`)**:
   - `fingerprint`: Hash criptográfico SHA-256 da chave pública de identidade.
   - `pubKey`: Chave pública crua X25519 (usada por contatos para envelopar mensagens para você).
   - `signingPubKey`: Chave pública crua Ed25519 (usada exclusivamente para validar a prova de posse do roteamento).
   - `currentAuthUid`: UID anônimo da sessão ativa associada ao fingerprint para entrega técnica de mensagens.
   - `revoked`: Flag booleano de moderação indicando se a rota técnica foi revogada por violação grave dos Termos de Uso.
3. **Registros de Conexão à Aplicação (MCI Art. 15 — Base Legal Art. 7º, II da LGPD)**:
   - Registrados na coleção isolada `connectionLogs`: endereço IP de origem, timestamp UTC da requisição, porta lógica de origem (quando fornecida pelo runtime) e nome do endpoint callable acionado.
   - **Isolamento Absoluto**: Estes registros **NÃO** possuem vínculo com UIDs, fingerprints, mensagens, contatos ou chaves criptográficas, destinando-se exclusivamente ao cumprimento de obrigação legal de segurança da informação.
4. **Ciphertext Efêmero da Mensagem**:
   - Texto cifrado através de **AES-256-GCM**. O servidor **não possui** a chave necessária para decifrar este conteúdo, tratando-o unicamente como sequência opaca de bytes.
5. **DEK Envelopada (*Sealed-Box*)**:
   - A Chave de Criptografia de Dados (DEK) é envelopada no dispositivo do remetente através de **Sealed-Box X25519 + HKDF-SHA256 + AES-256-GCM**. O servidor armazena exclusivamente bytes opacos (`wrappedDek`). O servidor **não aprende, não vê e não armazena a DEK em claro**.
6. **Chave Efêmera por Mensagem (`ephemeralPubKey`)**:
   - Chave pública temporária X25519 gerada pelo remetente exclusivamente para aquela mensagem específica, garantindo anonimato criptográfico do remetente perante o servidor.
7. **Conteúdo Voluntário de Denúncia (Fluxo Secundário Opcional com Consentimento Explícito)**:
   - Caso um usuário receba conteúdo ilícito, assédio ou ameaças e opte voluntariamente por denunciar com evidência, o aplicativo solicita **consentimento prévio e expresso**. Apenas a mensagem selecionada é descriptografada no dispositivo do denunciante e enviada ao backend de moderação (`reportAbuseWithContent`). Este procedimento **nunca** é o padrão e depende de ato deliberado e inequívoco da vítima.

---

## 3. Dados que NUNCA são Coletados ou Tratados pelo Servidor

Em razão da arquitetura **Zero-Knowledge** e **Zero-Trace** do Raix, o servidor **NUNCA** tem acesso aos seguintes dados no fluxo regular:

- ❌ **Conteúdo Legível de Conversas**: O texto decifrado existe unicamente na memória volátil dos aparelhos interlocutores durante o prazo do temporizador efêmero.
- ❌ **Livro de Contatos e Grafos Sociais**: Seus contatos, nomes locais e notas são salvos **exclusivamente no armazenamento local cifrado do seu dispositivo**.
- ❌ **Chaves Privadas e Frase Mnemônica**: O par de chaves privadas (X25519 e Ed25519) e a frase mnemônica de 12 palavras permanecem restritos ao cofre seguro do seu dispositivo (KeyStore / Keychain / DPAPI) e **jamais são transmitidos pela rede**.
- ❌ **Frames ou Imagens da Câmera**: O leitor de QR Code opera 100% offline dentro do dispositivo.
- ❌ **Dados Cadastrais Identificáveis (PII)**: Não solicitamos nome, e-mail, telefone ou documentos para utilização do mensageiro.

---

## 4. Bases Legais para o Tratamento (Art. 7º da LGPD)

- **Cumprimento de Obrigação Legal ou Regulatória (Art. 7º, II da LGPD)**: Guarda dos registros de acesso a aplicações de internet pelo prazo de 180 dias, em cumprimento estrito ao Art. 15 da Lei nº 12.965/2014 (Marco Civil da Internet).
- **Execução de Contrato e Termos de Uso (Art. 7º, V da LGPD)**: Roteamento técnico e entrega transitória de mensagens cifradas solicitadas pelo usuário.
- **Legítimo Interesse do Controlador (Art. 7º, IX da LGPD)**: Aplicação de limites de requisição (*rate limiting*), auditoria técnica de integridade, prevenção a fraudes e ataques Sybil.
- **Consentimento Específico e Destacado (Art. 7º, I da LGPD)**: Exclusivamente no fluxo secundário e voluntário de denúncia de conteúdo ilícito (`reportAbuseWithContent`).

---

## 5. Compartilhamento de Dados com Terceiros

**Não comercializamos, não compartilhamos e não monetizamos dados pessoais sob nenhuma hipótese.**

O único compartilhamento existente ocorre na qualidade de **operador de infraestrutura tecnológica**, estritamente vinculado à execução técnica do serviço:

- **Google Cloud Platform / Firebase (Google LLC)**: Provedor de computação em nuvem, banco de dados (Firestore), execução sem servidor (Cloud Functions) e autenticação anônima (Firebase Auth). A Google atua exclusivamente na condição de operadora dos serviços contratados.
- **Cloudflare Inc.**: Provedor de resolução DNS e proteção perimetral de rede do domínio institucional `raixtech.com`.

> O Raix **NÃO** inclui SDKs de analytics (Google Analytics, Firebase Analytics), SDKs de publicidade (AdMob), SDKs de redes sociais ou rastreadores de terceiros.

---

## 6. Transferência Internacional de Dados (Art. 33 da LGPD)

A infraestrutura de servidores do Raix está alocada na região **`us-central1` (Iowa, Estados Unidos)**, provida pela Google Cloud Platform. A transferência internacional cumpre os requisitos do Art. 33 da LGPD, respaldada por cláusulas contratuais padrão de proteção de dados e certificações internacionais de conformidade e segurança (ISO/IEC 27001, 27017, 27018 e relatórios SOC 1/2/3).

---

## 7. Prazos de Retenção e Descarte de Dados

| Tipo de Dado | Local de Armazenamento | Prazo de Retenção | Mecanismo de Expurgo |
|---|---|---|---|
| **Mensagens e DEKs Envelopadas** | Firestore (`messages`, `messageKeys`) | **Máximo de 24 horas** (ou menos, conforme TTL) | Destruição no consumo (*Vanish-After-Read*) ou purga horária pelo *Crypto-Shredder*. |
| **Identidade de Roteamento** | Firestore (`identities`) | Enquanto a identidade existir | Exclusão voluntária pelo usuário ou solicitação formal ao Encarregado. |
| **Registros de Conexão (MCI Art. 15)** | Firestore (`connectionLogs`) | **180 dias** (Art. 15 MCI) | Expurgo automático via política de TTL do banco. |
| **Denúncias com Conteúdo** | Firestore (`abuseReportsWithContent`) | **90 dias** ou conclusão da auditoria | Expurgo programado após análise e eventual revogação da chave ofensora. |
| **Logs Técnicos de Diagnóstico** | Google Cloud Logging | **30 dias** | Sobrescrita automática padrão do Cloud Logging. |
| **Contatos e Chaves Privadas** | Dispositivo Local do Usuário | Permanente até remoção local | Controle exclusivo do usuário pelo aplicativo ou botão de Pânico (*Panic Wipe*). |

---

## 8. Sanções de Moderação e Princípio de Não Bloqueio de IP

Para mitigar abusos, fraudes e assédio sem comprometer usuários legítimos que compartilham redes móveis ou CGNAT:
- A moderação do Raix **NUNCA aplica bloqueio por endereço IP**.
- A sanção máxima aplicável a identidades que violem comprovadamente as regras de uso consiste na **revogação da chave pública Ed25519 no diretório de roteamento** (`identities/{fingerprint}` marcada como `revoked: true`), impedindo que o infrator continue utilizando o servidor como caixa postal.

---

## 9. Direitos dos Titulares de Dados (Art. 18 da LGPD) e Bifurcação de Papéis

1. **Dados de Transporte (Raix como Operadora):**  
   Para confirmação, acesso ou exclusão da sua identidade pública de roteamento (`identities/{fingerprint}`), entre em contato com nosso Encarregado:
   - **E-mail Oficial:** `contato@raixtech.com`
   - **Assunto:** `[LGPD] Solicitação de Direitos do Titular`
   - **Prazo de Resposta:** Até **15 (quinze) dias**, nos termos do Art. 19 da LGPD.

2. **Conteúdo de Conversas Profissionais (O Profissional como Controlador):**  
   Caso você utilize o Raix para se comunicar com profissionais liberais (advogados, médicos, contadores, consultores), o conteúdo de suas comunicações pertence à relação profissional sob a **controladoria exclusiva do respectivo profissional**. Como a Raix opera em modelo de servidor cego e efêmero, requisições de acesso a históricos de atendimento devem ser dirigidas diretamente ao profissional ou empresa responsável.

---

## 10. Alterações e Versionamento desta Política

Esta Política de Privacidade (Versão 2.0) poderá ser atualizada periodicamente para refletir aprimoramentos técnicos ou regulatórios. Notificações sobre alterações materiais serão publicadas no portal oficial `https://raixtech.com` e na tela "Sobre seus dados" do aplicativo.
