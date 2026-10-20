import { ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest, map, Observable, Subscription } from 'rxjs';
import { PageContextService } from '../../cms/page/page-context.service';
import { ProductSearchPage } from '../../model/product.model';
import { ProductService } from '../../product/facade/product.service';

export const PRODUCT_LIST_PAGE_SIZE = 12;

/** 今の URL から OCC の検索の query を作る。/category/xxx はカテゴリで絞る。 */
export function searchQueryFromRoute(contextType: string, contextId: string, params: Record<string, string>): string {
  if (params['q']) {
    return params['q'];
  }
  const text = params['query'] ?? '';
  return contextType === 'CategoryPage' ? `${text}:relevance:category:${contextId}` : text;
}

/** ProductListComponent / SearchResultsListComponent(CMSFlexComponent)。 */
@Component({
  selector: 'cx-product-list',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (results$ | async; as results) {
      <section class="cx-product-list" aria-live="polite">
        <div class="cx-product-list-header">
          <p data-testid="result-count">{{ 'productList.results' | cxTranslate: { count: results.pagination?.totalResults ?? 0 } }}</p>
          <label>
            {{ 'productList.sortBy' | cxTranslate }}
            <select (change)="sort($any($event.target).value)">
              @for (s of results.sorts; track s.code) {
                <option [value]="s.code" [selected]="s.selected">{{ s.name }}</option>
              }
            </select>
          </label>
        </div>
        <ul class="cx-product-grid">
          @for (product of results.products; track product.code) {
            <li class="cx-product-grid-item" data-testid="product-item">
              <a [routerLink]="['/product', product.code]">
                <cx-media [src]="product.images?.[0]?.url" [alt]="product.name" [width]="200"></cx-media>
                <span class="name">{{ product.name }}</span>
              </a>
              <span class="price">{{ product.price?.formattedValue }}</span>
              @if (product.stock?.stockLevelStatus === 'outOfStock') {
                <span class="stock out">{{ 'product.outOfStock' | cxTranslate }}</span>
              }
            </li>
          } @empty {
            <li>{{ 'productList.noResults' | cxTranslate }}</li>
          }
        </ul>
        <nav class="cx-pagination" [attr.aria-label]="'productList.pagination' | cxTranslate">
          <button type="button" [disabled]="(results.pagination?.currentPage ?? 0) === 0" (click)="page((results.pagination?.currentPage ?? 0) - 1)">
            {{ 'productList.previous' | cxTranslate }}
          </button>
          <span>{{ (results.pagination?.currentPage ?? 0) + 1 }} / {{ results.pagination?.totalPages ?? 1 }}</span>
          <button
            type="button"
            [disabled]="(results.pagination?.currentPage ?? 0) + 1 >= (results.pagination?.totalPages ?? 1)"
            (click)="page((results.pagination?.currentPage ?? 0) + 1)"
          >
            {{ 'productList.next' | cxTranslate }}
          </button>
        </nav>
      </section>
    }
  `,
})
export class ProductListComponent implements OnInit, OnDestroy {
  private readonly productService = inject(ProductService);
  private readonly pageContext = inject(PageContextService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private subscription?: Subscription;

  readonly results$: Observable<ProductSearchPage> = this.productService.getResults();

  ngOnInit(): void {
    this.subscription = combineLatest([this.pageContext.context$, this.route.queryParams])
      .pipe(
        map(([ctx, params]) => ({
          query: searchQueryFromRoute(ctx.type, ctx.id, params as Record<string, string>),
          currentPage: Number(params['currentPage'] ?? 0),
          sort: params['sort'] as string | undefined,
        })),
      )
      .subscribe(({ query, currentPage, sort }) => this.productService.search(query, { currentPage, pageSize: PRODUCT_LIST_PAGE_SIZE, sort }));
  }

  sort(code: string): void {
    this.router.navigate([], { queryParams: { sort: code, currentPage: 0 }, queryParamsHandling: 'merge' });
  }

  page(currentPage: number): void {
    this.router.navigate([], { queryParams: { currentPage }, queryParamsHandling: 'merge' });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }
}

/** ProductFacetNavigationComponent(CMSFlexComponent)。絞り込みを押すと、その query で検索し直す。 */
@Component({
  selector: 'cx-facet-navigation',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (results$ | async; as results) {
      <aside class="cx-facets" [attr.aria-label]="'productList.filterBy' | cxTranslate">
        @for (facet of results.facets; track facet.name) {
          <fieldset>
            <legend>{{ facet.name }}</legend>
            @for (value of facet.values; track value.name) {
              <a [routerLink]="['/search']" [queryParams]="{ q: value.query?.query?.value }" [class.selected]="value.selected">
                {{ value.name }} ({{ value.count }})
              </a>
            }
          </fieldset>
        }
      </aside>
    }
  `,
})
export class ProductFacetNavigationComponent {
  readonly results$ = inject(ProductService).getResults();
}
