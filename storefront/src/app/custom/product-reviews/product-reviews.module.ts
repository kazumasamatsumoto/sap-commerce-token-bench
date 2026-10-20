import { NgModule } from '@angular/core';
import { OutletPosition, provideConfig, provideOutlet } from '@xyz/storefront';
import { CustomSharedModule } from '../shared';
import { ProductReviewAdapter } from './connectors/product-review.adapter';
import { OccProductReviewAdapter } from './occ/occ-product-review.adapter';
import { ProductReviewsComponent } from './product-reviews/product-reviews.component';
import { ReviewItemComponent } from './review-item/review-item.component';
import { ReviewSortComponent } from './review-sort/review-sort.component';
import { ReviewSummaryComponent } from './review-summary/review-summary.component';

/**
 * 商品のレビューの一覧。OCC: GET /products/{productCode}/reviews。
 * Adapter → Connector → Facade(ProductReviewService)の順に呼ぶ。
 */
@NgModule({
  imports: [CustomSharedModule],
  declarations: [ProductReviewsComponent, ReviewSummaryComponent, ReviewItemComponent, ReviewSortComponent],
  providers: [
    provideConfig({
      backend: { occ: { endpoints: { xyzProductReviews: 'products/${productCode}/reviews?fields=DEFAULT' } } },
    }),
    { provide: ProductReviewAdapter, useClass: OccProductReviewAdapter },
    provideOutlet({ id: 'Tabs', position: OutletPosition.AFTER, component: ProductReviewsComponent }),
  ],
})
export class ProductReviewsModule {}
