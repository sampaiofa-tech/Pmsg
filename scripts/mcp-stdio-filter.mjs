import { spawn } from 'node:child_process';
import readline from 'node:readline';

const cwd = 'C:\\Dev\\Pmsg';
const child = spawn(
  'cmd.exe',
  [
    '/c',
    'gradlew.bat',
    '--no-daemon',
    '--quiet',
    '--console=plain',
    ':composeApp:hotMcpServerDesktop'
  ],
  {
    cwd,
    stdio: ['pipe', 'pipe', 'pipe'],
    windowsHide: true,
  }
);

// Encaminha stderr do filho diretamente para stderr do pai (logs permitidos)
child.stderr.pipe(process.stderr);

// Encaminha stdin do pai (da IDE) para o stdin do filho (JSON-RPC requests)
process.stdin.pipe(child.stdin);

// Filtra o stdout linha a linha
const rl = readline.createInterface({
  input: child.stdout,
  crlfDelay: Infinity,
});

rl.on('line', (line) => {
  const trimmed = line.trim();
  if (trimmed.startsWith('{')) {
    // Linha JSON-RPC esperada pela IDE
    process.stdout.write(line + '\n');
  } else if (trimmed.length > 0) {
    // Linha de ruído (Gradle, SLF4J, etc) redirecionada para stderr
    process.stderr.write(line + '\n');
  }
});

child.on('error', (err) => {
  process.stderr.write(`[mcp-stdio-filter] Erro no processo filho: ${err.message}\n`);
  process.exit(1);
});

child.on('exit', (code, signal) => {
  if (code !== null) {
    process.exit(code);
  } else {
    process.exit(1);
  }
});

process.on('SIGINT', () => {
  child.kill('SIGINT');
});

process.on('SIGTERM', () => {
  child.kill('SIGTERM');
});
