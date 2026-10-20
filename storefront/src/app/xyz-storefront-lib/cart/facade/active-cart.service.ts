import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import { Store } from '@ngrx/store';
import { filter, map, Observable, take } from 'rxjs';
import { Cart, CartModification, OCC_USER_ID_ANONYMOUS, OrderEntry } from '../../model/cart.model';
import * as CartActions from '../store/cart.actions';
import { selectActiveCart, selectCartLoading, selectLastModification } from '../store/cart.reducer';

const STORAGE_KEY = 'xyz-anonymous-cart-guid';

/**
 * 今のカートの窓口(Spartacus の ActiveCartFacade に当たる)。ゲストのカートの guid はブラウザの localStorage に覚える。
 * SSR ではカートを読まない(ブラウザに戻ってから読む)。
 */
@Injectable({ providedIn: 'root' })
export class ActiveCartService {
  private readonly store = inject(Store);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly userId = OCC_USER_ID_ANONYMOUS;
  private initialized = false;

  getActive(): Observable<Cart | undefined> {
    this.initOnce();
    return this.store.select(selectActiveCart);
  }

  getEntries(): Observable<OrderEntry[]> {
    return this.getActive().pipe(map((cart) => cart?.entries ?? []));
  }

  getTotalItems(): Observable<number> {
    return this.getActive().pipe(map((cart) => cart?.totalUnitCount ?? 0));
  }

  isStable(): Observable<boolean> {
    return this.store.select(selectCartLoading).pipe(map((loading) => !loading));
  }

  getLastModification(): Observable<CartModification | undefined> {
    return this.store.select(selectLastModification);
  }

  /** 今のカートの id(ゲストは guid)。カートが無ければ undefined。 */
  getActiveCartId(): Observable<string | undefined> {
    return this.getActive().pipe(map((cart) => cart?.guid));
  }

  getUserId(): string {
    return this.userId;
  }

  addEntry(productCode: string, quantity: number): void {
    this.getActive()
      .pipe(take(1))
      .subscribe((cart) => {
        if (cart?.guid) {
          this.store.dispatch(CartActions.addEntry({ userId: this.userId, cartId: cart.guid, productCode, quantity }));
        } else {
          this.store.dispatch(CartActions.createCart({ userId: this.userId, addProductCode: productCode, quantity }));
        }
      });
  }

  updateEntry(entryNumber: number, quantity: number): void {
    this.withCartId((cartId) =>
      this.store.dispatch(
        quantity > 0
          ? CartActions.updateEntry({ userId: this.userId, cartId, entryNumber, quantity })
          : CartActions.removeEntry({ userId: this.userId, cartId, entryNumber }),
      ),
    );
  }

  reload(): void {
    this.withCartId((cartId) => this.store.dispatch(CartActions.loadCart({ userId: this.userId, cartId })));
  }

  /** 注文を確定したら、カートを忘れる。 */
  clear(): void {
    if (this.isBrowser) {
      localStorage.removeItem(STORAGE_KEY);
    }
    this.store.dispatch(CartActions.clearCart());
  }

  private withCartId(fn: (cartId: string) => void): void {
    this.getActiveCartId()
      .pipe(
        take(1),
        filter((id): id is string => !!id),
      )
      .subscribe(fn);
  }

  private initOnce(): void {
    if (this.initialized || !this.isBrowser) {
      return;
    }
    this.initialized = true;
    const guid = localStorage.getItem(STORAGE_KEY);
    if (guid) {
      this.store.dispatch(CartActions.loadCart({ userId: this.userId, cartId: guid }));
    }
    this.store
      .select(selectActiveCart)
      .pipe(filter((cart): cart is Cart => !!cart?.guid))
      .subscribe((cart) => localStorage.setItem(STORAGE_KEY, cart.guid!));
  }
}
