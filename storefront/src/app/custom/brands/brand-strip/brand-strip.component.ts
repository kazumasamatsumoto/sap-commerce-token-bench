import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { map } from 'rxjs';
import { Brand } from '../../shared';
import { BrandService } from '../facade/brand.service';

/** PREMIUM を先に並べる(同じ段の中は元の順)。 */
export function orderBrands(brands: Brand[]): Brand[] {
  const rank = (b: Brand) => (b.tier === 'PREMIUM' ? 0 : b.tier === 'OUTLET' ? 2 : 1);
  return [...brands].sort((a, b) => rank(a) - rank(b));
}

/** トップページの下(Outlet LandingPage2Template / AFTER)に、取り扱いブランドのロゴを並べる。OCC: GET /brands。 */
@Component({
  selector: 'xyz-brand-strip',
  standalone: false,
  templateUrl: './brand-strip.component.html',
  styleUrl: './brand-strip.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandStripComponent {
  readonly brands$ = inject(BrandService)
    .getAll()
    .pipe(map((brands) => orderBrands(brands)));
}
