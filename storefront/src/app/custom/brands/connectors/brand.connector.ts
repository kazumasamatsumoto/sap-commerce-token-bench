import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Brand } from '../../shared';
import { BrandAdapter } from './brand.adapter';

/** Facade と Adapter の間の層。ここでは Adapter に渡すだけ。 */
@Injectable({ providedIn: 'root' })
export class BrandConnector {
  private readonly adapter = inject(BrandAdapter);

  getAll(): Observable<Brand[]> {
    return this.adapter.loadAll();
  }

  get(brandCode: string): Observable<Brand> {
    return this.adapter.load(brandCode);
  }
}
