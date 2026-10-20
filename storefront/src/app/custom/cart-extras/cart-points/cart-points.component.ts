import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActiveCartService } from '@xyz/storefront';
import { map } from 'rxjs';
import { earnedPoints } from '../free-shipping';

/** カートの右側(Outlet CenterRightContentSlot / AFTER)に、このご注文で付くポイント(小計の 1%)を出す。 */
@Component({
  selector: 'xyz-cart-points',
  standalone: false,
  templateUrl: './cart-points.component.html',
  styleUrl: './cart-points.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CartPointsComponent {
  readonly points$ = inject(ActiveCartService)
    .getActive()
    .pipe(map((cart) => (cart?.entries?.length ? earnedPoints(cart.subTotal?.value) : undefined)));
}
