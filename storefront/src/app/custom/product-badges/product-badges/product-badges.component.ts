import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CurrentProductService } from '../../shared';

/** 商品詳細の価格の前(Outlet PDP.PRICE / BEFORE)に、ブランド・新着・在庫の札を並べる。 */
@Component({
  selector: 'xyz-product-badges',
  standalone: false,
  templateUrl: './product-badges.component.html',
  styleUrl: './product-badges.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductBadgesComponent {
  readonly product$ = inject(CurrentProductService).getProduct();
}
