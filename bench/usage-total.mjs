// 最初から今までの「動いた時間」と「使ったトークン」を、会話の記録(~/.claude/projects の JSONL)から数えて、
// bench/results/usage-total.md と usage-total.csv に書く。
//   node bench/usage-total.mjs
// 数えるもの:
//   - 計測台を作る会話(このリポジトリで開いた会話。担当=サブエージェントの分も含む)
//   - 計測(作業ツリー <リポジトリ名>-runs/ で開いた会話。1 本 1 会話。担当の分も含む。走っている途中の会話も今の時点まで数える)
// 時間は 3 つ出す:
//   - 期間: 最初の記録から最後の記録まで(寝ている間なども入る)
//   - 動いた時間: 記録と記録の間が 5 分以内のところだけを足したもの(人の返事待ちや止まっている間を除く。担当と本会話が同時に動いている間は 1 回だけ数える)
// トークンは 4 種(入力・キャッシュ書き込み・キャッシュ読み込み・出力)を分けたまま。同じ応答が何行かに分かれて残るので、応答の ID ごとに 1 回だけ数える。
import { existsSync, readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { basename, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const bench = dirname(fileURLToPath(import.meta.url));
const root = dirname(bench);
const results = join(bench, 'results');
const rates = JSON.parse(readFileSync(join(bench, 'rates.bedrock.json'), 'utf8'));
// 公式単価は cs-cost の rates.json(claude-opus-5-5)から読む。キャッシュ書き込みは 5 分と 1 時間で単価が違う
const official = JSON.parse(readFileSync(join(root, 'kit/.claude/skills/cs-cost/rates.json'), 'utf8')).models['claude-opus-5-5'];
const OFFICIAL = { input: official.input, cache_write: official.cache_write_5m, cache_write_1h: official.cache_write_1h, cache_read: official.cache_read, output: official.output };
// Bedrock の 1 時間のキャッシュ書き込みの単価は rates.bedrock.json に無い。Anthropic の料金と同じく入力の 2 倍と仮定する(推論・未確認)
const REGIONAL_1H = { ...rates.regional, cache_write_1h: rates.regional.input * 2 };
// これまでの summary.md と同じ数え方(書き込みを全部 5 分の単価で数える)
const REGIONAL_5M = { ...rates.regional };
const GAP = 5 * 60 * 1000;

const projects = join(homedir(), '.claude', 'projects');
// プロジェクトのフォルダ名は、パスの記号を - にしたもの
const slug = (p) => p.replace(/[:\\/._ ]/g, '-');
const buildDir = join(projects, slug(root.replace(/\//g, '\\')));
const runsPrefix = slug((root + '-runs').replace(/\//g, '\\'));

function jsonlFiles(dir) {
  const out = [];
  if (!existsSync(dir)) return out;
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    if (statSync(p).isDirectory()) out.push(...jsonlFiles(p));
    else if (name.endsWith('.jsonl')) out.push(p);
  }
  return out;
}

// 1 つの会話(本会話の JSONL と、その会話のフォルダの subagents/ の JSONL)をまとめて数える
function readSession(mainFile) {
  const files = [mainFile, ...jsonlFiles(join(dirname(mainFile), basename(mainFile, '.jsonl'), 'subagents'))];
  const times = [];
  const usage = new Map();
  let model = '';
  for (const f of files) {
    for (const line of readFileSync(f, 'utf8').split('\n')) {
      if (!line.trim()) continue;
      let e;
      try { e = JSON.parse(line); } catch { continue; }
      if (e.timestamp) times.push(Date.parse(e.timestamp));
      const m = e.message;
      if (e.type === 'assistant' && m?.usage && m.model !== '<synthetic>') {
        usage.set(`${f}:${m.id ?? e.uuid}`, m.usage);
        model ||= m.model;
      }
    }
  }
  times.sort((a, b) => a - b);
  let active = 0;
  for (let i = 1; i < times.length; i++) {
    const d = times[i] - times[i - 1];
    if (d <= GAP) active += d;
  }
  const t = { input: 0, cw: 0, cw1h: 0, cr: 0, out: 0, calls: usage.size };
  for (const u of usage.values()) {
    t.input += u.input_tokens ?? 0;
    t.cw += u.cache_creation_input_tokens ?? 0;
    t.cw1h += u.cache_creation?.ephemeral_1h_input_tokens ?? 0;
    t.cr += u.cache_read_input_tokens ?? 0;
    t.out += u.output_tokens ?? 0;
  }
  return { file: mainFile, start: times[0], end: times.at(-1), active, model, ...t };
}

const topLevel = (dir) => (existsSync(dir) ? readdirSync(dir).filter((n) => n.endsWith('.jsonl')).map((n) => join(dir, n)) : []);
const buildSessions = topLevel(buildDir).map(readSession).filter((s) => s.calls > 0);
const runDirs = existsSync(projects) ? readdirSync(projects).filter((n) => n.startsWith(runsPrefix)) : [];
const runSessions = runDirs.flatMap((d) => topLevel(join(projects, d)).map((f) => ({ ...readSession(f), tree: d.slice(runsPrefix.length + 1) }))).filter((s) => s.calls > 0);

// cw は書き込みの合計、cw1h はそのうち 1 時間の分
const price = (t, r) => (t.input * r.input + (t.cw - t.cw1h) * r.cache_write + t.cw1h * (r.cache_write_1h ?? r.cache_write) + t.cr * r.cache_read + t.out * r.output) / 1e6;
const sum = (xs) => xs.reduce((a, s) => ({ input: a.input + s.input, cw: a.cw + s.cw, cw1h: a.cw1h + s.cw1h, cr: a.cr + s.cr, out: a.out + s.out, calls: a.calls + s.calls, active: a.active + s.active,
  start: Math.min(a.start, s.start), end: Math.max(a.end, s.end) }), { input: 0, cw: 0, cw1h: 0, cr: 0, out: 0, calls: 0, active: 0, start: Infinity, end: -Infinity });

// 計測の会話を、試し(0 回目)・担当ごとの 45 本・E01 に分ける
const isTrial = (s) => /-r0$/.test(s.tree);
const isE01 = (s) => /^E01-/.test(s.tree) || /^e01-/i.test(s.tree);
const groups = [
  ['計測台を作る会話(担当の分を含む)', buildSessions],
  ['計測: 試しの 0 回目(集計に入れない)', runSessions.filter(isTrial)],
  ['計測: 担当ごとの 45 本', runSessions.filter((s) => !isTrial(s) && !isE01(s))],
  ['計測: 通し E01(走っている途中の分も含む)', runSessions.filter(isE01)],
];
const all = sum([...buildSessions, ...runSessions]);
// 期間全体での「動いた時間」は、全部の会話の時刻を合わせてから数える(計測と作る会話が同時に動いた間は 1 回だけ)
const unionActive = (() => {
  const iv = [];
  for (const s of [...buildSessions, ...runSessions]) iv.push(readSessionIntervals(s.file));
  const spans = iv.flat().sort((a, b) => a[0] - b[0]);
  let total = 0, cur = null;
  for (const [a, b] of spans) {
    if (!cur || a > cur[1]) { if (cur) total += cur[1] - cur[0]; cur = [a, b]; } else cur[1] = Math.max(cur[1], b);
  }
  if (cur) total += cur[1] - cur[0];
  return total;
})();
function readSessionIntervals(mainFile) {
  const files = [mainFile, ...jsonlFiles(join(dirname(mainFile), basename(mainFile, '.jsonl'), 'subagents'))];
  const times = [];
  for (const f of files) for (const line of readFileSync(f, 'utf8').split('\n')) { const m = line.match(/"timestamp":"([^"]+)"/); if (m) times.push(Date.parse(m[1])); }
  times.sort((a, b) => a - b);
  const out = [];
  for (let i = 1; i < times.length; i++) if (times[i] - times[i - 1] <= GAP) out.push([times[i - 1], times[i]]);
  return out;
}

const n = (v) => Math.round(v).toLocaleString('en-US');
const h = (ms) => (ms / 3600e3).toFixed(2);
const jst = (ms) => new Date(ms + 9 * 3600e3).toISOString().replace('T', ' ').slice(0, 16);
const usd = (v) => `$${v.toFixed(2)}`;

const lines = [];
lines.push('# 最初から今までの稼働時間とトークン');
lines.push('');
lines.push(`数えた時点: 日本時間 ${jst(Date.now())}。会話の記録(~/.claude/projects の JSONL)から \`node bench/usage-total.mjs\` で数えた(実測)。`);
lines.push('Bedrock の金額は `bench/rates.bedrock.json` の単価(未確認)。公式単価は cs-cost と同じ Anthropic の料金表。');
lines.push('');
lines.push('## 区分ごと');
lines.push('');
lines.push('| 区分 | 会話の数 | 期間(日本時間) | 動いた時間 | 呼び出し | 入力 | キャッシュ書き込み | キャッシュ読み込み | 出力 | 公式単価 | Bedrock 地域指定(書き込みを 5 分の単価で) | Bedrock 地域指定(1 時間の書き込みを入力の 2 倍で) |');
lines.push('|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|');
const csv = [['区分', '会話の数', '始まり(日本時間)', '終わり(日本時間)', '動いた時間(時間)', '呼び出し', '入力', 'キャッシュ書き込み', 'キャッシュ読み込み', '出力', '金額(公式単価)', '金額(Bedrock地域指定・5分の単価・未確認)', '金額(Bedrock地域指定・1時間は入力の2倍・未確認)']];
for (const [name, ss] of groups) {
  if (!ss.length) continue;
  const t = sum(ss);
  lines.push(`| ${name} | ${ss.length} | ${jst(t.start)}〜${jst(t.end)} | ${h(t.active)} 時間 | ${n(t.calls)} | ${n(t.input)} | ${n(t.cw)} | ${n(t.cr)} | ${n(t.out)} | ${usd(price(t, OFFICIAL))} | ${usd(price(t, REGIONAL_5M))} | ${usd(price(t, REGIONAL_1H))} |`);
  csv.push([name, ss.length, jst(t.start), jst(t.end), h(t.active), t.calls, t.input, t.cw, t.cr, t.out, price(t, OFFICIAL).toFixed(4), price(t, REGIONAL_5M).toFixed(4), price(t, REGIONAL_1H).toFixed(4)]);
}
lines.push(`| **全部** | ${buildSessions.length + runSessions.length} | ${jst(all.start)}〜${jst(all.end)} | ${h(unionActive)} 時間(重なりは 1 回) | ${n(all.calls)} | ${n(all.input)} | ${n(all.cw)} | ${n(all.cr)} | ${n(all.out)} | **${usd(price(all, OFFICIAL))}** | ${usd(price(all, REGIONAL_5M))} | **${usd(price(all, REGIONAL_1H))}** |`);
csv.push(['全部', buildSessions.length + runSessions.length, jst(all.start), jst(all.end), h(unionActive), all.calls, all.input, all.cw, all.cr, all.out, price(all, OFFICIAL).toFixed(4), price(all, REGIONAL_5M).toFixed(4), price(all, REGIONAL_1H).toFixed(4)]);

const spanH = (all.end - all.start) / 3600e3;
const activeH = unionActive / 3600e3;
const perActiveHour = price(all, REGIONAL_1H) / activeH;
const perSpanHour = price(all, REGIONAL_1H) / spanH;
lines.push('');
lines.push('## 1 日あたりにすると(推論)');
lines.push('');
lines.push(`- 期間は ${spanH.toFixed(1)} 時間、そのうち Claude が動いていたのは ${activeH.toFixed(1)} 時間(実測)。`);
lines.push(`- 期間で割ると 1 時間あたり ${usd(perSpanHour)}、**24 時間に延ばすと ${usd(perSpanHour * 24)}**(Bedrock 地域指定。今回と同じ使い方を 1 日続けた場合。推論)。`);
lines.push(`- 動いた時間で割ると 1 時間あたり ${usd(perActiveHour)}。**8 時間動かし続けると ${usd(perActiveHour * 8)}**、24 時間動かし続けると ${usd(perActiveHour * 24)}(推論)。`);
lines.push('- トークン数(4 種)も同じ割合で増える。1 日あたりのトークン数は下の表(推論)。');
lines.push('');
lines.push('| 1 日の見方 | 入力 | キャッシュ書き込み | キャッシュ読み込み | 出力 | Bedrock 地域指定(1 時間の書き込みを入力の 2 倍で) |');
lines.push('|---|---:|---:|---:|---:|---:|');
for (const [label, factor] of [['今回と同じ使い方で 24 時間(期間で割る)', 24 / spanH], ['8 時間動かし続ける(動いた時間で割る)', 8 / activeH], ['24 時間動かし続ける(動いた時間で割る)', 24 / activeH]]) {
  lines.push(`| ${label} | ${n(all.input * factor)} | ${n(all.cw * factor)} | ${n(all.cr * factor)} | ${n(all.out * factor)} | ${usd(price(all, REGIONAL_1H) * factor)} |`);
}
lines.push('');
lines.push('## 注意');
lines.push(`- **キャッシュ書き込みのうち ${(all.cw1h / all.cw * 100).toFixed(1)}% が「1 時間」のキャッシュ**(実測)。1 時間のキャッシュの書き込みは 5 分のものより高い(公式: Opus 5.5 で 100 万あたり $${OFFICIAL.cache_write} に対し $${OFFICIAL.cache_write_1h})。これまでの summary.md の Bedrock の金額は、書き込みを全部 5 分の単価で数えていたので、少なめに出ている。Bedrock の 1 時間の単価は確かめていない(未確認)ので、ここでは入力の 2 倍(Anthropic の料金と同じ関係)と仮定した(推論)。Bedrock で 1 時間のキャッシュが使えるか自体も未確認。`);
lines.push('- 計測台を作る会話は、人とのやり取りを含む(人が読む・考える間は「動いた時間」に入らない)。計測は人がいない自動の実行。');
lines.push('- 計測台を作る会話のトークンは `.build-log/cost-log.csv` と、計測のトークンは `bench/results/cost-log.csv` と、ほぼ同じになるはず(数え方は同じ。走っている途中の E01 と、この数えた後の分だけ違う)。');
lines.push('- 「動いた時間」は記録と記録の間が 5 分以内のところを足したもの。長いビルドやテストで 5 分以上記録が無い間は入らないので、少なめに出る(推論)。');
writeFileSync(join(results, 'usage-total.md'), lines.join('\n') + '\n');
writeFileSync(join(results, 'usage-total.csv'), csv.map((r) => r.join(',')).join('\n') + '\n');
console.log(lines.join('\n'));
