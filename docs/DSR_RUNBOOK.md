# Runbook Interno — Atendimento a Solicitações de Titulares (DSR / LGPD)

> **CONFIDENCIAL — DOCUMENTAÇÃO INTERNA DO CONTROLADOR**  
> **NÃO PUBLICAR NO GITHUB PAGES OU RECURSOS PÚBLICOS**  
> **Controlador & DPO:** Filippe Andrade Sampaio (`azfstick00@gmail.com`)  
> **Legislação Base:** Lei Geral de Proteção de Dados (Lei nº 13.709/2018), Arts. 18 e 19

---

## 1. Visão Geral e Prazos Legais

Como desenvolvedor e controlador independente do **Pmsg (nome provisório)**, você tem o dever legal de responder a solicitações dos titulares de dados (direitos do Art. 18 da LGPD).

- **Prazo Máximo de Resposta:** **Até 15 (quinze) dias**, contados a partir da data da solicitação do titular (Art. 19, II da LGPD).
- **Canal Oficial de Entrada:** `azfstick00@gmail.com`.
- **Natureza do Atendimento:** Manual e rastreada.

---

## 2. Passo a Passo do Fluxo de Atendimento

```
[E-mail recebido em azfstick00@gmail.com]
                 │
                 ▼
     Passo 1: Identificação
(Validar o Fingerprint informado)
                 │
                 ▼
    Passo 2: Análise do Pedido
(Acesso, Confirmação ou Eliminação)
                 │
                 ▼
    Passo 3: Execução Técnica
(Deleção do doc identities/{fingerprint})
                 │
                 ▼
    Passo 4: Resposta ao Titular
(Envio formal em ≤ 15 dias)
                 │
                 ▼
    Passo 5: Registro no Log DSR
(Documentação comprobatória)
```

---

## 3. Passo 1: Identificação e Validação do Titular

Como o Pmsg **não coleta nomes, e-mails, senhas nem telefones** para registro de conta (autenticação 100% anônima via Firebase Auth), a correlação técnica depende do **Fingerprint Criptográfico (Ed25519)** do titular.

1. Se o titular enviou o e-mail mas **não forneceu o fingerprint**:
   - Responder solicitando que abra o app Pmsg, acesse a tela *"Sobre seus dados"* ou *"Minha Identidade"*, copie o **Fingerprint Técnico** e envie em resposta.
2. Se o titular informou o fingerprint:
   - Validar se o formato corresponde a uma chave hexadecimal ou representação de 60 dígitos válida.

---

## 4. Passo 2 e 3: Procedimento Técnico de Eliminação / Acesso

### 4.1. O Que Deve Ser Deletado (Se solicitado direito de Eliminação):
O único dado persistente associado à identidade pública do usuário no servidor é o documento de roteamento no Firestore:
- **Caminho do Documento:** `identities/{fingerprint}`

#### Como Executar a Deleção:

**Opção A — Pelo Console do Firebase:**
1. Acessar o console do Firebase no projeto `gen-lang-client-0858445711`.
2. Navegar até **Firestore Database** > coleção `identities`.
3. Localizar o documento cujo Document ID seja o `{fingerprint}` informado.
4. Clicar nas opções do documento e selecionar **Excluir documento**.

**Opção B — Via Firebase CLI / Node.js Admin SDK:**
```bash
# Executar a partir da pasta raiz do projeto com credenciais de administração
node -e "
const admin = require('firebase-admin');
admin.initializeApp();
const db = admin.firestore();
const fingerprint = process.argv[1];
if (!fingerprint) { console.error('Informe o fingerprint'); process.exit(1); }
db.collection('identities').doc(fingerprint).delete().then(() => {
  console.log('Sucesso: doc identities/' + fingerprint + ' excluido');
  process.exit(0);
}).catch(err => { console.error(err); process.exit(1); });
" "<FINGERPRINT_DO_SOLICITANTE>"
```

