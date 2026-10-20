// OCC の 50 本を順に呼んで、期待した状態コードが返るかを確かめる(疑似 Commerce が動いている必要がある)。
// 使い方: node tools/occ-smoke.mjs [http://localhost:9002]
// ゲストの購入とログインした人の購入を、カートの作成から注文の確定まで通す。npm のパッケージは使わない。

const base = (process.argv[2] ?? "http://localhost:9002").replace(/\/$/, "");
const occ = `${base}/occ/v2`;
const site = "electronics-spa";
const results = [];

async function call(name, method, path, { expect = 200, token, body, form } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (form) {
    headers["Content-Type"] = "application/x-www-form-urlencoded";
    payload = new URLSearchParams(form).toString();
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }
  const url = path.startsWith("http") ? path : `${occ}${path}`;
  const res = await fetch(url, { method, headers, body: payload });
  const text = await res.text();
  let json;
  try {
    json = text ? JSON.parse(text) : undefined;
  } catch {
    json = undefined;
  }
  const ok = res.status === expect;
  results.push({ name, method, path, status: res.status, ok });
  if (!ok) console.log(`NG  ${method} ${path} -> ${res.status} (期待 ${expect}) ${text.slice(0, 300)}`);
  return json;
}

const s = `/${site}`;

// 1. 誰でも見られるもの
await call("basesites", "GET", "/basesites");
const search = await call("products/search", "GET", `${s}/products/search?query=&pageSize=5&fields=DEFAULT`);
const code = search?.products?.[0]?.code ?? "XYZ-000001";
await call("products/search (category)", "GET", `${s}/products/search?query=:price-asc:category:cat-01&pageSize=5`);
await call("products/suggestions", "GET", `${s}/products/suggestions?term=${encodeURIComponent("スマ")}&max=5`);
await call("products/{code}", "GET", `${s}/products/${code}?fields=FULL`);
await call("products/{code}/stock", "GET", `${s}/products/${code}/stock`);
await call("products/{code}/reviews", "GET", `${s}/products/${code}/reviews?maxCount=3`);
await call("products/{code} (unknown)", "GET", `${s}/products/NO-SUCH`, { expect: 400 });
await call("catalogs", "GET", `${s}/catalogs`);
await call("catalogs/.../categories/{id}", "GET", `${s}/catalogs/electronicsProductCatalog/Online/categories/cat-01`);
await call("categories/{id}/products", "GET", `${s}/categories/cat-01/products?pageSize=3`);
await call("languages", "GET", `${s}/languages`);
await call("currencies", "GET", `${s}/currencies`);
await call("countries", "GET", `${s}/countries`);
await call("titles", "GET", `${s}/titles`);
await call("cardtypes", "GET", `${s}/cardtypes`);
await call("cms/pages (home)", "GET", `${s}/cms/pages?pageType=ContentPage&pageLabelOrId=homepage`);
await call("cms/pages (product)", "GET", `${s}/cms/pages?pageType=ProductPage&code=${code}`);
await call("cms/pages/{id}", "GET", `${s}/cms/pages/cart`);
const home = await call("cms/pages (for ids)", "GET", `${s}/cms/pages?pageType=ContentPage&pageLabelOrId=homepage`);
const componentId = home?.contentSlots?.contentSlot?.flatMap((x) => x.components?.component ?? [])?.[0]?.uid ?? "none";
await call("cms/components/{id}", "GET", `${s}/cms/components/${componentId}`);
await call("cms/components", "GET", `${s}/cms/components?componentIds=${componentId}`);
await call("brands", "GET", `${s}/brands`);
const brands = await call("brands (for code)", "GET", `${s}/brands?fields=BASIC`);
await call("brands/{code}", "GET", `${s}/brands/${brands?.brands?.[0]?.code ?? "none"}?fields=FULL`);
await call("unknown site", "GET", "/no-such-site/products/search", { expect: 400 });

// 2. ゲストの購入
const guestCart = await call("carts (anonymous, create)", "POST", `${s}/users/anonymous/carts`, { expect: 201 });
const g = guestCart?.guid;
await call("carts/{id}/entries (add)", "POST", `${s}/users/anonymous/carts/${g}/entries`, { body: { product: { code }, quantity: 2 } });
await call("carts/{id}/entries", "GET", `${s}/users/anonymous/carts/${g}/entries`);
await call("carts/{id}/entries/{n} (update)", "PATCH", `${s}/users/anonymous/carts/${g}/entries/0`, { body: { quantity: 1 } });
await call("carts/{id}/email", "PUT", `${s}/users/anonymous/carts/${g}/email?email=guest@example.com`);
const address = { firstName: "太郎", lastName: "山田", line1: "千代田1-1", town: "千代田区", postalCode: "100-0001", country: { isocode: "JP" } };
await call("carts/{id}/addresses/delivery", "PUT", `${s}/users/anonymous/carts/${g}/addresses/delivery`, { body: address });
await call("carts/{id}/deliverymodes", "GET", `${s}/users/anonymous/carts/${g}/deliverymodes`);
await call("carts/{id}/deliverymode (set)", "PUT", `${s}/users/anonymous/carts/${g}/deliverymode?deliveryModeId=standard-gross`);
await call("carts/{id}/deliverymode", "GET", `${s}/users/anonymous/carts/${g}/deliverymode`);
const card = { accountHolderName: "TARO YAMADA", cardNumber: "4111111111111111", expiryMonth: "12", expiryYear: "2030" };
await call("carts/{id}/paymentdetails", "POST", `${s}/users/anonymous/carts/${g}/paymentdetails`, { body: card, expect: 201 });
await call("carts/{id}", "GET", `${s}/users/anonymous/carts/${g}?fields=FULL`);
const guestOrder = await call("orders (place, guest)", "POST", `${s}/users/anonymous/orders?cartId=${g}`, { expect: 201 });
await call("orders/{guid} (guest)", "GET", `${s}/orders/${guestOrder?.guid}`);

