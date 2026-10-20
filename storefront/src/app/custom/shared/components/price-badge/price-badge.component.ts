import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type BadgeVariant = 'info' | 'new' | 'premium' | 'outlet' | 'warning';

/** 小さな札(新着、プレミアム、残りわずか など)。文言は呼ぶ側で翻訳して渡す。 */
@Component({
  selector: 'xyz-price-badge',
  standalone: false,
  templateUrl: './price-badge.component.html',
  styleUrl: './price-badge.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PriceBadgeComponent {
  readonly label = input.required<string>();
  readonly variant = input<BadgeVariant>('info');
}
