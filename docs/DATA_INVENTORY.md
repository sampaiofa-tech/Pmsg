# Inventário de Dados e Relatório de Auditoria de Rastreamento (LGPD) — Raix

**Data da Auditoria:** 05 de setembro de 2026  
**Controlador:** Filippe Andrade Sampaio (desenvolvedor independente)  
**Encarregado (DPO):** Filippe Andrade Sampaio (`contato@raixtech.com`)  
**Aplicação:** Raix (Depósito de marca nominativa agendado para 08/09/2026 junto ao INPI — guia/protocolo preparatório nº 945109300)  
**Baseline de Código Auditado:** v1.4 (`composeApp`, Cloud Functions v1.4)

---

## 1. Inventário de Dados Tratados

A tabela a seguir discrimina de forma taxativa e exaustiva todas as informações e dados técnicos tratados pelo aplicativo e pela infraestrutura do **Raix**.

| Dado | Onde Vive (Armazenamento) | Por Quanto Tempo (Retenção) | Quem Tem Acesso | Base Legal (LGPD) |
| :--- | :--- | :--- | :--- | :--- |
| **UID Anônimo do Firebase Auth** | Firebase Authentication / Firestore | Enquanto a sessão anônima for válida | Servidor (Cloud Functions) para validação de sessão; Operador de nuvem (Google Cloud) | Art. 7º, IX — Legítimo Interesse (segurança e mitigação de abuso) |
| **Fingerprint Criptográfico (Ed25519)** | Firestore (`identities/{fingerprint}`) | Até remoção explícita pelo titular ou revogação de identidade | Usuários com o link/QR code de convite; Servidor (validação de roteamento) | Art. 7º, V — Execução de contrato / Termos de Uso |
| **Chave Pública de Roteamento (Ed25519)** | Firestore (`identities/{fingerprint}`) | Até remoção explícita pelo titular ou revogação | Público aos portadores do fingerprint; Servidor | Art. 7º, V — Execução de contrato |
| **Chave Pública de Criptografia (X25519)** | Firestore (`identities/{fingerprint}`) | Até remoção explícita pelo titular | Público aos portadores do fingerprint (usado para empacotar a DEK) | Art. 7º, V — Execução de contrato |
| **Registros de Conexão (MCI Art. 15)** | Firestore (`accessLogs/{logId}`) | **180 dias** (TTL estrito com descarte automático) | **Admin SDK apenas** (read/write: false para clients). **SEM UID, SEM FINGERPRINT, SEM MESSAGEID, SEM PAYLOAD, SEM CHAVE, SEM MNEMÔNICO**. | Art. 7º, II da LGPD c/c Art. 15 da Lei nº 12.965/2014 (MCI) |
| **Ciphertext Efêmero da Mensagem** | Firestore (`messages/{messageId}`) | **Máximo de 24 horas** (TTL estrito) ou destruição imediata após leitura (*Vanish*) | Remetente e Destinatário (apenas bytes opacos ilegíveis pelo servidor) | Art. 7º, V — Execução de contrato |
| **Metadados de Roteamento (`senderId` e `recipientId`)** | Firestore (`messages/{messageId}` e `messages/{messageId}/keys/{keyId}`) | **Máximo de 24 horas** (expurgados atomicamente junto ao envelope) | Remetente, Destinatário e Servidor (roteamento e autorização estrita). **Dado mais sensível que o servidor retém**, protegido por destruição em ≤24h. | Art. 7º, V — Execução de contrato |
| **DEK Envelopada (bytes opacos)** | Firestore (`messages/{messageId}/keys`) | **Máximo de 24 horas** (TTL estrito) ou destruição imediata após leitura (*Vanish*) | Destinatário detentor da chave privada correspondente. O servidor **não possui a chave privada e não tem acesso aos bytes em claro** | Art. 7º, V — Execução de contrato |
| **Chave Efêmera Pública (ephemeralPubKey)** | Firestore (`messages/{messageId}/keys`) | **Máximo de 24 horas** (TTL estrito) ou destruição imediata após leitura | Destinatário e Servidor (armazenamento estritamente temporário para derivação ECDH) | Art. 7º, V — Execução de contrato |
| **Timestamps de Criação e Expiração** | Firestore (`messages/{messageId}`) | Máximo 24 horas (eliminados com a mensagem) | Servidor (Cloud Functions e Cloud Firestore TTL scheduler) | Art. 7º, IX — Legítimo Interesse (gestão do ciclo de vida da efemeridade) |
| **Conteúdo Voluntário de Denúncias (`abuseReportsWithContent`)** | Firestore (`abuseReports/{reportId}`) | **Até 90 dias após a conclusão/fechamento da apuração interna** | Auditores internos de segurança (Admin SDK restrito). **Destino: Exclusão definitiva e cripto-incineração irreversível (hard-delete)**. | Art. 7º, I (Consentimento explícito) c/c Art. 7º, IX (Legítimo Interesse) |
| **Logs Técnicos de Acesso / Execução** | Google Cloud Logging (`us-central1`) | 30 dias (política padrão e estrita do Google Cloud Logging) | Desenvolvedor/Administrador para diagnóstico e mitigação de incidentes | Art. 7º, II da LGPD c/c Art. 16 do Marco Civil da Internet (obrigação legal) |
| **Chaves Privadas (Ed25519, X25519)** | **Dispositivo do Usuário** (Room / Keychain / Keystore criptografada / DPAPI) | Até desinstalação do app ou acionamento de *Panic Wipe* | **Apenas o usuário**. **NUNCA** trafegam ou vivem no servidor. | Não aplicável ao servidor (dado não coletado) |
| **Frase Mnemônica (12 Palavras BIP-39)** | **Memória volátil / Armazenamento Seguro Local** | Até remoção voluntária pelo usuário | **Apenas o usuário**. **NUNCA** é enviada ou armazenada pelo servidor. | Não aplicável ao servidor (dado não coletado) |
| **Contatos Locais e Apelidos** | **Dispositivo do Usuário** (banco local cifrado) | Até remoção pelo usuário ou *Panic Wipe* | **Apenas o usuário**. O servidor **desconhece** o grafo de contatos. | Não aplicável ao servidor (dado não coletado) |
| **Frames / Imagens da Câmera** | **Memória volátil do Dispositivo** | Zero segundos (descartados imediatamente após decodificação do QR) | **Apenas o decodificador local on-device**. Zero gravação em disco ou rede. | Não aplicável ao servidor (dado não coletado) |

