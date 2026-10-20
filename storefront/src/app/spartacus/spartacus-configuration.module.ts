import { NgModule } from '@angular/core';
import { provideConfig } from '@xyz/storefront';
import { translations } from '../../assets/i18n';

/** サイトの設定。Spartacus のアプリで schematics が作る spartacus-configuration.module.ts と同じ置き場所。 */
@NgModule({
  providers: [
    provideConfig({
      backend: {
        occ: {
          baseUrl: 'http://localhost:9002',
          prefix: '/occ/v2/',
        },
      },
      context: {
        baseSite: ['electronics-spa'],
        language: ['ja', 'en'],
        currency: ['JPY', 'USD'],
      },
      i18n: {
        resources: translations,
        fallbackLang: 'en',
      },
    }),
  ],
})
export class SpartacusConfigurationModule {}
