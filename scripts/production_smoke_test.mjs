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

// 1. Firebase Auth: create anonymous test user
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

// 2. Call Firebase Callable Cloud Function
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

// 3. Helper to get Google Cloud admin access token
function getAdminAccessToken() {
  const configPath = `${process.env.USERPROFILE}/.config/configstore/firebase-tools.json`;
  const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  return config.tokens.access_token;
}

// 4. Firestore REST check (Admin)
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

// 5. Firestore REST delete (Cleanup)
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

// Generate test ephemeral identity keypair (X25519 pub + Ed25519 sign)
function generateTestIdentity(name) {
  const x25519PubBytes = crypto.randomBytes(32);
  const x25519PubBase64 = x25519PubBytes.toString('base64');
  const fingerprint = crypto.createHash('sha256').update(x25519PubBytes).digest('hex');

  const { publicKey: edPub, privateKey: edPriv } = crypto.generateKeyPairSync('ed25519');
  const spki = edPub.export({ type: 'spki', format: 'der' });
  const rawEdPub = spki.subarray(spki.length - 32);
  const signingPubKeyBase64 = rawEdPub.toString('base64');

  return {
    name,
    x25519PubBytes,
    x25519PubBase64,
    fingerprint,
    edPub,
    edPriv,
    rawEdPub,
    signingPubKeyBase64
  };
}

