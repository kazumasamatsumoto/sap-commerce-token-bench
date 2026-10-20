import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** 新着の商品(newArrival = true)にだけ「新着」の札を出す。 */
@Component({
  selector: 'xyz-new-arrival-badge',
  standalone: false,
  templateUrl: './new-arrival-badge.component.html',
  styleUrl: './new-arrival-badge.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewArrivalBadgeComponent {
  readonly newArrival = input<boolean | undefined>(false);
}
