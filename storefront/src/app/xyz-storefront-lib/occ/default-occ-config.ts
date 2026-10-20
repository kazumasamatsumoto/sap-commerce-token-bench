import { OccConfig } from './occ-config';

/**
 * OCC の端点の既定値。Spartacus の defaultOccProductConfig などと同じ書き方で、
 * fields= の段階(BASIC / DEFAULT / FULL)も端点ごとに決める。
 */
export const defaultOccConfig: OccConfig = {
  backend: {
    occ: {
      baseUrl: 'http://localhost:9002',
      prefix: '/occ/v2/',
      endpoints: {
        // 一覧・検索は fields を絞る(本物の Spartacus と同じく、一覧に要る項目だけ)
        productSearchList:
          'products/search?fields=products(code,name,summary,price(FULL),images(DEFAULT),stock(FULL),averageRating,brand(BASIC),newArrival),facets,breadcrumbs,pagination(DEFAULT),sorts(DEFAULT),freeTextSearch,currentQuery',
        product: 'products/${productCode}?fields=FULL',
        productSearch: 'products/search?fields=products(code,name,summary,price(FULL),images(DEFAULT),stock(FULL),averageRating),facets,breadcrumbs,pagination(DEFAULT),sorts(DEFAULT),freeTextSearch',
        productReviews: 'products/${productCode}/reviews',
        pages: 'cms/pages?fields=DEFAULT',
        page: 'cms/pages/${id}?fields=DEFAULT',
        components: 'cms/components?fields=DEFAULT',
        carts: 'users/${userId}/carts?fields=carts(DEFAULT,potentialProductPromotions,appliedOrderPromotions,totalUnitCount,deliveryItemsQuantity,entries(totalPrice(formattedValue),product(images(FULL),stock(FULL)),basePrice(formattedValue,value)))',
        cart: 'users/${userId}/carts/${cartId}?fields=DEFAULT,potentialProductPromotions,appliedOrderPromotions,totalUnitCount,deliveryItemsQuantity,entries(totalPrice(formattedValue),product(images(FULL),stock(FULL)),basePrice(formattedValue,value))',
        createCart: 'users/${userId}/carts?fields=DEFAULT',
        addEntries: 'users/${userId}/carts/${cartId}/entries',
        updateEntries: 'users/${userId}/carts/${cartId}/entries/${entryNumber}',
        removeEntries: 'users/${userId}/carts/${cartId}/entries/${entryNumber}',
        setDeliveryAddress: 'users/${userId}/carts/${cartId}/addresses/delivery',
        deliveryModes: 'users/${userId}/carts/${cartId}/deliverymodes',
        setDeliveryMode: 'users/${userId}/carts/${cartId}/deliverymode',
        paymentDetails: 'users/${userId}/carts/${cartId}/paymentdetails',
        cartEmail: 'users/${userId}/carts/${cartId}/email',
        placeOrder: 'users/${userId}/orders?fields=FULL',
        guestOrder: 'orders/${guid}?fields=FULL',
        catalogs: 'catalogs',
        brand: 'brands/${brandCode}?fields=FULL',
        order: 'users/${userId}/orders/${orderCode}?fields=FULL',
        orderHistory: 'users/${userId}/orders',
        user: 'users/${userId}',
        addresses: 'users/${userId}/addresses',
        titles: 'titles',
        countries: 'countries',
        regions: 'countries/${isoCode}/regions',
        languages: 'languages',
        currencies: 'currencies',
      },
    },
    media: {
      baseUrl: 'http://localhost:9002',
    },
  },
  context: {
    baseSite: ['electronics-spa'],
    language: ['ja', 'en'],
    currency: ['JPY', 'USD'],
  },
};
