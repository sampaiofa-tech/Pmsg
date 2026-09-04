import https from 'https';
import { execSync } from 'child_process';

function getGitHubToken() {
  try {
    const creds = execSync('git credential fill', {
      input: 'url=https://github.com\n',
      encoding: 'utf8'
    });
    const match = creds.match(/password=(.*)/);
    return match ? match[1].trim() : null;
  } catch (e) {
    return null;
  }
}

const token = getGitHubToken();
const headers = {
  'User-Agent': 'Node.js',
  'Accept': 'application/vnd.github.v3+json'
};
if (token) {
  headers['Authorization'] = `token ${token}`;
}

function fetch(path) {
  return new Promise((resolve, reject) => {
    https.get(`https://api.github.com/repos/sampaiofa-tech/Pmsg${path}`, { headers }, (res) => {
      let d = '';
      res.on('data', c => d += c);
      res.on('end', () => {
        try {
          resolve(JSON.parse(d));
        } catch (e) {
          reject(e);
        }
      });
    }).on('error', reject);
  });
}

async function main() {
  const runs = await fetch('/actions/runs?per_page=3');
  if (!runs.workflow_runs) {
    console.log('No runs found:', runs);
    return;
  }

  for (const r of runs.workflow_runs) {
    console.log(`Run ID: ${r.id} | Name: ${r.name} | Status: ${r.status} | Conclusion: ${r.conclusion} | Commit: ${r.head_sha.slice(0, 7)}`);
    if (r.status === 'in_progress' || r.id === runs.workflow_runs[0].id) {
      const jobs = await fetch(`/actions/runs/${r.id}/jobs`);
      if (jobs.jobs) {
        for (const j of jobs.jobs) {
          console.log(`  -> Job: ${j.name} | Status: ${j.status} | Conclusion: ${j.conclusion}`);
          for (const s of (j.steps || [])) {
            console.log(`      * Step: ${s.name} [${s.status}] conclusion=${s.conclusion || 'pending'}`);
          }
        }
      }
    }
  }
}

main().catch(console.error);
