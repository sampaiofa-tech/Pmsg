# Termos de Uso — Raix

**Última atualização:** 05 de setembro de 2026  
**Versão:** 3.0 (Conforme arquitetura v1.5 do aplicativo)  
**Marca:** Raix (Depósito de marca nominativa agendado para 08/09/2026 junto ao INPI — guia/protocolo preparatório nº 945109300)  
**Entidade Operadora:** Cat Tech (CNPJ: 67.497.085/0001-36)  
**Canal Oficial:** `https://raixtech.com`

Bem-vindo ao **Raix**. Ao baixar, instalar, acessar ou utilizar este aplicativo e seus serviços correlatos, você concorda expressamente em vincular-se a estes Termos de Uso. Caso não concorde com qualquer disposição aqui estabelecida, você não deve utilizar o aplicativo.

---

## 1. Classificação Etária Estrita (18+)

O Raix é destinado **exclusivamente a maiores de 18 (dezoito) anos**. 

- É **terminantemente vedado** o uso do aplicativo por crianças ou adolescentes menores de 18 anos.
- Ao utilizar o aplicativo, você declara expressamente ser civilmente capaz sob as leis de seu domicílio e possuir idade igual ou superior a 18 anos.
- Uma verificação formal de faixa etária (*Age-Gate*) é obrigatória no primeiro acesso ao aplicativo.

---

## 2. Natureza do Serviço e Efemeridade Radical ("Como Está")

O Raix é um software de comunicação efêmera e privativa, fornecido **"NO ESTADO EM QUE SE ENCONTRA" ("AS IS")** e **"CONFORME DISPONÍVEL" ("AS AVAILABLE")**, sem garantias implícitas ou explícitas de qualquer natureza.

- O operador não garante que o serviço será ininterrupto, livre de instabilidades ou compatível com todas as configurações de hardware.
- As mensagens transitam de forma efêmera e autodestrutiva, possuindo tempo de vida máximo parametrizável de até **24 horas** (*Time-To-Live*), sendo incineradas imediatamente após a leitura (*Vanish-After-Read*).
- O operador não se responsabiliza pela perda definitiva de mensagens após o encerramento regular de seu prazo de expiração ou pelo acionamento voluntário do botão de Pânico (*Panic Wipe*).

### 2.1. Timeout Estrito de Não-Entrega (≤ 24h) como Funcionalidade de Privacidade
- **Destinatário Offline por Mais de 24 Horas:** Caso o destinatário permaneça desconectado ou inacessível por período superior a 24 (vinte e quatro) horas a contar do envio, a mensagem retida em fila no servidor é **definitiva e irreversivelmente incinerada** pela rotina automatizada (`scheduledMessageShredder`).
- **Feature Deliberada de Privacidade:** Essa perda de mensagens não entregues dentro do intervalo de 24 horas não constitui falha ou interrupção de serviço, mas sim uma **funcionalidade deliberada de privacidade radical e minimização de dados** (*Zero-Trace Privacy Feature*), garantindo que dados criptografados jamais permaneçam estacionados indefinidamente em servidores de trânsito.

---

## 3. Arquitetura de Servidor Cego (Zero-Knowledge) e Custódia Local

O Raix adota uma arquitetura criptográfica estrita de **servidor cego (*Zero-Knowledge*)**:

- **Impossibilidade Técnica de Acesso:** O operador dos servidores **não possui os meios matemáticos ou as chaves criptográficas para visualizar, decifrar, interceptar ou recuperar o conteúdo textual das mensagens trocadas**, nem para reconstituir conversas expurgadas.
- **Custódia Local das Chaves:** Todo o par de chaves assimétricas e a semente mnemônica de 12 palavras permanecem **custodiadas exclusivamente no dispositivo do Usuário, sob proteção do sistema operacional** (DPAPI no Windows, KeyStore no Android), sem sincronização com a nuvem ou servidores remotos.

---

## 4. Condutas Proibidas, Moderação e Sanções Técnicas

Você se compromete a utilizar o Raix em estrita conformidade com a legislação brasileira e internacional aplicável. É expressamente proibido:

1. Praticar, incentivar, intermediar ou disseminar crimes, fraudes, assédio, ameaças, extorsão, material de abuso ou exploração sexual, pedofilia ou incitação à violência.
2. Praticar ataques cibernéticos de negação de serviço (DoS/DDoS), automações abusivas ou envio massivo não solicitado (spam).
3. Tentar forjar assinaturas criptográficas ou quebrar a integridade do protocolo de roteamento.

