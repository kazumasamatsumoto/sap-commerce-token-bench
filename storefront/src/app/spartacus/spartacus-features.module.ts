import { NgModule } from '@angular/core';
import { CustomFeaturesModule } from '../custom/custom-features.module';

/**
 * 機能ごとのモジュールを並べる場所。Spartacus の spartacus-features.module.ts と同じ置き場所。
 * 標準の部品は疑似ライブラリ(StorefrontModule)に入っている。自社の部品は CustomFeaturesModule にまとめた。
 */
@NgModule({
  imports: [CustomFeaturesModule],
})
export class SpartacusFeaturesModule {}
