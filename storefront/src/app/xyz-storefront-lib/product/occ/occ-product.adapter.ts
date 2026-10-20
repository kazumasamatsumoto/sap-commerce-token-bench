import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Product, ProductSearchPage } from '../../model/product.model';
import { OccEndpointsService } from '../../occ/occ-endpoints.service';
import { ProductAdapter } from '../connectors/product.adapter';

@Injectable()
export class OccProductAdapter implements ProductAdapter {
  private readonly http = inject(HttpClient);
  private readonly endpoints = inject(OccEndpointsService);

  load(productCode: string): Observable<Product> {
    return this.http.get<Product>(this.endpoints.buildUrl('product', { urlParams: { productCode } }));
  }

  search(query: string, options: { currentPage?: number; pageSize?: number; sort?: string } = {}): Observable<ProductSearchPage> {
    return this.http.get<ProductSearchPage>(
      this.endpoints.buildUrl('productSearchList', {
        queryParams: { query, currentPage: options.currentPage, pageSize: options.pageSize, sort: options.sort },
      }),
    );
  }
}
