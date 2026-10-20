import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ComponentWrapperDirective } from './cms/page/component-wrapper.directive';
import { TranslatePipe } from './i18n/translate.pipe';
import { PageLayoutComponent } from './layout/page-layout.component';
import { PageSlotComponent } from './layout/page-slot.component';
import { StorefrontComponent } from './layout/storefront.component';
import { OutletDirective } from './outlet/outlet.directive';

/** 画面の部品が使う宣言をまとめたもの。自社の部品のモジュールはこれを import する。 */
@NgModule({
  imports: [CommonModule, RouterModule],
  declarations: [StorefrontComponent, PageLayoutComponent, PageSlotComponent, ComponentWrapperDirective, OutletDirective, TranslatePipe],
  exports: [StorefrontComponent, PageLayoutComponent, PageSlotComponent, ComponentWrapperDirective, OutletDirective, TranslatePipe],
})
export class StorefrontComponentsModule {}
