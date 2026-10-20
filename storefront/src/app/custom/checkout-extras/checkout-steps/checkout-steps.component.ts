import { ChangeDetectionStrategy, Component } from '@angular/core';

/** チェックアウトの画面の上(Outlet MultiStepCheckoutSummaryPageTemplate / BEFORE)に、手続きの流れを出す。 */
@Component({
  selector: 'xyz-checkout-steps',
  standalone: false,
  templateUrl: './checkout-steps.component.html',
  styleUrl: './checkout-steps.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckoutStepsComponent {
  readonly steps = ['address', 'deliveryMode', 'payment', 'review'];
}