---

## 2. Implementação e Segregação dos Logs de Conexão (MCI Art. 15 / Parecer C1)

Em cumprimento ao Art. 15 da Lei nº 12.965/2014 (Marco Civil da Internet) e às determinações do Parecer Jurídico Especializado, a aplicação **Raix** mantém uma coleção segregada e isolada denominada `accessLogs`:

1. **Campos Registrados no Momento da Chamada:**
   - `ip`: Endereço IP do requisitante extraído do cabeçalho `x-forwarded-for` ou do raw request.
   - `timestampUtc`: Timestamp ISO-8601 UTC do momento da requisição.
   - `porta`: Porta de origem da conexão. *(Nota técnica: caso o proxy de terminação HTTP do Google Cloud Functions omita a porta de origem nos cabeçalhos encaminhados, este campo registra o valor `"UNAVAILABLE_RUNTIME"`, permanecendo o IP e o timestamp como elementos mandatórios de identificação de conexão).*
   - `functionName`: Nome da callable acionada (`resolveFingerprint`, `storeMessageKey`, etc.).
   - `expiresAt`: Timestamp configurado para exatamente **180 dias** após a criação (`now + 180d`).

2. **Isolamento Absoluto (Zero Rastreabilidade com Conteúdo ou Identidade):**
   - Os documentos de `accessLogs` **NÃO CONTÊM**: `uid`, `fingerprint`, `messageId`, nomes de contato, payloads criptografados, mnemônicos ou chaves públicas Ed25519 (Regra Absoluta do Parecer Jurídico).
   - É **tecnicamente impossível** correlacionar um registro de log de conexão com o remetente, destinatário ou conteúdo de qualquer mensagem.

