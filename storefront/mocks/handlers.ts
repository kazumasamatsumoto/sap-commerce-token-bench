import { http, HttpResponse } from 'msw';
import catalogs from './fixtures/catalogs.json';
import cmsCart from './fixtures/cms-page-cart.json';
import cmsCategory from './fixtures/cms-page-category.json';
import cmsCheckout from './fixtures/cms-page-checkout.json';
import cmsHome from './fixtures/cms-page-homepage.json';
import cmsOrderConfirmation from './fixtures/cms-page-order-confirmation.json';
import cmsProduct from './fixtures/cms-page-product.json';
import cmsSearch from './fixtures/cms-page-search.json';
import search from './fixtures/products-search.json';
import { products } from './fixtures/products';

/**
 * OCC と同じ形の応答を返す MSW のハンドラー。バックエンド無しで画面を作るときに使う(npm run start:mock)。
 * 応答の元は mocks/capture-fixtures.mjs で録った JSON。カートと注文は、このファイルの中だけで覚える。
 */
const OCC = '*/occ/v2/:baseSiteId';

const cmsPages: Record<string, unknown> = {
  homepage: cmsHome,
  search: cmsSearch,
  '/cart': cmsCart,
  '/checkout': cmsCheckout,
  '/order-confirmation': cmsOrderConfirmation,
};

interface MockEntry {
  entryNumber: number;
  quantity: number;
  product: { code: string; name?: string; url?: string };
  basePrice: { value: number; formattedValue: string; currencyIso: 'JPY' };
  totalPrice: { value: number; formattedValue: string; currencyIso: 'JPY' };
}

interface MockCart {
  code: string;
  guid: string;
  entries: MockEntry[];
  deliveryMode?: { code: string; name: string; deliveryCost: { value: number; formattedValue: string } };
}

// カートと注文は sessionStorage にも置く(ページを読み直すとハンドラーのメモリが消えるため)
const STORE_KEY = 'xyz-msw-state';
const saved = typeof sessionStorage === 'undefined' ? null : sessionStorage.getItem(STORE_KEY);
const initial = saved ? (JSON.parse(saved) as { carts: [string, MockCart][]; orders: [string, unknown][] }) : { carts: [], orders: [] };
const carts = new Map<string, MockCart>(initial.carts);
const orders = new Map<string, unknown>(initial.orders);

function persist(): void {
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.setItem(STORE_KEY, JSON.stringify({ carts: [...carts], orders: [...orders] }));
  }
}
const yen = (v: number) => ({ value: v, formattedValue: `¥${v.toLocaleString('ja-JP')}`, currencyIso: 'JPY' as const });
const deliveryModes = [
  { code: 'standard-gross', name: '通常配送', deliveryCost: yen(500) },
  { code: 'premium-gross', name: 'お急ぎ便', deliveryCost: yen(1200) },
];

function cartWsDTO(cart: MockCart) {
  const subtotal = cart.entries.reduce((s, e) => s + e.totalPrice.value, 0);
  const delivery = cart.deliveryMode?.deliveryCost.value ?? 0;
  const total = subtotal + delivery;
  return {
    code: cart.code,
    guid: cart.guid,
    entries: cart.entries,
    totalItems: cart.entries.length,
    totalUnitCount: cart.entries.reduce((s, e) => s + e.quantity, 0),
    subTotal: yen(subtotal),
    deliveryCost: cart.deliveryMode ? yen(delivery) : undefined,
    totalTax: yen(Math.round((total * 10) / 110)),
    totalPrice: yen(total),
    totalPriceWithTax: yen(total),
    deliveryMode: cart.deliveryMode,
  };
}

