import { Observable } from 'rxjs';
import { CmsComponent } from '../../model/cms.model';

/**
 * CMS の部品に注入される、その部品のデータ。Spartacus の CmsComponentData と同じ使い方:
 * `private readonly data = inject(CmsComponentData<CmsBannerComponent>)`
 */
export abstract class CmsComponentData<T extends CmsComponent = CmsComponent> {
  abstract uid: string;
  abstract data$: Observable<T>;
}
