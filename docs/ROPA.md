# Registro das Operações de Tratamento de Dados Pessoais (ROPA) — Raix

**Última atualização:** 05 de setembro de 2026  
**Versão:** 1.0 (Conforme arquitetura v1.5 do aplicativo)  
**Controladora:** **Cat Tech** (CNPJ: **67.497.085/0001-36**)  
**Encarregado pelo Tratamento de Dados Pessoais (DPO):** Filippe Andrade Sampaio (`contato@raixtech.com`)  
**Portal Oficial:** `https://raixtech.com`  
**Fundamentação Legal:** Art. 37 da Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018 - LGPD)

Em atendimento ao Art. 37 da LGPD, a **Cat Tech** mantém o presente registro estruturado de todas as operações de tratamento de dados pessoais e técnicos executadas pela infraestrutura do mensageiro **Raix**.

---

## Tabela Geral das Operações de Tratamento (ROPA)

| # | Operação de Tratamento | Categoria de Dados | Finalidade Específica | Base Legal (LGPD) | Compartilhamento / Operador | Retenção / Descarte | Medidas de Segurança |
|:---:|:---|:---|:---|:---|:---|:---|:---|
| **1** | Autenticação Anônima de Sessão | UID alfanumérico anônimo (Firebase Auth) | Validar token de sessão e viabilizar transporte técnico de chamadas de API | Art. 7º, IX (Legítimo Interesse) | Google Cloud Platform (`us-central1`) | Duração da sessão ativa | Token efêmero, sem vínculo com dados civis ou biometria |
| **2** | Registro de Identidade de Roteamento | `fingerprint` (SHA-256), chave pública X25519 e chave pública Ed25519 | Permitir localização técnica e validação da prova de posse para entrega de mensagens | Art. 7º, V (Execução de Contrato) | Google Cloud Platform (Firestore) | Até exclusão voluntária pelo usuário no app ou requisição DSR | Assinatura digital Ed25519 obrigatória em cada atualização |
| **3** | Roteamento Efêmero de Mensagens | Ciphertext da mensagem (bytes cifrados via AES-256-GCM) | Armazenamento transitório em fila até entrega ao destinatário | Art. 7º, V (Execução de Contrato) | Google Cloud Platform (Firestore) | Máximo 24h (TTL) ou destruição imediata no consumo (*Vanish*) | Servidor cego (*Zero-Knowledge*), incapaz de ler o conteúdo |
| **4** | Envelopamento de Chave de Sessão | DEK envelopada (`wrappedDek` via Sealed-Box X25519 + HKDF-SHA256) | Permitir que o destinatário decifre a chave simétrica de sessão | Art. 7º, V (Execução de Contrato) | Google Cloud Platform (Firestore) | Máximo 24h (TTL) ou destruição imediata no consumo | Criptografia assimétrica opaca; chave privada inacessível ao servidor |
| **5** | Metadados Transitórios de Roteamento | `senderId` e `recipientId` nos envelopes de fila | Autorizar resgate da chave pelo destinatário legítimo e rotear mensagens | Art. 7º, V (Execução de Contrato) | Google Cloud Platform (Firestore) | Máximo 24h (expurgo com o envelope) | Não mantém logs relacionais persistentes; identificadores existem até a entrega ou máx. 24h, incinerados irreversivelmente |
| **6** | Purga Ativa e Incineração Automatizada | Envelopes e chaves expiradas remanescentes | Eliminar definitivamente mensagens e chaves de destinatários offline | Art. 7º, IX (Legítimo Interesse) | Google Cloud Platform (Cloud Functions) | Ciclo horário (`scheduledMessageShredder`) | Destruição atômica redundante à política de TTL do Firestore |
| **7** | Guarda de Registros de Conexão (MCI) | Endereço IP de origem, timestamp UTC, porta de origem e endpoint acionado | Cumprimento de obrigação legal de segurança da informação (Art. 15 MCI) | Art. 7º, II da LGPD c/c Art. 15 da Lei 12.965/2014 | Google Cloud Platform (`accessLogs`) | **180 dias** (Art. 15 MCI) | Datastore isolado, sem associação com identidade, payload ou chave Ed25519. Regra `deny-all` para clients |
| **8** | Criptografia em Trânsito de Rede | Pacotes de rede e requisições HTTPS | Proteção perimetral contra interceptação, espionagem e adulteração | Art. 7º, II c/c Art. 46 (Segurança) | Google Cloud / Cloudflare | Transitório (apenas em voo) | **TLS 1.2+ com preferência 1.3**, HSTS e certificados gerenciados |
| **9** | Moderação e Apuração de Denúncias | (a) Telemetria de rede local / reputação técnica (`reportAbuse`); (b) Conteúdo voluntário anexado (`reportAbuseWithContent`) | Apuração de abusos graves, assédio e mitigação de spam sem violar inocentes | Art. 7º, I (Consentimento explícito para anexo) c/c Art. 7º, IX | Equipe interna de auditoria (Admin SDK) | **No máximo 90 dias após encerramento da apuração** (hard-delete) | Sanção exclusiva de revogação de chave pública Ed25519 (`revoked: true`). **Sem bloqueio de IP** |
| **10** | Diagnóstico Operacional e Mitigação de Falhas | Logs técnicos de execução de funções (sem payloads de mensagens) | Monitoramento de erros de runtime, auditoria interna de estabilidade | Art. 7º, II da LGPD c/c Art. 16 MCI | Google Cloud Logging (`us-central1`) | 30 dias (política padrão Cloud Logging) | Acesso restrito via IAM a desenvolvedores autorizados |
| **11** | Proteção Perimetral e Resolução DNS | Consultas de DNS e tráfego HTTPS de borda | Proteção contra ataques distribuídos de negação de serviço (DDoS) | Art. 7º, IX (Legítimo Interesse) | Cloudflare Inc. | Transitório / telemetria de borda | Mitigação de tráfego malicioso e blindagem de infraestrutura |
| **12** | Atendimento a Direitos dos Titulares (DSR) | Fingerprint público da identidade e comprovante de prova de posse | Atender solicitações de confirmação, acesso ou exclusão (Art. 18 LGPD) | Art. 7º, II (Cumprimento de Obrigação Legal) | DPO Interno (`contato@raixtech.com`) | 5 anos (para defesa judicial em caso de contencioso) | Atendimento em até 15 dias corridos (Art. 19 da LGPD) |
| **13** | Gestão de Governança e Canal DPO | Registros de comunicações e notificações à ANPD | Governança institucional, conformidade contínua e reporte regulatório | Art. 41 da LGPD (Canal de Encarregado) | DPO Interno: Filippe Andrade Sampaio | Permanente durante a operação | Canal oficial exclusivo: `contato@raixtech.com` |

---

## Salvaguardas Críticas e Limites Técnicos

1. **Custódia Local de Chaves:** As chaves criptográficas privadas (X25519 e Ed25519) e o mnemônico de 12 palavras permanecem **custodiadas exclusivamente no dispositivo do Usuário, sob proteção do sistema operacional** (DPAPI no Windows, KeyStore no Android), não constando como dados tratados pelo servidor.
2. **Princípio da Não-Retenção Relacional:** A infraestrutura **não mantém logs relacionais persistentes** vinculando remetente e destinatário.
3. **Padrão de TLS:** Confirmado suporte a **TLS 1.2+ com preferência TLS 1.3** nos pontos de terminação de borda da nuvem Google Cloud.

---

**Cat Tech** — CNPJ 67.497.085/0001-36  
Encarregado (DPO): Filippe Andrade Sampaio — `contato@raixtech.com`
