import { TestBed } from '@angular/core/testing';
import { provideCustomTesting } from '../../../testing/custom-testing';
import { CustomSharedModule } from '../../custom-shared.module';
import { clampQuantity, QuantityStepperComponent } from './quantity-stepper.component';

describe('clampQuantity', () => {
  it('min〜max に収め、数字でなければ min にする', () => {
    expect(clampQuantity('5', 1, 10)).toBe(5);
    expect(clampQuantity(0, 1, 10)).toBe(1);
    expect(clampQuantity(99, 1, 10)).toBe(10);
    expect(clampQuantity('abc', 1, 10)).toBe(1);
    expect(clampQuantity('2.9', 1, 10)).toBe(2);
  });
});

describe('QuantityStepperComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [CustomSharedModule], providers: provideCustomTesting() });
  });

  it('+ と − で数が変わり、下限ではボタンが押せない', async () => {
    const fixture = TestBed.createComponent(QuantityStepperComponent);
    fixture.componentRef.setInput('max', 3);
    fixture.detectChanges();
    await fixture.whenStable();
    const [minus, plus] = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'));

    expect(minus.disabled).toBe(true);
    plus.click();
    plus.click();
    plus.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.value()).toBe(3);
    expect(plus.disabled).toBe(true);

    minus.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.value()).toBe(2);
  });

  it('入力欄にラベルが付いている', async () => {
    const fixture = TestBed.createComponent(QuantityStepperComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const input = el.querySelector('input')!;
    expect(el.querySelector(`label[for="${input.id}"]`)?.textContent).toContain('数量');
  });
});
