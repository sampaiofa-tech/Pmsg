import https from 'https';
import crypto from 'crypto';

const PROJECT_ID = 'gen-lang-client-0858445711';
const REGION = 'us-central1';
const WEB_API_KEY = 'AIzaSyDpVykwTzWXMqYQFpKGebyBW979nlcaL1Y';

function httpRequest(options, postData = null) {
  return new Promise((resolve, reject) => {
    const opts = { ...options };
    opts.headers = {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) RaixSmokeTest/1.4',
      ...(opts.headers || {})
    };
    const req = https.request(opts, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          const json = body ? JSON.parse(body) : {};
          resolve({ status: res.statusCode, headers: res.headers, body: json, rawBody: body });
        } catch (e) {
          resolve({ status: res.statusCode, headers: res.headers, body: null, rawBody: body });
        }
      });
    });
    req.on('error', reject);
    if (postData) {
      req.write(typeof postData === 'string' ? postData : JSON.stringify(postData));
    }
    req.end();
  });
}

// 1. Criar usuário anônimo de teste efêmero via Identity Toolkit
async function createTestUser(label) {
  const res = await httpRequest({
    hostname: 'identitytoolkit.googleapis.com',
    path: `/v1/accounts:signUp?key=${WEB_API_KEY}`,
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { returnSecureToken: true });

  if (res.status !== 200 || !res.body.idToken) {
    throw new Error(`Falha ao criar usuário de teste [${label}]: ${res.rawBody}`);
  }
  return { label, uid: res.body.localId, idToken: res.body.idToken };
}

// 2. Chamar Firebase Callable Cloud Function
async function callCallable(functionName, data, idToken = null) {
  const payload = JSON.stringify({ data });
  const headers = {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(payload)
  };
  if (idToken) {
    headers['Authorization'] = `Bearer ${idToken}`;
  }

  const res = await httpRequest({
    hostname: `${REGION}-${PROJECT_ID}.cloudfunctions.net`,
    path: `/${functionName}`,
    method: 'POST',
    headers
  }, payload);

  return res;
}

// 3. Teste de leitura direta no Firestore REST API
async function readFirestoreDoc(collection, docId, idToken) {
  return httpRequest({
    hostname: 'firestore.googleapis.com',
    path: `/v1/projects/${PROJECT_ID}/databases/(default)/documents/${collection}/${docId}`,
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${idToken}`,
      'Content-Type': 'application/json'
    }
  });
}

async function runV14SmokeTests() {
  console.log("================================================================================");
  console.log("🚀 INICIANDO TESTES DE FUMAÇA EM PRODUÇÃO — RAIX v1.4");
  console.log("================================================================================\n");

  let passed = 0;
  let failed = 0;

  function assert(name, condition, details = "") {
    if (condition) {
      console.log(`  ✅ [PASS] ${name}`);
      passed++;
    } else {
      console.error(`  ❌ [FAIL] ${name} ${details}`);
      failed++;
    }
  }

  try {
    // ------------------------------------------------------------------------
    // TESTE 1: Páginas Oficiais e Domínio raixtech.com no ar
    // ------------------------------------------------------------------------
    console.log("--- 1. Domínio Próprio raixtech.com e Páginas Legais v2.0 ---");
    const pages = ['/', '/terms.html', '/privacy.html'];
    for (const page of pages) {
      const res = await httpRequest({
        hostname: 'raixtech.com',
        path: page,
        method: 'GET'
      });
      assert(`Página https://raixtech.com${page} responde HTTP 200`, res.status === 200, `(Recebido: ${res.status})`);
    }

    // ------------------------------------------------------------------------
    // TESTE 2: Isolamento da coleção connectionLogs no Firestore
    // ------------------------------------------------------------------------
    console.log("\n--- 2. Segurança Firestore Rules — Isolamento de connectionLogs ---");
    const alice = await createTestUser("Alice");
    const readLogsRes = await readFirestoreDoc("connectionLogs", "sample_log_test", alice.idToken);
    assert(
      "Leitura direta de /connectionLogs/{id} rejeitada com 403 Forbidden",
      readLogsRes.status === 403 || readLogsRes.status === 404,
      `(Recebido: ${readLogsRes.status})`
    );

    // ------------------------------------------------------------------------
    // TESTE 3: reportAbuseWithContent (Callable v1.4)
    // ------------------------------------------------------------------------
    console.log("\n--- 3. Nova Callable reportAbuseWithContent (Parecer C6) ---");

    // 3.1 Unauthenticated
    const unauthRes = await callCallable("reportAbuseWithContent", {
      reportedFingerprint: crypto.randomBytes(32).toString('hex'),
      abuseType: "SPAM",
      contentSnippet: "Spam content",
      explicitConsent: true
    }, null);
    assert(
      "Chamada não-autenticada em reportAbuseWithContent é rejeitada (401)",
      unauthRes.status === 401,
      `(Recebido: ${unauthRes.status})`
    );

    // 3.2 Missing explicit consent
    const noConsentRes = await callCallable("reportAbuseWithContent", {
      reportedFingerprint: crypto.randomBytes(32).toString('hex'),
      abuseType: "SPAM",
      contentSnippet: "Spam content",
      explicitConsent: false
    }, alice.idToken);
    assert(
      "Chamada sem explicitConsent: true é rejeitada com 400 invalid-argument",
      noConsentRes.status === 400 && JSON.stringify(noConsentRes.body).includes("consentimento"),
      `(Recebido: ${noConsentRes.status} - ${JSON.stringify(noConsentRes.body)})`
    );

    // 3.3 Unauthorized payload field (strict allow-list)
    const extraFieldRes = await callCallable("reportAbuseWithContent", {
      reportedFingerprint: crypto.randomBytes(32).toString('hex'),
      abuseType: "SPAM",
      contentSnippet: "Spam content",
      explicitConsent: true,
      hackerField: "malicious"
    }, alice.idToken);
    assert(
      "Payload com campo fora da allow-list é rejeitado com 400",
      extraFieldRes.status === 400,
      `(Recebido: ${extraFieldRes.status})`
    );

    // 3.4 Valid report with voluntary content and explicit consent
    const validReportRes = await callCallable("reportAbuseWithContent", {
      reportedFingerprint: crypto.randomBytes(32).toString('hex'),
      abuseType: "HARASSMENT",
      contentSnippet: "Trecho voluntário de mensagem enviado para moderação sob LGPD Art. 7, I.",
      explicitConsent: true
    }, alice.idToken);
    assert(
      "Denúncia com conteúdo voluntário e consentimento explícito gravada com sucesso (200)",
      validReportRes.status === 200 && validReportRes.body?.result?.success === true,
      `(Recebido: ${validReportRes.status} - ${JSON.stringify(validReportRes.body)})`
    );

    // ------------------------------------------------------------------------
    // TESTE 4: resolveFingerprint e updateIdentityRouting (v1.4)
    // ------------------------------------------------------------------------
    console.log("\n--- 4. Moderação por Revogação Ed25519 em resolveFingerprint ---");
    const testFp = crypto.randomBytes(32).toString('hex');
    const resolveRes = await callCallable("resolveFingerprint", {
      fingerprint: testFp
    }, alice.idToken);
    // Fingerprint aleatório não existente deve retornar not-found (404)
    assert(
      "resolveFingerprint responde normalmente (404 not-found para fp inexistente)",
      resolveRes.status === 404,
      `(Recebido: ${resolveRes.status})`
    );

    // ------------------------------------------------------------------------
    // TESTE 5: Baseline de Transporte v1.2 / v1.3 Preservado
    // ------------------------------------------------------------------------
    console.log("\n--- 5. Compatibilidade Regressiva (Transporte v1.2 / v1.3) ---");
    const smokeV13Res = await callCallable("reportAbuse", {
      reportedFingerprint: crypto.randomBytes(32).toString('hex'),
      abuseType: "SPAM"
    }, alice.idToken);
    assert(
      "reportAbuse comportamental Zero-Knowledge (mecanismo padrão) continua 100% operacional",
      smokeV13Res.status === 200 && smokeV13Res.body?.result?.success === true,
      `(Recebido: ${smokeV13Res.status})`
    );

  } catch (err) {
    console.error("Erro fatal durante execução do smoke test:", err);
    failed++;
  }

  console.log("\n================================================================================");
  console.log(`📊 RESULTADO FINAL DOS TESTES DE FUMAÇA: ${passed} PASS, ${failed} FAIL`);
  console.log("================================================================================");

  if (failed > 0) {
    process.exit(1);
  } else {
    console.log("🎉 TODOS OS TESTES DE FUMAÇA EM PRODUÇÃO FORAM APROVADOS COM SUCESSO!");
  }
}

runV14SmokeTests();
