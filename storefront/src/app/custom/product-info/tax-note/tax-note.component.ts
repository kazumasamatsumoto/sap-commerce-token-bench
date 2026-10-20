import { ChangeDetectionStrategy, Component } from '@angular/core';

/** 価格の後(PDP.PRICE / AFTER)に「税込」と出す。 */
@Component({
  selector: 'xyz-tax-note',
  standalone: false,
  templateUrl: './tax-note.component.html',
  styleUrl: './tax-note.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaxNoteComponent {}
