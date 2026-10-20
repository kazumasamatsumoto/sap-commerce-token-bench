import { NgModule } from '@angular/core';
import { OutletPosition, provideOutlet } from '@xyz/storefront';
import { CustomSharedModule } from '../shared';
import { MaterialInfoComponent } from './material-info/material-info.component';
import { ProductRatingSummaryComponent } from './product-rating-summary/product-rating-summary.component';
import { ShareLinkComponent } from './share-link/share-link.component';
import { ShippingPolicyNoteComponent } from './shipping-policy-note/shipping-policy-note.component';
import { TaxNoteComponent } from './tax-note/tax-note.component';

/** 商品詳細に足す情報(評価、税込、素材、送料と返品、URL のコピー)。 */
@NgModule({
  imports: [CustomSharedModule],
  declarations: [MaterialInfoComponent, ProductRatingSummaryComponent, TaxNoteComponent, ShippingPolicyNoteComponent, ShareLinkComponent],
  providers: [
    provideOutlet({ id: 'PDP.PRICE', position: OutletPosition.AFTER, component: TaxNoteComponent }),
    provideOutlet({ id: 'PDP.PRICE', position: OutletPosition.AFTER, component: ProductRatingSummaryComponent }),
    provideOutlet({ id: 'PDP.ADDTOCART', position: OutletPosition.AFTER, component: MaterialInfoComponent }),
    provideOutlet({ id: 'PDP.ADDTOCART', position: OutletPosition.AFTER, component: ShippingPolicyNoteComponent }),
    provideOutlet({ id: 'PDP.ADDTOCART', position: OutletPosition.AFTER, component: ShareLinkComponent }),
  ],
})
export class ProductInfoModule {}
