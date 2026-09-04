# Diretrizes de Operação do Agente de IA (AGENTS.md)

Este documento estabelece as regras mandatórias e permanentes de segurança, governança e higiene técnica para agentes autônomos e assistentes de IA que operam no repositório **Pmsg**.

---

## Segurança de Credenciais (Regras Permanentes para o Agente)

1. **Prioridade Absoluta por Operações Anônimas**:
   - **NUNCA** extrair credenciais (`git credential fill`, tokens, chaves de API) quando a operação puder funcionar de forma anônima ou sem autenticação.
   - Requisições `GET` em repositórios, documentações e endpoints públicos devem ser **SEMPRE** anônimas.

2. **Proibição Estrita de Vazamento e Eco de Segredos**:
   - **NUNCA** imprimir, logar, ecoar no terminal, salvar em arquivo, incluir em artifacts ou adicionar em commits: tokens de acesso, chaves privadas, frases mnemônicas BIP-39, números de segurança comutativos ou segredos de qualquer natureza.
   - Todo comando de terminal que interaja com credenciais deve suprimir saídas diretas que contenham senhas ou tokens.

3. **Custódia do Mnemônico BIP-39 (12 Palavras)**:
   - A exibição ou revelação da frase mnemônica de recuperação ocorre **EXCLUSIVAMENTE na interface do usuário (UI)** do aplicativo, mediante autenticação biométrica ou PIN pelo próprio usuário.
   - O agente **NUNCA** manipula, lê, valida ou transcreve palavras do mnemônico, devendo apenas orientar o usuário até a tela correspondente.

4. **Gestão de Segredos de Nuvem (Secret Manager)**:
   - Segredos de infraestrutura (como `GEMINI_API_KEY`) são definidos e rotacionados **exclusivamente pelo usuário** através do CLI interativo (`firebase-tools functions:secrets:set`), sem eco em tela. O agente nunca recebe ou manipula esses valores diretamente.

5. **Padrão Mandatório para Chamadas Autenticadas em Runtime**:
   - Quando a autenticação for **estritamente necessária** (ex: automação de CI/CD ou APIs privadas autorizadas), o agente deve:
     a. Justificar formalmente a necessidade ao usuário antes da execução;
     b. Capturar a credencial via `git credential fill` diretamente em variável volátil de memória (zero eco no console);
     c. Utilizar a variável exclusivamente nos headers da requisição em memória;
     d. Descartar a variável imediatamente após o uso.