### 4.2. O Que NÃO Precisa de Ação Manual de Deleção (Autodestruição Criptográfica):
- **Mensagens (`messages/{messageId}`):** Já possuem política de eliminação no consumo (*Vanish*) e expiração estrita por TTL de no máximo 24 horas. Resíduo após 24h = zero.
- **DEKs envelopadas e ephemeralPubKeys (`messages/{messageId}/keys`):** Incineradas junto com as mensagens no TTL/Vanish (crypto-shredding).
- **Contatos locais e chaves privadas:** Residem unicamente no aparelho do usuário. O servidor nunca possuiu esses dados. Para eliminá-los, basta ao usuário acionar a opção *Pânico (Panic Wipe)* no app ou desinstalar o aplicativo.

### 4.3. Resíduos Técnicos em Logs:
- **Cloud Logging:** Logs de auditoria do Google Cloud possuem ciclo de retenção fixo e imutável de **30 dias**, sendo expurgados automaticamente pela política do provedor (`us-central1`), conforme amparo legal no Marco Civil da Internet (Art. 16 da Lei nº 12.965/2014 — guarda obrigatória de logs).

---

## 5. Passo 4: Modelos Formais de Resposta ao Titular

### Modelo 1 — Resposta de Confirmação de Eliminação Efetuada (Art. 18, VI da LGPD)

> **Assunto:** [Pmsg] Confirmação de Atendimento — Solicitação de Eliminação de Dados (LGPD)  
> **Para:** `<e-mail-do-solicitante>`  
>
> Olá,  
>  
> Em atenção à sua solicitação recebida em `[DATA_RECEBIMENTO]`, com fundamento no art. 18, inciso VI da Lei Geral de Proteção de Dados (Lei nº 13.709/2018 — LGPD), confirmo que foi realizada a **eliminação definitiva** do registro de sua identidade criptográfica (`identities/{fingerprint}`) de nossos servidores em nuvem.  
>  
> Esclarecemos que:  
> 1. O aplicativo Pmsg foi desenvolvido sob a arquitetura de servidor cego (*Zero-Knowledge*), não armazenando mensagens de forma permanente (as mensagens trocadas são autodestrutivas com tempo de vida máximo de 24 horas).  
> 2. O servidor nunca teve acesso ou posse do conteúdo de suas mensagens, de seus contatos, de sua frase mnemônica ou de suas chaves privadas, que residem exclusivamente na memória do seu dispositivo.  
> 3. Registros técnicos de conexão e logs de sistema são retidos pelo prazo estrito de 30 dias para cumprimento de obrigação legal de segurança (art. 16 do Marco Civil da Internet), findo o qual são expurgados automaticamente.  
>  
> Caso deseje descartar os dados presentes em seu próprio dispositivo, recomendamos acionar o botão de Pânico (*Panic Wipe*) nas opções do aplicativo ou desinstalá-lo.  
>  
> Permaneço à disposição para esclarecimentos adicionais.  
>  
> Atenciosamente,  
> **Filippe Andrade Sampaio**  
> Controlador e Encarregado pelo Tratamento de Dados Pessoais  
> Pmsg — `azfstick00@gmail.com`

---

### Modelo 2 — Resposta de Solicitação de Confirmação de Existência ou Acesso (Art. 18, I e II da LGPD)

> **Assunto:** [Pmsg] Resposta à Solicitação de Acesso / Informações sobre Tratamento de Dados (LGPD)  
> **Para:** `<e-mail-do-solicitante>`  
>  
> Olá,  
>  
> Em atenção à sua requisição formulada com base no art. 18 da LGPD, apresentamos o relatório dos dados vinculados ao fingerprint técnico `[FINGERPRINT]`:  
>  
> - **Dados Existentes no Servidor:**  
>   - Registro de identidade pública de roteamento (chave pública X25519 e chave pública Ed25519).  
>   - Identificador técnico de sessão anônima gerado pelo Firebase Authentication.  
> - **Dados Inexistentes no Servidor:**  
>   - Não possuímos seu nome, e-mail, telefone, lista de contatos, chaves privadas ou qualquer conteúdo de mensagens.  
> - **Localização dos Servidores:** Google Cloud Platform, região `us-central1` (EUA), sob certificação ISO/IEC 27001.  
>  
> Você poderá solicitar a exclusão desse registro a qualquer momento respondendo a este e-mail.  
>  
> Atenciosamente,  
> **Filippe Andrade Sampaio**  
> Controlador e Encarregado pelo Tratamento de Dados Pessoais  
> Pmsg — `azfstick00@gmail.com`

