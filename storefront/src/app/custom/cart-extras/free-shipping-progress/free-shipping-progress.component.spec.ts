import { TestBed } from '@angular/core/testing';
import { ActiveCartService, Cart, PageContext, PageContextService } from '@xyz/storefront';
import { of } from 'rxjs';
import { provideCustomTesting } from '../../testing/custom-testing';
import { CartExtrasModule } from '../cart-extras.module';
import { earnedPoints, freeShippingStatus } from '../free-shipping';
import { FreeShippingProgressComponent } from './free-shipping-progress.component';

describe('freeShippingStatus / earnedPoints', () => {
  it('5,000 円までの残りと割合を出す', () => {
    expect(freeShippingStatus(3000)).toEqual({ reached: false, remaining: 2000, percent: 60 });
    expect(freeShippingStatus(5000)).toEqual({ reached: true, remaining: 0, percent: 100 });
    expect(freeShippingStatus(undefined)).toEqual({ reached: false, remaining: 5000, percent: 0 });
  });

  it('ポイントは小計の 1%(切り捨て)', () => {
    expect(earnedPoints(16480)).toBe(164);
    expect(earnedPoints(undefined)).toBe(0);
  });
});

describe('FreeShippingProgressComponent', () => {
  function setup(context: PageContext, cart: Cart | undefined): HTMLElement {
    TestBed.configureTestingModule({
      imports: [CartExtrasModule],
      providers: [
        provideCustomTesting(),
        { provide: ActiveCartService, useValue: { getActive: () => of(cart) } },
        { provide: PageContextService, useValue: { context$: of(context) } },
      ],
    });
    const fixture = TestBed.createComponent(FreeShippingProgressComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const cart: Cart = { entries: [{ entryNumber: 0, quantity: 1 }], subTotal: { value: 3200 } };

  it('カートの画面で、あといくらかと進み具合を出す', () => {
    const el = setup({ type: 'ContentPage', id: '/cart' }, cart);
    expect(el.textContent).toContain('あと ¥1,800 で送料無料');
    expect(el.querySelector('[role="progressbar"]')?.getAttribute('aria-valuenow')).toBe('64');
  });

  it('チェックアウトの画面では出さない', () => {
    const el = setup({ type: 'ContentPage', id: '/checkout' }, cart);
    expect(el.querySelector('[data-testid="xyz-free-shipping-progress"]')).toBeNull();
  });

  it('カートが空なら出さない', () => {
    const el = setup({ type: 'ContentPage', id: '/cart' }, { entries: [] });
    expect(el.querySelector('[data-testid="xyz-free-shipping-progress"]')).toBeNull();
  });
});
