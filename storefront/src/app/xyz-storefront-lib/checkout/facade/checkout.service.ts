import { inject, Injectable } from '@angular/core';
import { concatMap, map, Observable, switchMap, take, tap, throwError } from 'rxjs';
import { ActiveCartService } from '../../cart/facade/active-cart.service';
import { Address, DeliveryMode, Order, PaymentDetails } from '../../model/cart.model';
import { CheckoutConnector } from '../connectors/checkout.connector';

/**
 * ゲストのチェックアウト(メール → 届け先 → 配送方法 → 支払い → 確定)。
 * Spartacus の CheckoutDeliveryAddressFacade などをまとめたもの。
 */
@Injectable({ providedIn: 'root' })
export class CheckoutService {
  private readonly connector = inject(CheckoutConnector);
  private readonly activeCart = inject(ActiveCartService);

  private withCart<T>(fn: (userId: string, cartId: string) => Observable<T>): Observable<T> {
    return this.activeCart.getActiveCartId().pipe(
      take(1),
      switchMap((cartId) => (cartId ? fn(this.activeCart.getUserId(), cartId) : throwError(() => new Error('カートがありません')))),
    );
  }

  setGuestEmail(email: string): Observable<unknown> {
    return this.withCart((u, c) => this.connector.setGuestEmail(u, c, email));
  }

  setDeliveryAddress(address: Address): Observable<DeliveryMode[]> {
    return this.withCart((u, c) => this.connector.setDeliveryAddress(u, c, address).pipe(concatMap(() => this.connector.getDeliveryModes(u, c))));
  }

  setDeliveryMode(code: string): Observable<unknown> {
    return this.withCart((u, c) => this.connector.setDeliveryMode(u, c, code)).pipe(tap(() => this.activeCart.reload()));
  }

  setPaymentDetails(payment: PaymentDetails): Observable<PaymentDetails> {
    return this.withCart((u, c) => this.connector.setPaymentDetails(u, c, payment));
  }

  /** 注文を確定し、カートを忘れる。返り値は注文(guid で注文確認のページを開く)。 */
  placeOrder(): Observable<Order> {
    return this.withCart((u, c) => this.connector.placeOrder(u, c)).pipe(
      tap(() => this.activeCart.clear()),
      map((order) => order),
    );
  }

  loadOrder(guid: string): Observable<Order> {
    return this.connector.loadGuestOrder(guid);
  }
}
