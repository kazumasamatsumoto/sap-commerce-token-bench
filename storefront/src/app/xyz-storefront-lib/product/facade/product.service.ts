import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { filter, map, Observable, tap } from 'rxjs';
import { Product, ProductSearchPage } from '../../model/product.model';
import * as ProductActions from '../store/product.actions';
import { selectProductDetails, selectSearchResult } from '../store/product.reducer';

/**
 * 画面の部品が使う窓口(Facade)。部品は Store や HttpClient を直接触らず、これだけを使う。
 * Spartacus の ProductService / ProductSearchService に当たる。
 */
@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly store = inject(Store);

  get(productCode: string): Observable<Product | undefined> {
    return this.store.select(selectProductDetails(productCode)).pipe(
      tap((state) => {
        if (!state || (!state.value && !state.loading && !state.error)) {
          this.store.dispatch(ProductActions.loadProduct({ productCode }));
        }
      }),
      map((state) => state?.value),
    );
  }

  isLoading(productCode: string): Observable<boolean> {
    return this.store.select(selectProductDetails(productCode)).pipe(map((s) => !!s?.loading));
  }

  search(query: string, options?: { currentPage?: number; pageSize?: number; sort?: string }): void {
    this.store.dispatch(ProductActions.searchProducts({ query, ...options }));
  }

  getResults(): Observable<ProductSearchPage> {
    return this.store.select(selectSearchResult).pipe(
      map((s) => s.value),
      filter((v): v is ProductSearchPage => !!v),
    );
  }
}
