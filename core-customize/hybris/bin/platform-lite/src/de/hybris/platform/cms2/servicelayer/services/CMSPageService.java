package de.hybris.platform.cms2.servicelayer.services;

import de.hybris.platform.cms2.model.contents.contentslot.ContentSlotModel;
import de.hybris.platform.cms2.model.pages.AbstractPageModel;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import java.util.Map;

public interface CMSPageService
{
	/** ラベル(/cart など)か uid でページを探す。無ければ CMSItemNotFoundException。 */
	ContentPageModel getPageForLabelOrId(String labelOrId) throws de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;

	ContentPageModel getHomepage() throws de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;

	/** 商品詳細・カテゴリのページ(既定のページ)。 */
	AbstractPageModel getDefaultPage(String pageTypeCode) throws de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;

	/** ページに表示するスロット(テンプレートのスロットとページのスロット。ページの方が勝つ)。position → slot。 */
	Map<String, ContentSlotModel> getContentSlotsForPage(AbstractPageModel page);
}
