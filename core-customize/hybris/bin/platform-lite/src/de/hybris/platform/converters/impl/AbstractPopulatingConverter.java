package de.hybris.platform.converters.impl;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import java.util.ArrayList;
import java.util.List;

/**
 * 本物と同じく、*-spring.xml で targetClass と populators を渡して使う:
 * <pre>
 * &lt;bean id="productConverter" parent="abstractPopulatingConverter"&gt;
 *     &lt;property name="targetClass" value="de.hybris.platform.commercefacades.product.data.ProductData"/&gt;
 *     &lt;property name="populators"&gt;&lt;list&gt;&lt;ref bean="productBasicPopulator"/&gt;&lt;/list&gt;&lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * 拡張から Populator を足すときは modifyPopulatorList を使う(ModifyPopulatorList)。
 */
public class AbstractPopulatingConverter<SOURCE, TARGET> implements Converter<SOURCE, TARGET>
{
	private Class<TARGET> targetClass;
	private List<Populator<SOURCE, TARGET>> populators = new ArrayList<>();

	@Override
	public TARGET convert(final SOURCE source) throws ConversionException
	{
		if (source == null)
		{
			return null;
		}
		return convert(source, createTarget());
	}

	@Override
	public TARGET convert(final SOURCE source, final TARGET prototype) throws ConversionException
	{
		for (final Populator<SOURCE, TARGET> p : populators)
		{
			p.populate(source, prototype);
		}
		return prototype;
	}

	protected TARGET createTarget()
	{
		try
		{
			return targetClass.getDeclaredConstructor().newInstance();
		}
		catch (final ReflectiveOperationException e)
		{
			throw new ConversionException("cannot create " + targetClass, e);
		}
	}

	public void setTargetClass(final Class<TARGET> targetClass)
	{
		this.targetClass = targetClass;
	}

	public Class<TARGET> getTargetClass()
	{
		return targetClass;
	}

	public void setPopulators(final List<Populator<SOURCE, TARGET>> populators)
	{
		this.populators = new ArrayList<>(populators);
	}

	public List<Populator<SOURCE, TARGET>> getPopulators()
	{
		return populators;
	}

	public void addPopulator(final Populator<SOURCE, TARGET> populator)
	{
		populators.add(populator);
	}

	public void removePopulator(final Populator<SOURCE, TARGET> populator)
	{
		populators.remove(populator);
	}
}
