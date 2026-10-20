import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { provideCustomTesting, TEST_OCC_BASE } from '../../testing/custom-testing';
import { orderBrands } from '../brand-strip/brand-strip.component';
import { BrandAdapter } from '../connectors/brand.adapter';
import { OccBrandAdapter } from '../occ/occ-brand.adapter';
import { BrandService } from './brand.service';

describe('BrandService(Adapter → Connector → Facade)', () => {
  let service: BrandService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideCustomTesting(), provideHttpClient(), provideHttpClientTesting(), { provide: BrandAdapter, useClass: OccBrandAdapter }],
    });
    service = TestBed.inject(BrandService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('一覧は GET /brands を 1 回だけ呼ぶ', async () => {
    const a = firstValueFrom(service.getAll());
    const b = firstValueFrom(service.getAll());
    http.expectOne(`${TEST_OCC_BASE}/brands?fields=DEFAULT`).flush({ brands: [{ code: 'brand-01', name: 'アルテア' }] });
    expect((await a).length).toBe(1);
    expect(await b).toEqual(await a);
  });

  it('1 件は GET /brands/{code} を呼ぶ', async () => {
    const result = firstValueFrom(service.get('brand-03'));
    http.expectOne(`${TEST_OCC_BASE}/brands/brand-03?fields=FULL`).flush({ code: 'brand-03', name: 'カイロ', tier: 'PREMIUM' });
    expect((await result)?.tier).toBe('PREMIUM');
  });

  it('1 件が失敗したら undefined にする', async () => {
    const result = firstValueFrom(service.get('none'));
    http.expectOne(() => true).flush({}, { status: 400, statusText: 'Bad Request' });
    expect(await result).toBeUndefined();
  });
});

describe('orderBrands', () => {
  it('PREMIUM を先に、OUTLET を後に並べる', () => {
    const ordered = orderBrands([
      { code: 'o', tier: 'OUTLET' },
      { code: 's', tier: 'STANDARD' },
      { code: 'p', tier: 'PREMIUM' },
    ]);
    expect(ordered.map((b) => b.code)).toEqual(['p', 's', 'o']);
  });
});
