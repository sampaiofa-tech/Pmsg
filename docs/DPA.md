# Acordo de Processamento de Dados (DPA) — Raix

**Última atualização:** 05 de setembro de 2026  
**Versão:** 1.0 (Conforme arquitetura v1.5 do aplicativo)  
**Canal Oficial:** `https://raixtech.com`  
**Referência Legal:** Art. 39 da Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018 - LGPD)

Este Acordo de Processamento de Dados ("DPA" — *Data Processing Agreement*) estabelece as condições técnicas, obrigações e garantias de segurança aplicáveis ao tratamento de dados pessoais no âmbito da disponibilização, execução e infraestrutura do aplicativo de comunicação privada **Raix**.

---

## Parte I — Objeto e Escopo

1. **Objeto:** O presente instrumento disciplina a relação técnica e jurídica de tratamento de dados pessoais e dados técnicos de tráfego, assegurando a estrita conformidade com a LGPD (Lei nº 13.709/2018), o Marco Civil da Internet (Lei nº 12.965/2014) e as melhores práticas internacionais de segurança da informação.
2. **Escopo do Tratamento:** O tratamento realizado pela Operadora restringe-se estritamente à disponibilização da infraestrutura técnica necessária ao roteamento efêmero de envelopes criptografados de dados, à validação de chaves públicas de roteamento e à guarda obrigatória de registros de conexão prevista no Art. 15 do Marco Civil da Internet.

---

## Parte II — Papéis e Responsabilidades no Tratamento

1. **Bifurcação de Papéis:**
   - **Controlador:** O usuário, organização ou profissional liberal que utiliza a aplicação para estabelecer comunicação com seus respectivos interlocutores e destinatários. No caso de profissionais que utilizam a aplicação no âmbito de suas atividades corporativas ou liberais (médicos, advogados, consultores), estes assumem a qualidade exclusiva de Controladores sobre o conteúdo de suas comunicações.
   - **Operadora:** A provedora da tecnologia, responsável pela manutenção da infraestrutura lógica em nuvem e execução do roteamento efêmero.
2. **Postura de Servidor Cego (*Zero-Knowledge*):**
   - A Operadora atua como uma entidade tecnicamente cega perante o conteúdo trafegado. As mensagens são cifradas de ponta a ponta (E2EE) no dispositivo de origem e decifradas apenas no dispositivo de destino, de modo que a Operadora não possui, sob nenhuma hipótese, capacidade matemática de acesso, interceptação, decifração ou recuperação de mensagens em claro.

---

## Parte III — Identificação das Partes e Qualificação Jurídica

1. **Operadora da Plataforma e Infraestrutura:**
   - **Razão Social:** **Cat Tech**
   - **Inscrição no CNPJ/MF:** **67.497.085/0001-36**
   - **Atuação:** Desenvolvedora, mantenedora e operadora do aplicativo e ecossistema **Raix**.
2. **Encarregado pelo Tratamento de Dados Pessoais (DPO):**
   - **Nome:** Filippe Andrade Sampaio
   - **Canal Oficial de Comunicação:** `contato@raixtech.com`
3. **Controlador Contratante:** A pessoa natural ou jurídica devidamente identificada no ato de download, instalação ou utilização corporativa da aplicação.

---

## Parte IV — Medidas Técnicas e Organizacionais de Segurança da Informação

A Operadora compromete-se a manter ativas as seguintes salvaguardas técnicas e operacionais de segurança da informação:

1. **Criptografia Ponta-a-Ponta e Gestão de Chaves:**
   - Cifragem de dados em repouso transitório com **AES-256-GCM**.
   - Envelopamento de chaves de criptografia de dados (DEK) via **Sealed-Box X25519 + HKDF-SHA256**.
   - Prova de posse e autenticação de rotas por assinaturas digitais **Ed25519**.