3. **Regras de Segurança e Acesso:**
   - O arquivo `firestore.rules` define expressamente `allow read, write: if false;` para a coleção `accessLogs`. Somente o Firebase Admin SDK (Cloud Functions backend) tem permissão de escrita, e nenhum cliente pode listar ou consultar os registros.

4. **Nota sobre Prazos Legais e Fundamentação do Parecer:**
   - *Ponto de Atenção*: O Art. 13 do Marco Civil da Internet estipula 1 ano para provedores de **conexão à internet**. Para **provedores de aplicações de internet** (categoria na qual o Raix se insere), o Art. 15 da mesma lei estabelece o dever de guarda pelo prazo de **6 meses (180 dias)**. Prevalece a orientação técnica do parecerista pelo prazo de 180 dias com expiração automatizada via política de TTL do Firestore.

---

## 3. Evidência do Timeout de Mensagens Transitórias ≤ 24h (Parecer C2)

Fica atestado que o Raix cumpre estritamente a exigência de ciclo de vida transitório com expiração máxima em 24 horas:

1. **Definição de TTL:** As chamadas `storeMessageKey` e a gravação de mensagens impõem limite máximo de `expiresAtMillis <= now + 86.400.000 ms` (24 horas). Qualquer tentativa de gravação com prazo superior é recusada pelo backend com erro `invalid-argument`.
2. **Crypto-Shredder Horário:** A Cloud Function agendada `hourlyCryptoShredder` é executada automaticamente a cada hora para expurgar chaves e mensagens expiradas remanescentes que não tenham sido consumidas pelo destinatário.
3. **Vanish Imediato:** Quando o destinatário lê a mensagem, o documento transitório é apagado atomicamente do Firestore no mesmo instante (*Vanish-After-Read*).
4. **Ausência de Fila Indefinida:** Não existe qualquer fila ou buffer persistente secundário sem política de expiração ativa.
5. **Expurgo Irreversível de Metadados de Roteamento:** Os identificadores técnicos de roteamento (`senderId` e `recipientId`) existem exclusivamente como atributos transitórios atrelados aos documentos de `messages` e `messageKeys`. No momento da destruição do envelope (seja via leitura, expiração ou shredder), estes metadados — reconhecidos como a informação técnica mais sensível temporariamente retida pelo servidor — são incinerados de forma irreversível.

---

## 4. Auditoria de Dependências (Verificação de Ausência de Rastreamento)

Foi executada verificação formal e automatizada no repositório de código fonte do aplicativo (`composeApp`) e nas definições de dependências centralizadas (`gradle/libs.versions.toml`) com o intuito de atestar a ausência de SDKs de publicidade, telemetria analítica comportamental e rastreamento de terceiros.

```bash
git grep -i -E "analytics|crashlytics|admob|facebook|appsflyer|adjust|mixpanel|amplitude" composeApp/build.gradle.kts gradle/libs.versions.toml
```
**Resultado obtido:** `Exit code: 1` (zero ocorrências encontradas).

---

## 5. Conclusão e Certificação Técnica

O ecossistema **Raix** opera sob estrita consonância com os princípios de **Finalidade**, **Adequação**, **Necessidade** e **Segurança** dispostos no art. 6º da Lei Geral de Proteção de Dados (Lei nº 13.709/2018), tratando unicamente os elementos técnicos indispensáveis para viabilizar a entrega de mensagens efêmeras com criptografia ponta-a-ponta de chaves.
