import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Address, DeliveryMode, Order, PaymentDetails } from '../../model/cart.model';
import { CheckoutAdapter } from './checkout.adapter';

@Injectable({ providedIn: 'root' })
export class CheckoutConnector {
  private readonly adapter = inject(CheckoutAdapter);

  setGuestEmail(userId: string, cartId: string, email: string): Observable<unknown> {
    return this.adapter.setGuestEmail(userId, cartId, email);
  }

  setDeliveryAddress(userId: string, cartId: string, address: Address): Observable<unknown> {
    return this.adapter.setDeliveryAddress(userId, cartId, address);
  }

  getDeliveryModes(userId: string, cartId: string): Observable<DeliveryMode[]> {
    return this.adapter.getDeliveryModes(userId, cartId);
  }

  setDeliveryMode(userId: string, cartId: string, deliveryModeId: string): Observable<unknown> {
    return this.adapter.setDeliveryMode(userId, cartId, deliveryModeId);
  }

  setPaymentDetails(userId: string, cartId: string, payment: PaymentDetails): Observable<PaymentDetails> {
    return this.adapter.setPaymentDetails(userId, cartId, payment);
  }

  placeOrder(userId: string, cartId: string): Observable<Order> {
    return this.adapter.placeOrder(userId, cartId);
  }

  loadGuestOrder(guid: string): Observable<Order> {
    return this.adapter.loadGuestOrder(guid);
  }
}