2. **Custódia Local de Chaves:**
   - As chaves criptográficas privadas e a semente mnemônica de 12 palavras permanecem **custodiadas exclusivamente no dispositivo do Usuário, sob proteção do sistema operacional** (DPAPI no Windows, KeyStore no Android), jamais sendo transmitidas ou salvas no servidor.
3. **Registros de Conexão à Aplicação (MCI Art. 15):**
   - A Operadora mantém **registros de conexão (IP, porta, timestamp UTC e nome do endpoint) retidos por até 180 dias em datastore isolado, sem associação com identidade, payload ou chave pública Ed25519**.
   - O armazém segregado `accessLogs` possui permissão `deny-all` para clientes (`read, write: if false;`), sendo gerenciado exclusivamente pelo backend automatizado para cumprimento do Art. 15 da Lei nº 12.965/2014.
4. **Ausência de Logs Relacionais Persistentes:**
   - A Operadora **não mantém logs relacionais persistentes; identificadores de roteamento (remetente/destinatário) existem apenas até a entrega ou no máximo 24 horas, sendo incinerados irreversivelmente após esse período** (*Vanish-After-Read* ou *Crypto-Shredder* horário).
5. **Moderação e Denúncias de Abuso:**
   - O fluxo padrão de apuração é comportamental e Zero-Knowledge (`reportAbuse`).
   - No fluxo secundário de denúncia com anexo voluntário de conteúdo (`reportAbuseWithContent`), o material fornecido mediante consentimento explícito da vítima é retido por **no máximo 90 dias após o encerramento da apuração**, com posterior **exclusão definitiva (hard-delete)**.
   - A sanção técnica consiste exclusivamente na **revogação da chave pública Ed25519**, sendo vedada a aplicação de bloqueio por endereço IP.
6. **Segurança em Trânsito:**
   - Toda comunicação entre cliente e servidor exige protocolo **TLS 1.2+ com preferência TLS 1.3**.
7. **Sub-operadores Autorizados:**
   - **Google Cloud Platform (Google LLC):** Infraestrutura de banco de dados (Firestore), execução em nuvem (Cloud Functions) e autenticação anônima (Firebase Auth) na região `us-central1`.
   - **Cloudflare Inc.:** Resolução DNS e mitigação perimetral de ataques no domínio `raixtech.com`.

---

## Parte V — Transferência Internacional de Dados

1. A infraestrutura de computação em nuvem operada pela Cat Tech encontra-se hospedada na região **`us-central1` (Estados Unidos da América)**, provida pela Google Cloud Platform.
2. A transferência internacional atende ao Art. 33 da LGPD, amparada por Cláusulas Padrão Contratuais e compromissos rigorosos de conformidade técnica e privacidade (ISO/IEC 27001, 27017, 27018 e relatórios SOC).

---

## Parte VI — Notificação de Incidentes e Atendimento a Direitos (DSR)

1. **Comunicação de Incidentes:** Em caso de incidente de segurança relevante que envolva dados técnicos de conexão sob custódia da Operadora, esta notificará os titulares e a ANPD nos prazos e termos regulamentares cabíveis.
2. **Cooperação com Solicitações de Titulares (DSR):** A Operadora prestará assistência aos titulares no atendimento aos direitos previstos no Art. 18 da LGPD em relação aos dados técnicos públicos de roteamento no prazo legal de até **15 (quinze) dias**, mediante solicitação dirigida ao canal `contato@raixtech.com`.

---

## Parte VII — Vigência, Rescisão e Expurgo

1. **Vigência:** O presente DPA vigorará durante todo o período em que o usuário ou entidade cliente fizer uso da plataforma Raix.
2. **Expurgo Definitivo:** Encerrada a utilização ou expirado o tempo de vida máximo parametrizado de mensagens (TTL $\le$ 24h), envelopes e chaves efêmeras são definitivamente destruídos sem retenção residual de backups legíveis.

---

**Cat Tech** — CNPJ 67.497.085/0001-36  
Canal de DPO: `contato@raixtech.com`  
Portal: `https://raixtech.com`
