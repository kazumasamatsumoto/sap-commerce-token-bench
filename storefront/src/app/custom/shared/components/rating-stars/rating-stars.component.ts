import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type StarState = 'full' | 'half' | 'empty';

/** 評価を星の並びにする。0.5 きざみに丸める。 */
export function toStars(rating: number | undefined, max = 5): StarState[] {
  const r = Math.round((rating ?? 0) * 2) / 2;
  return Array.from({ length: max }, (_, i) => (r >= i + 1 ? 'full' : r >= i + 0.5 ? 'half' : 'empty'));
}

/** 星で評価を出す。読み上げでは「5 点中 4.5 点」と読む。 */
@Component({
  selector: 'xyz-rating-stars',
  standalone: false,
  templateUrl: './rating-stars.component.html',
  styleUrl: './rating-stars.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RatingStarsComponent {
  readonly rating = input<number | undefined>(0);
  readonly max = input(5);

  readonly stars = computed(() => toStars(this.rating(), this.max()));
  readonly label = computed(() => (this.rating() ?? 0).toFixed(1));
}
