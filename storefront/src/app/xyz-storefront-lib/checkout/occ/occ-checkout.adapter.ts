import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { Address, DeliveryMode, Order, PaymentDetails } from '../../model/cart.model';
import { OccEndpointsService } from '../../occ/occ-endpoints.service';
import { CheckoutAdapter } from '../connectors/checkout.adapter';

@Injectable()
export class OccCheckoutAdapter implements CheckoutAdapter {
  private readonly http = inject(HttpClient);
  private readonly endpoints = inject(OccEndpointsService);

  setGuestEmail(userId: string, cartId: string, email: string): Observable<unknown> {
    return this.http.put(this.endpoints.buildUrl('cartEmail', { urlParams: { userId, cartId }, queryParams: { email } }), {});
  }

  setDeliveryAddress(userId: string, cartId: string, address: Address): Observable<unknown> {
    return this.http.put(this.endpoints.buildUrl('setDeliveryAddress', { urlParams: { userId, cartId } }), address);
  }

  getDeliveryModes(userId: string, cartId: string): Observable<DeliveryMode[]> {
    return this.http
      .get<{ deliveryModes?: DeliveryMode[] }>(this.endpoints.buildUrl('deliveryModes', { urlParams: { userId, cartId } }))
      .pipe(map((r) => r.deliveryModes ?? []));
  }

  setDeliveryMode(userId: string, cartId: string, deliveryModeId: string): Observable<unknown> {
    return this.http.put(this.endpoints.buildUrl('setDeliveryMode', { urlParams: { userId, cartId }, queryParams: { deliveryModeId } }), {});
  }

  setPaymentDetails(userId: string, cartId: string, payment: PaymentDetails): Observable<PaymentDetails> {
    return this.http.post<PaymentDetails>(this.endpoints.buildUrl('paymentDetails', { urlParams: { userId, cartId } }), payment);
  }

  placeOrder(userId: string, cartId: string): Observable<Order> {
    return this.http.post<Order>(this.endpoints.buildUrl('placeOrder', { urlParams: { userId }, queryParams: { cartId } }), {});
  }

  loadGuestOrder(guid: string): Observable<Order> {
    return this.http.get<Order>(this.endpoints.buildUrl('guestOrder', { urlParams: { guid } }));
  }
}
