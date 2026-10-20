import { createFeatureSelector, createReducer, createSelector, on } from '@ngrx/store';
import { Cart, CartModification } from '../../model/cart.model';
import * as CartActions from './cart.actions';

export const CART_FEATURE = 'cart';

export interface CartState {
  active?: Cart;
  loading: boolean;
  error: boolean;
  lastModification?: CartModification;
}

export const initialCartState: CartState = { loading: false, error: false };

export const cartReducer = createReducer(
  initialCartState,
  on(CartActions.loadCart, CartActions.createCart, CartActions.addEntry, CartActions.updateEntry, CartActions.removeEntry, (state) => ({
    ...state,
    loading: true,
    error: false,
  })),
  on(CartActions.loadCartSuccess, CartActions.createCartSuccess, (state, { cart }) => ({ ...state, active: cart, loading: false })),
  on(CartActions.addEntrySuccess, (state, { modification }) => ({ ...state, lastModification: modification })),
  on(CartActions.loadCartFail, CartActions.cartFail, (state) => ({ ...state, loading: false, error: true })),
  on(CartActions.clearCart, () => initialCartState),
);

export const selectCartState = createFeatureSelector<CartState>(CART_FEATURE);
export const selectActiveCart = createSelector(selectCartState, (s) => s.active);
export const selectCartLoading = createSelector(selectCartState, (s) => s.loading);
export const selectLastModification = createSelector(selectCartState, (s) => s.lastModification);
