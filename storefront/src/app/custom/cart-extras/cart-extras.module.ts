import { NgModule } from '@angular/core';
import { OutletPosition, provideOutlet } from '@xyz/storefront';
import { CustomSharedModule } from '../shared';
import { CartPointsComponent } from './cart-points/cart-points.component';
import { FreeShippingProgressComponent } from './free-shipping-progress/free-shipping-progress.component';
import { SecureCheckoutNoteComponent } from './secure-checkout-note/secure-checkout-note.component';

/** カートの画面に足すもの(送料無料までの残り、ポイント、安心のひとこと)。 */
@NgModule({
  imports: [CustomSharedModule],
  declarations: [FreeShippingProgressComponent, SecureCheckoutNoteComponent, CartPointsComponent],
  providers: [
    provideOutlet({ id: 'TopContent', position: OutletPosition.AFTER, component: FreeShippingProgressComponent }),
    provideOutlet({ id: 'CenterRightContentSlot', position: OutletPosition.BEFORE, component: SecureCheckoutNoteComponent }),
    provideOutlet({ id: 'CenterRightContentSlot', position: OutletPosition.AFTER, component: CartPointsComponent }),
  ],
})
export class CartExtrasModule {}
