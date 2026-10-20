import { NgModule } from '@angular/core';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { ProductAdapter } from './connectors/product.adapter';
import { OccProductAdapter } from './occ/occ-product.adapter';
import { ProductEffects } from './store/product.effects';
import { PRODUCT_FEATURE, productReducer } from './store/product.reducer';

@NgModule({
  imports: [StoreModule.forFeature(PRODUCT_FEATURE, productReducer), EffectsModule.forFeature([ProductEffects])],
  providers: [{ provide: ProductAdapter, useClass: OccProductAdapter }],
})
export class ProductModule {}
