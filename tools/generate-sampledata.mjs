// サンプルデータ(ImpEx)を作るスクリプト。
// 使い方: node tools/generate-sampledata.mjs
// 乱数は種を固定しているので、何度動かしても同じファイルができる。
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const OUT_DIR = resolve(dirname(fileURLToPath(import.meta.url)), '..',
  'core-customize/hybris/bin/custom/xyzcore/resources/xyzcore/import/sampledata');

// ---- 乱数と小さな道具 ----
function mulberry32(a) {
  return () => {
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const rnd = mulberry32(20261020);
const int = (min, max) => min + Math.floor(rnd() * (max - min + 1));
const pick = (arr) => arr[Math.floor(rnd() * arr.length)];
const chance = (p) => rnd() < p;
const pad = (n, w) => String(n).padStart(w, '0');
const range = (n) => Array.from({ length: n }, (_, i) => i + 1);
// 重み付きで選ぶ。例: wpick([['A', 3], ['B', 1]])
function wpick(pairs) {
  let r = rnd() * pairs.reduce((s, [, w]) => s + w, 0);
  for (const [v, w] of pairs) if ((r -= w) < 0) return v;
  return pairs[pairs.length - 1][0];
}
// 'ja|en' を { ja, en } に分ける
const je = (s) => { const [ja, en] = s.split('|'); return { ja, en }; };

// ---- 日付(Date.now は使わない。基準日から計算する) ----
const DAY = 86400000;
const BASE = Date.UTC(2026, 9, 1); // 01.10.2026 00:00:00
const NOW = BASE + 19 * DAY; // 20.10.2026
function fmtDate(ms) {
  const d = new Date(ms);
  return `${pad(d.getUTCDate(), 2)}.${pad(d.getUTCMonth() + 1, 2)}.${d.getUTCFullYear()} `
    + `${pad(d.getUTCHours(), 2)}:${pad(d.getUTCMinutes(), 2)}:${pad(d.getUTCSeconds(), 2)}`;
}

// ---- ImpEx を書く道具 ----
const PC = 'electronicsProductCatalog:Online';
const CC = 'electronicsContentCatalog:Online';
const pref = (code) => `${code}:${PC}`; // 商品カタログの参照
const cref = (uid) => `${uid}:${CC}`; // コンテンツカタログの参照
const CV = 'catalogVersion(catalog(id),version)';
const MACROS = [
  `$cv=${CV}[unique=true,default=${PC}]`,
  `$ccv=${CV}[unique=true,default=${CC}]`,
];
// 見出しとデータ行から1つのブロックを作る。値に ; と " が入っていたら止める
function block(header, rows) {
  const lines = rows.map((r) => {
    for (const c of r) if (/[;"\n]/.test(String(c))) throw new Error(`使えない文字: ${c}`);
    return ';' + r.join(';');
  });
  return [header, ...lines].join('\n');
}
const counts = {};
function writeImpex(name, comment, blocks) {
  const text = [`# ${comment}`, '# tools/generate-sampledata.mjs で生成。手で直さないこと。', '', ...MACROS, '', blocks.join('\n\n'), ''].join('\n');
  writeFileSync(resolve(OUT_DIR, name), text, 'utf8');
}

// ---- 単語の一覧 ----
const TOPS = [
  ['カメラ|Cameras', 'デジタル一眼|DSLR Camera,ミラーレス|Mirrorless Camera,コンパクトカメラ|Compact Camera,アクションカメラ|Action Camera,交換レンズ|Interchangeable Lens,三脚|Tripod,カメラバッグ|Camera Bag,ストロボ|Flash,フィルムカメラ|Film Camera'],
  ['オーディオ|Audio', 'ワイヤレスイヤホン|Wireless Earbuds,ヘッドホン|Headphones,スピーカー|Speaker,サウンドバー|Soundbar,アンプ|Amplifier,ターンテーブル|Turntable,マイク|Microphone,DAC|DAC,ポータブルプレーヤー|Portable Player'],
  ['パソコン|Computers', 'ノートPC|Laptop,デスクトップPC|Desktop PC,モニター|Monitor,キーボード|Keyboard,マウス|Mouse,SSD|SSD,メモリ|Memory Module,グラフィックボード|Graphics Card,Webカメラ|Webcam'],
  ['スマートフォン|Smartphones', 'スマートフォン|Smartphone,タブレット|Tablet,スマホケース|Phone Case,保護フィルム|Screen Protector,充電器|Charger,モバイルバッテリー|Power Bank,ケーブル|Cable,スタンド|Stand,電子書籍リーダー|E-Reader'],
  ['テレビ|TVs', '有機ELテレビ|OLED TV,液晶テレビ|LCD TV,プロジェクター|Projector,レコーダー|Recorder,ストリーミング端末|Streaming Device,アンテナ|Antenna,テレビ台|TV Stand,リモコン|Remote Control,スクリーン|Screen'],
  ['生活家電|Home Appliances', '掃除機|Vacuum Cleaner,空気清浄機|Air Purifier,加湿器|Humidifier,扇風機|Fan,電気ケトル|Electric Kettle,炊飯器|Rice Cooker,電子レンジ|Microwave,ドライヤー|Hair Dryer,電動歯ブラシ|Electric Toothbrush'],
  ['ゲーム|Gaming', 'ゲーム機|Game Console,コントローラー|Controller,ゲーミングヘッドセット|Gaming Headset,ゲーミングチェア|Gaming Chair,VRゴーグル|VR Headset,ゲーミングマウス|Gaming Mouse,キャプチャーボード|Capture Card,ゲームソフト|Game Software,携帯ゲーム機|Handheld Console'],
  ['ウェアラブル|Wearables', 'スマートウォッチ|Smartwatch,活動量計|Fitness Tracker,スマートリング|Smart Ring,スマートグラス|Smart Glasses,ウォッチバンド|Watch Band,GPSウォッチ|GPS Watch,睡眠トラッカー|Sleep Tracker,心拍センサー|Heart Rate Sensor,子ども用ウォッチ|Kids Watch'],
  ['ネットワーク|Networking', 'Wi-Fiルーター|Wi-Fi Router,メッシュWi-Fi|Mesh Wi-Fi,中継器|Range Extender,スイッチングハブ|Network Switch,LANケーブル|LAN Cable,NAS|NAS,モバイルルーター|Mobile Router,ネットワークカメラ|Network Camera,USBアダプター|USB Adapter'],
  ['アクセサリ|Accessories', 'メモリーカード|Memory Card,USBメモリ|USB Flash Drive,外付けHDD|External HDD,ドッキングステーション|Docking Station,電源タップ|Power Strip,乾電池|Battery,クリーナー|Cleaning Kit,収納ケース|Storage Case,変換アダプター|Converter'],
].map(([top, subs]) => ({ ...je(top), subs: subs.split(',').map(je) }));
const BRANDS = 'アルテア|Altea,ベルクス|Belx,カイロ|Kairo,ドーマ|Doma,エルナ|Elna,フォルテ|Forte,ギルダ|Gilda,ハルモ|Harmo,イオタ|Iota,ジェネス|Jenes,コルト|Kolt,ルミナ|Lumina,ミラス|Miras,ノヴァ|Nova,オルカ|Orca,ピクセラ|Pixela,クオラ|Quora,ラディア|Radia,ソレイユ|Soleil,テクナ|Tekna,ウルス|Urus,ヴェガ|Vega,ウィスプ|Wisp,ゼノ|Xeno,ユーラ|Yura,ゼファ|Zepha,アオバ|Aoba,サクラ電機|Sakura Denki,ミナト|Minato,ヒカリ|Hikari'.split(',').map(je);
const ADJS = 'プロ|Pro,ライト|Lite,エアー|Air,マックス|Max,ミニ|Mini,ネオ|Neo,プラス|Plus,ウルトラ|Ultra,スリム|Slim,エッジ|Edge'.split(',').map(je);
const MATERIALS = 'アルミニウム|Aluminium,ポリカーボネート|Polycarbonate,ステンレス|Stainless Steel,ABS樹脂|ABS Plastic,マグネシウム合金|Magnesium Alloy,強化ガラス|Tempered Glass,シリコン|Silicone,カーボン|Carbon Fiber,再生プラスチック|Recycled Plastic,天然木|Natural Wood'.split(',').map(je);
const FEATURES = '軽くて持ち運びやすい|Lightweight and easy to carry,長時間使えるバッテリー|Long-lasting battery,すっきりしたデザイン|Clean and simple design,省エネ設計|Energy-efficient design,かんたん設定|Easy setup,静かな動作音|Quiet operation,防水防じん|Water and dust resistant,高速充電に対応|Supports fast charging,アプリで操作できる|Controllable from an app,2年保証付き|Comes with a 2-year warranty'.split(',').map(je);
const LAST = '佐藤|Sato,鈴木|Suzuki,高橋|Takahashi,田中|Tanaka,伊藤|Ito,渡辺|Watanabe,山本|Yamamoto,中村|Nakamura,小林|Kobayashi,加藤|Kato,吉田|Yoshida,山田|Yamada,佐々木|Sasaki,山口|Yamaguchi,松本|Matsumoto,井上|Inoue,木村|Kimura,林|Hayashi,清水|Shimizu,森|Mori'.split(',').map(je);
const FIRST = '健太|Kenta,翔太|Shota,大輔|Daisuke,拓也|Takuya,直樹|Naoki,誠|Makoto,陽菜|Hina,結衣|Yui,美咲|Misaki,さくら|Sakura,葵|Aoi,真由美|Mayumi,花子|Hanako,太郎|Taro,恵|Megumi,亮|Ryo,彩|Aya,優子|Yuko,浩二|Koji,奈々|Nana'.split(',').map(je);
// [都道府県市区, 郵便番号, 市外局番]
const TOWNS = [['東京都千代田区', '100-0005', '03'], ['東京都渋谷区', '150-0002', '03'], ['東京都新宿区', '160-0022', '03'], ['東京都港区', '105-0011', '03'], ['東京都世田谷区', '154-0004', '03'], ['大阪府大阪市北区', '530-0001', '06'], ['大阪府大阪市中央区', '541-0041', '06'], ['神奈川県横浜市西区', '220-0012', '045'], ['愛知県名古屋市中村区', '450-0002', '052'], ['福岡県福岡市博多区', '812-0011', '092'], ['北海道札幌市中央区', '060-0001', '011'], ['京都府京都市下京区', '600-8216', '075']];
const STREETS = ['丸の内', '本町', '中央', '栄', '緑町', '桜木町', '昭和町', '若葉', '旭町', '東町'];
const BUILDINGS = ['XYZビル', 'グリーンハイツ', 'サンライズマンション', 'パークコート', 'リバーサイド荘'];
const REVIEW_TEXT = {
  1: ['期待外れでした|すぐに動かなくなりました。', '残念|説明と違う点がありました。'],
  2: ['いまひとつ|使えますが不満が残ります。', '普通以下|値段の割に作りが弱いと感じました。'],
  3: ['ふつう|可もなく不可もなくです。', 'まあまあ|価格相応だと思います。'],
  4: ['満足です|使いやすくて気に入っています。', 'おすすめ|少し重いですが性能は良いです。'],
  5: ['最高です|買ってよかったです。家族にもすすめました。', '大満足|デザインも性能も文句なしです。'],
};

mkdirSync(OUT_DIR, { recursive: true });

// ==== 01 カタログ、サイト、ストア ====
writeImpex('01-catalog.impex', 'カタログ、カタログバージョン、サイト、配送方法、ストア', [
  block('INSERT_UPDATE Catalog;id[unique=true];name[lang=ja];name[lang=en]', [
    ['electronicsProductCatalog', '電機商品カタログ', 'Electronics Product Catalog'],
    ['electronicsContentCatalog', '電機コンテンツカタログ', 'Electronics Content Catalog'],
  ]),
  block('INSERT_UPDATE CatalogVersion;catalog(id)[unique=true];version[unique=true];active', [
    ['electronicsProductCatalog', 'Online', 'true'],
    ['electronicsContentCatalog', 'Online', 'true'],
  ]),
  block('INSERT_UPDATE BaseSite;uid[unique=true];name[lang=ja];name[lang=en];channel(code);contentCatalog(id);defaultLanguage(isocode)', [
    ['electronics-spa', 'XYZ電機ストア', 'XYZ Electronics Store', 'B2C', 'electronicsContentCatalog', 'ja'],
  ]),
  block('INSERT_UPDATE DeliveryMode;code[unique=true];name[lang=ja];name[lang=en];description[lang=ja];description[lang=en];cost;active', [
    ['standard-gross', '通常配送', 'Standard Delivery', '3から5営業日でお届け', 'Delivered in 3-5 business days', '500', 'true'],
    ['premium-gross', 'お急ぎ便', 'Premium Delivery', '翌営業日にお届け', 'Delivered next business day', '1200', 'true'],
  ]),
  block('INSERT_UPDATE BaseStore;uid[unique=true];name[lang=ja];name[lang=en];productCatalog(id);defaultCurrency(isocode);net;warehouse;cmsSite(uid);deliveryModes(code)', [
    ['electronics', 'XYZ電機', 'XYZ Electronics', 'electronicsProductCatalog', 'JPY', 'false', 'xyzWarehouse', 'electronics-spa', 'standard-gross,premium-gross'],
  ]),
]);

// ==== 02 メディア(合計 1,000 件) ====
const IMG = 430; // 商品画像の組の数(product と thumbnail で 860 件)
const svg = (code) => `/medias/${code}.svg`;
const imgRows = [];
for (const n of range(IMG)) {
  imgRows.push([`img-${pad(n, 4)}-product`, '', svg(`img-${pad(n, 4)}-product`), 'image/svg+xml', `商品画像 ${n}`, 'product']);
  imgRows.push([`img-${pad(n, 4)}-thumbnail`, '', svg(`img-${pad(n, 4)}-thumbnail`), 'image/svg+xml', `商品サムネイル ${n}`, 'thumbnail']);
}
BRANDS.forEach((b, i) => imgRows.push([`brand-logo-${pad(i + 1, 2)}`, '', svg(`brand-logo-${pad(i + 1, 2)}`), 'image/svg+xml', `${b.en} logo`, 'logo']));
const categories = [];
TOPS.forEach((t, ti) => {
  const top = `cat-${pad(ti + 1, 2)}`;
  categories.push({ code: top, ja: t.ja, en: t.en, sup: null });
  t.subs.forEach((s, si) => categories.push({ code: `${top}-${pad(si + 1, 2)}`, ja: s.ja, en: s.en, sup: top }));
});
for (const c of categories) imgRows.push([`${c.code}-pic`, '', svg(`${c.code}-pic`), 'image/svg+xml', `${c.en}`, 'category']);
const bannerRows = range(10).map((n) => [`banner-${pad(n, 2)}`, '', svg(`banner-${pad(n, 2)}`), 'image/svg+xml', `バナー ${n}`, 'banner']);
counts.Media = imgRows.length + bannerRows.length;
writeImpex('02-media.impex', '商品、ブランド、カテゴリ、バナーの画像', [
  block('INSERT_UPDATE Media;code[unique=true];$cv;URL;mime;altText;mediaFormat', imgRows),
  block('INSERT_UPDATE Media;code[unique=true];$ccv;URL;mime;altText;mediaFormat', bannerRows),
]);

// ==== 03 カテゴリ(10 + 90) ====
const catRow = (c) => [c.code, '', c.ja, c.en, `${c.ja}の商品一覧です。`, `Browse our ${c.en} range.`, pref(`${c.code}-pic`)];
const catHeader = `INSERT_UPDATE Category;code[unique=true];$cv;name[lang=ja];name[lang=en];description[lang=ja];description[lang=en];picture(code,${CV})`;
const subCats = categories.filter((c) => c.sup);
counts.Category = categories.length;
writeImpex('03-categories.impex', 'カテゴリ。上位 10 件のあとに下位 90 件', [
  block(catHeader, categories.filter((c) => !c.sup).map(catRow)),
  block(`${catHeader};supercategories(code,${CV})`, subCats.map((c) => [...catRow(c), pref(c.sup)])),
]);

// ==== 04 ブランド ====
const brands = BRANDS.map((b, i) => ({ code: `brand-${pad(i + 1, 2)}`, ...b }));
counts.Brand = brands.length;
writeImpex('04-brands.impex', 'ブランド(独自の型)', [
  block(`INSERT_UPDATE Brand;code[unique=true];name[lang=ja];name[lang=en];description[lang=ja];description[lang=en];logo(code,${CV});tier(code);active`,
    brands.map((b, i) => [b.code, b.ja, b.en, `${b.ja}の製品です。`, `Products by ${b.en}.`, pref(`brand-logo-${pad(i + 1, 2)}`),
      wpick([['STANDARD', 6], ['PREMIUM', 3], ['OUTLET', 1]]), 'true'])),
]);

// ---- 顧客(レビューと注文で使うので先に作る) ----
const customers = range(1000).map((i) => {
  const last = pick(LAST); const first = pick(FIRST);
  const b2b = i % 5 === 0; // 200 人
  return { uid: `customer${pad(i, 4)}@example.com`, id: `cust-${pad(i, 4)}`, last, first, b2b,
    approver: i % 50 === 0, unit: b2b ? `unit-${pad(((i / 5 - 1) % 100) + 1, 3)}` : '' };
});
const taro = { uid: 'taro@example.com', id: 'cust-taro', last: je('山田|Yamada'), first: je('太郎|Taro'), b2b: false, approver: false, unit: '' };
const hanako = { uid: 'hanako@example.com', id: 'cust-hanako', last: je('佐藤|Sato'), first: je('花子|Hanako'), b2b: true, approver: true, unit: 'unit-001' };
const allCustomers = [taro, hanako, ...customers];

// ==== 05 商品 ====
const products = range(1000).map((i) => {
  const code = `XYZ-${pad(i, 6)}`;
  const cat1 = pick(subCats);
  const cats = [cat1.code];
  if (chance(0.4)) { const c2 = pick(subCats); if (c2.code !== cat1.code) cats.push(c2.code); }
  const brand = pick(brands); const adj = pick(ADJS); const mat = pick(MATERIALS);
  const model = `${String.fromCharCode(65 + int(0, 25))}${int(1, 9)}${pad(int(0, 99), 2)}`;
  const f1 = pick(FEATURES); const f2 = pick(FEATURES.filter((f) => f !== f1));
  const img = ((i - 1) % IMG) + 1;
  const gallery = range(int(1, 2)).map((k) => ((img - 1 + k * 37) % IMG) + 1);
  const jpy = Math.min(198000, Math.max(980, Math.round(Math.exp(Math.log(980) + rnd() * Math.log(198000 / 980)) / 10) * 10));
  const launch = chance(0.05) ? NOW + int(1, 72) * DAY : Date.UTC(2025, 0, 1) + int(0, 656) * DAY;
  return { code, cats, brand, adj, mat, model, f1, f2, img, gallery, jpy, launch, ean: ean13(`4900${pad(i, 8)}`), ratings: [],
    ja: `${brand.ja} ${adj.ja}${cat1.ja} ${model}`, en: `${brand.en} ${adj.en} ${cat1.en} ${model}`, sub: cat1 };
});
// JAN コード(13 桁)。最後の 1 桁はチェックディジット
function ean13(d12) {
  const s = [...d12].reduce((acc, ch, k) => acc + Number(ch) * (k % 2 ? 3 : 1), 0);
  return d12 + ((10 - (s % 10)) % 10);
}

// ---- レビュー(平均評価を商品に入れるので先に作る) ----
const reviews = [];
const reviewed = new Set();
for (let k = 0; k < 2000; k++) {
  const p = k < 1000 ? products[k] : pick(products); // 最初の 1,000 件で全商品に 1 件ずつ
  let u = pick(customers);
  while (reviewed.has(`${p.code}|${u.uid}`)) u = pick(customers);
  reviewed.add(`${p.code}|${u.uid}`);
  const rating = wpick([[1, 1], [2, 1], [3, 3], [4, 6], [5, 5]]);
  const [headline, comment] = pick(REVIEW_TEXT[rating]).split('|');
  reviews.push([pref(p.code), u.uid, String(rating), headline, comment, `${u.last.ja}さん`]);
  p.ratings.push(rating);
}

const media = (n, f) => pref(`img-${pad(n, 4)}-${f}`);
counts.Product = products.length;
writeImpex('05-products.impex', '商品 1,000 件', [
  block(`INSERT_UPDATE Product;code[unique=true];$cv;name[lang=ja];name[lang=en];summary[lang=ja];summary[lang=en];description[lang=ja];description[lang=en];manufacturerName;ean;approvalStatus(code);picture(code,${CV});thumbnail(code,${CV});galleryImages(code,${CV});unit;averageRating;keywords;supercategories(code,${CV});brand(code);xyzMaterial[lang=ja];xyzMaterial[lang=en];xyzLaunchDate`,
    products.map((p) => [p.code, '', p.ja, p.en, `${p.sub.ja}。${p.f1.ja}。`, `${p.f1.en}.`,
      `${p.ja}は${p.mat.ja}を使った${p.sub.ja}です。特長は、${p.f1.ja}、${p.f2.ja}です。`,
      `The ${p.en} is a ${p.sub.en} made of ${p.mat.en}. ${p.f1.en}. ${p.f2.en}.`,
      `${p.brand.en} Corporation`, p.ean, 'approved', media(p.img, 'product'), media(p.img, 'thumbnail'),
      p.gallery.map((n) => media(n, 'product')).join(','), 'pieces',
      (p.ratings.reduce((a, b) => a + b, 0) / p.ratings.length).toFixed(1),
      [p.sub.ja, p.sub.en, p.brand.en, p.adj.en].join(','), p.cats.map(pref).join(','), p.brand.code,
      p.mat.ja, p.mat.en, fmtDate(p.launch)])),
]);

// ==== 06 価格と在庫 ====
const priceRows = products.flatMap((p) => [
  [pref(p.code), String(p.jpy), 'JPY', '1', 'false', 'pieces'],
  [pref(p.code), (p.jpy / 150).toFixed(2), 'USD', '1', 'false', 'pieces'],
]);
const stockRows = products.map((p) => {
  const r = rnd();
  if (r < 0.85) { const a = int(1, 500); return [p.code, 'xyzWarehouse', String(a), String(int(0, Math.min(5, a))), 'notSpecified']; }
  return [p.code, 'xyzWarehouse', '0', '0', r < 0.95 ? 'notSpecified' : 'forceOutOfStock'];
});
counts.PriceRow = priceRows.length; counts.StockLevel = stockRows.length;
writeImpex('06-prices-stock.impex', '価格(JPY と USD)と在庫', [
  block(`INSERT PriceRow;product(code,${CV});price;currency(isocode);minqtd;net;unit`, priceRows),
  block('INSERT_UPDATE StockLevel;productCode[unique=true];warehouse[unique=true];available;reserved;inStockStatus(code)', stockRows),
]);

// ==== 07 B2B ユニット、顧客、住所 ====
const unitRows = range(100).map((n) => [`unit-${pad(n, 3)}`, `XYZ Trading ${pad(n, 3)}`, `XYZ商事 第${n}事業所`, `XYZ Trading Office ${n}`, 'true']);
const groupsOf = (c) => ['customergroup', ...(c.b2b ? ['b2bcustomergroup'] : []), ...(c.approver ? ['b2bapprovergroup'] : [])].join(',');
const custRow = (c) => [c.uid, `${c.last.ja} ${c.first.ja}`, c.id, '{noop}Test1234!', groupsOf(c),
  ...(c.b2b ? [c.unit] : []), wpick([['BRONZE', 6], ['SILVER', 3], ['GOLD', 1]]), 'ja', 'JPY'];
const addrRows = allCustomers.map((c, k) => {
  const [town, postal, area] = TOWNS[k % TOWNS.length];
  return [c.uid, c.first.ja, c.last.ja, `${pick(STREETS)}${int(1, 5)}-${int(1, 20)}-${int(1, 30)}`,
    chance(0.5) ? `${pick(BUILDINGS)} ${int(1, 12)}0${int(1, 9)}号室` : '', town, postal,
    `${area}-${pad(int(0, 9999), 4)}-${pad(int(0, 9999), 4)}`, c.uid, 'JP', 'true', 'true'];
});
const custHead = 'INSERT_UPDATE Customer;uid[unique=true];name;customerID;encodedPassword;groups(uid)';
const custTail = 'xyzMemberRank;sessionLanguage(isocode);sessionCurrency(isocode)';
counts.B2BUnit = unitRows.length; counts.Customer = allCustomers.length; counts.Address = addrRows.length;
writeImpex('07-b2b-customers.impex', 'B2B ユニット、顧客(B2C と B2B)、住所。パスワードは全員 Test1234!', [
  block('INSERT_UPDATE B2BUnit;uid[unique=true];name;locName[lang=ja];locName[lang=en];active', unitRows),
  `# B2C の顧客\n${block(`${custHead};${custTail}`, allCustomers.filter((c) => !c.b2b).map(custRow))}`,
  `# B2B の顧客\n${block(`${custHead};defaultB2BUnit(uid);${custTail}`, allCustomers.filter((c) => c.b2b).map(custRow))}`,
  block('INSERT Address;owner(uid);firstname;lastname;line1;line2;town;postalcode;phone1;email;country(isocode);shippingAddress;billingAddress', addrRows),
]);

// ==== 08 注文 ====
const hex = () => (Math.floor(rnd() * 2 ** 32) >>> 0).toString(16).padStart(8, '0');
const guid = () => { const h = hex() + hex() + hex() + hex(); return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`; };
const orderRows = []; const entryRows = [];
for (const i of range(5000)) {
  const code = `order-${pad(i, 6)}`;
  const date = NOW - 365 * DAY + Math.floor(((i - 1 + rnd()) / 5000) * 365 * DAY / 1000) * 1000; // 古い順
  const user = i % 500 === 0 ? taro : i % 500 === 250 ? hanako : pick(customers);
  const recent = NOW - date < 14 * DAY;
  const status = user.b2b
    ? wpick([['COMPLETED', 55], ['APPROVED', 20], ['PENDING_APPROVAL', recent ? 60 : 15], ['REJECTED', 10]])
    : wpick([['COMPLETED', 88], ['CREATED', recent ? 80 : 6], ['CANCELLED', 6]]);
  const mode = chance(0.8) ? ['standard-gross', 500] : ['premium-gross', 1200];
  const used = new Set();
  let subtotal = 0;
  const lines = int(1, 3);
  for (let e = 0; e < lines; e++) {
    let p = pick(products);
    while (used.has(p.code)) p = pick(products);
    used.add(p.code);
    const qty = user.b2b ? int(1, 10) : int(1, 3);
    subtotal += p.jpy * qty;
    entryRows.push([code, String(e), pref(p.code), String(qty), String(p.jpy), String(p.jpy * qty), 'pieces']);
  }
  const total = subtotal + mode[1];
  orderRows.push([code, guid(), fmtDate(date), 'JPY', 'electronics-spa', 'electronics', status, 'false', 'true',
    String(subtotal), String(mode[1]), String(Math.round((total * 10) / 110)), String(total), mode[0], user.uid,
    user.b2b ? `account-${user.unit}` : `card-${pad(i, 6)}`]);
}
counts.Order = orderRows.length; counts.OrderEntry = entryRows.length;
writeImpex('08-orders.impex', '注文 5,000 件と明細。合計 = 明細の合計 + 送料、税 = 合計 x 10/110', [
  block('INSERT_UPDATE Order;code[unique=true];guid;date;currency(isocode);site(uid);store(uid);status(code);net;calculated;subtotal;deliveryCost;totalTax;totalPrice;deliveryMode(code);user(uid);paymentInfoCode', orderRows),
  block(`INSERT_UPDATE OrderEntry;order(code)[unique=true];entryNumber[unique=true];product(code,${CV});quantity;basePrice;totalPrice;unit`, entryRows),
]);

// ==== 09 レビュー ====
counts.CustomerReview = reviews.length;
writeImpex('09-reviews.impex', 'カスタマーレビュー 2,000 件。全商品に 1 件以上', [
  block(`INSERT CustomerReview;product(code,${CV});user(uid);rating;headline;comment;alias`, reviews),
]);

// ==== 10 CMS ====
const TEMPLATES = {
  LandingPage2Template: ['Section1', 'Section2A', 'Section2B', 'Section3', 'Section4', 'Section5'],
  ContentPage1Template: ['Section2A', 'Section2B'],
  ProductDetailsPageTemplate: ['Summary', 'UpSelling', 'CrossSelling', 'Tabs'],
  ProductListPageTemplate: ['ProductLeftRefinements', 'ProductListSlot'],
  SearchResultsListPageTemplate: ['SearchResultsListSlot'],
  CategoryPageTemplate: ['ProductLeftRefinements', 'ProductListSlot'],
  CartPageTemplate: ['TopContent', 'CenterRightContentSlot', 'EmptyCartMiddleContent'],
  MultiStepCheckoutSummaryPageTemplate: ['BodyContent', 'SideContent'],
  OrderConfirmationPageTemplate: ['BodyContent'],
};
// 部品は種類ごとに行をためる。関数は uid を返すのでスロットにそのまま並べられる
const comps = { para: [], link: [], banner: [], carousel: [], flex: [] };
const add = (kind, uid, cells) => { comps[kind].push([uid, '', uid, 'true', ...cells]); return uid; };
const para = (uid, ja, en) => add('para', uid, [ja, en]);
const link = (uid, ja, en, url) => add('link', uid, [ja, en, url]);
const banner = (uid, ja, en, n, url) => add('banner', uid, [ja, en, cref(`banner-${pad(n, 2)}`), url]);
const carousel = (uid, ja, en) => add('carousel', uid, [ja, en, range(8).map(() => pick(products).code).join(',')]);
const flex = (uid, type = uid) => add('flex', uid, [type]);

const header = {
  PreHeader: [para('PreHeaderParagraph', '5,000円以上で送料無料', 'Free shipping on orders over 5,000 yen')],
  SiteLogo: [flex('SiteLogoComponent')],
  SearchBox: [flex('SearchBoxComponent', 'SearchBox')],
  MiniCart: [flex('MiniCartComponent')],
  NavigationBar: [flex('NavigationComponent'), ...categories.filter((c) => !c.sup).map((c) => link(`NavLink-${c.code}`, c.ja, c.en, `/category/${c.code}`))],
  Footer: [flex('FooterComponent')],
};
const INFO = 'faq|よくある質問|FAQ,terms|利用規約|Terms of Use,privacy|個人情報保護方針|Privacy Policy,contact|お問い合わせ|Contact Us,about|XYZ電機について|About Us,shipping|配送について|Shipping,returns|返品について|Returns,help|ヘルプ|Help,sitemap|サイトマップ|Sitemap,company|会社概要|Company,my-account|マイアカウント|My Account,store-finder|店舗を探す|Store Finder'
  .split(',').map((s) => { const [id, ja, en] = s.split('|'); return { id, ja, en }; });
INFO.slice(0, 8).forEach((p) => header.Footer.push(link(`FooterLink-${p.id}`, p.ja, p.en, `/${p.id}`)));

// ページ。slots は 位置 → 部品 uid の配列
const pages = [
  { type: 'ContentPage', uid: 'homepage', ja: 'ホーム', en: 'Home', tpl: 'LandingPage2Template', label: 'homepage', home: true, slots: {
    Section1: [1, 2, 3].map((n) => banner(`HomeHeroBanner${n}`, `特集 ${n}`, `Feature ${n}`, n, `/campaign-${pad(n, 2)}`)),
    Section2A: [banner('HomeSplitBannerA', '新生活セール', 'New Life Sale', 4, '/campaign-04')],
    Section2B: [banner('HomeSplitBannerB', 'ゲーミング特集', 'Gaming Picks', 5, '/category/cat-07')],
    Section3: [carousel('HomeNewArrivalsCarousel', '新着商品', 'New Arrivals'), carousel('HomeBestSellersCarousel', '売れ筋ランキング', 'Best Sellers')],
    Section4: [para('HomeAboutParagraph', 'XYZ電機は家電とデジタル機器の専門店です。', 'XYZ Electronics is your store for gadgets and appliances.'), carousel('HomeRecommendCarousel', 'おすすめ', 'Recommended')],
    Section5: [para('HomeNoticeParagraph', '年末年始の配送についてのお知らせ', 'Notice about holiday shipping')] } },
  { type: 'ContentPage', uid: 'cartPage', ja: 'カート', en: 'Cart', tpl: 'CartPageTemplate', label: '/cart', slots: {
    TopContent: [flex('CartComponent')],
    CenterRightContentSlot: [para('CartTotalsNote', '送料は次の画面で確定します。', 'Shipping is calculated at checkout.'), link('ContinueShoppingLink', '買い物を続ける', 'Continue Shopping', '/')],
    EmptyCartMiddleContent: [para('EmptyCartParagraph', 'カートに商品がありません。', 'Your cart is empty.'), carousel('EmptyCartCarousel', '人気の商品', 'Popular Products')] } },
  { type: 'ContentPage', uid: 'search', ja: '検索結果', en: 'Search Results', tpl: 'SearchResultsListPageTemplate', label: 'search', slots: {
    SearchResultsListSlot: [flex('SearchResultsListComponent')] } },
  { type: 'ContentPage', uid: 'checkout', ja: 'ご注文手続き', en: 'Checkout', tpl: 'MultiStepCheckoutSummaryPageTemplate', label: '/checkout', slots: {
    BodyContent: [flex('CheckoutComponent')],
    SideContent: [para('CheckoutSideParagraph', 'お支払いは安全に処理されます。', 'Your payment is processed securely.')] } },
  { type: 'ContentPage', uid: 'orderConfirmation', ja: 'ご注文完了', en: 'Order Confirmation', tpl: 'OrderConfirmationPageTemplate', label: '/order-confirmation', slots: {
    BodyContent: [flex('OrderConfirmationComponent')] } },
  { type: 'ProductPage', uid: 'productDetails', ja: '商品詳細', en: 'Product Details', tpl: 'ProductDetailsPageTemplate', slots: {
    Summary: [flex('ProductDetailsComponent')],
    UpSelling: [carousel('UpSellingCarousel', 'こちらもおすすめ', 'You may also like')],
    CrossSelling: [carousel('CrossSellingCarousel', '一緒に買われている商品', 'Frequently bought together')],
    Tabs: [para('TabDescription', '商品説明', 'Description'), para('TabSpecification', '仕様', 'Specifications'), para('TabShipping', '配送と返品', 'Shipping and Returns')] } },
  { type: 'CategoryPage', uid: 'productList', ja: '商品一覧', en: 'Product List', tpl: 'ProductListPageTemplate', slots: {
    ProductLeftRefinements: [flex('ProductFacetNavigationComponent')],
    ProductListSlot: [flex('ProductListComponent')] } },
  { type: 'CategoryPage', uid: 'categoryLanding', ja: 'カテゴリトップ', en: 'Category Landing', tpl: 'CategoryPageTemplate', notDefault: true, slots: {
    ProductLeftRefinements: ['ProductFacetNavigationComponent'],
    ProductListSlot: [banner('CategoryBanner', 'カテゴリ特集', 'Category Feature', 6, '/campaign-06'), 'ProductListComponent'] } },
];
// 残りのページ: 案内ページ 12 件 + キャンペーン 80 件
const extra = [...INFO, ...range(80).map((n) => ({ id: `campaign-${pad(n, 2)}`, ja: `キャンペーン ${n}`, en: `Campaign ${n}` }))];
const fixedComps = () => Object.values(comps).reduce((s, a) => s + a.length, 0);
const remaining = 500 - fixedComps() - extra.length; // 各ページの Section2A の段落を引いた残り
extra.forEach((p, j) => {
  const n = Math.floor(remaining / extra.length) + (j < remaining % extra.length ? 1 : 0);
  const body = range(n).map((k) => {
    const uid = `${p.id}-item${k}`;
    if (k % 4 === 0) return banner(uid, `${p.ja} バナー`, `${p.en} Banner`, ((j + k) % 10) + 1, `/${p.id}`);
    if (k % 4 === 1) return carousel(uid, `${p.ja} の商品`, `${p.en} Products`);
    return link(uid, `${p.ja} 詳細 ${k}`, `${p.en} Details ${k}`, `/${p.id}#${k}`);
  });
  pages.push({ type: 'ContentPage', uid: p.id, ja: p.ja, en: p.en, tpl: 'ContentPage1Template', label: `/${p.id}`, slots: {
    Section2A: [para(`${p.id}-intro`, `${p.ja}のページです。`, `This is the ${p.en} page.`)], Section2B: body } });
});

const slotRows = Object.entries(header).map(([pos, list]) => [`${pos}Slot`, '', `${pos} Slot`, 'true', list.map(cref).join(',')]);
const tplSlotRows = Object.keys(TEMPLATES).flatMap((t) => Object.keys(header).map((pos) => [`csft-${pos}-${t}`, '', `${pos} for ${t}`, pos, cref(t), cref(`${pos}Slot`)]));
const pageSlotRows = [];
for (const p of pages) {
  for (const pos of TEMPLATES[p.tpl]) {
    const list = p.slots[pos];
    if (!list) throw new Error(`${p.uid} に ${pos} がない`);
    slotRows.push([`${pos}-${p.uid}`, '', `${pos} for ${p.uid}`, 'true', list.map(cref).join(',')]);
    pageSlotRows.push([`csfp-${pos}-${p.uid}`, '', `${pos} for ${p.uid}`, pos, cref(p.uid), cref(`${pos}-${p.uid}`)]);
  }
}
const pageRow = (p) => [p.uid, '', p.en, p.ja, p.en, cref(p.tpl), p.notDefault ? 'false' : 'true', 'approved'];
const H = (type, cols) => `INSERT_UPDATE ${type};uid[unique=true];$ccv;name;${cols}`;
const REF = (attr) => `${attr}(uid,${CV})`;
counts.CMSComponent = fixedComps(); counts.ContentSlot = slotRows.length; counts.Page = pages.length;
counts.ContentSlotForTemplate = tplSlotRows.length; counts.ContentSlotForPage = pageSlotRows.length;
if (counts.CMSComponent !== 500 || pages.length !== 100) throw new Error('CMS の件数が違う');
writeImpex('10-cms.impex', 'CMS: テンプレート → 部品 → スロット → ページ → スロットの割り当て', [
  block(H('PageTemplate', 'frontendTemplateName;active'), Object.keys(TEMPLATES).map((t) => [t, '', t, t, 'true'])),
  block(H('CMSParagraphComponent', 'visible;content[lang=ja];content[lang=en]'), comps.para),
  block(H('CMSLinkComponent', 'visible;linkName[lang=ja];linkName[lang=en];url'), comps.link),
  block(H('SimpleBannerComponent', `visible;headline[lang=ja];headline[lang=en];media(code,${CV});urlLink`), comps.banner),
  block(H('ProductCarouselComponent', 'visible;title[lang=ja];title[lang=en];productCodes'), comps.carousel),
  block(H('CMSFlexComponent', 'visible;flexType'), comps.flex),
  block(H('ContentSlot', `active;${REF('cmsComponents')}`), slotRows),
  block(H('ContentPage', `title[lang=ja];title[lang=en];${REF('masterTemplate')};defaultPage;approvalStatus(code);label;homepage`),
    pages.filter((p) => p.type === 'ContentPage').map((p) => [...pageRow(p), p.label, p.home ? 'true' : 'false'])),
  block(H('ProductPage', `title[lang=ja];title[lang=en];${REF('masterTemplate')};defaultPage;approvalStatus(code)`), pages.filter((p) => p.type === 'ProductPage').map(pageRow)),
  block(H('CategoryPage', `title[lang=ja];title[lang=en];${REF('masterTemplate')};defaultPage;approvalStatus(code)`), pages.filter((p) => p.type === 'CategoryPage').map(pageRow)),
  block(H('ContentSlotForTemplate', `position;${REF('pageTemplate')};${REF('contentSlot')}`), tplSlotRows),
  block(H('ContentSlotForPage', `position;${REF('page')};${REF('contentSlot')}`), pageSlotRows),
]);

console.log(`出力先: ${OUT_DIR}`);
for (const [k, v] of Object.entries(counts)) console.log(`${k.padEnd(24)} ${v}`);
