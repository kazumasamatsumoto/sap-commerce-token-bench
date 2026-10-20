package de.hybris.platform.commercefacades.product.converters.populators;

import de.hybris.platform.commercefacades.product.data.ImageData;
import de.hybris.platform.commercefacades.product.data.ImageDataType;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.core.model.product.ProductModel;
import java.util.ArrayList;
import java.util.List;

/** 主画像(picture、thumbnail)と、ギャラリーの画像。 */
public class ProductImagePopulator implements Populator<ProductModel, ProductData>
{
	@Override
	public void populate(final ProductModel source, final ProductData target)
	{
		final List<ImageData> images = new ArrayList<>();
		add(images, source.getPicture(), "product", ImageDataType.PRIMARY, null, source);
		add(images, source.getThumbnail(), "thumbnail", ImageDataType.PRIMARY, null, source);
		int i = 0;
		for (final MediaModel m : source.getGalleryImages())
		{
			add(images, m, m.getMediaFormat() == null ? "product" : m.getMediaFormat(), ImageDataType.GALLERY, i++, source);
		}
		target.setImages(images);
	}

	private static void add(final List<ImageData> images, final MediaModel media, final String format, final ImageDataType type, final Integer index,
			final ProductModel product)
	{
		if (media == null)
		{
			return;
		}
		final ImageData d = new ImageData();
		d.setUrl(media.getURL());
		d.setAltText(media.getAltText() != null ? media.getAltText() : product.getName());
		d.setFormat(format);
		d.setImageType(type);
		d.setGalleryIndex(index);
		images.add(d);
	}
}
