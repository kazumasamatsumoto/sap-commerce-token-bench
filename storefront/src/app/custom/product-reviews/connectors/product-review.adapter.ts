import { Observable } from 'rxjs';
import { Review } from '../../shared';

/** 商品のレビューの取得の抽象。OCC 版は OccProductReviewAdapter。 */
export abstract class ProductReviewAdapter {
  abstract load(productCode: string, maxCount?: number): Observable<Review[]>;
}
