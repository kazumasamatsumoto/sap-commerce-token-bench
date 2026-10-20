import p1 from './product-XYZ-000001.json';
import p2 from './product-XYZ-000002.json';
import p3 from './product-XYZ-000003.json';
import p4 from './product-XYZ-000004.json';
import p5 from './product-XYZ-000005.json';
import p6 from './product-XYZ-000006.json';
import p7 from './product-XYZ-000007.json';
import p8 from './product-XYZ-000008.json';
import p9 from './product-XYZ-000009.json';
import p10 from './product-XYZ-000010.json';
import p11 from './product-XYZ-000011.json';
import p12 from './product-XYZ-000012.json';

/** 商品詳細のモック(code → ProductWsDTO)。録った 12 件だけを持つ。 */
export const products: Record<string, unknown> = Object.fromEntries(
  [p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12].map((p) => [(p as { code: string }).code, p]),
);
