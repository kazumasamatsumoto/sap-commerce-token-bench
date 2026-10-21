// 通し(E01: cs-pdm から順に 1 つの会話で引き継ぐ)と、担当ごとに別の会話で回したもの(S03〜S07)を、担当ごとに並べる。
//   node bench/compare-e01.mjs   → bench/results/e01-vs-separate.md
// 1 回の中で同じ担当が 2 度呼ばれたら(やり直しなど)足してから、回の間の真ん中の値と最大値を取る。
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const results = join(dirname(fileURLToPath(import.meta.url)), 'results');
const [header, ...lines] = readFileSync(join(results, 'cost-by-agent-detail.csv'), 'utf8').trim().split('\n');
const cols = header.split(',');
const rows = lines.map((line) => {
  const cells = line.match(/("([^"]|"")*"|[^,]*)(,|$)/g).map((c) => c.replace(/,$/, '').replace(/^"|"$/g, '').replace(/""/g, '"'));
  return Object.fromEntries(cols.map((c, i) => [c, cells[i]]));
}).filter((r) => r['集計に入れる'] === 'はい');

const SEPARATE = { 'cs-occ-liaison': 'S03', 'cs-backend': 'S04', 'cs-frontend': 'S05', 'cs-qa': 'S06', 'cs-auditor': 'S07' };
const AGENTS = ['cs-pdm', 'cs-occ-liaison', 'cs-backend', 'cs-frontend', 'cs-qa', 'cs-auditor', '本会話'];
const K = [['入力', 'input'], ['キャッシュ書き込み', 'cw'], ['キャッシュ読み込み', 'cr'], ['出力', 'out'], ['この行の秒', 'sec'], ['金額(Bedrock地域指定・未確認)', 'usd']];

// (シナリオ, 回, 担当) ごとに足す
const perRun = new Map();
for (const r of rows) {
  const key = `${r['シナリオ']}\t${r['回']}\t${r['担当']}`;
  const t = perRun.get(key) ?? { calls: 0, ...Object.fromEntries(K.map(([, k]) => [k, 0])) };
  t.calls += 1;
  for (const [j, k] of K) t[k] += Number(r[j]);
  perRun.set(key, t);
}
const pick = (sid, agent) => [...perRun].filter(([k]) => k.startsWith(`${sid}\t`) && k.endsWith(`\t${agent}`)).map(([, v]) => v);
const median = (xs) => { const s = [...xs].sort((a, b) => a - b); return s.length % 2 ? s[(s.length - 1) / 2] : (s[s.length / 2 - 1] + s[s.length / 2]) / 2; };
const stat = (vs) => Object.fromEntries(['calls', ...K.map(([, k]) => k)].map((k) => [k, { med: median(vs.map((v) => v[k])), max: Math.max(...vs.map((v) => v[k])) }]));
const n = (v) => Math.round(v).toLocaleString('en-US');
const usd = (v) => `$${v.toFixed(2)}`;
const cell = (s, k) => (k === 'usd' ? `${usd(s[k].med)} / ${usd(s[k].max)}` : `${n(s[k].med)} / ${n(s[k].max)}`);

const out = [];
out.push('# 通し(E01)と、担当ごとに別の会話(S03〜S07)の比べ');
out.push('');
out.push('値は「真ん中の値 / 最大値」(各 3 回。実測)。金額は Bedrock 地域指定で、キャッシュ書き込みは全部 5 分の単価(未確認)。');
out.push('1 回の中で同じ担当が 2 度呼ばれた分(やり直し)は足してある。本会話は、担当を呼んで結果を受け渡す側の会話。');
out.push('');
out.push('| 担当 | やり方 | 呼び出し回数 | 入力 | キャッシュ書き込み | キャッシュ読み込み | 出力 | 秒 | 金額 |');
out.push('|---|---|---:|---:|---:|---:|---:|---:|---:|');
const tot = { E01: [], SEP: [] };
for (const a of AGENTS) {
  const e = pick('E01', a);
  if (e.length) { const s = stat(e); out.push(`| ${a} | 通し E01 | ${cell(s, 'calls')} | ${K.map(([, k]) => cell(s, k)).join(' | ')} |`); }
  if (SEPARATE[a]) { const s = stat(pick(SEPARATE[a], a)); out.push(`| ${a} | 別の会話 ${SEPARATE[a]} | ${cell(s, 'calls')} | ${K.map(([, k]) => cell(s, k)).join(' | ')} |`); }
}
// 回ごとの合計(担当 + 本会話)
const runTotals = (sids) => sids.map((sid) => [1, 2, 3].map((round) => [...perRun].filter(([k]) => k.startsWith(`${sid}\t${round}\t`)).reduce((a, [, v]) => { for (const [, k] of K) a[k] += v[k]; return a; }, Object.fromEntries(K.map(([, k]) => [k, 0])))));
const e01 = runTotals(['E01'])[0];
const sep = runTotals(Object.values(SEPARATE));
const sepSum = (which) => Object.fromEntries(K.map(([, k]) => [k, sep.reduce((a, rs) => a + (which === 'med' ? median(rs.map((r) => r[k])) : Math.max(...rs.map((r) => r[k]))), 0)]));
const e01s = Object.fromEntries(K.map(([, k]) => [k, { med: median(e01.map((r) => r[k])), max: Math.max(...e01.map((r) => r[k])) }]));
out.push('');
out.push('## 1 ストーリーの合計(担当と本会話を足したもの)');
out.push('');
out.push('| やり方 | 入力 | キャッシュ書き込み | キャッシュ読み込み | 出力 | 秒 | 金額 |');
out.push('|---|---:|---:|---:|---:|---:|---:|');
out.push(`| 通し E01(1 回の合計の真ん中 / 最大) | ${K.map(([, k]) => cell(e01s, k)).join(' | ')} |`);
const sm = sepSum('med'), sx = sepSum('max');
out.push(`| 別の会話 S03〜S07(真ん中の合計 / 最大の合計) | ${K.map(([, k]) => (k === 'usd' ? `${usd(sm[k])} / ${usd(sx[k])}` : `${n(sm[k])} / ${n(sx[k])}`)).join(' | ')} |`);
out.push('');
out.push('E01 の 3 回の合計: ' + e01.map((r, i) => `${i + 1} 回目 ${usd(r.usd)}(${n(r.sec)} 秒は担当と本会話の秒を足したもの。重なるので実時間より長い)`).join('、'));
writeFileSync(join(results, 'e01-vs-separate.md'), out.join('\n') + '\n');
console.log(out.join('\n'));
