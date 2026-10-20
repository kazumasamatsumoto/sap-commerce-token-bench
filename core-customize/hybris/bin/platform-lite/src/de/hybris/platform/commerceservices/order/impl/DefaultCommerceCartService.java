package de.hybris.platform.commerceservices.order.impl;

import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartService;
import de.hybris.platform.commerceservices.stock.CommerceStockService;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.order.CalculationService;
import de.hybris.platform.order.exceptions.CalculationException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.site.BaseSiteService;
import de.hybris.platform.store.model.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultCommerceCartService extends AbstractItemDao implements CommerceCartService
{
	private BaseSiteService baseSiteService;
	private BaseStoreService baseStoreService;
	private CommonI18NService commonI18NService;
	private CommerceStockService commerceStockService;
	private CalculationService calculationService;
	private long maxQuantityPerEntry = 999;

	@Override
	public CartModel createCartForUser(final UserModel user)
	{
		final CartModel cart = getModelService().create(CartModel.class);
		cart.setCode(nextCartCode());
		cart.setGuid(UUID.randomUUID().toString());
		cart.setUser(user);
		cart.setSite(baseSiteService.getCurrentBaseSite());
		cart.setStore(baseStoreService.getCurrentBaseStore());
		cart.setCurrency(commonI18NService.getCurrentCurrency());
		cart.setDate(new java.util.Date());
		cart.setNet(Boolean.FALSE);
		cart.setCalculated(Boolean.FALSE);
		cart.setSubtotal(0d);
		cart.setTotalPrice(0d);
		cart.setTotalTax(0d);
		cart.setDeliveryCost(0d);
		getModelService().save(cart);
		return cart;
	}

	private String nextCartCode()
	{
		final Long seq = getFlexibleSearchService().<Long> search(new de.hybris.platform.servicelayer.search.FlexibleSearchQuery(
				"SELECT count({pk}) FROM {Cart}", Map.of()) {
			{
				setResultClassList(List.of(Long.class));
			}
		}).getResult().get(0);
		return String.format("%08d", 100000 + seq) + "-" + UUID.randomUUID().toString().substring(0, 4);
	}

	@Override
	public CartModel getCartForCodeAndUser(final String code, final UserModel user)
	{
		final List<CartModel> carts = find("SELECT {pk} FROM {Cart} WHERE ({code} = ?code OR {guid} = ?code) AND {user} = ?user",
				Map.of("code", code, "user", user));
		return carts.isEmpty() ? null : carts.get(0);
	}

	@Override
	public CartModel getCartForGuid(final String guid)
	{
		final List<CartModel> carts = find("SELECT {pk} FROM {Cart} WHERE {guid} = ?guid", Map.of("guid", guid));
		return carts.isEmpty() ? null : carts.get(0);
	}

	@Override
	public List<CartModel> getCartsForUser(final UserModel user)
	{
		return find("SELECT {pk} FROM {Cart} WHERE {user} = ?user AND {site} = ?site ORDER BY {modifiedtime} DESC",
				Map.of("user", user, "site", baseSiteService.getCurrentBaseSite()));
	}

	@Override
	public Modification addToCart(final CartModel cart, final ProductModel product, final long quantity) throws CommerceCartModificationException
	{
		if (quantity < 1)
		{
			throw new CommerceCartModificationException("Quantity must not be less than one");
		}
		final AbstractOrderEntryModel existing = cart.getEntries().stream().filter(e -> e.getProduct().getCode().equals(product.getCode()))
				.findFirst().orElse(null);
		final long already = existing == null ? 0 : existing.getQuantity();
		final long allowed = allowedQuantity(cart.getStore(), product, already + quantity);
		final long added = Math.max(0, allowed - already);
		if (added == 0)
		{
			return new Modification("noStock", 0, already, existing instanceof CartEntryModel ce ? ce : null);
		}
		final CartEntryModel entry;
		if (existing != null)
		{
			entry = (CartEntryModel) existing;
			entry.setQuantity(already + added);
			getModelService().save(entry);
		}
		else
		{
			entry = getModelService().create(CartEntryModel.class);
			entry.setProduct(product);
			entry.setQuantity(added);
			entry.setUnit(product.getUnit());
			entry.setEntryNumber(nextEntryNumber(cart));
			entry.setOrder(cart);
			getModelService().save(entry);
			getModelService().refresh(cart);
		}
		recalculate(cart);
		return new Modification(added < quantity ? "lowStock" : "success", added, entry.getQuantity(), entry);
	}

	@Override
	public Modification updateQuantityForCartEntry(final CartModel cart, final int entryNumber, final long newQuantity)
			throws CommerceCartModificationException
	{
		final AbstractOrderEntryModel entry = cart.getEntries().stream().filter(e -> e.getEntryNumber() == entryNumber).findFirst()
				.orElseThrow(() -> new CommerceCartModificationException("Entry not found"));
		if (newQuantity <= 0)
		{
			getModelService().remove(entry);
			getModelService().refresh(cart);
			recalculate(cart);
			return new Modification("success", -entry.getQuantity(), 0, null);
		}
		final long allowed = allowedQuantity(cart.getStore(), entry.getProduct(), newQuantity);
		final long before = entry.getQuantity();
		entry.setQuantity(allowed);
		getModelService().save(entry);
		recalculate(cart);
		return new Modification(allowed < newQuantity ? "lowStock" : "success", allowed - before, allowed, (CartEntryModel) entry);
	}

	private long allowedQuantity(final BaseStoreModel store, final ProductModel product, final long wanted)
	{
		final Long stock = commerceStockService.getStockLevelForProductAndBaseStore(product, store);
		final long cap = stock == null ? maxQuantityPerEntry : Math.min(stock, maxQuantityPerEntry);
		return Math.min(wanted, cap);
	}

	private int nextEntryNumber(final CartModel cart)
	{
		return cart.getEntries().stream().mapToInt(AbstractOrderEntryModel::getEntryNumber).max().orElse(-1) + 1;
	}

	@Override
	public void removeCart(final CartModel cart)
	{
		getModelService().removeAll(new ArrayList<>(cart.getEntries()));
		getModelService().remove(cart);
	}

	@Override
	public void recalculate(final CartModel cart) throws CommerceCartModificationException
	{
		try
		{
			calculationService.calculate(cart);
		}
		catch (final CalculationException e)
		{
			throw new CommerceCartModificationException(e.getMessage());
		}
	}

	public void setBaseSiteService(final BaseSiteService baseSiteService)
	{
		this.baseSiteService = baseSiteService;
	}

	public void setBaseStoreService(final BaseStoreService baseStoreService)
	{
		this.baseStoreService = baseStoreService;
	}

	public void setCommonI18NService(final CommonI18NService commonI18NService)
	{
		this.commonI18NService = commonI18NService;
	}

	public void setCommerceStockService(final CommerceStockService commerceStockService)
	{
		this.commerceStockService = commerceStockService;
	}

	public void setCalculationService(final CalculationService calculationService)
	{
		this.calculationService = calculationService;
	}

	public void setMaxQuantityPerEntry(final long maxQuantityPerEntry)
	{
		this.maxQuantityPerEntry = maxQuantityPerEntry;
	}
}
