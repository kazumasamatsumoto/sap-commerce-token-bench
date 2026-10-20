import { ChangeDetectionStrategy, Component, inject, Input, OnChanges } from '@angular/core';
import { Observable, of, switchMap } from 'rxjs';
import { CmsService } from '../cms/facade/cms.service';
import { PageContextService } from '../cms/page/page-context.service';
import { CmsComponent } from '../model/cms.model';

/** 1 つのスロットに入っている CMS コンポーネントを順に描く。スロットの前後は Outlet で足せる。 */
@Component({
  selector: 'cx-page-slot',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-template [cxOutlet]="position">
      @for (component of components$ | async; track component.uid) {
        <ng-container [cxComponentWrapper]="component"></ng-container>
      }
    </ng-template>
  `,
  host: { '[class]': 'position' },
})
export class PageSlotComponent implements OnChanges {
  @Input({ required: true }) position!: string;

  private readonly cms = inject(CmsService);
  private readonly pageContext = inject(PageContextService);
  components$: Observable<CmsComponent[]> = of([]);

  ngOnChanges(): void {
    this.components$ = this.pageContext.context$.pipe(switchMap((ctx) => this.cms.getComponents(ctx, this.position)));
  }
}
