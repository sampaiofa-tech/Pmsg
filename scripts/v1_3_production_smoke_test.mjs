import fs from 'fs';
import https from 'https';
import crypto from 'crypto';

const PROJECT_ID = 'gen-lang-client-0858445711';
const REGION = 'us-central1';
const WEB_API_KEY = 'AIzaSyDpVykwTzWXMqYQFpKGebyBW979nlcaL1Y';

function httpRequest(options, postData = null) {
  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
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

// 1. Criar usuário anônimo de teste efêmero
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

// 3. Obter token de acesso do Firebase Tools (Admin)
function getAdminAccessToken() {
  const configPath = `${process.env.USERPROFILE}/.config/configstore/firebase-tools.json`;
  const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  return config.tokens.access_token;
}

// 4. Firestore REST Doc Read (Admin)
async function getFirestoreDoc(collection, docId) {
  const token = getAdminAccessToken();
  const res = await httpRequest({
    hostname: 'firestore.googleapis.com',
    path: `/v1/projects/${PROJECT_ID}/databases/(default)/documents/${collection}/${docId}`,
    method: 'GET',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return res;
}

// 5. Firestore REST Query (Admin)
async function queryAbuseReports(reportedFingerprint) {
  const token = getAdminAccessToken();
  const queryPayload = {
    structuredQuery: {
      from: [{ collectionId: 'abuseReports' }],
      where: {
        fieldFilter: {
          field: { fieldPath: 'reportedFingerprint' },
          op: 'EQUAL',
          value: { stringValue: reportedFingerprint }
        }
      },
      limit: 10
    }
  };
  const res = await httpRequest({
    hostname: 'firestore.googleapis.com',
    path: `/v1/projects/${PROJECT_ID}/databases/(default)/documents:runQuery`,
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  }, queryPayload);
  return res;
}

// 6. Firestore REST Doc Delete (Admin Cleanup)
async function deleteFirestoreDoc(collection, docId) {
  const token = getAdminAccessToken();
  const res = await httpRequest({
    hostname: 'firestore.googleapis.com',
    path: `/v1/projects/${PROJECT_ID}/databases/(default)/documents/${collection}/${docId}`,
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return res;
}

async function runSmokeTests() {
  console.log("=== PMSG v1.3: TESTES DE FUMAÇA EM PRODUÇÃO ===");
  console.log(`Projeto: ${PROJECT_ID} | Região: ${REGION}`);
  console.log(`Timestamp: ${new Date().toISOString()}\n`);

  const testTargetFingerprint = crypto.randomBytes(32).toString('hex');
  console.log(`[SETUP] Fingerprint alvo do teste: ${testTargetFingerprint}`);

  // Teste 2a: report válido registrado em abuseReports
  console.log("\n--- TESTE 2a: Report Válido Registrado em abuseReports ---");
  const userA = await createTestUser("Reporter_A");
  console.log(`[USER_A] Criado anon: UID=${userA.uid}`);

  const reportRes = await callCallable('reportAbuse', {
    reportedFingerprint: testTargetFingerprint,
    abuseType: "SPAM",
    inviteId: "invite_smoke_test_01"
  }, userA.idToken);

  console.log(`[STATUS] HTTP ${reportRes.status}`);
  console.log(`[RESPONSE]`, JSON.stringify(reportRes.body));
  if (reportRes.status !== 200 || !reportRes.body.result?.success) {
    throw new Error(`TESTE 2a FALHOU: report não foi aceito. Status: ${reportRes.status}`);
  }
  const reportId = reportRes.body.result.reportId;
  console.log(`[OK] Denúncia aceita com reportId: ${reportId}`);

  // Leitura Admin de abuseReports/{reportId}
  const docCheck = await getFirestoreDoc('abuseReports', reportId);
  console.log(`[ADMIN CHECK abuseReports/${reportId}] HTTP ${docCheck.status}`);
  if (docCheck.status !== 200) {
    throw new Error(`TESTE 2a FALHOU: Doc abuseReports/${reportId} não encontrado via Admin.`);
  }
  console.log(`[DOC FIELDS]:`, JSON.stringify(docCheck.body.fields, null, 2));

  // Teste 2b: 3 denunciantes independentes no mesmo fingerprint -> abuseFlag: true em abuseMetrics
  console.log("\n--- TESTE 2b: 3 Denunciantes Independentes -> abuseFlag: true em abuseMetrics ---");
  const userB = await createTestUser("Reporter_B");
  const userC = await createTestUser("Reporter_C");
  console.log(`[USER_B] Criado anon: UID=${userB.uid}`);
  console.log(`[USER_C] Criado anon: UID=${userC.uid}`);

  const reportBRes = await callCallable('reportAbuse', {
    reportedFingerprint: testTargetFingerprint,
    abuseType: "HARASSMENT"
  }, userB.idToken);
  console.log(`[USER_B REPORT] HTTP ${reportBRes.status} -> ${JSON.stringify(reportBRes.body)}`);

  const reportCRes = await callCallable('reportAbuse', {
    reportedFingerprint: testTargetFingerprint,
    abuseType: "ILLEGAL_CONTENT"
  }, userC.idToken);
  console.log(`[USER_C REPORT] HTTP ${reportCRes.status} -> ${JSON.stringify(reportCRes.body)}`);

  // Checar abuseMetrics/{testTargetFingerprint}
  const metricsCheck = await getFirestoreDoc('abuseMetrics', testTargetFingerprint);
  console.log(`[ADMIN CHECK abuseMetrics/${testTargetFingerprint}] HTTP ${metricsCheck.status}`);
  if (metricsCheck.status !== 200) {
    throw new Error(`TESTE 2b FALHOU: abuseMetrics/${testTargetFingerprint} não encontrado.`);
  }
  const fields = metricsCheck.body.fields;
  console.log(`[METRICS DOC FIELDS]:`, JSON.stringify(fields, null, 2));
  const abuseFlag = fields.abuseFlag?.booleanValue;
  const distinctReporters = fields.distinctReporters?.integerValue;
  console.log(`[VERIFICAÇÃO] distinctReporters: ${distinctReporters} | abuseFlag: ${abuseFlag}`);
  if (abuseFlag !== true) {
    throw new Error(`TESTE 2b FALHOU: abuseFlag esperado true, obtido: ${abuseFlag}`);
  }
  console.log(`[OK] abuseFlag: true confirmado após 3 denunciantes independentes!`);

  // Teste 2c: 6ª denúncia do mesmo uid na janela -> 429
  console.log("\n--- TESTE 2c: Rate Limit (6ª Denúncia do Mesmo UID na Janela -> 429) ---");
  console.log(`[INFO] userA já fez 1 denúncia. Realizando denúncias 2, 3, 4, 5...`);
  for (let i = 2; i <= 5; i++) {
    const dummyFp = crypto.randomBytes(32).toString('hex');
    const r = await callCallable('reportAbuse', {
      reportedFingerprint: dummyFp,
      abuseType: "SPAM"
    }, userA.idToken);
    console.log(`  Chamada ${i}/5: HTTP ${r.status}`);
  }
  console.log(`[INFO] Executando a 6ª chamada do userA (esperado: 429)...`);
  const dummyFp6 = crypto.randomBytes(32).toString('hex');
  const res6 = await callCallable('reportAbuse', {
    reportedFingerprint: dummyFp6,
    abuseType: "SPAM"
  }, userA.idToken);
  console.log(`[6ª CHAMADA STATUS] HTTP ${res6.status}`);
  console.log(`[6ª CHAMADA BODY]`, JSON.stringify(res6.body));
  if (res6.status !== 429 && res6.body?.error?.status !== 'RESOURCE_EXHAUSTED') {
    throw new Error(`TESTE 2c FALHOU: 6ª chamada esperava HTTP 429 ou RESOURCE_EXHAUSTED, mas obteve HTTP ${res6.status}`);
  }
  console.log(`[OK] Rate limiting ativo em produção: 429 / RESOURCE_EXHAUSTED na 6ª denúncia!`);

  // Teste 2d: Payload com campo de conteúdo ("texto") -> invalid-argument (Zero-Knowledge)
  console.log("\n--- TESTE 2d: Payload com Campo de Conteúdo ('texto') -> Rejeição 400 invalid-argument ---");
  const userD = await createTestUser("Reporter_D");
  const payloadTexto = {
    reportedFingerprint: testTargetFingerprint,
    abuseType: "SPAM",
    texto: "Tentativa de enviar conteúdo de mensagem para o servidor"
  };
  const resTexto = await callCallable('reportAbuse', payloadTexto, userD.idToken);
  console.log(`[PAYLOAD 'texto' STATUS] HTTP ${resTexto.status}`);
  console.log(`[PAYLOAD 'texto' BODY]`, JSON.stringify(resTexto.body));
  if (resTexto.status !== 400 && resTexto.body?.error?.status !== 'INVALID_ARGUMENT') {
    throw new Error(`TESTE 2d FALHOU: esperado erro 400 / INVALID_ARGUMENT para campo 'texto'`);
  }
  console.log(`[OK] Zero-Knowledge ativo em produção: campo 'texto' rejeitado com invalid-argument!`);

  // Teste 2e: Payload com campo extra ("nota") -> invalid-argument (Allow-List Estrita)
  console.log("\n--- TESTE 2e: Payload com Campo Extra ('nota') -> Rejeição 400 invalid-argument ---");
  const payloadNota = {
    reportedFingerprint: testTargetFingerprint,
    abuseType: "OTHER",
    nota: "Observacao do operador"
  };
  const resNota = await callCallable('reportAbuse', payloadNota, userD.idToken);
  console.log(`[PAYLOAD 'nota' STATUS] HTTP ${resNota.status}`);
  console.log(`[PAYLOAD 'nota' BODY]`, JSON.stringify(resNota.body));
  if (resNota.status !== 400 && resNota.body?.error?.status !== 'INVALID_ARGUMENT') {
    throw new Error(`TESTE 2e FALHOU: esperado erro 400 / INVALID_ARGUMENT para campo 'nota'`);
  }
  console.log(`[OK] Allow-list estrita ativa em produção: campo 'nota' rejeitado com invalid-argument!`);

  // Cleanup de teste
  console.log("\n--- CLEANUP: Excluindo documentos de teste em produção ---");
  try {
    await deleteFirestoreDoc('abuseReports', reportId);
    await deleteFirestoreDoc('abuseMetrics', testTargetFingerprint);
    await deleteFirestoreDoc('userRateLimits', userA.uid);
    await deleteFirestoreDoc('userRateLimits', userB.uid);
    await deleteFirestoreDoc('userRateLimits', userC.uid);
    await deleteFirestoreDoc('userRateLimits', userD.uid);
    console.log("[OK] Documentos de teste efêmeros excluídos com sucesso.");
  } catch (err) {
    console.warn("[WARN] Erro no cleanup:", err.message);
  }

  console.log("\n========================================================");
  console.log(" TODOS OS 5 TESTES DE FUMAÇA EM PRODUÇÃO PASSARAM (100%)");
  console.log("========================================================\n");
}

runSmokeTests().catch(err => {
  console.error("\n[ERRO CRÍTICO NO SMOKE TEST DE PRODUÇÃO]:", err);
  process.exit(1);
});
