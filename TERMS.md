# Termos de Uso — Pmsg (nome provisório)

**Última atualização:** 4 de setembro de 2026  
**Versão:** 1.0

Bem-vindo ao **Pmsg (nome provisório)**. Ao baixar, instalar, acessar ou utilizar este aplicativo e seus serviços correlatos, você concorda expressamente em vincular-se a estes Termos de Uso. Caso não concorde com qualquer termo aqui disposto, você não deve utilizar o aplicativo.

---

## 1. Classificação Etária Estrita (18+)

O Pmsg é destinado **exclusivamente a maiores de 18 (dezoito) anos**. 

- É **terminantemente proibido** o uso do aplicativo por crianças ou adolescentes menores de 18 anos.
- Ao utilizar o aplicativo, você declara expressamente ser civilmente capaz e possuir idade igual ou superior a 18 anos.
- Uma verificação formal de faixa etária (*Age-Gate*) poderá ser solicitada no primeiro acesso ao aplicativo.

---

## 2. Natureza do Serviço e Disponibilidade ("Como Está")

O Pmsg é um software de comunicação efêmera e segura, fornecido **"NO ESTADO EM QUE SE ENCONTRA" ("AS IS")** e **"CONFORME DISPONÍVEL" ("AS AVAILABLE")**, sem garantias de qualquer natureza, expressas ou implícitas.

- O desenvolvedor e operador não garante que o serviço será ininterrupto, livre de erros, pontual, totalmente imune a ataques cibernéticos externos ou compatível com todos os dispositivos e sistemas operacionais.
- Manutenções de infraestrutura, atualizações técnicas ou interrupções programadas podem ocorrer sem aviso prévio.

---

## 3. Arquitetura de Sigilo e Limitação Técnica do Operador (Servidor Cego)

O Pmsg foi construído sobre uma arquitetura criptográfica estrita de **servidor cego (*Zero-Knowledge*)**:

- **Impossibilidade Técnica de Acesso**: O operador dos servidores **NÃO possui os meios matemáticos ou criptográficos para visualizar, decifrar, interceptar ou recuperar o conteúdo textual das mensagens trocadas**, nem para reconstituir conversas apagadas.
- **Denúncias e Abusos**: Em virtude dessa limitação arquitetural, relatórios ou notificações de abuso **não contemplam e não podem contemplar revisão de conteúdo de mensagens por parte do operador**, pois este material é tecnicamente inacessível a qualquer entidade externa aos dois dispositivos envolvidos.

---

## 4. Condutas Proibidas e Mecanismo de Denúncia

Você se compromete a utilizar o Pmsg em estrita conformidade com a legislação brasileira e internacional aplicável. É expressamente proibido:

1. Praticar, incentivar, intermediar ou disseminar atividades criminosas, ilícitas, fraudulentas ou que violem direitos de terceiros.
2. Utilizar o aplicativo para assédio, perseguição (*stalking*), ameaças, extorsão, disseminação de conteúdo de exploração sexual, pornografia infantil, terrorismo ou incitação à violência.
3. Praticar ataques de negação de serviço (DoS/DDoS), exploração de vulnerabilidades ou envio massivo não solicitado (spam).
4. Tentar quebrar a integridade criptográfica do roteamento ou forjar assinaturas de outros usuários.

> **Mecanismo Comportamental de Denúncia (Roadmap v1.3)**: A partir da versão v1.3, o aplicativo disponibilizará um canal de denúncia estritamente comportamental e estatístico (reputação local ou bloqueio de fingerprint ofensivo), preservando a ausência de moderação invasiva de conteúdo. Usuários infratores identificados por comportamento abusivo sistemático de rede poderão ter seu acesso técnico bloqueado via *rate limiting* e revogação de sessão.

---

## 5. Responsabilidade Exclusiva pela Frase Mnemônica e Chaves Privadas

O Pmsg não adota contas tradicionais com login, senha ou recuperação por e-mail:

- **Sua Frase Mnemônica (12 Palavras) é sua Identidade**: Todo o seu par de chaves criptográficas é derivado deterministicamente de sua semente mnemônica.
- **Sem Custódia e Sem Recuperação**: O operador **NÃO armazena, NÃO conhece e NÃO custodia seu mnemônico ou suas chaves privadas**.
- **Perda Irreversível**: Se você desinstalar o aplicativo, trocar de dispositivo ou esquecer sua frase de 12 palavras, **sua identidade criptográfica, seus contatos e seu histórico serão irremediavelmente perdidos**. O operador não possui qualquer capacidade técnica de recuperar, redefinir ou restaurar identidades perdidas.

---

## 6. Efemeridade Inerente e Limitação de Responsabilidade

A **autodestruição e o descarte temporal de dados** constituem o propósito fundamental do Pmsg:

- As mensagens possuem tempo de vida limitado (TTL de até 24 horas) e são incineradas imediatamente após o consumo (*Vanish-After-Read*).
- O desenvolvedor e operador não se responsabiliza por:
  - Perda de mensagens decorrente do encerramento regular de TTL ou acionamento voluntário/acidental do modo de Pânico (*Panic Wipe*);
  - Falhas de hardware, corrupção de memória local do aparelho ou apreensão física do dispositivo do usuário;
  - Decisões, ações ou omissões tomadas por usuários com base nas comunicações mantidas através do aplicativo;
  - Danos indiretos, lucros cessantes, perdas financeiras ou prejuízos de qualquer ordem decorrentes da utilização ou impossibilidade de utilização do serviço.

---

## 7. Foro e Legislação Aplicável

Estes Termos de Uso são regidos e interpretados de acordo com a legislação da República Federativa do Brasil, em especial o Marco Civil da Internet (Lei nº 12.965/2014) e a Lei Geral de Proteção de Dados (Lei nº 13.709/2018).

Para a resolução de eventuais litígios oriundos deste instrumento, fica eleito o foro da comarca de domicílio do desenvolvedor, com renúncia expressa a qualquer outro, por mais privilegiado que seja.
