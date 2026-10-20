import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Review } from '../../shared';

/** レビュー 1 件(見出し、星、投稿者、日付、本文)。 */
@Component({
  selector: 'xyz-review-item',
  standalone: false,
  templateUrl: './review-item.component.html',
  styleUrl: './review-item.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewItemComponent {
  readonly review = input.required<Review>();
}
