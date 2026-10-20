import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, concatMap, map, mergeMap, of, switchMap } from 'rxjs';
import { CartConnector } from '../connectors/cart.connector';
import * as CartActions from './cart.actions';

@Injectable()
export class CartEffects {
  private readonly actions$ = inject(Actions);
  private readonly connector = inject(CartConnector);

  loadCart$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CartActions.loadCart),
      switchMap(({ userId, cartId }) =>
        this.connector.load(userId, cartId).pipe(
          map((cart) => CartActions.loadCartSuccess({ cart })),
          catchError((error) => of(CartActions.loadCartFail({ error }))),
        ),
      ),
    ),
  );

  /** カートが無いときに「カートに入れる」を押したら、カートを作ってから入れる。 */
  createCart$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CartActions.createCart),
      mergeMap(({ userId, addProductCode, quantity }) =>
        this.connector.create(userId).pipe(
          mergeMap((cart) => {
            const actions: ReturnType<typeof CartActions.createCartSuccess | typeof CartActions.addEntry>[] = [CartActions.createCartSuccess({ cart })];
            if (addProductCode) {
              const cartId = userId === 'anonymous' ? cart.guid! : cart.code!;
              actions.push(CartActions.addEntry({ userId, cartId, productCode: addProductCode, quantity: quantity ?? 1 }));
            }
            return actions;
          }),
          catchError((error) => of(CartActions.cartFail({ error }))),
        ),
      ),
    ),
  );

  addEntry$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CartActions.addEntry),
      concatMap(({ userId, cartId, productCode, quantity }) =>
        this.connector.addEntry(userId, cartId, productCode, quantity).pipe(
          mergeMap((modification) => [CartActions.addEntrySuccess({ modification }), CartActions.entryChangeSuccess({ userId, cartId })]),
          catchError((error) => of(CartActions.cartFail({ error }))),
        ),
      ),
    ),
  );

  updateEntry$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CartActions.updateEntry),
      concatMap(({ userId, cartId, entryNumber, quantity }) =>
        this.connector.updateEntry(userId, cartId, entryNumber, quantity).pipe(
          map(() => CartActions.entryChangeSuccess({ userId, cartId })),
          catchError((error) => of(CartActions.cartFail({ error }))),
        ),
      ),
    ),
  );

  removeEntry$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CartActions.removeEntry),
      concatMap(({ userId, cartId, entryNumber }) =>
        this.connector.removeEntry(userId, cartId, entryNumber).pipe(
          map(() => CartActions.entryChangeSuccess({ userId, cartId })),
          catchError((error) => of(CartActions.cartFail({ error }))),
        ),
      ),
    ),
  );

  /** 明細を変えたら、カートを読み直す(合計を出し直すため)。 */
  reload$ = createEffect(() =>
    this.actions$.pipe(
      ofType(CartActions.entryChangeSuccess),
      map(({ userId, cartId }) => CartActions.loadCart({ userId, cartId })),
    ),
  );
}
