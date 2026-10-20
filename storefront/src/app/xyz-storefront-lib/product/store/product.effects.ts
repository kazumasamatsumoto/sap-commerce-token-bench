import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, mergeMap, of, switchMap } from 'rxjs';
import { ProductConnector } from '../connectors/product.connector';
import * as ProductActions from './product.actions';

@Injectable()
export class ProductEffects {
  private readonly actions$ = inject(Actions);
  private readonly connector = inject(ProductConnector);

  loadProduct$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProductActions.loadProduct),
      mergeMap(({ productCode }) =>
        this.connector.get(productCode).pipe(
          map((product) => ProductActions.loadProductSuccess({ product })),
          catchError((error) => of(ProductActions.loadProductFail({ productCode, error }))),
        ),
      ),
    ),
  );

  search$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ProductActions.searchProducts),
      switchMap(({ query, ...options }) =>
        this.connector.search(query, options).pipe(
          map((result) => ProductActions.searchProductsSuccess({ result })),
          catchError((error) => of(ProductActions.searchProductsFail({ error }))),
        ),
      ),
    ),
  );
}
