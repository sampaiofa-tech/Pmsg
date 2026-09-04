# Dívida Técnica e Triagem de Avisos de Build (TECH-DEBT)

Este documento registra a triagem formal, justificativas arquiteturais, impactos e gatilhos de resolução para os 8 avisos de build identificados durante o ciclo de auditoria e pré-deploy do projeto Pmsg.

## Baseline Congelado de Engenharia
- **Kotlin**: 2.2.20
- **Compose Multiplatform (CMP)**: 1.10.3
- **Gradle**: 9.3.1
- **Android Gradle Plugin (AGP)**: 9.1.1
- **KSP**: 2.2.20-1.0.29
- **Firebase Tools CLI**: 15.29.x
- **Runtime Cloud Functions**: Node.js 20 LTS (`nodejs20`)

---

## Tabela Consolidada de Triagem dos 8 Avisos

| # | Item | Origem | Impacto Atual | Gatilho de Resolução | Versão-Alvo |
|---|------|--------|---------------|----------------------|-------------|
| **1** | `android.builtInKotlin=false` depreciado | `gradle.properties` (AGP) | **Nenhum**. Flag necessária para coexistência estável entre AGP 9 e KMP; removê-la agora quebraria o build. | Remover flag após migração estrutural para o AGP 10. | AGP 10.0.0 |
| **2** | `android.newDsl=false` depreciado | `gradle.properties` (AGP) | **Nenhum**. Mantém retrocompatibilidade com a sintaxe DSL declarativa atual do projeto. | Adotar a nova DSL declarativa no ciclo de migração do AGP 10. | AGP 10.0.0 |
| **3** | `Unused Kotlin Source Sets (iosMain)` | Plugin KMP (`composeApp/build.gradle.kts`) | **Nenhum**. Comportamento esperado em host Windows. O target nativo iOS compila exclusivamente no CI macOS via GitHub Actions (`.github/workflows/ios-build.yml`). | Nenhuma ação local. Mantido como esperado no fluxo multiplatforma. | Kotlin 2.2.x+ / CMP |
| **4** | `SDK processing XML version 4` | Android SDK Command-line Tools / AGP | **Nenhum**. Mensagem puramente informativa: o cmdline-tools possui schema XML v4 enquanto o parser AGP suporta até v3. Não realizar downgrade das tools. | Atualização transparente nas próximas releases do AGP. | AGP 9.2.x / 10.0.0 |
| **5** | `The archives configuration has been deprecated for artifact declaration` | Plugin KMP (JetBrains) x Gradle 9.3.1 | **Nenhum**. Aviso de depreciação do Gradle para declaração de artefatos legados na publicação. Resolução pertence ao plugin oficial KMP da JetBrains. | Atualização do plugin Kotlin Multiplatform compatível com Gradle 10. | KMP Gradle 10 |
| **6** | `Deprecated Gradle features used (Gradle 10 incompatibility)` | Gradle 9.3.1 (`build/reports/problems/problems-report.html`) | **Nenhum**. Consolidação formal do item anterior. O Gradle 9.3.1 é o baseline estável e congelado até o deploy. | Atualização de plugins de terceiros e migração de scripts para o Gradle 10 em ciclo dedicado. | Gradle 10.0.0 |
| **7** | `firebase-functions indicates an outdated version (v6.6.0)` | Firebase Functions CLI (`functions/package.json`) | **Nenhum**. A suíte de testes unitários e E2E está 100% verde. Auditoria confirmou **0 vulnerabilidades HIGH ou CRITICAL** (13 moderadas transitivas em SDKs GCP). A v7 introduz breaking changes. | Planejar migração para `firebase-functions` v7+ em ciclo pós-deploy com revalidação de compatibilidade de runtime. | `firebase-functions` 7.x |
| **8** | `engines node "20" vs Node 24 local host` | `functions/package.json` vs Node local | **Nenhum**. O Google Cloud Functions executa no runtime gerenciado oficial `nodejs20` LTS. Localmente, o emulador executa no Node 24 do host sem divergências funcionais. | Opcional: padronização do ambiente local com Node 20 LTS via `nvm-windows` em ciclo de infraestrutura pós-deploy. | Node.js 20 LTS |
| **9** | `X25519 Pure-Kotlin Timing Hardening` | `Curve25519Engine.kt` (commonMain) | **Baixo/Teórico**. Implementação pura RFC 7748 §5 utiliza Montgomery ladder com operações aritméticas de 64 bits. Em runtimes JIT JVM/JS, branchless e constant-time estrito podem sofrer variações sutis de microarquitetura comparado a C/libsodium nativo. | Avaliar integração opcional de libsodium via FFI nativo caso o modelo de ameaça exija proteção contra ataques físicos de canal lateral local. | v1.3 / v2.0 |
| **10** | `Scanner de Câmera no WasmJs (Web)` | `QrScannerView.wasmJs.kt` (wasmJsMain) | **Nenhum (Fallback funcional)**. No target Web (WasmJs), o scanner por câmera foi declarado como stub com fallback para colar o URI de contato ou convite. A API padrão `BarcodeDetector` ainda possui suporte variável entre navegadores móveis/desktop. | Implementar captura via `navigator.mediaDevices.getUserMedia` combinada com `BarcodeDetector` ou lib JS leve de WASM. | v1.3 |

---

## Política de Segurança de Dependências (NPM Audit)
Conforme auditoria executada em `functions/`:
- **Vulnerabilidades Críticas**: 0
- **Vulnerabilidades Altas (High)**: 0
- **Vulnerabilidades Moderadas**: 13 (todas em dependências transitivas profundas de bibliotecas Google Cloud como `teeny-request`, `gaxios` e `retry-request`).
- **Diretriz**: Nenhuma dependência maior ou com breaking change deve ser forçada antes da conclusão e homologação do deploy em produção.
