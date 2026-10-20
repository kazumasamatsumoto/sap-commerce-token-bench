package de.hybris.platform.converters.config;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.converters.impl.AbstractPopulatingConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * 標準の Converter に、拡張から Populator を足す(または外す)。本物と同じく *-spring.xml にこう書く:
 * <pre>
 * &lt;bean parent="modifyPopulatorList"&gt;
 *     &lt;property name="list" ref="productConverter"/&gt;
 *     &lt;property name="add" ref="xyzProductBrandPopulator"/&gt;
 * &lt;/bean&gt;
 * </pre>
 */
public class ModifyPopulatorList<S, T> implements InitializingBean
{
	private AbstractPopulatingConverter<S, T> list;
	private Populator<S, T> add;
	private Populator<S, T> remove;

	@Override
	public void afterPropertiesSet()
	{
		if (list == null)
		{
			throw new IllegalStateException("modifyPopulatorList needs the property 'list' (the converter to change)");
		}
		if (remove != null)
		{
			list.removePopulator(remove);
		}
		if (add != null && !list.getPopulators().contains(add))
		{
			list.addPopulator(add);
		}
	}

	public void setList(final AbstractPopulatingConverter<S, T> list)
	{
		this.list = list;
	}

	public void setAdd(final Populator<S, T> add)
	{
		this.add = add;
	}

	public void setRemove(final Populator<S, T> remove)
	{
		this.remove = remove;
	}
}
