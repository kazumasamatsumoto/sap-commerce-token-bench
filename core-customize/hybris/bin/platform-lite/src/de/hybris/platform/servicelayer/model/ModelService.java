package de.hybris.platform.servicelayer.model;

import de.hybris.platform.core.PK;
import java.util.Collection;

/** Model の作成・保存・削除。本物の ModelService のうち、よく使うものだけ。 */
public interface ModelService
{
	<T> T create(Class<T> modelClass);

	<T> T create(String typeCode);

	void save(Object model);

	void saveAll(Collection<?> models);

	void saveAll(Object... models);

	void remove(Object model);

	void removeAll(Collection<?> models);

	void refresh(Object model);

	<T> T get(PK pk);

	boolean isNew(Object model);

	boolean isModified(Object model);

	<T> T getAttributeValue(Object model, String attributeQualifier);

	void setAttributeValue(Object model, String attributeQualifier, Object value);
}
