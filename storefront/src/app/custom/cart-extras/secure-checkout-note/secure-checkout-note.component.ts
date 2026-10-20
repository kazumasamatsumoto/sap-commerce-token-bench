import { ChangeDetectionStrategy, Component } from '@angular/core';

/** カートの右側(Outlet CenterRightContentSlot / BEFORE)に、安心してお支払いできることの一言。 */
@Component({
  selector: 'xyz-secure-checkout-note',
  standalone: false,
  templateUrl: './secure-checkout-note.component.html',
  styleUrl: './secure-checkout-note.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecureCheckoutNoteComponent {}
