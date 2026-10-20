import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

let nextId = 0;

/** 数を min〜max に収める。数字でなければ min。 */
export function clampQuantity(value: unknown, min: number, max: number): number {
  const n = Math.floor(Number(value));
  if (!Number.isFinite(n)) {
    return min;
  }
  return Math.min(max, Math.max(min, n));
}

/** − / + のボタンと入力欄で数量を選ぶ。[(value)] で受け渡す。 */
@Component({
  selector: 'xyz-quantity-stepper',
  standalone: false,
  templateUrl: './quantity-stepper.component.html',
  styleUrl: './quantity-stepper.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuantityStepperComponent {
  readonly value = model(1);
  readonly min = input(1);
  readonly max = input(99);
  readonly inputId = `xyz-qty-${nextId++}`;

  decrement(): void {
    this.value.set(clampQuantity(this.value() - 1, this.min(), this.max()));
  }

  increment(): void {
    this.value.set(clampQuantity(this.value() + 1, this.min(), this.max()));
  }

  onInput(raw: string): void {
    this.value.set(clampQuantity(raw, this.min(), this.max()));
  }
}