// 3. ログインした人の購入
const tokenRes = await fetch(`${base}/authorizationserver/oauth/token`, {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({ grant_type: "password", client_id: "mobile_android", client_secret: "secret", username: "taro@example.com", password: "Test1234!" }),
});
results.push({ name: "oauth/token (password)", method: "POST", path: "/authorizationserver/oauth/token", status: tokenRes.status, ok: tokenRes.status === 200 });
const token = (await tokenRes.json()).access_token;
await call("users/current (no token)", "GET", `${s}/users/current`, { expect: 401 });
await call("users/{id}", "GET", `${s}/users/current`, { token });
await call("users/{id} (update)", "PATCH", `${s}/users/current`, { token, body: { firstName: "太郎" } });
await call("users/{other} (forbidden)", "GET", `${s}/users/hanako@example.com`, { token, expect: 403 });
const created = await call("users/{id}/addresses (create)", "POST", `${s}/users/current/addresses`, { token, body: address, expect: 201 });
await call("users/{id}/addresses", "GET", `${s}/users/current/addresses`, { token });
await call("users/{id}/addresses/{a}", "GET", `${s}/users/current/addresses/${created?.id}`, { token });
await call("users/{id}/addresses/{a} (update)", "PATCH", `${s}/users/current/addresses/${created?.id}`, { token, body: { line2: "101" } });
const cart = await call("carts (create)", "POST", `${s}/users/current/carts`, { token, expect: 201 });
await call("carts", "GET", `${s}/users/current/carts`, { token });
await call("carts/{id}/entries (add, member)", "POST", `${s}/users/current/carts/${cart?.code}/entries`, { token, body: { product: { code }, quantity: 1 } });
await call("carts/{id}/addresses/delivery (by id)", "PUT", `${s}/users/current/carts/${cart?.code}/addresses/delivery?addressId=${created?.id}`, { token });
await call("carts/{id}/deliverymode (member)", "PUT", `${s}/users/current/carts/${cart?.code}/deliverymode?deliveryModeId=premium-gross`, { token });
await call("carts/{id}/paymentdetails (member)", "POST", `${s}/users/current/carts/${cart?.code}/paymentdetails`, { token, body: card, expect: 201 });
await call("orders (place, empty cart id)", "POST", `${s}/users/current/orders?cartId=NO-SUCH`, { token, expect: 400 });
const order = await call("orders (place)", "POST", `${s}/users/current/orders?cartId=${cart?.code}`, { token, expect: 201 });
await call("orders", "GET", `${s}/users/current/orders?pageSize=5`, { token });
await call("orders/{code}", "GET", `${s}/users/current/orders/${order?.code}`, { token });
await call("products/{code}/reviews (post)", "POST", `${s}/products/${code}/reviews`, { token, body: { headline: "良い", comment: "満足です", rating: 5 }, expect: 201 });
const extraCart = await call("carts (create, to delete)", "POST", `${s}/users/current/carts`, { token, expect: 201 });
await call("carts/{id} (delete)", "DELETE", `${s}/users/current/carts/${extraCart?.code}`, { token });
await call("carts/{id}/addresses/delivery (remove)", "DELETE", `${s}/users/anonymous/carts/${g}/addresses/delivery`, { expect: 400 });
await call("carts/{id}/entries/{n} (delete)", "DELETE", `${s}/users/current/carts/${cart?.code}/entries/0`, { token, expect: 400 });
await call("users/{id}/addresses/{a} (delete)", "DELETE", `${s}/users/current/addresses/${created?.id}`, { token });
await call("users (register)", "POST", `${s}/users`, { body: { uid: `smoke-${Date.now()}@example.com`, password: "Test1234!", firstName: "花子", lastName: "鈴木" }, expect: 201 });
await call("orgUnits (B2C user)", "GET", `${s}/orgUnits`, { token, expect: 403 });

// 4. B2B
const b2bToken = (await (await fetch(`${base}/authorizationserver/oauth/token`, {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({ grant_type: "password", client_id: "mobile_android", client_secret: "secret", username: "hanako@example.com", password: "Test1234!" }),
})).json()).access_token;
await call("orgUnits", "GET", `${s}/orgUnits`, { token: b2bToken });
await call("orgUnits/{id}", "GET", `${s}/orgUnits/unit-001`, { token: b2bToken });

const ng = results.filter((r) => !r.ok);
const endpoints = new Set(results.map((r) => `${r.method} ${r.path.replace(/\?.*$/, "").replace(/\/(XYZ-\d+|NO-SUCH|[0-9a-f-]{36}|\d{8}-[0-9a-f]{4}|order-[\w-]+|\d{10,}|unit-\d+|cat-[\d-]+|[a-z]+-[a-z0-9-]+|comp\w*)/g, "/{id}")}`));
console.log(`\n${results.length} 回呼んで ${results.length - ng.length} 回が期待どおり、${ng.length} 回が違った。呼んだ端点の種類はおよそ ${endpoints.size}。`);
process.exit(ng.length === 0 ? 0 : 1);
