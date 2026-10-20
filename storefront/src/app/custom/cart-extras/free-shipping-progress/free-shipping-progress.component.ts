import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActiveCartService, PageContextService } from '@xyz/storefront';
import { combineLatest, map } from 'rxjs';
import { formatYen } from '../../shared';
import { freeShippingStatus } from '../free-shipping';

/**
 * カートの画面(Outlet TopContent / AFTER)に、送料無料(5,000 円)まであといくらかを出す。
 * TopContent はチェックアウトの画面にもあるので、カートの画面(/cart)だけで出す。
 */
@Component({
  selector: 'xyz-free-shipping-progress',
  standalone: false,
  templateUrl: './free-shipping-progress.component.html',
  styleUrl: './free-shipping-progress.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FreeShippingProgressComponent {
  readonly view$ = combineLatest([inject(PageContextService).context$, inject(ActiveCartService).getActive()]).pipe(
    map(([ctx, cart]) => {
      if (ctx.id !== '/cart' || !cart?.entries?.length) {
        return undefined;
      }
      const status = freeShippingStatus(cart.subTotal?.value);
      return { ...status, remainingText: formatYen(status.remaining) };
    }),
  );
}
