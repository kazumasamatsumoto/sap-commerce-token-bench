package com.xyz.core.attributehandlers;

import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.model.attribute.AbstractDynamicAttributeHandler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Product.xyzNewArrival(動的属性)。発売日から newArrivalDays 日以内なら true。発売前と、発売日が無い商品は false。 */
public class XyzNewArrivalAttributeHandler extends AbstractDynamicAttributeHandler<Boolean, ProductModel>
{
	private int newArrivalDays = 30;
	private Clock clock = Clock.systemDefaultZone();

	@Override
	public Boolean get(final ProductModel product)
	{
		final Date launch = product.getXyzLaunchDate();
		if (launch == null)
		{
			return Boolean.FALSE;
		}
		final Instant now = clock.instant();
		final Instant launched = launch.toInstant();
		return !launched.isAfter(now) && Duration.between(launched, now).toDays() < newArrivalDays;
	}

	public void setNewArrivalDays(final int newArrivalDays)
	{
		this.newArrivalDays = newArrivalDays;
	}

	public void setClock(final Clock clock)
	{
		this.clock = clock;
	}
}
