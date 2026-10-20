import { NgModule } from '@angular/core';
import { OutletPosition, provideOutlet } from '@xyz/storefront';
import { CustomSharedModule } from '../shared';
import { BrandBadgeComponent } from './brand-badge/brand-badge.component';
import { NewArrivalBadgeComponent } from './new-arrival-badge/new-arrival-badge.component';
import { ProductBadgesComponent } from './product-badges/product-badges.component';
import { StockBadgeComponent } from './stock-badge/stock-badge.component';

/** 商品の札(ブランド、新着、残りわずか)。商品詳細の価格の前に出す。 */
@NgModule({
  imports: [CustomSharedModule],
  declarations: [ProductBadgesComponent, BrandBadgeComponent, NewArrivalBadgeComponent, StockBadgeComponent],
  exports: [BrandBadgeComponent, NewArrivalBadgeComponent],
  providers: [provideOutlet({ id: 'PDP.PRICE', position: OutletPosition.BEFORE, component: ProductBadgesComponent })],
})
export class ProductBadgesModule {}
