const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const { Marked } = require('marked');
const { markedHighlight } = require('marked-highlight');
const hljs = require('highlight.js');

const ROOT = __dirname;
const SRC = path.join(ROOT, 'src');
const DIAGRAMS = path.join(ROOT, 'assets', 'diagrams');
const OUT_HTML = path.join(ROOT, 'manual.html');
const OUT_PDF = path.join(ROOT, 'manual-tecnico-gea.pdf');

const marked = new Marked(
  markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code, lang) {
      const language = hljs.getLanguage(lang) ? lang : 'plaintext';
      return hljs.highlight(code, { language }).value;
    },
  })
);

const PARTS = [
  '00-introduccion.md',
  '01-arranque.md',
  '02-backend.md',
  '03-frontend.md',
  '04-app-flutter.md',
  '05-integracion.md',
  '06-despliegue.md',
  '07-troubleshooting.md',
  '08-anexos.md',
];

function renderDiagrams() {
  if (!fs.existsSync(DIAGRAMS)) return;
  const mmds = fs.readdirSync(DIAGRAMS).filter(f => f.endsWith('.mmd'));
  for (const mmd of mmds) {
    const input = path.join(DIAGRAMS, mmd);
    const output = path.join(DIAGRAMS, mmd.replace('.mmd', '.svg'));
    console.log(`Renderizando diagrama: ${mmd}`);
    execSync(`npx mmdc -i "${input}" -o "${output}" -b transparent`, { stdio: 'inherit', cwd: ROOT });
  }
}

function buildHtml() {
  const css = fs.readFileSync(path.join(ROOT, 'styles', 'manual.css'), 'utf8');
  const hljsCss = fs.readFileSync(
    path.join(ROOT, 'node_modules', 'highlight.js', 'styles', 'github-dark.css'), 'utf8'
  );
  let bodyHtml = '';
  for (const part of PARTS) {
    const file = path.join(SRC, part);
    if (!fs.existsSync(file)) {
      console.warn(`AVISO: falta ${part}`);
      continue;
    }
    const md = fs.readFileSync(file, 'utf8');
    bodyHtml += marked.parse(md);
  }
  const html = `<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<style>${hljsCss}\n${css}</style>
</head>
<body>
${bodyHtml}
</body>
</html>`;
  fs.writeFileSync(OUT_HTML, html, 'utf8');
  console.log(`HTML generado: ${OUT_HTML}`);
}

function printPdf() {
  const chrome = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
  const cmd = `"${chrome}" --headless --disable-gpu --no-pdf-header-footer --print-to-pdf="${OUT_PDF}" "file:///${OUT_HTML.replace(/\\/g, '/')}"`;
  console.log('Generando PDF...');
  execSync(cmd, { stdio: 'inherit' });
  console.log(`PDF generado: ${OUT_PDF}`);
}

const diagramsOnly = process.argv.includes('--diagrams-only');
renderDiagrams();
if (!diagramsOnly) {
  buildHtml();
  printPdf();
}
