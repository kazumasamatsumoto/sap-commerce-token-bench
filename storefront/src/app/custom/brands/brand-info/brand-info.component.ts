import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { map, of, switchMap } from 'rxjs';
import { CurrentProductService } from '../../shared';
import { BrandService } from '../facade/brand.service';

/** 商品詳細の「ブランドについて」(Outlet UpSelling / BEFORE)。OCC: GET /brands/{brandCode}。 */
@Component({
  selector: 'xyz-brand-info',
  standalone: false,
  templateUrl: './brand-info.component.html',
  styleUrl: './brand-info.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandInfoComponent {
  private readonly brandService = inject(BrandService);

  readonly brand$ = inject(CurrentProductService)
    .getProduct()
    .pipe(
      map((product) => product?.brand?.code),
      switchMap((code) => (code ? this.brandService.get(code) : of(undefined))),
    );
}
