import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Stock } from '@xyz/storefront';

/** 在庫が少ない商品に「残りわずか」の札を出す(在庫の文言そのものは標準の部品が出す)。 */
@Component({
  selector: 'xyz-stock-badge',
  standalone: false,
  templateUrl: './stock-badge.component.html',
  styleUrl: './stock-badge.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StockBadgeComponent {
  readonly stock = input<Stock | undefined>();
}
