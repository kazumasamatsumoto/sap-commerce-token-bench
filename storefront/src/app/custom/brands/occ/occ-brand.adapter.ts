import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { OccEndpointsService } from '@xyz/storefront';
import { map, Observable } from 'rxjs';
import { Brand } from '../../shared';
import { BrandAdapter } from '../connectors/brand.adapter';

/**
 * GET /brands(端点の名前は xyzBrands。このモジュールで足す)と
 * GET /brands/{brandCode}(端点の名前は brand。疑似ライブラリの既定にある)。
 */
@Injectable()
export class OccBrandAdapter implements BrandAdapter {
  private readonly http = inject(HttpClient);
  private readonly endpoints = inject(OccEndpointsService);

  loadAll(): Observable<Brand[]> {
    return this.http.get<{ brands?: Brand[] }>(this.endpoints.buildUrl('xyzBrands')).pipe(map((res) => res.brands ?? []));
  }

  load(brandCode: string): Observable<Brand> {
    return this.http.get<Brand>(this.endpoints.buildUrl('brand', { urlParams: { brandCode } }));
  }
}
