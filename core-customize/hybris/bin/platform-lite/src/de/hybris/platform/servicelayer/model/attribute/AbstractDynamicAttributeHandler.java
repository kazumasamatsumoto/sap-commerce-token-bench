package de.hybris.platform.servicelayer.model.attribute;

import de.hybris.platform.core.model.ItemModel;

/** 読むだけの動的属性の土台。set は呼ばれると例外にする。 */
public abstract class AbstractDynamicAttributeHandler<VALUE, MODEL extends ItemModel> implements DynamicAttributeHandler<VALUE, MODEL>
{
	@Override
	public void set(final MODEL model, final VALUE value)
	{
		throw new UnsupportedOperationException("This dynamic attribute is read-only");
	}
}
