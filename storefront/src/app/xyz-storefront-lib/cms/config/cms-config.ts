import { Injectable, Type } from '@angular/core';
import { Config } from '../../config/config';

export interface CmsComponentMapping {
  component: Type<unknown>;
  /** 標準の部品を表示しない(Spartacus の disableSSR とは別。ここでは非表示の印だけ)。 */
  disabled?: boolean;
}

export interface CmsComponentsMapping {
  [typeCode: string]: CmsComponentMapping;
}

/**
 * CMS のコンポーネント(typeCode)と Angular の部品の対応表。
 * 標準の部品を自社の部品に差し替えるときは、同じ typeCode で provideConfig する。
 */
@Injectable({ providedIn: 'root', useExisting: Config })
export abstract class CmsConfig {
  cmsComponents?: CmsComponentsMapping;
}
