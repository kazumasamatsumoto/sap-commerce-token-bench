import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Cart, CartModification } from '../../model/cart.model';
import { CartAdapter } from './cart.adapter';

@Injectable({ providedIn: 'root' })
export class CartConnector {
  private readonly adapter = inject(CartAdapter);

  create(userId: string): Observable<Cart> {
    return this.adapter.create(userId);
  }

  load(userId: string, cartId: string): Observable<Cart> {
    return this.adapter.load(userId, cartId);
  }

  addEntry(userId: string, cartId: string, productCode: string, quantity: number): Observable<CartModification> {
    return this.adapter.addEntry(userId, cartId, productCode, quantity);
  }

  updateEntry(userId: string, cartId: string, entryNumber: number, quantity: number): Observable<CartModification> {
    return this.adapter.updateEntry(userId, cartId, entryNumber, quantity);
  }

  removeEntry(userId: string, cartId: string, entryNumber: number): Observable<unknown> {
    return this.adapter.removeEntry(userId, cartId, entryNumber);
  }
}
