import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { XyzProduct } from '../../models/custom-product.model';

/** 小さな商品カード(最近見た商品、お気に入りで使う)。 */
@Component({
  selector: 'xyz-product-card',
  standalone: false,
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductCardComponent {
  readonly product = input.required<XyzProduct>();
}
