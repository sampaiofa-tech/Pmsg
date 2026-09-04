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

// RFC 7748 private key clamping
function clampPrivateKey(priv) {
  const c = Buffer.from(priv);
  c[0] &= 248;
  c[31] &= 127;
  c[31] |= 64;
  return c;
}

// Generate X25519 identity keypair
function generateX25519Keypair() {
  const rawPriv = clampPrivateKey(crypto.randomBytes(32));
  const pkcs8 = Buffer.concat([Buffer.from('302e020100300506032b656e04220420', 'hex'), rawPriv]);
  const privKey = crypto.createPrivateKey({ key: pkcs8, format: 'der', type: 'pkcs8' });
  const pubKey = crypto.createPublicKey(privKey);
  const rawPub = pubKey.export({ format: 'der', type: 'spki' }).subarray(-32);

  return {
    rawPriv,
    rawPub,
    privKey,
    pubKey,
    pubHex: rawPub.toString('hex')
  };
}

// SealedBox Seal (RFC 7748 + HKDF-SHA256 + AES-256-GCM)
function sealBox(dek, recipientRawPub) {
  const eph = generateX25519Keypair();
  const recipientSpki = Buffer.concat([Buffer.from('302a300506032b656e032100', 'hex'), recipientRawPub]);
  const recipientPubKey = crypto.createPublicKey({ key: recipientSpki, format: 'der', type: 'spki' });

  // 1. Compute shared secret
  const sharedSecret = crypto.diffieHellman({ privateKey: eph.privKey, publicKey: recipientPubKey });

  // 2. Derive KEK via HKDF-SHA256
  const salt = crypto.createHash('sha256').update(eph.rawPub).digest();
  const kek = crypto.hkdfSync('sha256', sharedSecret, salt, Buffer.from('pmsg-dek-wrap-v1'), 32);

  // 3. Encrypt DEK with AES-256-GCM (12-byte nonce, 16-byte tag)
  const nonce = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', kek, nonce);
  const encrypted = Buffer.concat([cipher.update(dek), cipher.final()]);
  const tag = cipher.getAuthTag();

  // 4. Pack wrappedDek = nonce (12) + encrypted (32) + tag (16)
  const wrappedBytes = Buffer.concat([nonce, encrypted, tag]);

  return {
    ephemeralPubKeyHex: eph.rawPub.toString('hex'),
    wrappedDekBase64: wrappedBytes.toString('base64')
  };
}

// SealedBox Unseal
function unsealBox(ephemeralPubKeyHex, wrappedDekBase64, recipientRawPriv) {
  const ephPubRaw = Buffer.from(ephemeralPubKeyHex, 'hex');
  const ephSpki = Buffer.concat([Buffer.from('302a300506032b656e032100', 'hex'), ephPubRaw]);
  const ephPubKey = crypto.createPublicKey({ key: ephSpki, format: 'der', type: 'spki' });

  const recipientPkcs8 = Buffer.concat([Buffer.from('302e020100300506032b656e04220420', 'hex'), recipientRawPriv]);
  const recipientPrivKey = crypto.createPrivateKey({ key: recipientPkcs8, format: 'der', type: 'pkcs8' });

  // 1. Compute shared secret
  const sharedSecret = crypto.diffieHellman({ privateKey: recipientPrivKey, publicKey: ephPubKey });

  // 2. Derive KEK
  const salt = crypto.createHash('sha256').update(ephPubRaw).digest();
  const kek = crypto.hkdfSync('sha256', sharedSecret, salt, Buffer.from('pmsg-dek-wrap-v1'), 32);

  // 3. Unpack and decrypt
  const wrappedBytes = Buffer.from(wrappedDekBase64, 'base64');
  const nonce = wrappedBytes.subarray(0, 12);
  const tag = wrappedBytes.subarray(wrappedBytes.length - 16);
  const ciphertext = wrappedBytes.subarray(12, wrappedBytes.length - 16);

  const decipher = crypto.createDecipheriv('aes-256-gcm', kek, nonce);
  decipher.setAuthTag(tag);
  return Buffer.concat([decipher.update(ciphertext), decipher.final()]);
}

// Encrypt plaintext message with DEK
function encryptMessage(plaintext, dek) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', dek, iv);
  const encrypted = Buffer.concat([cipher.update(Buffer.from(plaintext, 'utf8')), cipher.final()]);
  const tag = cipher.getAuthTag();
  return {
    iv: iv.toString('base64'),
    ciphertext: Buffer.concat([encrypted, tag]).toString('base64')
  };
}

