import { ChangeDetectionStrategy, Component, inject, Input } from '@angular/core';
import { map, Observable, of, switchMap } from 'rxjs';
import { CmsService } from '../cms/facade/cms.service';
import { PageContextService } from '../cms/page/page-context.service';
import { LayoutConfig } from './layout-config';

/**
 * section を指定すれば header / footer などの固定の並び、指定しなければ
 * 今のページのテンプレート(CMS の template)に合わせてスロットを並べる。
 */
@Component({
  selector: 'cx-page-layout',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (layout$ | async; as layout) {
      <div [class]="layout.name">
        <ng-template [cxOutlet]="layout.name">
          @for (slot of layout.slots; track slot) {
            <cx-page-slot [position]="slot"></cx-page-slot>
          }
        </ng-template>
      </div>
    }
  `,
})
export class PageLayoutComponent {
  @Input() section?: string;

  private readonly config = inject(LayoutConfig);
  private readonly cms = inject(CmsService);
  private readonly pageContext = inject(PageContextService);

  readonly layout$: Observable<{ name: string; slots: string[] }> = of(null).pipe(
    switchMap(() =>
      this.section
        ? of(this.section)
        : this.pageContext.context$.pipe(
            switchMap((ctx) => this.cms.getPage(ctx)),
            map((page) => page.template ?? 'ContentPage1Template'),
          ),
    ),
    map((name) => ({ name, slots: this.config.layoutSlots?.[name]?.slots ?? [] })),
  );
}
