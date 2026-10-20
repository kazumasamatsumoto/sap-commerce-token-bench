import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Review } from '../../shared';

export interface ReviewSummary {
  average: number;
  count: number;
  /** 星 5 → 1 の順。percent は 0〜100。 */
  rows: { stars: number; count: number; percent: number }[];
}

/** レビューの平均と、星ごとの件数を数える。 */
export function summarizeReviews(reviews: Review[]): ReviewSummary {
  const count = reviews.length;
  const total = reviews.reduce((sum, r) => sum + (r.rating ?? 0), 0);
  const rows = [5, 4, 3, 2, 1].map((stars) => {
    const n = reviews.filter((r) => Math.round(r.rating ?? 0) === stars).length;
    return { stars, count: n, percent: count ? Math.round((n / count) * 100) : 0 };
  });
  return { average: count ? Math.round((total / count) * 10) / 10 : 0, count, rows };
}

/** レビューの平均と、星ごとの内訳の棒。 */
@Component({
  selector: 'xyz-review-summary',
  standalone: false,
  templateUrl: './review-summary.component.html',
  styleUrl: './review-summary.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewSummaryComponent {
  readonly reviews = input.required<Review[]>();
  readonly summary = computed(() => summarizeReviews(this.reviews()));
}
