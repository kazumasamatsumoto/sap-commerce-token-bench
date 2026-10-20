import { createAction, props } from '@ngrx/store';
import { Product, ProductSearchPage } from '../../model/product.model';

export const loadProduct = createAction('[Product] Load Product', props<{ productCode: string }>());
export const loadProductSuccess = createAction('[Product] Load Product Success', props<{ product: Product }>());
export const loadProductFail = createAction('[Product] Load Product Fail', props<{ productCode: string; error: unknown }>());

export const searchProducts = createAction(
  '[Product] Search Products',
  props<{ query: string; currentPage?: number; pageSize?: number; sort?: string }>(),
);
export const searchProductsSuccess = createAction('[Product] Search Products Success', props<{ result: ProductSearchPage }>());
export const searchProductsFail = createAction('[Product] Search Products Fail', props<{ error: unknown }>());
