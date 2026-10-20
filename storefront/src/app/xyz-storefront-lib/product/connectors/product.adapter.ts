import { Observable } from 'rxjs';
import { Product, ProductSearchPage } from '../../model/product.model';

/** 商品の取得の抽象。OCC 版は OccProductAdapter。差し替えるときはこのクラスを provide する。 */
export abstract class ProductAdapter {
  abstract load(productCode: string): Observable<Product>;
  abstract search(query: string, options?: { currentPage?: number; pageSize?: number; sort?: string }): Observable<ProductSearchPage>;
}
