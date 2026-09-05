# Runbook Interno — Atendimento a Solicitações de Titulares (DSR / LGPD) — Raix

> **CONFIDENCIAL — DOCUMENTAÇÃO INTERNA DO CONTROLADOR**  
> **NÃO PUBLICAR NO GITHUB PAGES OU RECURSOS PÚBLICOS**  
> **Controlador & DPO:** Filippe Andrade Sampaio (`contato@raixtech.com`)  
> **Aplicação:** Raix (Depósito de marca nominativa agendado para 08/09/2026 junto ao INPI — guia/protocolo preparatório nº 945109300)  
> **Legislação Base:** Lei Geral de Proteção de Dados (Lei nº 13.709/2018), Arts. 18 e 19

---

## 1. Visão Geral, Prazos Legais e Princípio da Bifurcação (Parecer C7)

Como desenvolvedor e controlador independente da infraestrutura **Raix**, você tem o dever legal de responder a solicitações dos titulares de dados (direitos do Art. 18 da LGPD).

- **Prazo Máximo de Resposta:** **Até 15 (quinze) dias**, contados a partir da data da solicitação do titular (Art. 19, II da LGPD).
- **Canal Oficial de Entrada:** `contato@raixtech.com`.
- **Natureza do Atendimento:** Manual e rastreada.

### 🏛️ 1.1. Bifurcação de Papéis: Operadora de Transporte vs. Controlador de Conteúdo Profissional (C7)

Em conformidade com o Parecer Jurídico Especializado, o atendimento DSR deve distinguir claramente a natureza da solicitação:

1. **Fluxo A — Dados Técnicos de Transporte (Raix como Operadora / Controladora de Infraestrutura):**
   - **Objeto:** Identificador público de roteamento (`identities/{fingerprint}`), registros técnicos de conexão do Marco Civil (`accessLogs`) e dados de sessão anônima.
   - **Competência:** A equipe da **Raix** é diretamente responsável por confirmar, prestar informações sobre a infraestrutura ou efetuar a eliminação do registro de identidade do titular em até 15 dias.

2. **Fluxo B — Conteúdo de Conversas Profissionais (O Profissional / Empresa como Controlador):**
   - **Objeto:** Pedidos de acesso a histórico de conversas, registros de atendimento, prontuários ou mensagens trocadas entre clientes/pacientes e profissionais liberais (médicos, advogados, contadores, consultores) que utilizam o aplicativo.
   - **Competência e Limitação Técnica:** A Raix opera sob o princípio estrito de **servidor cego (*Zero-Knowledge*)**, de modo que as mensagens cifradas não são armazenadas permanentemente e nunca são acessíveis ao servidor. O **Profissional interlocutor é o Controlador exclusivo** dos dados daquela relação profissional.
   - **Ação do Atendente Raix:** Informar formalmente ao titular que a plataforma não custodia nem possui acesso ao conteúdo das comunicações, orientando-o a requerer tais informações diretamente ao profissional ou escritório com o qual interagiu.

---

## 2. Passo a Passo do Fluxo de Atendimento

```
[E-mail recebido em contato@raixtech.com]
                 │
                 ▼
     Passo 1: Identificação & Triagem
(Bifurcação: Transporte vs. Conteúdo Profissional)
                 │
       ┌─────────┴─────────┐
       ▼                   ▼
  [Fluxo A:           [Fluxo B:
   Transporte]         Conteúdo Profissional]
       │                   │
  Validar Fingerprint   Explicar Servidor Cego
       │                e Direcionar ao
  Executar Acesso/      Profissional (Controlador)
  Deleção Técnica          │
       │                   │
       └─────────┬─────────┘
                 ▼
     Passo 4: Resposta Formal (≤ 15 dias)
                 │
                 ▼
     Passo 5: Registro no Livro DSR
```

---

## 3. Passo 1: Identificação e Validação do Titular

Como o Raix **não coleta nomes, e-mails, senhas nem telefones** para registro de conta (autenticação 100% anônima via Firebase Auth), a correlação técnica depende do **Fingerprint Criptográfico (Ed25519)** do titular.

1. Se o titular enviou o e-mail mas **não forneceu o fingerprint**:
   - Responder solicitando que abra o app Raix, acesse a tela *"Sobre seus dados"* ou *"Minha Identidade"*, copie o **Fingerprint Técnico** e envie em resposta.
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

### 4.3. Registros Legais Isolados (MCI Art. 15):
- **AccessLogs (`accessLogs`):** Mantidos isoladamente sem identificador de usuário pelo prazo estrito de **180 dias** em estrito cumprimento do Art. 15 do Marco Civil da Internet (obrigação legal — Art. 7º, II da LGPD), findo o qual são expurgados automaticamente via política de TTL do banco.

