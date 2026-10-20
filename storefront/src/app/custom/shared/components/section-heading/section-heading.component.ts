import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** 自社の部品の見出し(h2)。headingId を渡すと aria-labelledby で使える。 */
@Component({
  selector: 'xyz-section-heading',
  standalone: false,
  templateUrl: './section-heading.component.html',
  styleUrl: './section-heading.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionHeadingComponent {
  readonly heading = input.required<string>();
  readonly sub = input<string>();
  readonly headingId = input<string>();
}
