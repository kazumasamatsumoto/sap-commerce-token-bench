import { Provider } from '@angular/core';
import { configProviders, provideConfig } from '@xyz/storefront';
import { translations } from '../../../assets/i18n';

export const TEST_OCC_BASE = 'http://occ.test/occ/v2/electronics-spa';

/**
 * 自社の部品の単体テスト用の設定(Config、OCC の端点、翻訳)。
 * 足したい設定があれば chunks で渡す。
 */
export function provideCustomTesting(...chunks: object[]): Provider[] {
  return [
    ...configProviders,
    provideConfig({
      backend: {
        occ: {
          baseUrl: 'http://occ.test',
          prefix: '/occ/v2/',
          endpoints: {
            brand: 'brands/${brandCode}?fields=FULL',
            xyzBrands: 'brands?fields=DEFAULT',
            xyzProductReviews: 'products/${productCode}/reviews?fields=DEFAULT',
          },
        },
      },
      context: { baseSite: ['electronics-spa'], language: ['ja', 'en'], currency: ['JPY'] },
      i18n: { resources: translations, fallbackLang: 'en' },
    }),
    ...chunks.map((c) => provideConfig(c)),
  ];
}