### 4.1. Sanções de Moderação: Revogação de Chave Ed25519 (Sem Bloqueio de IP)
- Em respeito aos usuários que compartilham conexões móveis, Wi-Fi público ou faixas de CGNAT, a moderação do Raix **NUNCA aplica bloqueio por endereço IP**.
- A penalidade técnica máxima aplicada a identidades infratoras consiste na **REVOGAÇÃO da chave pública Ed25519 de roteamento** (`identities/{fingerprint}` marcada como `revoked: true`), impedindo que o infrator continue utilizando o servidor como caixa postal.

---

## 5. Mecanismos de Denúncia (Comportamental e Conteúdo Voluntário)

- **Mecanismo Principal (Zero-Knowledge):** A aplicação disponibiliza canal de denúncia comportamental assíncrono (`reportAbuse`), baseado em reputação local e telemetria de integridade de rede, sem acesso ou análise de conteúdo.
- **Fluxo Secundário Opcional com Consentimento Explícito:** Usuários que recebam assédio, ameaça ou conteúdo ilícito podem, deliberadamente e mediante consentimento formal (Art. 7º, I da LGPD), optar por decifrar localmente e submeter a mensagem ofensiva selecionada para auditoria da moderação (`reportAbuseWithContent`).
- **Prazo de Retenção de Denúncias:** O conteúdo submetido voluntariamente permanece armazenado pelo prazo estrito de **no máximo 90 dias após o encerramento da apuração**, sendo em seguida submetido a **exclusão definitiva e cripto-incineração irreversível (hard-delete)**.

---

## 6. Responsabilidade Exclusiva pela Frase Mnemônica (12 Palavras)

O Raix não utiliza contas convencionais atreladas a e-mail, telefone, CPF ou senhas recuperáveis:

- **Sua Frase de 12 Palavras é sua Identidade Única:** Todo o seu conjunto de chaves criptográficas é derivado deterministicamente de sua semente mnemônica.
- **Sem Custódia:** O operador **NÃO conhece, NÃO armazena e NÃO custodia sua frase mnemônica**.
- **Perda Irreversível:** Caso você desinstale o aplicativo, troque de dispositivo ou perca sua anotação das 12 palavras, **sua identidade criptográfica, seus contatos e seu histórico serão irremediavelmente perdidos**. O operador não possui capacidade técnica de redefinir ou restaurar dados perdidos.

---

## 7. Limitação de Responsabilidade

O operador e desenvolvedores do Raix não serão responsáveis por:
- Danos indiretos, lucros cessantes, perdas financeiras ou prejuízos decorrentes do uso ou da impossibilidade de uso do mensageiro;
- Decisões, condutas ou transações realizadas entre os interlocutores através do aplicativo;
- Apreensão física, perda, furto, roubo ou invasão do dispositivo do próprio usuário.

---

## 8. Verificação da Integridade da Conexão Mútua (Safety Number / QR / Convite)

Em consonância com o modelo de segurança criptográfica ponta-a-ponta:

- **Dever de Verificação do Usuário:** O Usuário obriga-se a **verificar a integridade da conexão mútua** ao estabelecer novos canais de comunicação com seus contatos.
- **Número de Segurança de 60 Dígitos:** O aplicativo expõe na interface de detalhes de cada contato o **fingerprint mútuo (Número de Segurança composto por 60 dígitos numéricos)**, derivado criptograficamente das chaves públicas de ambos os interlocutores.
- **Marcação de Verificado:** O usuário deve comparar visualmente os blocos numéricos por um canal alternativo e seguro (ou presencialmente via leitura de QR Code) e acionar a marcação **"Marcar como Verificado"** no aplicativo para certificar a autenticidade e a integridade da rota de comunicação contra potenciais ataques de personificação (*Man-in-the-Middle*).

---

## 9. Foro e Legislação Aplicável

Estes Termos de Uso são regidos e interpretados de acordo com a legislação da República Federativa do Brasil, em especial o Marco Civil da Internet (Lei nº 12.965/2014) e a Lei Geral de Proteção de Dados (Lei nº 13.709/2018).

Fica eleito o foro da comarca da sede da **Cat Tech** para dirimir quaisquer controvérsias oriundas deste instrumento, com renúncia a qualquer outro, por mais privilegiado que seja.

Dúvidas ou solicitações devem ser encaminhadas para: `contato@raixtech.com`.
