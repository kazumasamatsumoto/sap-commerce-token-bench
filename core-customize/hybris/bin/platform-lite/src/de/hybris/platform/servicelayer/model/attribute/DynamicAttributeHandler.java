package de.hybris.platform.servicelayer.model.attribute;

import de.hybris.platform.core.model.ItemModel;

/** items.xml で persistence type="dynamic" attributeHandler="<bean 名>" にした属性の値を作る。 */
public interface DynamicAttributeHandler<VALUE, MODEL extends ItemModel>
{
	VALUE get(MODEL model);

	void set(MODEL model, VALUE value);
}
