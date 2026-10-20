import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { concatMap } from 'rxjs';
import { ActiveCartService } from '../../cart/facade/active-cart.service';
import { CheckoutService } from '../../checkout/facade/checkout.service';
import { DeliveryMode } from '../../model/cart.model';

/**
 * CheckoutComponent(CMSFlexComponent)。ゲストの購入手続きを 1 ページで行う:
 * 1) メールと届け先 → 2) 配送方法 → 3) 支払い → 注文の確定。
 */
@Component({
  selector: 'cx-checkout',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="cx-checkout">
      <h1>{{ 'checkout.title' | cxTranslate }}</h1>
      @if (cart$ | async; as cart) {
        <p class="cx-checkout-total">{{ 'cart.total' | cxTranslate }}: <span data-testid="checkout-total">{{ cart.totalPrice?.formattedValue }}</span></p>
      }

      <form [formGroup]="addressForm" (ngSubmit)="submitAddress()" data-testid="address-form">
        <h2>1. {{ 'checkout.deliveryAddress' | cxTranslate }}</h2>
        <label>{{ 'checkout.email' | cxTranslate }} <input formControlName="email" type="email" autocomplete="email" /></label>
        <label>{{ 'checkout.lastName' | cxTranslate }} <input formControlName="lastName" autocomplete="family-name" /></label>
        <label>{{ 'checkout.firstName' | cxTranslate }} <input formControlName="firstName" autocomplete="given-name" /></label>
        <label>{{ 'checkout.postalCode' | cxTranslate }} <input formControlName="postalCode" autocomplete="postal-code" /></label>
        <label>{{ 'checkout.town' | cxTranslate }} <input formControlName="town" /></label>
        <label>{{ 'checkout.line1' | cxTranslate }} <input formControlName="line1" /></label>
        <button type="submit" [disabled]="addressForm.invalid || busy()">{{ 'checkout.next' | cxTranslate }}</button>
      </form>

      @if (deliveryModes().length) {
        <fieldset data-testid="delivery-modes">
          <legend>2. {{ 'checkout.deliveryMode' | cxTranslate }}</legend>
          @for (mode of deliveryModes(); track mode.code) {
            <label>
              <input type="radio" name="deliveryMode" [value]="mode.code" (change)="selectDeliveryMode(mode.code!)" />
              {{ mode.name }} ({{ mode.deliveryCost?.formattedValue }})
            </label>
          }
        </fieldset>
      }

      @if (deliveryModeSet()) {
        <form [formGroup]="paymentForm" (ngSubmit)="submitPayment()" data-testid="payment-form">
          <h2>3. {{ 'checkout.payment' | cxTranslate }}</h2>
          <label>{{ 'checkout.cardHolder' | cxTranslate }} <input formControlName="accountHolderName" autocomplete="cc-name" /></label>
          <label>{{ 'checkout.cardNumber' | cxTranslate }} <input formControlName="cardNumber" inputmode="numeric" autocomplete="cc-number" /></label>
          <label>{{ 'checkout.expiryMonth' | cxTranslate }} <input formControlName="expiryMonth" autocomplete="cc-exp-month" /></label>
          <label>{{ 'checkout.expiryYear' | cxTranslate }} <input formControlName="expiryYear" autocomplete="cc-exp-year" /></label>
          <button type="submit" [disabled]="paymentForm.invalid || busy()" data-testid="place-order">{{ 'checkout.placeOrder' | cxTranslate }}</button>
        </form>
      }

      @if (error()) {
        <p role="alert" class="error">{{ error() }}</p>
      }
    </section>
  `,
})
export class CheckoutComponent {
  private readonly checkout = inject(CheckoutService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly cart$ = inject(ActiveCartService).getActive();
  readonly deliveryModes = signal<DeliveryMode[]>([]);
  readonly deliveryModeSet = signal(false);
  readonly busy = signal(false);
  readonly error = signal<string | undefined>(undefined);

  readonly addressForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    lastName: ['', Validators.required],
    firstName: ['', Validators.required],
    postalCode: ['', [Validators.required, Validators.pattern(/^\d{3}-?\d{4}$/)]],
    town: ['', Validators.required],
    line1: ['', Validators.required],
  });

  readonly paymentForm = this.fb.nonNullable.group({
    accountHolderName: ['', Validators.required],
    cardNumber: ['', [Validators.required, Validators.pattern(/^\d{12,19}$/)]],
    expiryMonth: ['', [Validators.required, Validators.pattern(/^(0?[1-9]|1[0-2])$/)]],
    expiryYear: ['', [Validators.required, Validators.pattern(/^20\d{2}$/)]],
  });

  submitAddress(): void {
    const { email, ...address } = this.addressForm.getRawValue();
    this.run(
      this.checkout.setGuestEmail(email).pipe(concatMap(() => this.checkout.setDeliveryAddress({ ...address, country: { isocode: 'JP' } }))),
      (modes) => this.deliveryModes.set(modes as DeliveryMode[]),
    );
  }

  selectDeliveryMode(code: string): void {
    this.run(this.checkout.setDeliveryMode(code), () => this.deliveryModeSet.set(true));
  }

  submitPayment(): void {
    this.run(this.checkout.setPaymentDetails(this.paymentForm.getRawValue()).pipe(concatMap(() => this.checkout.placeOrder())), (order) =>
      this.router.navigate(['/order-confirmation'], { queryParams: { guid: (order as { guid?: string }).guid } }),
    );
  }

  private run<T>(obs: import('rxjs').Observable<T>, next: (value: T) => void): void {
    this.busy.set(true);
    this.error.set(undefined);
    obs.subscribe({
      next: (v) => {
        this.busy.set(false);
        next(v);
      },
      error: (e: { error?: { errors?: { message?: string }[] }; message?: string }) => {
        this.busy.set(false);
        this.error.set(e.error?.errors?.[0]?.message ?? e.message ?? 'error');
      },
    });
  }
}
