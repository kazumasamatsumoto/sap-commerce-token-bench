import { ChangeDetectionStrategy, Component, model } from '@angular/core';

export type ReviewSort = 'newest' | 'highest' | 'lowest';

/** レビューの順番を選ぶ。[(sort)] で受け渡す。 */
@Component({
  selector: 'xyz-review-sort',
  standalone: false,
  templateUrl: './review-sort.component.html',
  styleUrl: './review-sort.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewSortComponent {
  readonly sort = model<ReviewSort>('newest');
  readonly options: ReviewSort[] = ['newest', 'highest', 'lowest'];

  onChange(value: string): void {
    this.sort.set(value as ReviewSort);
  }
}