async function runProductionSmokeTests() {
  console.log('================================================================');
  console.log('   PMSG v1.1 — TESTES DE FUMAÇA EM PRODUÇÃO (gen-lang-client)');
  console.log('================================================================\n');

  let passed = 0;
  let failed = 0;

  function assert(condition, message) {
    if (condition) {
      console.log(`  [PASS] ${message}`);
      passed++;
    } else {
      console.error(`  [FAIL] ${message}`);
      failed++;
      throw new Error(`Asserção falhou: ${message}`);
    }
  }

  try {
    console.log('[FASE 0] Inicializando identidades e sessões temporárias de teste...');
    const aliceUser = await createTestUser('Alice (Test)');
    const bobUser = await createTestUser('Bob (Test)');
    const eveUser = await createTestUser('Eve (Attacker Test)');
    console.log(`  - Alice UID: ${aliceUser.uid}`);
    console.log(`  - Bob UID:   ${bobUser.uid}`);
    console.log(`  - Eve UID:   ${eveUser.uid}`);

    const alice = generateTestIdentity('Alice');
    const bob = generateTestIdentity('Bob');
    console.log(`  - Alice Fingerprint: ${alice.fingerprint}`);
    console.log(`  - Bob Fingerprint:   ${bob.fingerprint}\n`);

    // -------------------------------------------------------------
    // 2.a: resolveFingerprint
    // -------------------------------------------------------------
    console.log('[TESTE 2.a] resolveFingerprint: doc de teste vs inexistente');
    
    // Inexistente -> not-found
    const nonExistentFp = 'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff';
    const notFoundRes = await callCallable('resolveFingerprint', { fingerprint: nonExistentFp }, aliceUser.idToken);
    assert(notFoundRes.status === 404, 'resolveFingerprint com fingerprint inexistente retorna HTTP 404');
    assert(notFoundRes.body?.error?.status === 'NOT_FOUND', 'Status de erro é NOT_FOUND');

    // -------------------------------------------------------------
    // 2.b: createInvite (Alice)
    // -------------------------------------------------------------
    console.log('\n[TESTE 2.b] createInvite: Alice gera convite efêmero');
    const inviteRes = await callCallable('createInvite', {
      creatorFingerprint: alice.fingerprint,
      creatorPubKey: alice.x25519PubBase64,
      creatorSigningPubKey: alice.signingPubKeyBase64
    }, aliceUser.idToken);

    console.log(`  -> createInvite response: status=${inviteRes.status}, body=${JSON.stringify(inviteRes.body)}`);
    assert(inviteRes.status === 200, 'createInvite retorna HTTP 200');
    assert(inviteRes.body?.result?.inviteToken, 'createInvite retorna inviteToken não-vazio');
    assert(inviteRes.body?.result?.inviteLink?.startsWith('pmsg://invite?i='), 'inviteLink usa schema pmsg://invite?i=');
    
    const inviteToken = inviteRes.body.result.inviteToken;
    const expiresAtMillis = inviteRes.body.result.expiresAtMillis;
    const now = Date.now();
    const diffHours = (expiresAtMillis - now) / (1000 * 60 * 60);
    assert(diffHours > 23 && diffHours <= 24.1, `TTL do convite é de aproximadamente 24h (calculado: ${diffHours.toFixed(2)}h)`);

    // Agora Alice existe em identities/{fingerprint}, testamos resolveFingerprint para Alice
    const resolveAliceRes = await callCallable('resolveFingerprint', { fingerprint: alice.fingerprint }, bobUser.idToken);
    assert(resolveAliceRes.status === 200, 'resolveFingerprint de Alice registrada retorna HTTP 200');
    assert(resolveAliceRes.body?.result?.pubKey === alice.x25519PubBase64, 'Retorna a chave pública X25519 de Alice (pubKey)');
    assert(resolveAliceRes.body?.result?.signingPubKey === alice.signingPubKeyBase64, 'Retorna a signingPubKey Ed25519 de Alice');
    assert(resolveAliceRes.body?.result?.currentAuthUid === aliceUser.uid, 'Retorna o currentAuthUid de Alice');

    // -------------------------------------------------------------
    // 2.c: acceptInvite (Bob) + Vanish + Replay Rejection
    // -------------------------------------------------------------
    console.log('\n[TESTE 2.c] acceptInvite: Bob aceita convite, vanish e rejeição de reuso');
    const acceptRes = await callCallable('acceptInvite', {
      token: inviteToken
    }, bobUser.idToken);

    console.log(`  -> acceptInvite response: status=${acceptRes.status}, body=${JSON.stringify(acceptRes.body)}`);
    assert(acceptRes.status === 200, 'acceptInvite retorna HTTP 200');
    assert(acceptRes.body?.result?.creatorFingerprint === alice.fingerprint, 'Bob recebe creatorFingerprint da Alice');
    assert(acceptRes.body?.result?.creatorPubKey === alice.x25519PubBase64, 'Bob recebe creatorPubKey da Alice');

    // Vanish confirmado no Firestore (hard delete)
    const inviteDocCheck = await getFirestoreDoc('invites', inviteToken);
    assert(inviteDocCheck.status === 404, 'Vanish confirmado: doc invites/{token} foi hard-deleted do Firestore (HTTP 404)');

    // Reuso rejeitado
    const reuseRes = await callCallable('acceptInvite', {
      token: inviteToken
    }, bobUser.idToken);
    console.log(`  -> reuse response: status=${reuseRes.status}, body=${JSON.stringify(reuseRes.body)}`);
    assert(reuseRes.status === 404, 'Reuso do token consumido é rejeitado com HTTP 404');
    assert(reuseRes.body?.error?.status === 'NOT_FOUND', 'Reuso rejeitado com NOT_FOUND');

    // -------------------------------------------------------------
    // 2.d: updateIdentityRouting (F0 Proof of Possession in Production)
    // -------------------------------------------------------------
    console.log('\n[TESTE 2.d] updateIdentityRouting: Prova de posse Ed25519 (F0)');
    
    // Subcenário 1: Dono legítimo com nova sessão/UID assina payload canônico
    const aliceNewUser = await createTestUser('Alice (New Device Session)');
    const tsValid = Date.now();
    const payloadValid = `pmsg-routing-v1|${alice.fingerprint}|${aliceNewUser.uid}|${tsValid}`;
    const sigValid = crypto.sign(null, Buffer.from(payloadValid, 'utf8'), alice.edPriv).toString('base64');

    const updateValidRes = await callCallable('updateIdentityRouting', {
      fingerprint: alice.fingerprint,
      pubKey: alice.x25519PubBase64,
      timestamp: tsValid,
      signature: sigValid
    }, aliceNewUser.idToken);

    console.log(`  -> updateValid response: status=${updateValidRes.status}, body=${JSON.stringify(updateValidRes.body)}`);
    assert(updateValidRes.status === 200, 'Dono legítimo com assinatura Ed25519 atualiza roteamento com HTTP 200');
    assert(updateValidRes.body?.result?.success === true, 'Retorno indica success = true');

    const aliceDocAfterUpdate = await getFirestoreDoc('identities', alice.fingerprint);
    assert(aliceDocAfterUpdate.status === 200, 'Documento de identidade lido com sucesso no Firestore');
    const storedUid = aliceDocAfterUpdate.body?.fields?.currentAuthUid?.stringValue;
    assert(storedUid === aliceNewUser.uid, `currentAuthUid atualizado para nova sessão (${storedUid})`);

    // Subcenário 2: [ATAQUE F0] Eve possui a pubKey pública de Alice e tenta sequestrar o roteamento
    console.log('\n  -> Testando Cenário de Ataque F0: Eve tenta sequestrar roteamento com pubKey de Alice...');
    const tsEve = Date.now();
    const payloadEve = `pmsg-routing-v1|${alice.fingerprint}|${eveUser.uid}|${tsEve}`;
    // Eve tenta assinar com sua própria chave privada (não possui a de Alice)
    const { privateKey: evePrivKey } = crypto.generateKeyPairSync('ed25519');
    const forgedSig = crypto.sign(null, Buffer.from(payloadEve, 'utf8'), evePrivKey).toString('base64');

    const eveAttackRes = await callCallable('updateIdentityRouting', {
      fingerprint: alice.fingerprint,
      pubKey: alice.x25519PubBase64,
      timestamp: tsEve,
      signature: forgedSig
    }, eveUser.idToken);

    console.log(`  -> eveAttack response: status=${eveAttackRes.status}, body=${JSON.stringify(eveAttackRes.body)}`);
    assert(eveAttackRes.status === 403, 'Ataque de sequestro de Eve é REJEITADO com HTTP 403');
    assert(eveAttackRes.body?.error?.status === 'PERMISSION_DENIED', 'Erro retornado é PERMISSION_DENIED');
    assert(eveAttackRes.body?.error?.message?.includes('assinatura Ed25519'), 'Mensagem confirma falha de assinatura Ed25519');

    // Confirma que o roteamento de Alice NÃO foi alterado
    const aliceDocPostAttack = await getFirestoreDoc('identities', alice.fingerprint);
    const storedUidPostAttack = aliceDocPostAttack.body?.fields?.currentAuthUid?.stringValue;
    assert(storedUidPostAttack === aliceNewUser.uid, 'Roteamento de Alice permanece intacto (Eve não sequestrou)');

    // Subcenário 3: Replay attack com timestamp expirado (> 5 minutos atrás)
    console.log('\n  -> Testando Replay Attack: assinatura válida mas timestamp velho...');
    const tsOld = Date.now() - (6 * 60 * 1000); // 6 minutos atrás
    const payloadOld = `pmsg-routing-v1|${alice.fingerprint}|${aliceNewUser.uid}|${tsOld}`;
    const sigOld = crypto.sign(null, Buffer.from(payloadOld, 'utf8'), alice.edPriv).toString('base64');

    const replayRes = await callCallable('updateIdentityRouting', {
      fingerprint: alice.fingerprint,
      pubKey: alice.x25519PubBase64,
      timestamp: tsOld,
      signature: sigOld
    }, aliceNewUser.idToken);

    console.log(`  -> replay response: status=${replayRes.status}, body=${JSON.stringify(replayRes.body)}`);
    assert(replayRes.status === 400, 'Replay com timestamp de 6 min atrás é rejeitado com HTTP 400');
    assert(replayRes.body?.error?.status === 'INVALID_ARGUMENT', 'Erro retornado é INVALID_ARGUMENT');
    assert(replayRes.body?.error?.message?.includes('tolerância de 5 minutos'), 'Mensagem confirma rejeição da janela de 5 min');

    // -------------------------------------------------------------
    // 2.e: Integridade dos serviços legados
    // -------------------------------------------------------------
    console.log('\n[TESTE 2.e] Integridade dos serviços existentes: geminiProxy, storeMessageKey, getMessageKey');
    
    // geminiProxy sem token
    const geminiRes = await httpRequest({
      hostname: `${REGION}-${PROJECT_ID}.cloudfunctions.net`,
      path: '/geminiProxy',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, { prompt: 'ping' });
    assert(geminiRes.status === 401, 'geminiProxy sem token retorna HTTP 401 (barreira de segurança intacta)');

    // storeMessageKey sem token
    const storeKeyRes = await callCallable('storeMessageKey', { messageId: 'dummy', encryptedKey: 'dummy' }, null);
    assert(storeKeyRes.status === 401, 'storeMessageKey sem auth retorna HTTP 401 Unauthenticated');

    // getMessageKey sem token
    const getKeyRes = await callCallable('getMessageKey', { messageId: 'dummy' }, null);
    assert(getKeyRes.status === 401, 'getMessageKey sem auth retorna HTTP 401 Unauthenticated');

    // -------------------------------------------------------------
    // Limpeza de documentos de teste no Firestore
    // -------------------------------------------------------------
    console.log('\n[CLEANUP] Excluindo identidades de teste do Firestore...');
    await deleteFirestoreDoc('identities', alice.fingerprint);
    await deleteFirestoreDoc('identities', bob.fingerprint);
    console.log('  - Documentos de teste removidos com sucesso.');

    console.log('\n================================================================');
    console.log(`RESULTADO FINAL: ${passed} asserções PASSOU | ${failed} FALHOU`);
    console.log('TODOS OS TESTES DE FUMAÇA EM PRODUÇÃO FORAM CONCLUÍDOS COM SUCESSO!');
    console.log('================================================================\n');

  } catch (err) {
    console.error('\n[ERRO CRÍTICO DURANTE OS TESTES DE FUMAÇA]:', err);
    process.exit(1);
  }
}

runProductionSmokeTests();
