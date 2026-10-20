import { createFeatureSelector, createReducer, createSelector, on } from '@ngrx/store';
import { Product, ProductSearchPage } from '../../model/product.model';
import * as ProductActions from './product.actions';

export const PRODUCT_FEATURE = 'product';

/** Spartacus の loader state と同じく、値と loading / error を一緒に持つ。 */
export interface LoaderState<T> {
  value?: T;
  loading: boolean;
  error: boolean;
}

export interface ProductState {
  details: Record<string, LoaderState<Product>>;
  search: LoaderState<ProductSearchPage>;
}

export const initialState: ProductState = {
  details: {},
  search: { loading: false, error: false },
};

export const productReducer = createReducer(
  initialState,
  on(ProductActions.loadProduct, (state, { productCode }) => ({
    ...state,
    details: { ...state.details, [productCode]: { ...state.details[productCode], loading: true, error: false } },
  })),
  on(ProductActions.loadProductSuccess, (state, { product }) => ({
    ...state,
    details: { ...state.details, [product.code ?? '']: { value: product, loading: false, error: false } },
  })),
  on(ProductActions.loadProductFail, (state, { productCode }) => ({
    ...state,
    details: { ...state.details, [productCode]: { loading: false, error: true } },
  })),
  on(ProductActions.searchProducts, (state) => ({ ...state, search: { ...state.search, loading: true, error: false } })),
  on(ProductActions.searchProductsSuccess, (state, { result }) => ({ ...state, search: { value: result, loading: false, error: false } })),
  on(ProductActions.searchProductsFail, (state) => ({ ...state, search: { loading: false, error: true } })),
);

export const selectProductState = createFeatureSelector<ProductState>(PRODUCT_FEATURE);
export const selectProductDetails = (code: string) => createSelector(selectProductState, (s) => s.details[code]);
export const selectSearchResult = createSelector(selectProductState, (s) => s.search);
