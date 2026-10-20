import { Product } from '@xyz/storefront';

/** ブランドのロゴ(OCC の ImageWsDTO。format は 'logo')。 */
export interface BrandLogo {
  url?: string;
  altText?: string;
  format?: string;
}

/** OCC の BrandWsDTO(自社拡張 xyzocc)。 */
export interface Brand {
  code?: string;
  name?: string;
  description?: string;
  /** STANDARD / PREMIUM / OUTLET */
  tier?: string;
  logo?: BrandLogo;
}

/** 自社拡張で足した項目を含む商品(fields=FULL のとき返る)。 */
export interface XyzProduct extends Product {
  brand?: Brand;
  newArrival?: boolean;
  material?: string;
}

/** OCC の ReviewWsDTO。 */
export interface Review {
  id?: string;
  headline?: string;
  comment?: string;
  rating?: number;
  date?: string;
  alias?: string;
  principal?: { uid?: string; name?: string };
}

/** 円の表示(¥1,234)。Intl の ja-JP は全角の ¥ になるので自分で組む。 */
export function formatYen(value: number): string {
  return `¥${Math.round(value).toLocaleString('ja-JP')}`;
}
