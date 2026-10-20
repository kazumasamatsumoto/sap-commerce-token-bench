import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CurrentProductService } from '../../shared';

/** 価格の後(PDP.PRICE / AFTER)に、星とレビューの件数を出す。押すとレビューの欄へ移る。 */
@Component({
  selector: 'xyz-product-rating-summary',
  standalone: false,
  templateUrl: './product-rating-summary.component.html',
  styleUrl: './product-rating-summary.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductRatingSummaryComponent {
  private readonly document = inject(DOCUMENT);
  readonly product$ = inject(CurrentProductService).getProduct();

  /** レビューの欄(xyz-product-reviews)へ移る。押されるのはブラウザだけなので window は使わない。 */
  goToReviews(): void {
    const target = this.document.getElementById('xyz-reviews');
    target?.scrollIntoView({ behavior: 'smooth' });
    target?.focus();
  }
}
