# Termos de Uso — Raix

**Última atualização:** 05 de setembro de 2026  
**Versão:** 2.0 (Conforme v1.4 da arquitetura do aplicativo)  
**Marca:** Raix (Marca nominativa depositada no INPI sob protocolo nº 945109300)  
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

- O desenvolvedor e operador não garante que o serviço será ininterrupto, livre de instabilidades ou compatível com todas as configurações de hardware.
- As mensagens transitam de forma efêmera e autodestrutiva, possuindo tempo de vida máximo parametrizável de até **24 horas** (*Time-To-Live*), sendo incineradas imediatamente após a leitura (*Vanish-After-Read*).
- O desenvolvedor e operador não se responsabiliza pela perda definitiva de mensagens após o encerramento regular de seu prazo de expiração ou pelo acionamento voluntário do botão de Pânico (*Panic Wipe*).

---

## 3. Arquitetura de Servidor Cego (Zero-Knowledge)

O Raix adota uma arquitetura criptográfica estrita de **servidor cego (*Zero-Knowledge*)**:

- **Impossibilidade Técnica de Acesso:** O operador dos servidores **não possui os meios matemáticos ou as chaves criptográficas para visualizar, decifrar, interceptar ou recuperar o conteúdo textual das mensagens trocadas**, nem para reconstituir conversas expurgadas.
- **Custódia Local das Chaves:** Todo o par de chaves assimétricas e a semente mnemônica de 12 palavras residem unicamente nos cofres seguros do dispositivo do usuário (KeyStore, Keychain ou DPAPI), sem sincronização com a nuvem.

---

## 4. Condutas Proibidas, Moderação e Sanções Técnicas

Você se compromete a utilizar o Raix em estrita conformidade com a legislação brasileira e internacional aplicável. É expressamente proibido:

1. Praticar, incentivar, intermediar ou disseminar crimes, fraudes, assédio, ameaças, extorsão, material de exploração sexual, pornografia infantil ou incitação à violência.
2. Praticar ataques cibernéticos de negação de serviço (DoS/DDoS), automações abusivas ou envio massivo não solicitado (spam).
3. Tentar forjar assinaturas criptográficas ou quebrar a integridade do protocolo de roteamento.

### 4.1. Mecanismos de Denúncia (Comportamental e Conteúdo Voluntário)
- **Mecanismo Principal (Zero-Knowledge):** A aplicação disponibiliza canal de denúncia comportamental assíncrono (`reportAbuse`), baseado em reputação local e estatística de rede, sem análise de conteúdo.
- **Fluxo Secundário Opcional com Consentimento Explícito:** Usuários que recebam assédio ou conteúdos ilícitos podem, voluntariamente e mediante consentimento informado, optar por decifrar e submeter a mensagem ofensiva selecionada para auditoria da moderação (`reportAbuseWithContent`).

### 4.2. Sanções de Moderação: Revogação de Chave Ed25519 (Sem Bloqueio de IP)
- Em respeito aos usuários que compartilham conexões móveis, Wi-Fi público ou faixas de CGNAT, a moderação do Raix **NUNCA aplica bloqueio por endereço IP**.
- A penalidade técnica máxima aplicada a identidades infratoras consiste na **REVOGAÇÃO da chave pública Ed25519 de roteamento** (`identities/{fingerprint}` marcada como `revoked: true`), impedindo que o infrator continue utilizando o servidor como caixa postal.

---

## 5. Responsabilidade Exclusiva pela Frase Mnemônica (12 Palavras)

O Raix não utiliza contas convencionais atreladas a e-mail, telefone, CPF ou senhas recuperáveis:

- **Sua Frase de 12 Palavras é sua Identidade Única:** Todo o seu conjunto de chaves criptográficas é derivado deterministicamente de sua semente mnemônica.
- **Sem Custódia:** O operador **NÃO conhece, NÃO armazena e NÃO custodia sua frase mnemônica**.
- **Perda Irreversível:** Caso você desinstale o aplicativo, troque de dispositivo ou perca sua anotação das 12 palavras, **sua identidade criptográfica, seus contatos e seu histórico serão irremediavelmente perdidos**. O operador não possui capacidade técnica de redefinir ou restaurar dados perdidos.

---

## 6. Limitação de Responsabilidade

O operador e desenvolvedor do Raix não será responsável por:
- Danos indiretos, lucros cessantes, perdas financeiras ou prejuízos decorrentes do uso ou da impossibilidade de uso do mensageiro;
- Decisões, condutas ou transações realizadas entre os interlocutores através do aplicativo;
- Apreensão física, perda, roubo ou invasão do dispositivo do próprio usuário.

---

## 7. Foro e Legislação Aplicável

Estes Termos de Uso são regidos e interpretados de acordo com a legislação da República Federativa do Brasil, em especial o Marco Civil da Internet (Lei nº 12.965/2014) e a Lei Geral de Proteção de Dados (Lei nº 13.709/2018).

Fica eleito o foro da comarca de domicílio do desenvolvedor para dirimir quaisquer controvérsias oriundas deste instrumento, com renúncia a qualquer outro, por mais privilegiado que seja.

Dúvidas ou solicitações devem ser encaminhadas para: `contato@raixtech.com`.
