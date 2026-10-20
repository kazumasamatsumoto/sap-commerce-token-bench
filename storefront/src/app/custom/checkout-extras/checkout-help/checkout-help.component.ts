import { ChangeDetectionStrategy, Component } from '@angular/core';

/** チェックアウトと注文確認の横(Outlet SideContent / AFTER)に、問い合わせ先を出す。 */
@Component({
  selector: 'xyz-checkout-help',
  standalone: false,
  templateUrl: './checkout-help.component.html',
  styleUrl: './checkout-help.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckoutHelpComponent {}
