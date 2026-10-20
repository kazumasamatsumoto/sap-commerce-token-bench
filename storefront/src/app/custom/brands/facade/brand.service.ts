import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of, shareReplay } from 'rxjs';
import { Brand } from '../../shared';
import { BrandConnector } from '../connectors/brand.connector';

/**
 * 画面の部品が使うブランドの窓口。一覧は 1 回だけ、1 件はブランドごとに 1 回だけ読む。
 * 失敗したら空(一覧は []、1 件は undefined)にして、画面は止めない。
 */
@Injectable({ providedIn: 'root' })
export class BrandService {
  private readonly connector = inject(BrandConnector);
  private all$?: Observable<Brand[]>;
  private readonly cache = new Map<string, Observable<Brand | undefined>>();

  getAll(): Observable<Brand[]> {
    this.all$ ??= this.connector.getAll().pipe(
      catchError(() => of<Brand[]>([])),
      shareReplay(1),
    );
    return this.all$;
  }

  get(brandCode: string): Observable<Brand | undefined> {
    if (!this.cache.has(brandCode)) {
      this.cache.set(
        brandCode,
        this.connector.get(brandCode).pipe(
          catchError(() => of(undefined)),
          shareReplay(1),
        ),
      );
    }
    return this.cache.get(brandCode)!;
  }
}
