import { Directive, inject, Input, OnChanges, TemplateRef, ViewContainerRef } from '@angular/core';
import { OutletPosition } from './outlet.model';
import { OutletService } from './outlet.service';

/**
 * `<ng-template [cxOutlet]="'PDP.SUMMARY'">標準の中身</ng-template>` のように使う。
 * REPLACE が登録されていれば標準の中身の代わりに、BEFORE / AFTER は前後に描く。
 */
@Directive({ selector: '[cxOutlet]', standalone: false })
export class OutletDirective implements OnChanges {
  @Input({ required: true }) cxOutlet!: string;
  @Input() cxOutletContext?: unknown;

  private readonly vcr = inject(ViewContainerRef);
  private readonly template = inject(TemplateRef<unknown>);
  private readonly outlets = inject(OutletService);

  ngOnChanges(): void {
    this.vcr.clear();
    for (const c of this.outlets.get(this.cxOutlet, OutletPosition.BEFORE)) {
      this.vcr.createComponent(c);
    }
    const replace = this.outlets.get(this.cxOutlet, OutletPosition.REPLACE);
    if (replace.length) {
      replace.forEach((c) => this.vcr.createComponent(c));
    } else {
      this.vcr.createEmbeddedView(this.template, { $implicit: this.cxOutletContext });
    }
    for (const c of this.outlets.get(this.cxOutlet, OutletPosition.AFTER)) {
      this.vcr.createComponent(c);
    }
  }
}
