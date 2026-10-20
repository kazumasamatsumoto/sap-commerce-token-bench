import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { filter, map, switchMap } from 'rxjs';
import { CheckoutService } from '../../checkout/facade/checkout.service';

/** OrderConfirmationComponent(CMSFlexComponent)。?guid= の注文を出す。 */
@Component({
  selector: 'cx-order-confirmation',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (order$ | async; as order) {
      <section class="cx-order-confirmation">
        <h1 data-testid="order-confirmation">{{ 'checkout.orderConfirmation' | cxTranslate }}</h1>
        <p>{{ 'checkout.orderNumber' | cxTranslate }}: <strong data-testid="order-code">{{ order.code }}</strong></p>
        <p>{{ 'cart.total' | cxTranslate }}: {{ order.totalPrice?.formattedValue }}</p>
        <ul>
          @for (entry of order.entries; track entry.entryNumber) {
            <li>{{ entry.product?.name }} × {{ entry.quantity }}</li>
          }
        </ul>
      </section>
    }
  `,
})
export class OrderConfirmationComponent {
  private readonly checkout = inject(CheckoutService);
  readonly order$ = inject(ActivatedRoute).queryParams.pipe(
    map((p) => p['guid'] as string | undefined),
    filter((guid): guid is string => !!guid),
    switchMap((guid) => this.checkout.loadOrder(guid)),
  );
}
