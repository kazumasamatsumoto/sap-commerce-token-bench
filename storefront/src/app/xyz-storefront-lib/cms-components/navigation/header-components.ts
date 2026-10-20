import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ActiveCartService } from '../../cart/facade/active-cart.service';
import { NavigationService } from '../../navigation/navigation.service';

/** SiteLogoComponent(CMSFlexComponent)。 */
@Component({
  selector: 'cx-site-logo',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<a routerLink="/" class="cx-site-logo" aria-label="XYZ Store">XYZ Store</a>`,
})
export class SiteLogoComponent {}

/** SearchBox(CMSFlexComponent)。送ると /search?query=... に移る。 */
@Component({
  selector: 'cx-searchbox',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form role="search" (submit)="submit($event, box.value)">
      <label class="visually-hidden" for="cx-searchbox-input">{{ 'common.search' | cxTranslate }}</label>
      <input id="cx-searchbox-input" #box type="search" name="query" [placeholder]="'searchBox.placeholder' | cxTranslate" />
      <button type="submit">{{ 'common.search' | cxTranslate }}</button>
    </form>
  `,
})
export class SearchBoxComponent {
  private readonly router = inject(Router);

  submit(event: Event, value: string): void {
    event.preventDefault();
    this.router.navigate(['/search'], { queryParams: { query: value.trim() } });
  }
}

/** MiniCartComponent(CMSFlexComponent)。カートの点数を出す。 */
@Component({
  selector: 'cx-mini-cart',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a routerLink="/cart" class="cx-mini-cart" [attr.aria-label]="'miniCart.label' | cxTranslate: { count: (count$ | async) ?? 0 }">
      {{ 'cart.title' | cxTranslate }} <span class="count" data-testid="mini-cart-count">{{ (count$ | async) ?? 0 }}</span>
    </a>
  `,
})
export class MiniCartComponent {
  readonly count$ = inject(ActiveCartService).getTotalItems();
}

/** NavigationComponent(CMSFlexComponent)。カテゴリの最上位を並べる。 */
@Component({
  selector: 'cx-category-navigation',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav [attr.aria-label]="'navigation.categories' | cxTranslate">
      <ul>
        @for (node of nodes$ | async; track node.id) {
          <li><a [routerLink]="node.url">{{ node.name }}</a></li>
        }
      </ul>
    </nav>
  `,
})
export class CategoryNavigationComponent {
  readonly nodes$ = inject(NavigationService).getCategoryNodes();
}

/** FooterComponent(CMSFlexComponent)。 */
@Component({
  selector: 'cx-footer-navigation',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<p class="cx-footer">© XYZ Store</p>`,
})
export class FooterNavigationComponent {}
