// 本物の(疑似 Commerce の)OCC から応答を録って、MSW のモックが返す JSON にする。
// 使い方: node mocks/capture-fixtures.mjs [http://localhost:9002]
// 出力: mocks/fixtures/*.json(コミットする)。疑似 Commerce のデータを作り直したら、録り直す。
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const base = `${(process.argv[2] ?? "http://localhost:9002").replace(/\/$/, "")}/occ/v2/electronics-spa`;
const out = join(dirname(fileURLToPath(import.meta.url)), "fixtures");
mkdirSync(out, { recursive: true });

const PRODUCT_LIST_FIELDS =
  "products(code,name,summary,price(FULL),images(DEFAULT),stock(FULL),averageRating,brand(BASIC),newArrival),facets,breadcrumbs,pagination(DEFAULT),sorts(DEFAULT),freeTextSearch,currentQuery";

const targets = {
  "cms-page-homepage": "/cms/pages?pageType=ContentPage&pageLabelOrId=homepage",
  "cms-page-search": "/cms/pages?pageType=ContentPage&pageLabelOrId=search",
  "cms-page-cart": "/cms/pages?pageType=ContentPage&pageLabelOrId=/cart",
  "cms-page-checkout": "/cms/pages?pageType=ContentPage&pageLabelOrId=/checkout",
  "cms-page-order-confirmation": "/cms/pages?pageType=ContentPage&pageLabelOrId=/order-confirmation",
  "cms-page-product": "/cms/pages?pageType=ProductPage&code=XYZ-000001",
  "cms-page-category": "/cms/pages?pageType=CategoryPage&code=cat-01",
  catalogs: "/catalogs",
  "products-search": `/products/search?query=&pageSize=12&fields=${PRODUCT_LIST_FIELDS}`,
};
for (let i = 1; i <= 12; i++) {
  const code = `XYZ-${String(i).padStart(6, "0")}`;
  targets[`product-${code}`] = `/products/${code}?fields=FULL`;
}

for (const [name, path] of Object.entries(targets)) {
  const res = await fetch(base + path);
  if (!res.ok) {
    console.error(`NG ${res.status} ${path}`);
    process.exitCode = 1;
    continue;
  }
  writeFileSync(join(out, `${name}.json`), JSON.stringify(await res.json(), null, 2) + "\n");
  console.log(`saved ${name}.json`);
}
