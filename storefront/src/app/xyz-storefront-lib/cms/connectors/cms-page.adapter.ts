import { Observable } from 'rxjs';
import { CmsPage, PageContext } from '../../model/cms.model';

export abstract class CmsPageAdapter {
  abstract load(context: PageContext): Observable<CmsPage>;
}
