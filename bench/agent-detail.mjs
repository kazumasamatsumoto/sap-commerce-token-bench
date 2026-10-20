// 担当ごとの記録を、読む人がそのまま使える形の CSV に作り直す(元の CSV は cs-cost が書いたまま残す)。
//   node bench/agent-detail.mjs        (bench/summarize.mjs の最後でも呼ばれる)
// 出すもの:
//   bench/results/cost-by-agent-detail.csv  … 1 回ごと・本会話と担当ごとの行。シナリオ、回、モデル、判定、やり直し、日本時間、
//                                             金額 3 種(cs-cost の公式単価 / Bedrock 地域指定 / Bedrock Global)を足したもの
//   bench/results/cost-by-agent-summary.csv … シナリオ × 区分 × 担当ごとの平均・真ん中の値・最大値(OVERVIEW.md の表の元)
//   .build-log/cost-by-step.csv             … 計測台を作る作業の、段ごとの差分(cost-log.csv は累計なので、引き算して 1 段ごとにする)
// 4 種のトークン(入力・キャッシュ書き込み・キャッシュ読み込み・出力)は分けたまま。金額は 4 つ別々に単価を掛けてから足す。
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const bench = dirname(fileURLToPath(import.meta.url));
const root = dirname(bench);
const results = join(bench, 'results');
const rates = JSON.parse(readFileSync(join(bench, 'rates.bedrock.json'), 'utf8'));