---

## 6. Passo 5: Livro de Registro de Solicitações (Auditoria Interna)

Para fins de prestação de contas (*Accountability* — Art. 6º, X da LGPD) e eventuais fiscalizações da Autoridade Nacional de Proteção de Dados (ANPD), mantenha o registro das requisições atendidas na tabela abaixo:

| ID | Data Recebimento | Solicitante (E-mail) | Fingerprint Técnico | Tipo de Solicitação | Ação Executada | Data Resposta | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| DSR-001 | DD/MM/AAAA | usuario@exemplo.com | `0123456789abcdef...` | Eliminação (Art. 18, VI) | Doc identities deletado | DD/MM/AAAA | Concluído (≤15 dias) |

---

## 7. Protocolo de Governança de Denúncias de Abuso (v1.3)

### 7.1. Natureza do Sinal `abuseFlag` (Zero-Knowledge)

A Cloud Function `reportAbuse` opera de forma estritamente comportamental e assíncrona, registrando métricas agregadas na coleção `abuseMetrics/{fingerprint}`. Quando o limiar de 3 denunciantes independentes é atingido na janela temporal, o campo `abuseFlag: true` é acionado no documento correspondente.

> [!WARNING]
> **DIRETRIZ MANDATÓRIA DE OPERAÇÃO: SINAL, NUNCA SANÇÃO AUTOMÁTICA**  
> O `abuseFlag: true` é **EXCLUSIVAMENTE UM SINAL DE ALERTA PARA REVISÃO MANUAL DO OPERADOR**. É **TERMINANTEMENTE PROIBIDO** implementar punições, banimentos ou revogações automáticas de rota baseadas isoladamente neste indicador.

### 7.2. Análise do Risco de Ataque Sybil (Autenticação Anônima)

O Pmsg adota por premissa arquitetural a **autenticação 100% anônima**, sem atrelamento a número de telefone (SMS), CPF ou identificadores estatais. Embora essa decisão elimine riscos de vazamento de dados de identidade, ela introduz um vetor conhecido de **Ataque Sybil**:
- Um único agente mal-intencionado pode gerar programaticamente múltiplos UIDs efêmeros anônimos e orquestrar denúncias coordenadas contra um fingerprint legítimo para tentar induzir uma sanção.
- Por essa razão, sanções automáticas transformariam o sistema de denúncias em uma ferramenta de censura e assédio contra alvos legítimos.

### 7.3. Camadas de Mitigação e Ação Operacional

1. **Defesa em Profundidade Client-Side (Imediata e Soberana):**  
   O usuário vítima de assédio ou spam não depende do servidor ou de moderação externa. O app oferece bloqueio client-side instantâneo com `auto-purge`: toda mensagem de contato bloqueado é descartada no dispositivo sem descriptografia e sem rastro.
2. **Revisão Manual pelo Operador:**  
   Periodicamente, o operador inspeciona a coleção `abuseMetrics` onde `abuseFlag == true`. A análise deve correlacionar:
   - Dispersão de IPs / horários dos reporters (quando disponível via Cloud Logging);
   - Reincidência de convites efêmeros (Modelo C) vinculados;
   - Padrão de tráfego de mensagens associado ao fingerprint.
3. **Ações Discricionárias Manuais:**  
   Apenas após constatação manual cabal de abuso volumétrico ou conduta contrária aos Termos de Serviço, o operador poderá intervir manualmente deletando o registro em `identities/{fingerprint}`, impedindo novas resoluções de chave no diretório técnico.

