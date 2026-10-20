import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** 中身が無いときの一言。 */
@Component({
  selector: 'xyz-empty-state',
  standalone: false,
  templateUrl: './empty-state.component.html',
  styleUrl: './empty-state.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  readonly message = input.required<string>();
}