const mockBrands = [
  { code: 'brand-01', name: 'アルテア', tier: 'STANDARD', logo: { url: '/medias/brand-logo-01.svg', altText: 'アルテア', format: 'logo' } },
  { code: 'brand-03', name: 'カイロ', tier: 'PREMIUM', logo: { url: '/medias/brand-logo-03.svg', altText: 'カイロ', format: 'logo' } },
  { code: 'brand-10', name: 'ジェネス', tier: 'OUTLET', logo: { url: '/medias/brand-logo-10.svg', altText: 'ジェネス', format: 'logo' } },
];

function cartOr400(guid: string) {
  const cart = carts.get(guid);
  return cart ?? HttpResponse.json({ errors: [{ type: 'CartError', reason: 'notFound', message: 'Cart not found.' }] }, { status: 400 });
}

export const handlers = [
  http.get(`${OCC}/cms/pages`, ({ request }) => {
    const url = new URL(request.url);
    const type = url.searchParams.get('pageType');
    if (type === 'ProductPage') {
      return HttpResponse.json(cmsProduct);
    }
    if (type === 'CategoryPage') {
      return HttpResponse.json(cmsCategory);
    }
    const page = cmsPages[url.searchParams.get('pageLabelOrId') ?? 'homepage'];
    return page
      ? HttpResponse.json(page)
      : HttpResponse.json({ errors: [{ type: 'CMSItemNotFoundError', message: 'No page found' }] }, { status: 400 });
  }),

  http.get(`${OCC}/catalogs`, () => HttpResponse.json(catalogs)),

  http.get(`${OCC}/products/search`, () => HttpResponse.json(search)),

  http.get(`${OCC}/products/:productCode`, ({ params }) => {
    const product = products[params['productCode'] as string];
    return product
      ? HttpResponse.json(product)
      : HttpResponse.json({ errors: [{ type: 'UnknownIdentifierError', message: `Product with code '${params['productCode']}' not found!` }] }, { status: 400 });
  }),

  // 自社の部品(src/app/custom)が呼ぶ端点。中身は手で書いた少しの例
  http.get(`${OCC}/products/:productCode/reviews`, () =>
    HttpResponse.json({
      reviews: [
        { id: 'mock-1', headline: '良い', comment: '満足です', rating: 5, date: '2026-10-01T10:00:00+0000', alias: '山田 太郎' },
        { id: 'mock-2', headline: 'ふつう', comment: '可もなく不可もなくです。', rating: 3, date: '2026-09-20T10:00:00+0000', alias: '加藤さん' },
      ],
    }),
  ),

  http.get(`${OCC}/brands`, () => HttpResponse.json({ brands: mockBrands })),

  http.get(`${OCC}/brands/:brandCode`, ({ params }) => {
    const brand = mockBrands.find((b) => b.code === params['brandCode']);
    return brand
      ? HttpResponse.json({ ...brand, description: `${brand.name}の製品です。` })
      : HttpResponse.json({ errors: [{ type: 'UnknownIdentifierError', message: 'Brand not found' }] }, { status: 400 });
  }),

  http.post(`${OCC}/users/:userId/carts`, () => {
    const guid = crypto.randomUUID();
    const cart: MockCart = { code: String(10000000 + carts.size), guid, entries: [] };
    carts.set(guid, cart);
    persist();
    return HttpResponse.json(cartWsDTO(cart), { status: 201 });
  }),

  http.get(`${OCC}/users/:userId/carts/:cartId`, ({ params }) => {
    const cart = cartOr400(params['cartId'] as string);
    return cart instanceof Response ? cart : HttpResponse.json(cartWsDTO(cart));
  }),

  http.post(`${OCC}/users/:userId/carts/:cartId/entries`, async ({ params, request }) => {
    const cart = cartOr400(params['cartId'] as string);
    if (cart instanceof Response) {
      return cart;
    }
    const body = (await request.json()) as { product: { code: string }; quantity?: number };
    const product = products[body.product.code] as { code: string; name?: string; url?: string; price?: { value: number } } | undefined;
    const price = product?.price?.value ?? 1000;
    const existing = cart.entries.find((e) => e.product.code === body.product.code);
    const quantity = (existing?.quantity ?? 0) + (body.quantity ?? 1);
    const entry: MockEntry = existing ?? {
      entryNumber: cart.entries.length,
      quantity: 0,
      product: { code: body.product.code, name: product?.name, url: product?.url },
      basePrice: yen(price),
      totalPrice: yen(0),
    };
    entry.quantity = quantity;
    entry.totalPrice = yen(price * quantity);
    if (!existing) {
      cart.entries.push(entry);
    }
    persist();
    return HttpResponse.json({ statusCode: 'success', quantityAdded: body.quantity ?? 1, quantity, entry });
  }),

  http.patch(`${OCC}/users/:userId/carts/:cartId/entries/:entryNumber`, async ({ params, request }) => {
    const cart = cartOr400(params['cartId'] as string);
    if (cart instanceof Response) {
      return cart;
    }
    const { quantity } = (await request.json()) as { quantity: number };
    const entry = cart.entries.find((e) => e.entryNumber === Number(params['entryNumber']));
    if (entry) {
      entry.quantity = quantity;
      entry.totalPrice = yen(entry.basePrice.value * quantity);
      cart.entries = cart.entries.filter((e) => e.quantity > 0);
    }
    persist();
    return HttpResponse.json({ statusCode: 'success', quantity });
  }),

  http.delete(`${OCC}/users/:userId/carts/:cartId/entries/:entryNumber`, ({ params }) => {
    const cart = carts.get(params['cartId'] as string);
    if (cart) {
      cart.entries = cart.entries.filter((e) => e.entryNumber !== Number(params['entryNumber']));
    }
    persist();
    return new HttpResponse(null, { status: 200 });
  }),

  http.put(`${OCC}/users/:userId/carts/:cartId/email`, () => new HttpResponse(null, { status: 200 })),
  http.put(`${OCC}/users/:userId/carts/:cartId/addresses/delivery`, () => new HttpResponse(null, { status: 200 })),
  http.get(`${OCC}/users/:userId/carts/:cartId/deliverymodes`, () => HttpResponse.json({ deliveryModes })),

  http.put(`${OCC}/users/:userId/carts/:cartId/deliverymode`, ({ params, request }) => {
    const cart = carts.get(params['cartId'] as string);
    const code = new URL(request.url).searchParams.get('deliveryModeId');
    if (cart) {
      cart.deliveryMode = deliveryModes.find((m) => m.code === code);
    }
    persist();
    return new HttpResponse(null, { status: 200 });
  }),

  http.post(`${OCC}/users/:userId/carts/:cartId/paymentdetails`, async ({ request }) => {
    const body = (await request.json()) as { cardNumber: string; accountHolderName: string };
    return HttpResponse.json({ id: 'mock-payment', accountHolderName: body.accountHolderName, cardNumber: `************${body.cardNumber.slice(-4)}` }, { status: 201 });
  }),

  http.post(`${OCC}/users/:userId/orders`, ({ request }) => {
    const cartId = new URL(request.url).searchParams.get('cartId') ?? '';
    const cart = carts.get(cartId);
    if (!cart) {
      return HttpResponse.json({ errors: [{ type: 'CartError', reason: 'notFound', message: 'Cart not found.' }] }, { status: 400 });
    }
    const order = { ...cartWsDTO(cart), code: `order-mock-${orders.size + 1}`, guid: crypto.randomUUID(), status: 'CREATED', created: new Date().toISOString() };
    orders.set(order.guid, order);
    carts.delete(cartId);
    persist();
    return HttpResponse.json(order, { status: 201 });
  }),

  http.get(`${OCC}/orders/:guid`, ({ params }) => {
    const order = orders.get(params['guid'] as string);
    return order ? HttpResponse.json(order) : HttpResponse.json({ errors: [{ type: 'UnknownIdentifierError', message: 'Order not found' }] }, { status: 400 });
  }),
];
