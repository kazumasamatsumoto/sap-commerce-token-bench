import { Directive, inject, Injector, Input, OnChanges, ViewContainerRef } from '@angular/core';
import { of } from 'rxjs';
import { CmsComponent } from '../../model/cms.model';
import { CmsConfig } from '../config/cms-config';
import { CmsComponentData } from '../model/cms-component-data';

/**
 * CMS のコンポーネント 1 つを、対応表(cmsComponents)で引いた Angular の部品として描く。
 * Spartacus の ComponentWrapperDirective と同じ役目。対応が無い typeCode は何も描かない。
 */
@Directive({ selector: '[cxComponentWrapper]', standalone: false })
export class ComponentWrapperDirective implements OnChanges {
  @Input({ required: true }) cxComponentWrapper!: CmsComponent;

  private readonly vcr = inject(ViewContainerRef);
  private readonly injector = inject(Injector);
  private readonly config = inject(CmsConfig);

  ngOnChanges(): void {
    this.vcr.clear();
    const data = this.cxComponentWrapper;
    const mapping = this.config.cmsComponents?.[data.flexType ?? data.typeCode ?? ''];
    if (!mapping || mapping.disabled) {
      return;
    }
    const componentData: CmsComponentData = { uid: data.uid ?? '', data$: of(data) };
    const injector = Injector.create({
      providers: [{ provide: CmsComponentData, useValue: componentData }],
      parent: this.injector,
    });
    this.vcr.createComponent(mapping.component, { injector });
  }
}
