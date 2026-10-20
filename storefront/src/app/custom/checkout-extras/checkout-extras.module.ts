import { NgModule } from '@angular/core';
import { OutletPosition, provideOutlet } from '@xyz/storefront';
import { CustomSharedModule } from '../shared';
import { CheckoutHelpComponent } from './checkout-help/checkout-help.component';
import { CheckoutStepsComponent } from './checkout-steps/checkout-steps.component';
import { OrderNextStepsComponent } from './order-next-steps/order-next-steps.component';

/** チェックアウトと注文確認に足すもの(手続きの流れ、問い合わせ先、このあとの流れ)。 */
@NgModule({
  imports: [CustomSharedModule],
  declarations: [CheckoutStepsComponent, CheckoutHelpComponent, OrderNextStepsComponent],
  providers: [
    provideOutlet({ id: 'MultiStepCheckoutSummaryPageTemplate', position: OutletPosition.BEFORE, component: CheckoutStepsComponent }),
    provideOutlet({ id: 'SideContent', position: OutletPosition.AFTER, component: CheckoutHelpComponent }),
    provideOutlet({ id: 'OrderConfirmationPageTemplate', position: OutletPosition.AFTER, component: OrderNextStepsComponent }),
  ],
})
export class CheckoutExtrasModule {}
