import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { BadgeVariant, Brand } from '../../shared';

/** ブランドの札。ロゴと名前、PREMIUM / OUTLET のときは段の札も付ける。 */
@Component({
  selector: 'xyz-brand-badge',
  standalone: false,
  templateUrl: './brand-badge.component.html',
  styleUrl: './brand-badge.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandBadgeComponent {
  readonly brand = input<Brand | undefined>();

  /** STANDARD は札を出さない。 */
  readonly tierVariant = computed<BadgeVariant | undefined>(() => {
    switch (this.brand()?.tier) {
      case 'PREMIUM':
        return 'premium';
      case 'OUTLET':
        return 'outlet';
      default:
        return undefined;
    }
  });
}
