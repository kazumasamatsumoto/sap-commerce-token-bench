/** 送料が無料になる金額(円、税込の小計)。 */
export const FREE_SHIPPING_THRESHOLD = 5000;

/** ポイントの還元率(小計の 1%)。 */
export const POINT_RATE = 0.01;

export interface FreeShippingStatus {
  reached: boolean;
  /** あと何円で無料か(届いていれば 0)。 */
  remaining: number;
  /** 0〜100 */
  percent: number;
}

/** 小計から、送料無料までの残りを計算する。 */
export function freeShippingStatus(subtotal: number | undefined, threshold = FREE_SHIPPING_THRESHOLD): FreeShippingStatus {
  const value = Math.max(0, subtotal ?? 0);
  const remaining = Math.max(0, threshold - value);
  return {
    reached: remaining === 0,
    remaining,
    percent: Math.min(100, Math.floor((value / threshold) * 100)),
  };
}

/** 小計から、付くポイントを計算する(1 円未満は切り捨て)。 */
export function earnedPoints(subtotal: number | undefined, rate = POINT_RATE): number {
  return Math.floor(Math.max(0, subtotal ?? 0) * rate);
}
