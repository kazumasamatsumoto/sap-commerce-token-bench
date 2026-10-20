import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Brand } from '../../shared';

/** ブランドのロゴと名前。押すとブランド名で商品を検索する(ブランドのページは無いため)。 */
@Component({
  selector: 'xyz-brand-logo',
  standalone: false,
  templateUrl: './brand-logo.component.html',
  styleUrl: './brand-logo.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandLogoComponent {
  readonly brand = input.required<Brand>();
}
