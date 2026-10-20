import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FREE_SHIPPING_THRESHOLD } from '../../cart-extras/free-shipping';
import { formatYen } from '../../shared';

/** フッターの上(Outlet Footer / BEFORE)に、お店の約束を 3 つ並べる。 */
@Component({
  selector: 'xyz-trust-badges',
  standalone: false,
  templateUrl: './trust-badges.component.html',
  styleUrl: './trust-badges.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrustBadgesComponent {
  readonly threshold = formatYen(FREE_SHIPPING_THRESHOLD);
  readonly badges = ['warranty', 'freeShipping', 'securePayment'];
}