// Decrypt message with DEK
function decryptMessage(ciphertextBase64, ivBase64, dek) {
  const iv = Buffer.from(ivBase64, 'base64');
  const rawCipher = Buffer.from(ciphertextBase64, 'base64');
  const tag = rawCipher.subarray(rawCipher.length - 16);
  const encrypted = rawCipher.subarray(0, rawCipher.length - 16);

  const decipher = crypto.createDecipheriv('aes-256-gcm', dek, iv);
  decipher.setAuthTag(tag);
  return Buffer.concat([decipher.update(encrypted), decipher.final()]).toString('utf8');
}

export async function runV12ProductionSmokeTests() {
  console.log('================================================================');
  console.log('   PMSG v1.2 — TESTES DE FUMAÇA EM PRODUÇÃO (E2E DE DEK + ZK)');
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

  const messageId = `msg_smoke_v12_${Date.now()}`;

  try {
    console.log('[FASE 0] Inicializando identidades de teste e sessões de Auth...');
    const aliceUser = await createTestUser('Alice (Test)');
    const bobUser = await createTestUser('Bob (Test)');
    const eveUser = await createTestUser('Eve (Attacker Test)');
    console.log(`  - Alice UID: ${aliceUser.uid}`);
    console.log(`  - Bob UID:   ${bobUser.uid}`);
    console.log(`  - Eve UID:   ${eveUser.uid}`);

    const alice = generateX25519Keypair();
    const bob = generateX25519Keypair();
    const eve = generateX25519Keypair();
    console.log(`  - Alice PubHex: ${alice.pubHex.slice(0, 16)}...`);
    console.log(`  - Bob PubHex:   ${bob.pubHex.slice(0, 16)}...`);
    console.log(`  - Eve PubHex:   ${eve.pubHex.slice(0, 16)}...\n`);

    // -------------------------------------------------------------
    // FASE 1: Alice gera DEK, cifra mensagem, e envelopa DEK via SealedBox
    // -------------------------------------------------------------
    console.log('[TESTE 1] Alice gera DEK aleatória e envelopa para Bob via SealedBox...');
    const secretPlaintext = 'TOP SECRET: Pmsg v1.2 Zero-Trace / Zero-Knowledge DEK Architecture';
    const dek = crypto.randomBytes(32);
    const encryptedMsg = encryptMessage(secretPlaintext, dek);
    console.log(`  - Plaintext original: "${secretPlaintext}"`);
    console.log(`  - Ciphertext Base64:  ${encryptedMsg.ciphertext.slice(0, 32)}...`);

    const envelope = sealBox(dek, bob.rawPub);
    console.log(`  - Ephemeral PubKey:  ${envelope.ephemeralPubKeyHex}`);
    console.log(`  - Wrapped DEK (B64): ${envelope.wrappedDekBase64.slice(0, 32)}...`);
    assert(envelope.ephemeralPubKeyHex.length === 64, 'Ephemeral public key tem 64 caracteres hex (32 bytes)');
    assert(Buffer.from(envelope.wrappedDekBase64, 'base64').length === 60, 'Wrapped DEK tem 60 bytes (12 IV + 32 DEK + 16 Tag)');

    // -------------------------------------------------------------
    // FASE 2: storeMessageKey — Servidor recebe SOMENTE payload opaco
    // -------------------------------------------------------------
    console.log('\n[TESTE 2] Alice chama storeMessageKey enviando payload opaco (sem DEK em claro)...');
    const expiresAtMillis = Date.now() + 3600000; // 1 hora
    const storeRes = await callCallable('storeMessageKey', {
      messageId: messageId,
      senderId: aliceUser.uid,
      recipientId: bobUser.uid,
      ephemeralPubKey: envelope.ephemeralPubKeyHex,
      wrappedDek: envelope.wrappedDekBase64,
      expiresAtMillis: expiresAtMillis
    }, aliceUser.idToken);

    console.log(`  -> storeMessageKey status: ${storeRes.status}, body: ${JSON.stringify(storeRes.body)}`);
    assert(storeRes.status === 200, 'storeMessageKey aceita payload opaco com HTTP 200');
    assert(storeRes.body?.result?.success === true, 'storeMessageKey retorna success = true');

    // -------------------------------------------------------------
    // FASE 3: DEMONSTRAÇÃO DA GARANTIA ZERO-KNOWLEDGE NO FIRESTORE
    // -------------------------------------------------------------
    console.log('\n[TESTE 3] DEMONSTRAÇÃO DA GARANTIA: Inspeção direta no Firestore (simulando vazamento total)...');
    const firestoreKeyDoc = await getFirestoreDoc('messageKeys', messageId);
    assert(firestoreKeyDoc.status === 200, 'Documento messageKeys/{messageId} existe no Firestore');

    const fields = firestoreKeyDoc.body?.fields || {};
    console.log('  - Campos armazenados no doc Firestore:');
    for (const key of Object.keys(fields)) {
      console.log(`      * ${key}`);
    }

    assert(fields.dek === undefined, 'GARANTIA CRIPTOGRÁFICA 1: Campo "dek" NÃO EXISTE no Firestore');
    assert(fields.ephemeralPubKey?.stringValue === envelope.ephemeralPubKeyHex, 'ephemeralPubKey está presente como string opaca');
    assert(fields.wrappedDek?.stringValue === envelope.wrappedDekBase64, 'wrappedDek está presente como string opaca');
    assert(fields.senderId?.stringValue === aliceUser.uid, 'senderId corresponde a Alice');
    assert(fields.recipientId?.stringValue === bobUser.uid, 'recipientId corresponde a Bob');

    // -------------------------------------------------------------
    // FASE 4: Bob chama getMessageKey e desemvelopa a DEK
    // -------------------------------------------------------------
    console.log('\n[TESTE 4] Bob chama getMessageKey e faz o unseal da DEK com sua chave privada...');
    const getRes = await callCallable('getMessageKey', {
      messageId: messageId
    }, bobUser.idToken);

    console.log(`  -> getMessageKey status: ${getRes.status}, body: ${JSON.stringify(getRes.body)}`);
    assert(getRes.status === 200, 'getMessageKey retorna HTTP 200 para o destinatário Bob');
    assert(getRes.body?.result?.ephemeralPubKey === envelope.ephemeralPubKeyHex, 'Bob recebe ephemeralPubKey');
    assert(getRes.body?.result?.wrappedDek === envelope.wrappedDekBase64, 'Bob recebe wrappedDek');

    // Bob unseals DEK
    const bobRecoveredDek = unsealBox(
      getRes.body.result.ephemeralPubKey,
      getRes.body.result.wrappedDek,
      bob.rawPriv
    );
    assert(bobRecoveredDek.equals(dek), 'DEK recuperada por Bob é IDENTICA à DEK gerada por Alice!');

    // Bob decrypts message
    const decryptedMessage = decryptMessage(encryptedMsg.ciphertext, encryptedMsg.iv, bobRecoveredDek);
    console.log(`  - Mensagem decriptada por Bob: "${decryptedMessage}"`);
    assert(decryptedMessage === secretPlaintext, 'Mensagem decriptada corresponde com perfeição ao texto original!');

    // -------------------------------------------------------------
    // FASE 5: TESTE NEGATIVO — Eve (chave privada errada) TENTA UNSEAL
    // -------------------------------------------------------------
    console.log('\n[TESTE 5] TESTE NEGATIVO: Eve (atacante) tenta fazer unseal com chave privada errada...');
    let eveUnsealFailed = false;
    try {
      unsealBox(envelope.ephemeralPubKeyHex, envelope.wrappedDekBase64, eve.rawPriv);
    } catch (e) {
      eveUnsealFailed = true;
      console.log(`  -> Falha esperada e confirmada ao tentar unseal com chave de Eve: ${e.message}`);
    }
    assert(eveUnsealFailed, 'GARANTIA CRIPTOGRÁFICA 2: Falha matemática ao tentar unseal sem a chave privada de Bob (tag mismatch)');

    // Eve tenta chamar getMessageKey (deve ser rejeitada pela function com 403)
    const eveGetRes = await callCallable('getMessageKey', {
      messageId: messageId
    }, eveUser.idToken);
    console.log(`  -> Eve getMessageKey status: ${eveGetRes.status}`);
    assert(eveGetRes.status === 403, 'Eve é barrada pelo controle de acesso de getMessageKey com HTTP 403');

    // -------------------------------------------------------------
    // FASE 6: VANISH / DESTRUCTION DA DEK
    // -------------------------------------------------------------
    console.log('\n[TESTE 6] Vanish / Incineração: destruição da chave no Firestore...');
    const deleteRes = await deleteFirestoreDoc('messageKeys', messageId);
    assert(deleteRes.status === 200, 'Doc messageKeys/{messageId} removido');

    const checkShredded = await getFirestoreDoc('messageKeys', messageId);
    assert(checkShredded.status === 404, 'Incineração confirmada: chave permanentemente inacessível (HTTP 404)');

    const getKeyAfterShred = await callCallable('getMessageKey', {
      messageId: messageId
    }, bobUser.idToken);
    assert(getKeyAfterShred.status === 404, 'getMessageKey após vanish retorna NOT_FOUND (HTTP 404)');

    console.log('\n================================================================');
    console.log(`RESULTADO FINAL: ${passed} asserções PASSOU | ${failed} FALHOU`);
    console.log('TODOS OS TESTES DE FUMAÇA v1.2 EM PRODUÇÃO CONCLUÍDOS COM SUCESSO!');
    console.log('================================================================\n');

  } catch (err) {
    console.error('\n[ERRO CRÍTICO DURANTE OS TESTES DE FUMAÇA v1.2]:', err);
    process.exit(1);
  }
}

if (process.argv[1]?.endsWith('v1_2_production_smoke_test.mjs')) {
  runV12ProductionSmokeTests();
}