---

## 5. Passo 4: Modelos Formais de Resposta ao Titular

### Modelo 1 — Resposta de Confirmação de Eliminação Efetuada (Art. 18, VI da LGPD)

> **Assunto:** [Raix] Confirmação de Atendimento — Solicitação de Eliminação de Dados (LGPD)  
> **Para:** `<e-mail-do-solicitante>`  
>
> Olá,  
>  
> Em atenção à sua solicitação recebida em `[DATA_RECEBIMENTO]`, com fundamento no art. 18, inciso VI da Lei Geral de Proteção de Dados (Lei nº 13.709/2018 — LGPD), confirmo que foi realizada a **eliminação definitiva** do registro de sua identidade técnica de roteamento (`identities/{fingerprint}`) de nossos servidores em nuvem.  
>  
> Esclarecemos que:  
> 1. O aplicativo Raix foi desenvolvido sob a arquitetura de servidor cego (*Zero-Knowledge*), não armazenando mensagens de forma permanente (as mensagens trocadas são autodestrutivas com tempo de vida máximo de 24 horas).  
> 2. O servidor nunca teve acesso ou posse do conteúdo de suas mensagens, de seus contatos, de sua frase mnemônica ou de suas chaves privadas, que residem exclusivamente na memória do seu dispositivo.  
> 3. Registros de conexão técnica são retidos pelo prazo estrito de 180 dias exclusivamente para cumprimento de obrigação legal (art. 15 do Marco Civil da Internet), em base isolada e desprovida de qualquer associação com sua identidade ou conteúdo.  
>  
> Caso deseje descartar os dados presentes em seu próprio dispositivo, recomendamos acionar o botão de Pânico (*Panic Wipe*) nas opções do aplicativo ou desinstalá-lo.  
>  
> Permaneço à disposição para esclarecimentos adicionais.  
>  
> Atenciosamente,  
> **Filippe Andrade Sampaio**  
> Controlador e Encarregado pelo Tratamento de Dados Pessoais  
> Raix — `contato@raixtech.com`

---

### Modelo 2 — Resposta para Solicitação de Conteúdo de Conversas Profissionais (Bifurcação C7)

> **Assunto:** [Raix] Esclarecimento sobre Conteúdo de Comunicações Profissionais (LGPD)  
> **Para:** `<e-mail-do-solicitante>`  
>  
> Olá,  
>  
> Em atenção à sua solicitação referente ao acesso a mensagens ou conteúdos trocados através do aplicativo Raix com `[NOME_DO_PROFISSIONAL_OU_EMPRESA]`, prestamos os seguintes esclarecimentos técnicos e jurídicos:  
>  
> 1. **Arquitetura Servidor Cego (Zero-Knowledge):** O Raix atua unicamente como provedor da tecnologia de transporte e canal efêmero de mensagens com criptografia ponta a ponta. Os nossos servidores **não armazenam e não possuem a chave criptográfica** necessária para ler ou recuperar o conteúdo das conversas.  
> 2. **Controlador do Conteúdo:** Para os fins da Lei Geral de Proteção de Dados (Lei nº 13.709/2018), o profissional ou organização com a qual você manteve interlocução atua como **Controlador dos dados e prontuários da relação profissional**, cabendo a ele, se aplicável, prestar informações sobre o histórico retido em seus próprios dispositivos ou sistemas locais.  
> 3. Orientamos que a requisição de histórico ou cópia de atendimentos seja endereçada diretamente ao profissional ou entidade com quem manteve contato.  
>  
> Permanecemos à disposição para quaisquer esclarecimentos relativos à infraestrutura técnica do aplicativo.  
>  
> Atenciosamente,  
> **Filippe Andrade Sampaio**  
> Encarregado pelo Tratamento de Dados Pessoais  
> Raix — `contato@raixtech.com`

---

## 6. Passo 5: Livro de Registro de Solicitações (Auditoria Interna)

Para fins de prestação de contas (*Accountability* — Art. 6º, X da LGPD) e eventuais fiscalizações da Autoridade Nacional de Proteção de Dados (ANPD), mantenha o registro das requisições atendidas na tabela abaixo:

| ID | Data Recebimento | Solicitante (E-mail) | Fingerprint Técnico | Tipo de Solicitação | Ação Executada | Data Resposta | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| DSR-001 | DD/MM/AAAA | usuario@exemplo.com | `0123456789abcdef...` | Eliminação (Art. 18, VI) | Doc identities deletado | DD/MM/AAAA | Concluído (≤15 dias) |