function readCsv(file) {
  if (!existsSync(file)) return [];
  const [header, ...lines] = readFileSync(file, 'utf8').trim().split('\n');
  const cols = header.split(',');
  return lines.map((line) => {
    const cells = line.match(/("([^"]|"")*"|[^,]*)(,|$)/g).map((c) => c.replace(/,$/, '').replace(/^"|"$/g, '').replace(/""/g, '"'));
    return Object.fromEntries(cols.map((c, i) => [c, cells[i]]));
  });
}
const q = (v) => (/[",\n]/.test(String(v)) ? `"${String(v).replace(/"/g, '""')}"` : String(v));
const writeCsv = (file, header, rows) => writeFileSync(file, [header, ...rows].map((r) => r.map(q).join(',')).join('\n') + '\n');
const jst = (iso) => new Date(new Date(iso).getTime() + 9 * 3600e3).toISOString().replace('T', ' ').slice(0, 19);
const price = (t, r) => (t.input * r.input + t.cw * r.cache_write + t.cr * r.cache_read + t.out * r.output) / 1e6;
const tokens = (row) => ({ input: Number(row['入力']), cw: Number(row['キャッシュ書き込み']), cr: Number(row['キャッシュ読み込み']), out: Number(row['出力']) });

// ---------------------------------------------------------------- 計測: 1 回ごとの行
const runs = new Map(readCsv(join(results, 'runs.csv')).map((r) => [r['ラベル'], r]));
const agentRows = readCsv(join(results, 'cost-by-agent.csv'));
const detail = agentRows.map((r) => {
  const [sid, labelAgent, roundText] = r['依頼'].split('/');
  const round = Number(roundText.replace('回目', ''));
  const run = runs.get(r['依頼']) ?? {};
  const t = tokens(r);
  return {
    jst: jst(r['日時']), sid, round, counted: round === 0 ? 'いいえ(仕組みの試し)' : 'はい', labelAgent,
    kind: r['区分'], agent: r['区分'] === '本会話' ? '本会話' : r['担当の種類'], task: r['担当の依頼'] ?? '',
    // 0 回目は BENCH_MODEL の既定が無かったころの試しで、実際は claude-fable-5-1 で動いた(会話の記録で確認)
    model: round === 0 && run['モデル'] === '既定' ? '既定(実際は claude-fable-5-1)' : run['モデル'] ?? '', passed: run['判定'] ?? '', retries: run['やり直し'] ?? '', runSec: run['秒'] ?? '',
    calls: Number(r['呼び出し']), ...t, sec: Number(r['秒']),
    usdOfficial: Number(r['USD']), usdRegional: price(t, rates.regional), usdGlobal: price(t, rates.global),
    session: r['会話ID'], agentId: r['担当ID'],
  };
});
writeCsv(join(results, 'cost-by-agent-detail.csv'),
  ['日時(日本時間)', 'シナリオ', '回', '集計に入れる', '依頼した担当', '区分', '担当', '担当の依頼', 'モデル', '判定', 'やり直し', 'その回の秒(runs.csv)',
    '呼び出し', '入力', 'キャッシュ書き込み', 'キャッシュ読み込み', '出力', 'この行の秒', '金額(公式単価)', '金額(Bedrock地域指定・未確認)', '金額(Bedrock Global・未確認)', '会話ID', '担当ID'],
  detail.map((d) => [d.jst, d.sid, d.round, d.counted, d.labelAgent, d.kind, d.agent, d.task, d.model, d.passed, d.retries, d.runSec,
    d.calls, d.input, d.cw, d.cr, d.out, d.sec, d.usdOfficial.toFixed(4), d.usdRegional.toFixed(4), d.usdGlobal.toFixed(4), d.session, d.agentId]));

// ---------------------------------------------------------------- 計測: シナリオ × 区分 × 担当の集計
const median = (xs) => { const s = [...xs].sort((a, b) => a - b); return s.length % 2 ? s[(s.length - 1) / 2] : (s[s.length / 2 - 1] + s[s.length / 2]) / 2; };
const mean = (xs) => xs.reduce((a, b) => a + b, 0) / xs.length;
const groups = new Map();
for (const d of detail.filter((x) => x.round !== 0)) {
  const key = `${d.sid}\t${d.kind}\t${d.agent}`;
  (groups.get(key) ?? groups.set(key, []).get(key)).push(d);
}
const order = (k) => { const s = k.split('\t')[0]; return (s[0] === 'S' ? '0' : s[0] === 'E' ? '1' : '2') + s + (k.includes('\t担当\t') ? '0' : '1'); };
const METRICS = [['input', '入力'], ['cw', 'キャッシュ書き込み'], ['cr', 'キャッシュ読み込み'], ['out', '出力'], ['sec', '秒'], ['usdOfficial', '金額(公式単価)'], ['usdRegional', '金額(Bedrock地域指定・未確認)']];
const sumHeader = ['シナリオ', '区分', '担当', '回数', '通過', ...METRICS.flatMap(([, j]) => [`${j}・平均`, `${j}・真ん中`, `${j}・最大`])];
const sumRows = [...groups.keys()].sort((a, b) => order(a).localeCompare(order(b))).map((key) => {
  const v = groups.get(key);
  const [sid, kind, agent] = key.split('\t');
  const fmt = (k, x) => (k.startsWith('usd') ? x.toFixed(4) : Math.round(x));
  return [sid, kind, agent, v.length, v.filter((d) => d.passed === '通過').length,
    ...METRICS.flatMap(([k]) => { const xs = v.map((d) => d[k]); return [fmt(k, mean(xs)), fmt(k, median(xs)), fmt(k, Math.max(...xs))]; })];
});
writeCsv(join(results, 'cost-by-agent-summary.csv'), sumHeader, sumRows);

// ---------------------------------------------------------------- 計測台を作る作業: 段ごとの差分
const steps = readCsv(join(root, '.build-log', 'cost-log.csv'));
let prev = null;
const stepRows = steps.map((s) => {
  const cur = { calls: Number(s['呼び出し']), ...tokens(s) };
  // 会話が変わったら(会話ID が違う)差分を取らずにそのまま
  const base = prev && prev.session === s['会話ID'] ? prev.cur : { calls: 0, input: 0, cw: 0, cr: 0, out: 0 };
  prev = { session: s['会話ID'], cur };
  const d = Object.fromEntries(Object.keys(cur).map((k) => [k, cur[k] - base[k]]));
  return [jst(s['日時']), s['依頼'], d.calls, d.input, d.cw, d.cr, d.out, Number(s['今回USD']).toFixed(4), price(d, rates.regional).toFixed(4),
    cur.calls, cur.input, cur.cw, cur.cr, cur.out, Number(s['累計USD']).toFixed(4)];
});
writeCsv(join(root, '.build-log', 'cost-by-step.csv'),
  ['日時(日本時間)', '段', '呼び出し', '入力', 'キャッシュ書き込み', 'キャッシュ読み込み', '出力', '金額(公式単価)', '金額(Bedrock地域指定・未確認)',
    '累計の呼び出し', '累計の入力', '累計のキャッシュ書き込み', '累計のキャッシュ読み込み', '累計の出力', '累計の金額(公式単価)'],
  stepRows);
console.log(`cost-by-agent-detail.csv ${detail.length} 行 / cost-by-agent-summary.csv ${sumRows.length} 行 / .build-log/cost-by-step.csv ${stepRows.length} 行`);
