import { inject, Injectable } from '@angular/core';
import { PageContextService, ProductService } from '@xyz/storefront';
import { distinctUntilChanged, map, Observable, of, switchMap } from 'rxjs';
import { XyzProduct } from '../models/custom-product.model';

/**
 * 今開いている商品詳細の商品。Outlet の部品は商品を受け取れないので、ここから引く。
 * Spartacus の CurrentProductService に当たる。商品のページでなければ undefined。
 */
@Injectable({ providedIn: 'root' })
export class CurrentProductService {
  private readonly pageContext = inject(PageContextService);
  private readonly productService = inject(ProductService);

  getCode(): Observable<string | undefined> {
    return this.pageContext.context$.pipe(
      map((ctx) => (ctx.type === 'ProductPage' ? ctx.id : undefined)),
      distinctUntilChanged(),
    );
  }

  getProduct(): Observable<XyzProduct | undefined> {
    return this.getCode().pipe(
      switchMap((code) => (code ? this.productService.get(code) : of(undefined))),
      map((p) => p as XyzProduct | undefined),
    );
  }
}
