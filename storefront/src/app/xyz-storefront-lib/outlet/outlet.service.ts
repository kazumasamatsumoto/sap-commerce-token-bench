import { inject, Injectable, InjectionToken, Provider, Type } from '@angular/core';
import { OutletPosition, OutletProviderConfig } from './outlet.model';

export const PROVIDE_OUTLET_OPTIONS = new InjectionToken<OutletProviderConfig[]>('PROVIDE_OUTLET_OPTIONS');

/** モジュールの providers に書くだけで、名前の付いた場所(Outlet)に部品を足せる。Spartacus の provideOutlet と同じ。 */
export function provideOutlet(options: OutletProviderConfig): Provider {
  return { provide: PROVIDE_OUTLET_OPTIONS, useValue: options, multi: true };
}

@Injectable({ providedIn: 'root' })
export class OutletService {
  private readonly registered = inject(PROVIDE_OUTLET_OPTIONS, { optional: true }) ?? [];

  get(id: string, position: OutletPosition): Type<unknown>[] {
    return this.registered.filter((o) => o.id === id && (o.position ?? OutletPosition.REPLACE) === position).map((o) => o.component);
  }
}
