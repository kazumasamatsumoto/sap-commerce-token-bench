import { Observable } from 'rxjs';
import { Address, DeliveryMode, Order, PaymentDetails } from '../../model/cart.model';

export abstract class CheckoutAdapter {
  abstract setGuestEmail(userId: string, cartId: string, email: string): Observable<unknown>;
  abstract setDeliveryAddress(userId: string, cartId: string, address: Address): Observable<unknown>;
  abstract getDeliveryModes(userId: string, cartId: string): Observable<DeliveryMode[]>;
  abstract setDeliveryMode(userId: string, cartId: string, deliveryModeId: string): Observable<unknown>;
  abstract setPaymentDetails(userId: string, cartId: string, payment: PaymentDetails): Observable<PaymentDetails>;
  abstract placeOrder(userId: string, cartId: string): Observable<Order>;
  abstract loadGuestOrder(guid: string): Observable<Order>;
}
