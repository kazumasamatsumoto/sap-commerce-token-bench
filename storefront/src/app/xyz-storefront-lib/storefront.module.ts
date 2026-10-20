import { provideHttpClient, withFetch } from '@angular/common/http';
import { ModuleWithProviders, NgModule } from '@angular/core';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { CartAdapter } from './cart/connectors/cart.adapter';
import { OccCartAdapter } from './cart/occ/occ-cart.adapter';
import { CartEffects } from './cart/store/cart.effects';
import { CART_FEATURE, cartReducer } from './cart/store/cart.reducer';
import { CheckoutAdapter } from './checkout/connectors/checkout.adapter';
import { OccCheckoutAdapter } from './checkout/occ/occ-checkout.adapter';
import { CmsPageAdapter } from './cms/connectors/cms-page.adapter';
import { OccCmsPageAdapter } from './cms/occ/occ-cms-page.adapter';
import { CmsComponentsModule } from './cms-components/cms-components.module';
import { configProviders, provideDefaultConfig } from './config/config';
import { defaultLayoutConfig } from './layout/layout-config';
import { defaultOccConfig } from './occ/default-occ-config';
import { ProductModule } from './product/product.module';
import { StorefrontComponentsModule } from './storefront-components.module';

/** Spartacus の StorefrontModule に当たる。アプリの最上位で 1 回だけ forRoot() する。 */
@NgModule({
  imports: [
    StoreModule.forRoot({}),
    EffectsModule.forRoot([]),
    StoreModule.forFeature(CART_FEATURE, cartReducer),
    EffectsModule.forFeature([CartEffects]),
    ProductModule,
    StorefrontComponentsModule,
    CmsComponentsModule,
  ],
  exports: [StorefrontComponentsModule, CmsComponentsModule],
})
export class StorefrontModule {
  static forRoot(): ModuleWithProviders<StorefrontModule> {
    return {
      ngModule: StorefrontModule,
      providers: [
        provideHttpClient(withFetch()),
        ...configProviders,
        provideDefaultConfig(defaultOccConfig),
        provideDefaultConfig(defaultLayoutConfig),
        { provide: CmsPageAdapter, useClass: OccCmsPageAdapter },
        { provide: CartAdapter, useClass: OccCartAdapter },
        { provide: CheckoutAdapter, useClass: OccCheckoutAdapter },
      ],
    };
  }
}
