import { Observable } from 'rxjs';
import { Brand } from '../../shared';

/** ブランドの取得の抽象。OCC 版は OccBrandAdapter。 */
export abstract class BrandAdapter {
  abstract loadAll(): Observable<Brand[]>;
  abstract load(brandCode: string): Observable<Brand>;
}
