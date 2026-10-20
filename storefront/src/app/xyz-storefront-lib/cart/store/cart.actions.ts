import { createAction, props } from '@ngrx/store';
import { Cart, CartModification } from '../../model/cart.model';

export const loadCart = createAction('[Cart] Load Cart', props<{ userId: string; cartId: string }>());
export const loadCartSuccess = createAction('[Cart] Load Cart Success', props<{ cart: Cart }>());
export const loadCartFail = createAction('[Cart] Load Cart Fail', props<{ error: unknown }>());

export const createCart = createAction('[Cart] Create Cart', props<{ userId: string; addProductCode?: string; quantity?: number }>());
export const createCartSuccess = createAction('[Cart] Create Cart Success', props<{ cart: Cart }>());

export const addEntry = createAction('[Cart] Add Entry', props<{ userId: string; cartId: string; productCode: string; quantity: number }>());
export const addEntrySuccess = createAction('[Cart] Add Entry Success', props<{ modification: CartModification }>());

export const updateEntry = createAction('[Cart] Update Entry', props<{ userId: string; cartId: string; entryNumber: number; quantity: number }>());
export const removeEntry = createAction('[Cart] Remove Entry', props<{ userId: string; cartId: string; entryNumber: number }>());
export const entryChangeSuccess = createAction('[Cart] Entry Change Success', props<{ userId: string; cartId: string }>());

export const cartFail = createAction('[Cart] Fail', props<{ error: unknown }>());
export const clearCart = createAction('[Cart] Clear Cart');
