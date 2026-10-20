import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FREE_SHIPPING_THRESHOLD } from '../../cart-extras/free-shipping';
import { formatYen } from '../../shared';

/**
 * 送料と返品の決まり(固定の文言)。カートのボタンの後(PDP.ADDTOCART / AFTER)に出す。
 * 届く日の目安は出さない(配送予定日は別の機能で扱う)。
 */
@Component({
  selector: 'xyz-shipping-policy-note',
  standalone: false,
  templateUrl: './shipping-policy-note.component.html',
  styleUrl: './shipping-policy-note.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShippingPolicyNoteComponent {
  readonly threshold = formatYen(FREE_SHIPPING_THRESHOLD);
}
