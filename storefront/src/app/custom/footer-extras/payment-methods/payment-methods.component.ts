import { ChangeDetectionStrategy, Component } from '@angular/core';

/** フッター(Outlet Footer / AFTER)に、使えるお支払い方法を並べる。 */
@Component({
  selector: 'xyz-payment-methods',
  standalone: false,
  templateUrl: './payment-methods.component.html',
  styleUrl: './payment-methods.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentMethodsComponent {
  readonly methods = ['card', 'convenience', 'bank'];
}
