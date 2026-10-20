import { Type } from '@angular/core';

export enum OutletPosition {
  REPLACE = 'replace',
  BEFORE = 'before',
  AFTER = 'after',
}

export interface OutletProviderConfig {
  id: string;
  component: Type<unknown>;
  position?: OutletPosition;
}
