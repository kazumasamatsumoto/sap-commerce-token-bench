// S09(障害の切り分け)で担当に渡すログを作る。種を固定しているので、何度作っても同じ中身になる。
// 使い方: node bench/incidents/generate-s09-logs.mjs → bench/incidents/S09/*.log
// 筋書き(答え。担当には渡さない): 2026-10-19 21:02 からキャンペーンで商品詳細のアクセスが約 6 倍になり、
// ProductReviewSummaryPopulator のレビュー件数の問い合わせ(商品ごとに 2 本)が増えて、DB の接続(Hikari の上限 10)が足りなくなった。
// OCC の応答が 30 秒待って失敗し、SSR が 500 を返した。CDN のエラーと GC のログは目くらまし。
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const out = join(dirname(fileURLToPath(import.meta.url)), 'S09');
mkdirSync(out, { recursive: true });

let seed = 20261019;
const rand = () => {
  seed = (seed + 0x6d2b79f5) | 0;
  let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};
const pick = (a) => a[Math.floor(rand() * a.length)];
const pad = (n, w = 2) => String(n).padStart(w, '0');
const ts = (sec) => {
  const h = 20 + Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  return `2026-10-19 ${pad(h)}:${pad(m)}:${pad(sec % 60)},${pad(Math.floor(rand() * 1000), 3)}`;
};

const commerce = [];
const ssr = [];
const cdn = [];
const products = Array.from({ length: 40 }, (_, i) => `XYZ-${pad(i + 1, 6)}`);

// 20:30〜22:00 を 10 秒刻みで作る。21:02〜21:31 が山。
for (let sec = 1800; sec < 7200; sec += 10) {
  const peak = sec >= 3720 && sec < 5460;
  const requests = peak ? 18 + Math.floor(rand() * 8) : 3 + Math.floor(rand() * 3);
  for (let r = 0; r < requests; r++) {
    const code = pick(products);
    const slow = peak && rand() < 0.35;
    const failed = peak && sec >= 3900 && sec < 5300 && rand() < 0.12;
    const ms = failed ? 30000 + Math.floor(rand() * 200) : slow ? 2500 + Math.floor(rand() * 9000) : 40 + Math.floor(rand() * 160);
    if (r % 3 === 0) {
      commerce.push(`${ts(sec)} INFO  [http-nio-9002-exec-${1 + Math.floor(rand() * 200)}] [RequestLog] GET /occ/v2/electronics-spa/products/${code}?fields=FULL ${failed ? 500 : 200} ${ms}ms`);
    }
    if (failed) {
      // スタックトレースは ERROR の行と 1 まとまりにする(並べ替えで離れないように)
      commerce.push(
        [
          `${ts(sec)} ERROR [http-nio-9002-exec-${1 + Math.floor(rand() * 200)}] [OccExceptionHandler] OCC request failed`,
          `org.springframework.jdbc.CannotGetJdbcConnectionException: Failed to obtain JDBC Connection`,
          `Caused by: java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30000ms (total=10, active=10, idle=0, waiting=${40 + Math.floor(rand() * 60)})`,
          `\tat de.hybris.platform.servicelayer.search.impl.DefaultFlexibleSearchService.search(DefaultFlexibleSearchService.java:61)`,
          `\tat de.hybris.platform.customerreview.impl.DefaultCustomerReviewService.getNumberOfReviews(DefaultCustomerReviewService.java:56)`,
          `\tat de.hybris.platform.commercefacades.product.converters.populators.ProductReviewSummaryPopulator.populate(ProductReviewSummaryPopulator.java:17)`,
        ].join('\n'),
      );
      ssr.push(`${ts(sec + 30)} ERROR [ssr] Rendering /product/${code} failed: HttpErrorResponse 500 from http://commerce:9002/occ/v2/electronics-spa/products/${code}?fields=FULL (30.1s)`);
      ssr.push(`${ts(sec + 30)} INFO  [ssr] GET /product/${code} 500 ${30100 + Math.floor(rand() * 400)}ms`);
    } else if (r % 4 === 0) {
      ssr.push(`${ts(sec)} INFO  [ssr] GET /product/${code} 200 ${ms + 60 + Math.floor(rand() * 80)}ms`);
    }
    if (rand() < (peak ? 0.03 : 0.01)) {
      cdn.push(`${ts(sec)} cdn edge=NRT54 status=502 path=/medias/img-${pad(1 + Math.floor(rand() * 400), 4)}-product.svg origin=commerce:9002 reason=origin_timeout`);
    } else if (r % 5 === 0) {
      cdn.push(`${ts(sec)} cdn edge=NRT54 status=200 cache=${rand() < 0.8 ? 'HIT' : 'MISS'} path=/product/${code}`);
    }
  }
  if (peak && sec % 300 === 0) {
    commerce.push(`${ts(sec)} WARN  [HikariPool-1 housekeeper] [HikariPool] HikariPool-1 - Thread starvation or clock leap detected (housekeeper delta=${(30 + rand() * 20).toFixed(1)}s).`);
  }
  if (sec % 600 === 0) {
    commerce.push(`${ts(sec)} INFO  [GC] Pause Young (Normal) (G1 Evacuation Pause) ${(250 + rand() * 300).toFixed(0)}M->${(120 + rand() * 80).toFixed(0)}M(1024M) ${(8 + rand() * 25).toFixed(1)}ms`);
  }
}
commerce.push(`${ts(3700)} INFO  [cronJob] [CampaignStartJob] campaign 'autumn-sale' started: homepage banners switched, newsletter sent to 52,310 recipients`);
commerce.sort();

writeFileSync(join(out, 'commerce.log'), commerce.join('\n') + '\n');
writeFileSync(join(out, 'storefront-ssr.log'), ssr.sort().join('\n') + '\n');
writeFileSync(join(out, 'cdn-access.log'), cdn.sort().join('\n') + '\n');
writeFileSync(
  join(out, 'README.md'),
  [
    '# 2026-10-19 夜の障害のログ',
    '',
    '- commerce.log … 疑似 Commerce(OCC)のアプリのログ(抜粋)',
    '- storefront-ssr.log … SSR のストアフロントのログ',
    '- cdn-access.log … CDN のアクセスログ(抜粋)',
    '',
    '利用者からの報告: 21 時過ぎから商品詳細が開かない・とても遅い。21 時半ごろには直った。',
    '',
  ].join('\n'),
);
console.log(`commerce ${commerce.length} lines, ssr ${ssr.length} lines, cdn ${cdn.length} lines`);
