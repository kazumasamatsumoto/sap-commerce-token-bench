import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Product, ProductSearchPage } from '../../model/product.model';
import { ProductAdapter } from './product.adapter';

/** Facade と Adapter の間の層。Spartacus と同じく、ここでは Adapter に渡すだけ。 */
@Injectable({ providedIn: 'root' })
export class ProductConnector {
  private readonly adapter = inject(ProductAdapter);

  get(productCode: string): Observable<Product> {
    return this.adapter.load(productCode);
  }

  search(query: string, options?: { currentPage?: number; pageSize?: number; sort?: string }): Observable<ProductSearchPage> {
    return this.adapter.search(query, options);
  }
}
