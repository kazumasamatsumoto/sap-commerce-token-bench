import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of, shareReplay } from 'rxjs';
import { Review } from '../../shared';
import { ProductReviewConnector } from '../connectors/product-review.connector';

/**
 * 画面の部品が使うレビューの窓口(Spartacus の ProductReviewService に当たる)。
 * 商品ごとに 1 回だけ読む。失敗したら空の一覧にして、商品詳細の画面は止めない。
 */
@Injectable({ providedIn: 'root' })
export class ProductReviewService {
  private readonly connector = inject(ProductReviewConnector);
  private readonly cache = new Map<string, Observable<Review[]>>();

  getByProductCode(productCode: string): Observable<Review[]> {
    if (!this.cache.has(productCode)) {
      this.cache.set(
        productCode,
        this.connector.get(productCode).pipe(
          catchError(() => of<Review[]>([])),
          shareReplay(1),
        ),
      );
    }
    return this.cache.get(productCode)!;
  }
}
