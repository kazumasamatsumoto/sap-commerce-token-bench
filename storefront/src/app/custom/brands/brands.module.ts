import { NgModule } from '@angular/core';
import { OutletPosition, provideConfig, provideOutlet } from '@xyz/storefront';
import { CustomSharedModule } from '../shared';
import { BrandInfoComponent } from './brand-info/brand-info.component';
import { BrandLogoComponent } from './brand-logo/brand-logo.component';
import { BrandStripComponent } from './brand-strip/brand-strip.component';
import { BrandAdapter } from './connectors/brand.adapter';
import { OccBrandAdapter } from './occ/occ-brand.adapter';

/**
 * ブランド(自社拡張 xyzocc の /brands)。トップのロゴの帯と、商品詳細の「ブランドについて」。
 * Adapter → Connector → Facade(BrandService)の順に呼ぶ。
 */
@NgModule({
  imports: [CustomSharedModule],
  declarations: [BrandStripComponent, BrandLogoComponent, BrandInfoComponent],
  providers: [
    provideConfig({ backend: { occ: { endpoints: { xyzBrands: 'brands?fields=DEFAULT' } } } }),
    { provide: BrandAdapter, useClass: OccBrandAdapter },
    provideOutlet({ id: 'LandingPage2Template', position: OutletPosition.AFTER, component: BrandStripComponent }),
    provideOutlet({ id: 'UpSelling', position: OutletPosition.BEFORE, component: BrandInfoComponent }),
  ],
})
export class BrandsModule {}
