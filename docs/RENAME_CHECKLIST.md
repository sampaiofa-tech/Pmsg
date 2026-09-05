# Checklist de Renomeação do Produto (Transição de Marca: Pmsg ➔ RAIX)

Este checklist consolida as ações técnicas, regulatórias e operacionais da transição oficial da marca provisória **Pmsg** para a marca comercial definitiva **RAIX**, formalizada e iniciada em **05 de setembro de 2026**.

---

## 📋 Ledger de Execução da Transição de Marca (v1.4)

| Data | Componente / Ação | Responsável | Status | Referência / Evidência |
| :--- | :--- | :--- | :--- | :--- |
| 05/09/2026 | **Aprovação do Nome Definitivo: RAIX** | Gestão | ✅ **Concluído** | Decisão homologada no ciclo v1.4 |
| 05/09/2026 | **Depósito de Marca INPI** | Usuário | ✅ **Concluído** | **Protocolo nº 945109300** (Classes 9 e 42) |
| 05/09/2026 | **Aquisição de Domínio e DNS** | Usuário/Agente | ✅ **Concluído** | `raixtech.com` ativo no Cloudflare |
| 05/09/2026 | **Configuração CNAME GitHub Pages** | Agente | ✅ **Concluído** | CNAME `raixtech.com` via API e `pages/CNAME` |
| 05/09/2026 | **Canal de E-mail Institucional** | Usuário | ✅ **Concluído** | `contato@raixtech.com` redirecionado via Cloudflare |
| 05/09/2026 | **Rename do Repositório GitHub** | Usuário | ✅ **Concluído** | `sampaiofa-tech/Pmsg` ➔ `sampaiofa-tech/Raix` |
| 05/09/2026 | **Atualização do Remote Git Local** | Agente | ✅ **Concluído** | `https://github.com/sampaiofa-tech/Raix.git` |
| 05/09/2026 | **Decisão de `applicationId` / `bundleId`** | Gestão/Agente | ✅ **Registrado** | Mantido `com.aistudio.vanishchat.zr7k` nesta fase |
| 05/09/2026 | **Importação de Ativos de Marca (Logos)** | Usuário/Agente | ✅ **Concluído** | `branding/logo-app.png` e `branding/logo-empresa.png` |
| 05/09/2026 | **Atualização dos Documentos Legais** | Agente | ✅ **Concluído** | `LICENSE-COMMERCIAL.md`, `CLA.md`, `PRIVACY.md`, `TERMS.md` |

---

## 1. Repositório e Infraestrutura de Código (GitHub)
- [x] **Renomear Repositório no GitHub** (Concluído em 05/09/2026):
  - Repositório renomeado com sucesso para `https://github.com/sampaiofa-tech/Raix`.
- [x] **Atualizar Remoto Local nos Ambientes de Desenvolvimento** (Concluído em 05/09/2026):
  ```bash
  git remote set-url origin https://github.com/sampaiofa-tech/Raix.git
  ```
- [x] **Configurar Domínio Próprio para GitHub Pages** (Concluído em 05/09/2026):
  - Domínio `raixtech.com` vinculado via API e arquivo `pages/CNAME`.

---

## 2. Identidade Visual e Interface do Usuário (UI/UX)
- [x] **Ativos Oficiais Fornecidos e Integrados**:
  - `branding/logo-app.png`: Escudo com centro esmeralda (extração pura para o ícone, sem o texto inferior "RAIX").
  - `branding/logo-empresa.png`: Marca corporativa com acabamento translúcido (exclusiva das telas institucionais/"Sobre").
- [ ] **Display Name do Aplicativo**:
  - **Android**: `composeApp/src/androidMain/res/values/strings.xml` (`app_name = "Raix"`).
  - **iOS**: `iosApp/iosApp/Info.plist` (`CFBundleDisplayName = "Raix"`).
  - **Desktop**: Título da janela em `composeApp/src/desktopMain/kotlin/com/example/Main.kt` (`Window(title = "Raix")`).
  - **Web**: Tag `<title>Raix</title>` em `composeApp/src/wasmJsMain/resources/index.html`.
- [ ] **Paleta de Cores Compose**:
  - Verde Esmeralda (`#00E676`), Dourado (`#D4AF37`), Azul-Marinho Profundo (`#0B1325`).

---

## 3. Decisões Estruturais CRÍTICAS (Pré-Publicação em Loja)

### a. `applicationId` / `bundleIdentifier`
- [x] **Decisão Consciente de Manutenção Temporária (v1.4)**:
  - **Valor atual confirmado via `aapt dump badging`:** `com.aistudio.vanishchat.zr7k`
  - **Deliberação:** **NÃO alterar** na versão v1.4 para preservar compatibilidade com instalações existentes em testes e assinaturas keystore de release locais.
  - ⚠️ **Decisão Pendente para a Publicação Oficial em Loja**: Antes do primeiro deploy público na Google Play Store e Apple App Store, a gestão deverá deliberar se o package final será mantido como `com.aistudio.vanishchat.zr7k` ou se será migrado para um namespace dedicado (ex: `tech.raix.app`). Se for alterado, um novo `google-services.json` deverá ser provisionado.

### b. Deep Link Custom Scheme (`URI Protocol`)
- [ ] **Esquema de Protocolo**:
  - Mantido suporte a `pmsg://` com transição e suporte a `raix://` para garantir que convites gerados permaneçam resolvíveis.

---

## 4. Registro de Propriedade Intelectual e Marca (INPI)
- [x] **Busca de Anterioridade no INPI**: Realizada nas classes 09 e 42.
- [x] **Depósito de Pedido de Registro de Marca**:
  - **Número de Protocolo INPI:** `945109300`
  - **Natureza:** Marca de Produto / Serviço
  - **Apresentação:** NOMINATIVA (`RAIX`)
  - **Classes:** 9 (softwares e aplicativos) e 42 (serviços de tecnologia e SaaS)
  - **Titular:** Filippe Andrade Sampaio (Pessoa Física)
- [ ] **Acompanhamento na RPI**:
  - Acompanhar despachos e deferimento no portal do INPI.

---

## 5. Documentos Legais e de Licenciamento
- [x] **Atualizar `LICENSE-COMMERCIAL.md`**: Nome Raix e canal `contato@raixtech.com` configurados.
- [x] **Atualizar `CLA.md`**: Referência Raix configurada.
- [x] **Atualizar `PRIVACY.md`**: Versão 2.0 com Raix, canal oficial `contato@raixtech.com`, DPO pessoa natural mantido e logs MCI Art. 15.
- [x] **Atualizar `TERMS.md`**: Termos de Uso atualizados para Raix com sanção de moderação restrita à revogação Ed25519 (nunca bloqueio de IP).
- [x] **Atualizar `README.md`**: Atualizado para Raix com matriz de segurança e referências de conformidade.
