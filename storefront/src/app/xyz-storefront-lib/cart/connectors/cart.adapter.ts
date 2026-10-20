import { Observable } from 'rxjs';
import { Cart, CartModification } from '../../model/cart.model';

export abstract class CartAdapter {
  abstract create(userId: string): Observable<Cart>;
  abstract load(userId: string, cartId: string): Observable<Cart>;
  abstract addEntry(userId: string, cartId: string, productCode: string, quantity: number): Observable<CartModification>;
  abstract updateEntry(userId: string, cartId: string, entryNumber: number, quantity: number): Observable<CartModification>;
  abstract removeEntry(userId: string, cartId: string, entryNumber: number): Observable<unknown>;
}
