import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Review } from '../../shared';
import { ProductReviewAdapter } from './product-review.adapter';

/** Facade と Adapter の間の層。ここでは Adapter に渡すだけ。 */
@Injectable({ providedIn: 'root' })
export class ProductReviewConnector {
  private readonly adapter = inject(ProductReviewAdapter);

  get(productCode: string, maxCount?: number): Observable<Review[]> {
    return this.adapter.load(productCode, maxCount);
  }
}
