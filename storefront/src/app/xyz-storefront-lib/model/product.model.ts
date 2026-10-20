/** OCC の ProductWsDTO と同じ形(使う項目だけ)。 */
export interface Price {
  currencyIso?: string;
  value?: number;
  formattedValue?: string;
  priceType?: 'BUY' | 'FROM';
}

export interface Image {
  url?: string;
  altText?: string;
  format?: 'zoom' | 'product' | 'thumbnail' | 'cartIcon';
  imageType?: 'PRIMARY' | 'GALLERY';
  galleryIndex?: number;
}

export interface Stock {
  stockLevel?: number;
  stockLevelStatus?: 'inStock' | 'lowStock' | 'outOfStock';
}

export interface Category {
  code?: string;
  name?: string;
  url?: string;
}

export interface Product {
  code?: string;
  name?: string;
  summary?: string;
  description?: string;
  url?: string;
  price?: Price;
  images?: Image[];
  stock?: Stock;
  averageRating?: number;
  numberOfReviews?: number;
  manufacturer?: string;
  categories?: Category[];
  purchasable?: boolean;
}

export interface Facet {
  name?: string;
  category?: boolean;
  multiSelect?: boolean;
  values?: { name?: string; count?: number; selected?: boolean; query?: { query?: { value?: string } } }[];
}

export interface Pagination {
  currentPage?: number;
  pageSize?: number;
  totalPages?: number;
  totalResults?: number;
  sort?: string;
}

export interface ProductSearchPage {
  freeTextSearch?: string;
  products?: Product[];
  facets?: Facet[];
  pagination?: Pagination;
  sorts?: { code?: string; name?: string; selected?: boolean }[];
}
