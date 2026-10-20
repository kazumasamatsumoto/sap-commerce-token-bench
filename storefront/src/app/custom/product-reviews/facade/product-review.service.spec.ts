import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Review } from '../../shared';
import { provideCustomTesting, TEST_OCC_BASE } from '../../testing/custom-testing';
import { ProductReviewAdapter } from '../connectors/product-review.adapter';
import { OccProductReviewAdapter } from '../occ/occ-product-review.adapter';
import { ProductReviewService } from './product-review.service';

describe('ProductReviewService(Adapter → Connector → Facade)', () => {
  let service: ProductReviewService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideCustomTesting(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ProductReviewAdapter, useClass: OccProductReviewAdapter },
      ],
    });
    service = TestBed.inject(ProductReviewService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('GET /products/{code}/reviews を呼び、reviews の配列を返す', async () => {
    const result = firstValueFrom(service.getByProductCode('XYZ-000001'));
    const req = http.expectOne(`${TEST_OCC_BASE}/products/XYZ-000001/reviews?fields=DEFAULT`);
    expect(req.request.method).toBe('GET');
    const reviews: Review[] = [{ id: '1', headline: '良い', rating: 5 }];
    req.flush({ reviews });
    expect(await result).toEqual(reviews);
  });

  it('同じ商品は 1 回だけ読む', async () => {
    const a = firstValueFrom(service.getByProductCode('XYZ-000002'));
    const b = firstValueFrom(service.getByProductCode('XYZ-000002'));
    http.expectOne(() => true).flush({ reviews: [] });
    expect(await a).toEqual([]);
    expect(await b).toEqual([]);
  });

  it('失敗したら空の一覧にする(画面を止めない)', async () => {
    const result = firstValueFrom(service.getByProductCode('NOPE'));
    http.expectOne(() => true).flush({ errors: [] }, { status: 400, statusText: 'Bad Request' });
    expect(await result).toEqual([]);
  });
});
