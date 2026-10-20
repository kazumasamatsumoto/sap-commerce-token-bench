import { ChangeDetectionStrategy, Component } from '@angular/core';

/** 注文確認の画面の下(Outlet OrderConfirmationPageTemplate / AFTER)に、このあとの流れを出す。 */
@Component({
  selector: 'xyz-order-next-steps',
  standalone: false,
  templateUrl: './order-next-steps.component.html',
  styleUrl: './order-next-steps.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderNextStepsComponent {}
