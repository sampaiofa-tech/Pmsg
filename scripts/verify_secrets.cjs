const { execSync } = require("child_process");

const diff = execSync("git diff HEAD", { encoding: "utf8" });
const addedLines = diff.split("\n").filter(l => l.startsWith("+") && !l.startsWith("+++"));

const patterns = [
  { name: "GitHub Token (gho_)", regex: /gho_[A-Za-z0-9_]+/ },
  { name: "GitHub Token (ghp_)", regex: /ghp_[A-Za-z0-9_]+/ },
  { name: "GitHub PAT (github_pat_)", regex: /github_pat_[A-Za-z0-9_]+/ },
  { name: "Google API Key (AIzaSy)", regex: /AIzaSy[A-Za-z0-9_-]{33}/ },
  { name: "Password Parameter", regex: /password\s*=\s*["'][^\s"']+["']/i },
  { name: "Refresh Token", regex: /refresh_token\s*[:=]\s*["'][^\s"']+["']/i },
  { name: "Client Email Credential", regex: /client_email\s*[:=]\s*["'][^\s"']+["']/i },
  { name: "Private Key Header", regex: /BEGIN (RSA |EC )?PRIVATE KEY/ }
];

let leaksFound = 0;
for (let i = 0; i < addedLines.length; i++) {
  const line = addedLines[i];
  for (const p of patterns) {
    if (p.regex.test(line)) {
      console.error(`ALERTA: Padrão proibido detectado no diff: ${p.name}`);
      leaksFound++;
    }
  }
}

if (leaksFound > 0) {
  console.error(`FALHA NA VARREDURA: ${leaksFound} ocorrências proibidas encontradas.`);
  process.exit(1);
} else {
  console.log("SUCESSO: Varredura de segredos limpa! Zero assinaturas de segredos encontradas nas modificações.");
}
