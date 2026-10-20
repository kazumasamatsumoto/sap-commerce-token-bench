import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { OccEndpointsService } from '@xyz/storefront';
import { map, Observable } from 'rxjs';
import { Review } from '../../shared';
import { ProductReviewAdapter } from '../connectors/product-review.adapter';

/** GET /products/{productCode}/reviews(端点の名前は xyzProductReviews)。 */
@Injectable()
export class OccProductReviewAdapter implements ProductReviewAdapter {
  private readonly http = inject(HttpClient);
  private readonly endpoints = inject(OccEndpointsService);

  load(productCode: string, maxCount?: number): Observable<Review[]> {
    const url = this.endpoints.buildUrl('xyzProductReviews', { urlParams: { productCode }, queryParams: { maxCount } });
    return this.http.get<{ reviews?: Review[] }>(url).pipe(map((res) => res.reviews ?? []));
  }
}
