import { Injectable } from '@angular/core';
import { Config } from '../config/config';

export interface LayoutSections {
  [templateOrSection: string]: { slots: string[] };
}

/** ページのテンプレートごとに、どのスロットをどの順で並べるか。Spartacus の LayoutConfig と同じ形。 */
@Injectable({ providedIn: 'root', useExisting: Config })
export abstract class LayoutConfig {
  layoutSlots?: LayoutSections;
}

export const defaultLayoutConfig: LayoutConfig = {
  layoutSlots: {
    header: { slots: ['PreHeader', 'SiteContext', 'SiteLinks', 'SiteLogo', 'SearchBox', 'SiteLogin', 'MiniCart', 'NavigationBar'] },
    footer: { slots: ['Footer'] },
    LandingPage2Template: { slots: ['Section1', 'Section2A', 'Section2B', 'Section3', 'Section4', 'Section5'] },
    ContentPage1Template: { slots: ['Section2A', 'Section2B'] },
    CategoryPageTemplate: { slots: ['Section1', 'Section2', 'Section3'] },
    ProductListPageTemplate: { slots: ['ProductLeftRefinements', 'ProductListSlot'] },
    SearchResultsListPageTemplate: { slots: ['Section2', 'ProductLeftRefinements', 'SearchResultsListSlot', 'Section4'] },
    ProductDetailsPageTemplate: { slots: ['Summary', 'UpSelling', 'CrossSelling', 'Tabs', 'PlaceholderContentSlot'] },
    CartPageTemplate: { slots: ['TopContent', 'CenterRightContentSlot', 'EmptyCartMiddleContent', 'BottomContentSlot'] },
    MultiStepCheckoutSummaryPageTemplate: { slots: ['TopContent', 'BodyContent', 'SideContent', 'BottomContent'] },
    OrderConfirmationPageTemplate: { slots: ['BodyContent', 'SideContent'] },
  },
};
