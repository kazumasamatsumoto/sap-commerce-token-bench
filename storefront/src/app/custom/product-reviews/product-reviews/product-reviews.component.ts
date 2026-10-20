import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { of, switchMap } from 'rxjs';
import { CurrentProductService, Review } from '../../shared';
import { ProductReviewService } from '../facade/product-review.service';
import { ReviewSort } from '../review-sort/review-sort.component';

/** 最初に見せる件数。残りは「もっと見る」で出す。 */
export const REVIEW_PAGE_SIZE = 5;

/** レビューを並べ替える(元の配列は変えない)。 */
export function sortReviews(reviews: Review[], sort: ReviewSort): Review[] {
  const list = [...reviews];
  switch (sort) {
    case 'highest':
      return list.sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0));
    case 'lowest':
      return list.sort((a, b) => (a.rating ?? 0) - (b.rating ?? 0));
    default:
      return list.sort((a, b) => (b.date ?? '').localeCompare(a.date ?? ''));
  }
}

/**
 * 商品詳細のレビューの欄(Outlet Tabs / AFTER)。OCC の GET /products/{code}/reviews を読む。
 * 評価の内訳、並べ替え、一覧、もっと見る。
 */
@Component({
  selector: 'xyz-product-reviews',
  standalone: false,
  templateUrl: './product-reviews.component.html',
  styleUrl: './product-reviews.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductReviewsComponent {
  private readonly currentProduct = inject(CurrentProductService);
  private readonly reviewService = inject(ProductReviewService);

  readonly code = toSignal(this.currentProduct.getCode());
  readonly reviews = toSignal(
    this.currentProduct.getCode().pipe(switchMap((code) => (code ? this.reviewService.getByProductCode(code) : of<Review[]>([])))),
    { initialValue: [] as Review[] },
  );
  readonly sort = signal<ReviewSort>('newest');
  readonly showAll = signal(false);

  readonly sorted = computed(() => sortReviews(this.reviews(), this.sort()));
  readonly visible = computed(() => (this.showAll() ? this.sorted() : this.sorted().slice(0, REVIEW_PAGE_SIZE)));
  readonly hiddenCount = computed(() => this.sorted().length - this.visible().length);
}
