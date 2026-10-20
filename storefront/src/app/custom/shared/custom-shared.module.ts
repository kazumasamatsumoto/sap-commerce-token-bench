import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CmsComponentsModule, StorefrontComponentsModule } from '@xyz/storefront';
import { EmptyStateComponent } from './components/empty-state/empty-state.component';
import { PriceBadgeComponent } from './components/price-badge/price-badge.component';
import { ProductCardComponent } from './components/product-card/product-card.component';
import { QuantityStepperComponent } from './components/quantity-stepper/quantity-stepper.component';
import { RatingStarsComponent } from './components/rating-stars/rating-stars.component';
import { SectionHeadingComponent } from './components/section-heading/section-heading.component';

const COMPONENTS = [
  RatingStarsComponent,
  PriceBadgeComponent,
  QuantityStepperComponent,
  ProductCardComponent,
  SectionHeadingComponent,
  EmptyStateComponent,
];

/**
 * 自社の部品で使い回す小さな UI。各機能のモジュールはこれを import する。
 * cxTranslate と cx-media も一緒に export する(機能のモジュールの import を短くするため)。
 */
@NgModule({
  imports: [CommonModule, RouterModule, StorefrontComponentsModule, CmsComponentsModule],
  declarations: COMPONENTS,
  exports: [...COMPONENTS, CommonModule, RouterModule, StorefrontComponentsModule, CmsComponentsModule],
})
export class CustomSharedModule {}
