import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Cart, CartModification } from '../../model/cart.model';
import { OccEndpointsService } from '../../occ/occ-endpoints.service';
import { CartAdapter } from '../connectors/cart.adapter';

@Injectable()
export class OccCartAdapter implements CartAdapter {
  private readonly http = inject(HttpClient);
  private readonly endpoints = inject(OccEndpointsService);

  create(userId: string): Observable<Cart> {
    return this.http.post<Cart>(this.endpoints.buildUrl('createCart', { urlParams: { userId } }), {});
  }

  load(userId: string, cartId: string): Observable<Cart> {
    return this.http.get<Cart>(this.endpoints.buildUrl('cart', { urlParams: { userId, cartId } }));
  }

  addEntry(userId: string, cartId: string, productCode: string, quantity: number): Observable<CartModification> {
    return this.http.post<CartModification>(this.endpoints.buildUrl('addEntries', { urlParams: { userId, cartId } }), {
      product: { code: productCode },
      quantity,
    });
  }

  updateEntry(userId: string, cartId: string, entryNumber: number, quantity: number): Observable<CartModification> {
    return this.http.patch<CartModification>(this.endpoints.buildUrl('updateEntries', { urlParams: { userId, cartId, entryNumber } }), {
      quantity,
    });
  }

  removeEntry(userId: string, cartId: string, entryNumber: number): Observable<unknown> {
    return this.http.delete(this.endpoints.buildUrl('removeEntries', { urlParams: { userId, cartId, entryNumber } }));
  }
}
