package de.hybris.platform.commercefacades.product.converters.populators;

import de.hybris.platform.commercefacades.product.data.ReviewData;
import de.hybris.platform.commercefacades.user.data.PrincipalData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.customerreview.model.CustomerReviewModel;

public class ReviewPopulator implements Populator<CustomerReviewModel, ReviewData>
{
	@Override
	public void populate(final CustomerReviewModel source, final ReviewData target)
	{
		target.setId(source.getPk().toString());
		target.setHeadline(source.getHeadline());
		target.setComment(source.getComment());
		target.setRating(source.getRating());
		target.setDate(source.getCreationtime());
		target.setAlias(source.getAlias());
		if (source.getUser() != null)
		{
			final PrincipalData p = new PrincipalData();
			p.setUid(source.getUser().getUid());
			p.setName(source.getUser().getName());
			target.setPrincipal(p);
		}
	}
}
