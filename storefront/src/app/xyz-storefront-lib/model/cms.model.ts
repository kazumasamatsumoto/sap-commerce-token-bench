/** OCC の CMSPageWsDTO と CMS コンポーネントの形(使う項目だけ)。 */
export interface CmsComponent {
  uid?: string;
  typeCode?: string;
  name?: string;
  flexType?: string;
  [property: string]: unknown;
}

export interface ContentSlot {
  slotId?: string;
  position?: string;
  components?: { component?: CmsComponent[] };
}

export interface CmsPage {
  uid?: string;
  title?: string;
  template?: string;
  typeCode?: string;
  label?: string;
  contentSlots?: { contentSlot?: ContentSlot[] };
}

export interface PageContext {
  id: string;
  type: 'ContentPage' | 'ProductPage' | 'CategoryPage' | 'CatalogPage';
}
