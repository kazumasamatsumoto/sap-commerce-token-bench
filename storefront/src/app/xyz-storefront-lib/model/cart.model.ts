import { Price, Product } from './product.model';

/** OCC の CartWsDTO / OrderWsDTO / AddressWsDTO と同じ形(使う項目だけ)。 */
export interface OrderEntry {
  entryNumber?: number;
  quantity?: number;
  basePrice?: Price;
  totalPrice?: Price;
  product?: Product;
  updateable?: boolean;
}

export interface Country {
  isocode?: string;
  name?: string;
}

export interface Address {
  id?: string;
  titleCode?: string;
  firstName?: string;
  lastName?: string;
  line1?: string;
  line2?: string;
  town?: string;
  postalCode?: string;
  phone?: string;
  email?: string;
  country?: Country;
  formattedAddress?: string;
}

export interface DeliveryMode {
  code?: string;
  name?: string;
  description?: string;
  deliveryCost?: Price;
}

export interface PaymentDetails {
  id?: string;
  accountHolderName?: string;
  cardNumber?: string;
  expiryMonth?: string;
  expiryYear?: string;
}

export interface Cart {
  code?: string;
  guid?: string;
  entries?: OrderEntry[];
  totalItems?: number;
  totalUnitCount?: number;
  subTotal?: Price;
  deliveryCost?: Price;
  totalTax?: Price;
  totalPrice?: Price;
  totalPriceWithTax?: Price;
  deliveryAddress?: Address;
  deliveryMode?: DeliveryMode;
  paymentInfo?: PaymentDetails;
  user?: { uid?: string; name?: string };
}

export interface Order extends Cart {
  created?: string;
  status?: string;
  statusDisplay?: string;
  guestCustomer?: boolean;
}

export interface CartModification {
  statusCode?: 'success' | 'lowStock' | 'noStock';
  quantityAdded?: number;
  quantity?: number;
  entry?: OrderEntry;
}

export const OCC_USER_ID_ANONYMOUS = 'anonymous';
export const OCC_USER_ID_CURRENT = 'current';
