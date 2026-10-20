// 計測の記録(bench/results/cost-log.csv と runs.csv)を集計して、bench/results/summary.md に書く。
// 使い方: node bench/summarize.mjs [--rates global|regional]
// 4 種のトークン(入力・キャッシュ書き込み・キャッシュ読み込み・出力)は最後まで分けたまま扱い、金額は 4 つ別々に単価を掛けてから足す。
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));
const results = join(dir, 'results');
const ratesKind = process.argv.includes('--rates') ? process.argv[process.argv.indexOf('--rates') + 1] : 'regional';
const ratesFile = JSON.parse(readFileSync(join(dir, 'rates.bedrock.json'), 'utf8'));
const rates = ratesFile[ratesKind];
const STORY = ['S03', 'S04', 'S05', 'S06', 'S07'];

function readCsv(file) {
  if (!existsSync(file)) {
    return [];
  }
  const [header, ...lines] = readFileSync(file, 'utf8').trim().split('\n');
  const cols = header.split(',');
  return lines.map((line) => {
    const cells = line.match(/("([^"]|"")*"|[^,]*)(,|$)/g).map((c) => c.replace(/,$/, '').replace(/^"|"$/g, '').replace(/""/g, '"'));
    return Object.fromEntries(cols.map((c, i) => [c, cells[i]]));
  });
}

const cost = readCsv(join(results, 'cost-log.csv'));
const runs = readCsv(join(results, 'runs.csv'));
if (cost.length === 0) {
  console.log('bench/results/cost-log.csv がありません(まだ回していない)');
  process.exit(0);
}

// 同じラベルの行が何回あっても、最後の行がその回の累計。
// 「0回目」は仕組みの試し(計測に入れない)。
const byLabel = new Map();
for (const row of cost) {
  if (!/\/0回目$/.test(row['依頼'])) {
    byLabel.set(row['依頼'], row);
  }
}
const secondsByLabel = new Map(runs.map((r) => [r['ラベル'], { sec: Number(r['秒']), passed: r['判定'], retries: Number(r['やり直し']) }]));

const scenarios = new Map();
for (const [label, row] of byLabel) {
  const [sid] = label.split('/');
  const t = {
    label,
    input: Number(row['入力']),
    cw: Number(row['キャッシュ書き込み']),
    cr: Number(row['キャッシュ読み込み']),
    out: Number(row['出力']),
    sec: secondsByLabel.get(label)?.sec ?? NaN,
    passed: secondsByLabel.get(label)?.passed ?? '?',
    retries: secondsByLabel.get(label)?.retries ?? NaN,
  };
  t.usd = (t.input * rates.input + t.cw * rates.cache_write + t.cr * rates.cache_read + t.out * rates.output) / 1e6;
  (scenarios.get(sid) ?? scenarios.set(sid, []).get(sid)).push(t);
}

const median = (xs) => {
  const s = xs.filter((x) => !Number.isNaN(x)).sort((a, b) => a - b);
  if (s.length === 0) return NaN;
  return s.length % 2 ? s[(s.length - 1) / 2] : (s[s.length / 2 - 1] + s[s.length / 2]) / 2;
};
const max = (xs) => Math.max(...xs.filter((x) => !Number.isNaN(x)));
const n = (v) => (Number.isNaN(v) || !Number.isFinite(v) ? '-' : Math.round(v).toLocaleString('en-US'));
const usd = (v) => (Number.isNaN(v) || !Number.isFinite(v) ? '-' : `$${v.toFixed(2)}`);
const KEYS = ['input', 'cw', 'cr', 'out', 'sec', 'usd'];

const lines = [];
lines.push('# 計測の集計');
lines.push('');
lines.push(`単価: Bedrock ${ratesKind}(${ratesFile.checked}。${ratesFile.note})`);
lines.push('');
lines.push('## シナリオごと(真ん中の値 / 最大値)');
lines.push('');
lines.push('| シナリオ | 回数 | 通過 | 入力 | キャッシュ書き込み | キャッシュ読み込み | 出力 | 秒 | 金額 |');
lines.push('|---|---:|---:|---:|---:|---:|---:|---:|---:|');
const summary = new Map();
for (const sid of [...scenarios.keys()].sort()) {
  const rows = scenarios.get(sid);
  const med = Object.fromEntries(KEYS.map((k) => [k, median(rows.map((r) => r[k]))]));
  const mx = Object.fromEntries(KEYS.map((k) => [k, max(rows.map((r) => r[k]))]));
  summary.set(sid, { med, mx });
  const passed = rows.filter((r) => r.passed === '通過').length;
  lines.push(
    `| ${sid} | ${rows.length} | ${passed} | ${n(med.input)} / ${n(mx.input)} | ${n(med.cw)} / ${n(mx.cw)} | ${n(med.cr)} / ${n(mx.cr)} | ${n(med.out)} / ${n(mx.out)} | ${n(med.sec)} / ${n(mx.sec)} | ${usd(med.usd)} / ${usd(mx.usd)} |`,
  );
}

lines.push('');
lines.push('## 1 ストーリー(S03〜S07 の真ん中の値の合計 / 最大値の合計)');
lines.push('');
const missing = STORY.filter((s) => !summary.has(s));
if (missing.length) {
  lines.push(`まだ回していないシナリオがある: ${missing.join(', ')}`);
} else {
  const sum = (which, k) => STORY.reduce((a, s) => a + summary.get(s)[which][k], 0);
  lines.push('| 区分 | 入力 | キャッシュ書き込み | キャッシュ読み込み | 出力 | 秒 | 金額 |');
  lines.push('|---|---:|---:|---:|---:|---:|---:|');
  lines.push(`| 真ん中の値の合計 | ${n(sum('med', 'input'))} | ${n(sum('med', 'cw'))} | ${n(sum('med', 'cr'))} | ${n(sum('med', 'out'))} | ${n(sum('med', 'sec'))} | ${usd(sum('med', 'usd'))} |`);
  lines.push(`| 最大値の合計(予算はこちら) | ${n(sum('mx', 'input'))} | ${n(sum('mx', 'cw'))} | ${n(sum('mx', 'cr'))} | ${n(sum('mx', 'out'))} | ${n(sum('mx', 'sec'))} | ${usd(sum('mx', 'usd'))} |`);
}
lines.push('');
lines.push('1 スプリント・1 人月への積み上げは、スプリントあたりのストーリー数とバグ修正の件数を人が決めてから行う(Claude は決めない)。');
lines.push('S10(全体の監査)は別枠で、ストーリーの集計に入れない。');
lines.push('');
lines.push('## 回ごとの記録');
lines.push('');
lines.push('| ラベル | 判定 | やり直し | 入力 | キャッシュ書き込み | キャッシュ読み込み | 出力 | 秒 | 金額 |');
lines.push('|---|---|---:|---:|---:|---:|---:|---:|---:|');
for (const sid of [...scenarios.keys()].sort()) {
  for (const r of scenarios.get(sid)) {
    lines.push(`| ${r.label} | ${r.passed} | ${n(r.retries)} | ${n(r.input)} | ${n(r.cw)} | ${n(r.cr)} | ${n(r.out)} | ${n(r.sec)} | ${usd(r.usd)} |`);
  }
}
writeFileSync(join(results, 'summary.md'), lines.join('\n') + '\n');
console.log(lines.slice(0, 30).join('\n'));
