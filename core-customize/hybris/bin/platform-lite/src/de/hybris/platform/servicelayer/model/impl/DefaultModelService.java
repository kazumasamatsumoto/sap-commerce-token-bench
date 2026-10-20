package de.hybris.platform.servicelayer.model.impl;

import de.hybris.bootstrap.typesystem.TypeSystem;
import de.hybris.bootstrap.typesystem.TypeSystem.AttributeDef;
import de.hybris.bootstrap.typesystem.TypeSystem.ItemTypeDef;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.lite.persistence.ItemPersistence;
import de.hybris.platform.lite.runtime.TypeSystemHolder;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.exceptions.ModelRemovalException;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.interceptor.Interceptor;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.PrepareInterceptor;
import de.hybris.platform.servicelayer.interceptor.RemoveInterceptor;
import de.hybris.platform.servicelayer.interceptor.ValidateInterceptor;
import de.hybris.platform.servicelayer.interceptor.impl.InterceptorMapping;
import de.hybris.platform.servicelayer.model.ModelService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ModelService の本体。保存の順番は本物に合わせる:
 * PrepareInterceptor → 必須の属性の確かめ → ValidateInterceptor(unique の確かめを含む) → DB に書く。
 */
public class DefaultModelService implements ModelService
{
	private final ItemPersistence persistence;
	private final ObjectProvider<InterceptorMapping> interceptorMappings;
	private final TransactionTemplate tx;

	public DefaultModelService(final ItemPersistence persistence, final ObjectProvider<InterceptorMapping> interceptorMappings,
			final TransactionTemplate tx)
	{
		this.persistence = persistence;
		this.interceptorMappings = interceptorMappings;
		this.tx = tx;
	}

	private TypeSystem ts()
	{
		return persistence.getTypeSystem();
	}

	// ------------------------------------------------------------------ 作る

	@Override
	@SuppressWarnings("unchecked")
	public <T> T create(final Class<T> modelClass)
	{
		return (T) create(TypeSystemHolder.typeCodeOf(modelClass));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T create(final String typeCode)
	{
		final ItemTypeDef type = ts().getItemType(typeCode);
		if (type.isAbstract)
		{
			throw new ModelSavingException("cannot create a model of the abstract type " + typeCode);
		}
		final ItemModel model = persistence.instantiate(type.code);
		for (final AttributeDef a : ts().allAttributes(type).values())
		{
			final Object dv = defaultValue(a);
			if (dv != null)
			{
				model.internals().set(a.qualifier, dv);
			}
		}
		return (T) model;
	}

	private Object defaultValue(final AttributeDef a)
	{
		if (a.defaultValue == null)
		{
			return null;
		}
		final String v = a.defaultValue.replace("java.lang.", "");
		if (v.equals("Boolean.TRUE"))
		{
			return Boolean.TRUE;
		}
		if (v.equals("Boolean.FALSE"))
		{
			return Boolean.FALSE;
		}
		if (v.matches("-?\\d+"))
		{
			return ts().kindOf(a) == TypeSystem.Kind.LONG ? (Object) Long.valueOf(v) : (Object) Integer.valueOf(v);
		}
		if (v.matches("-?\\d+\\.\\d+d?"))
		{
			return Double.valueOf(v.replace("d", ""));
		}
		if (v.startsWith("\"") && v.endsWith("\""))
		{
			return v.substring(1, v.length() - 1);
		}
		final java.util.regex.Matcher m = java.util.regex.Pattern.compile("em\\(\\)\\.getEnumerationValue\\(\"(\\w+)\",\\s*\"(\\w+)\"\\)").matcher(v);
		if (m.matches())
		{
			return ItemPersistence.enumValue(m.group(1), m.group(2));
		}
		return null;
	}

	// ------------------------------------------------------------------ 保存

	@Override
	public void save(final Object model)
	{
		saveAll(List.of(model));
	}

	@Override
	public void saveAll(final Object... models)
	{
		saveAll(Arrays.asList(models));
	}

	@Override
	public void saveAll(final Collection<?> models)
	{
		tx.executeWithoutResult(status -> {
			final Set<ItemModel> inProgress = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
			for (final Object o : models)
			{
				saveOne((ItemModel) o, inProgress);
			}
		});
	}

	private void saveOne(final ItemModel model, final Set<ItemModel> inProgress)
	{
		if (!inProgress.add(model))
		{
			return;
		}
		final boolean isNew = model.getPk() == null;
		if (!isNew && model.internals().dirtyAttributes().isEmpty())
		{
			return;
		}
		final Context ctx = new Context();
		try
		{
			for (final Interceptor i : interceptorsFor(model, PrepareInterceptor.class))
			{
				((PrepareInterceptor<ItemModel>) i).onPrepare(model, ctx);
			}
			saveNewReferences(model, inProgress);
			checkMandatory(model, isNew);
			checkUnique(model);
			for (final Interceptor i : interceptorsFor(model, ValidateInterceptor.class))
			{
				((ValidateInterceptor<ItemModel>) i).onValidate(model, ctx);
			}
		}
		catch (final InterceptorException e)
		{
			throw new ModelSavingException("[" + e.getClass().getName() + "]: " + e.getMessage(), e);
		}
		try
		{
			persistence.write(model, ref -> saveOne(ref, inProgress));
		}
		catch (final org.springframework.dao.DataAccessException e)
		{
			throw new ModelSavingException("could not save " + model + ": " + rootMessage(e), e);
		}
	}

	/** 参照している未保存の Model(1 つの参照)を先に保存する。unique の確かめに参照先の pk が要るため。 */
	private void saveNewReferences(final ItemModel model, final Set<ItemModel> inProgress)
	{
		for (final String qualifier : List.copyOf(model.internals().dirtyAttributes()))
		{
			if (model.internals().peek(qualifier) instanceof ItemModel ref && ref.getPk() == null && !inProgress.contains(ref))
			{
				saveOne(ref, inProgress);
			}
		}
	}

	private void checkMandatory(final ItemModel model, final boolean isNew) throws InterceptorException
	{
		final List<String> missing = new ArrayList<>();
		for (final AttributeDef a : ts().allAttributes(ts().getItemType(model.getItemtype())).values())
		{
			if (a.optional || "Item".equals(a.declaringType) || !ts().hasColumn(a) || a.isLocalized())
			{
				continue;
			}
			if (model.internals().peek(a.qualifier) == null)
			{
				missing.add(a.qualifier);
			}
		}
		if (!missing.isEmpty())
		{
			throw new InterceptorException("[de.hybris.platform.servicelayer.interceptor.impl.MandatoryAttributesValidator]:missing values for "
					+ missing + " in model " + model.getClass().getSimpleName() + " (" + (isNew ? "<unsaved>" : model.getPk()) + ") to "
					+ (isNew ? "create a new " : "update ") + model.getItemtype());
		}
	}

	private void checkUnique(final ItemModel model) throws InterceptorException
	{
		final List<AttributeDef> unique = ts().allAttributes(ts().getItemType(model.getItemtype())).values().stream()
				.filter(a -> a.unique && ts().hasColumn(a) && !"Item".equals(a.declaringType)).toList();
		if (unique.isEmpty() || unique.stream().anyMatch(a -> model.internals().get(a.qualifier) == null))
		{
			return;
		}
		if (unique.stream().noneMatch(a -> model.getPk() == null || model.internals().dirtyAttributes().contains(a.qualifier)))
		{
			return;
		}
		if (persistence.existsOtherWithSameUniqueKey(model, unique))
		{
			throw new InterceptorException("[de.hybris.platform.servicelayer.interceptor.impl.UniqueAttributesInterceptor]:ambiguous unique keys "
					+ unique.stream().map(a -> a.qualifier + "=" + display(model.internals().get(a.qualifier))).toList() + " for model "
					+ model.getClass().getSimpleName() + " (" + (model.getPk() == null ? "<unsaved>" : model.getPk())
					+ ") - found 1 item(s) using the same keys");
		}
	}

	private static String display(final Object v)
	{
		return v instanceof ItemModel m ? m.getItemtype() + "(" + m.getPk() + ")" : String.valueOf(v);
	}

	@SuppressWarnings("unchecked")
	private <I extends Interceptor> List<Interceptor> interceptorsFor(final ItemModel model, final Class<I> kind)
	{
		final ItemTypeDef type = ts().getItemType(model.getItemtype());
		return interceptorMappings.orderedStream()
				.filter(m -> kind.isInstance(m.getInterceptor()))
				.filter(m -> ts().findItemType(m.getTypeCode()).map(t -> ts().isAssignableFrom(t, type)).orElse(false))
				.sorted(Comparator.comparingInt(InterceptorMapping::getOrder))
				.map(InterceptorMapping::getInterceptor)
				.toList();
	}

	// ------------------------------------------------------------------ 削除・読み直し

	@Override
	public void remove(final Object model)
	{
		removeAll(List.of(model));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void removeAll(final Collection<?> models)
	{
		tx.executeWithoutResult(status -> {
			for (final Object o : models)
			{
				final ItemModel m = (ItemModel) o;
				if (m.getPk() == null)
				{
					continue;
				}
				try
				{
					for (final Interceptor i : interceptorsFor(m, RemoveInterceptor.class))
					{
						((RemoveInterceptor<ItemModel>) i).onRemove(m, new Context());
					}
					persistence.delete(m);
				}
				catch (final InterceptorException e)
				{
					throw new ModelRemovalException(e.getMessage(), e);
				}
			}
		});
	}

	@Override
	public void refresh(final Object model)
	{
		final ItemModel m = (ItemModel) model;
		if (m.getPk() == null)
		{
			return;
		}
		final ItemModel fresh = persistence.load(m.getPk(), m.getItemtype());
		if (fresh == null)
		{
			throw new ModelNotFoundException("item " + m + " has been removed");
		}
		m.internals().clearValues();
		for (final String q : fresh.internals().loadedQualifiers())
		{
			m.internals().loadValue(q, fresh.internals().peek(q));
		}
		fresh.internals().localizedValues().forEach((q, v) -> m.internals().loadLocalized(q, v));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(final PK pk)
	{
		final ItemModel m = persistence.loadAnyType(pk);
		if (m == null)
		{
			throw new ModelNotFoundException("no item found for given pk " + pk);
		}
		return (T) m;
	}

	@Override
	public boolean isNew(final Object model)
	{
		return ((ItemModel) model).getPk() == null;
	}

	@Override
	public boolean isModified(final Object model)
	{
		return !((ItemModel) model).internals().dirtyAttributes().isEmpty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAttributeValue(final Object model, final String attributeQualifier)
	{
		final ItemModel m = (ItemModel) model;
		final AttributeDef a = ts().findAttribute(ts().getItemType(m.getItemtype()), attributeQualifier)
				.orElseThrow(() -> new IllegalArgumentException("unknown attribute " + m.getItemtype() + "." + attributeQualifier));
		if (a.isLocalized())
		{
			return (T) invokeGetter(m, a.qualifier);
		}
		return (T) m.internals().get(a.qualifier);
	}

	@Override
	public void setAttributeValue(final Object model, final String attributeQualifier, final Object value)
	{
		final ItemModel m = (ItemModel) model;
		final AttributeDef a = ts().findAttribute(ts().getItemType(m.getItemtype()), attributeQualifier)
				.orElseThrow(() -> new IllegalArgumentException("unknown attribute " + m.getItemtype() + "." + attributeQualifier));
		m.internals().set(a.qualifier, value);
	}

	private static Object invokeGetter(final ItemModel m, final String qualifier)
	{
		try
		{
			return m.getClass().getMethod("get" + Character.toUpperCase(qualifier.charAt(0)) + qualifier.substring(1)).invoke(m);
		}
		catch (final ReflectiveOperationException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static String rootMessage(final Throwable t)
	{
		Throwable c = t;
		while (c.getCause() != null)
		{
			c = c.getCause();
		}
		return c.getMessage();
	}

	private final class Context implements InterceptorContext
	{
		@Override
		public ModelService getModelService()
		{
			return DefaultModelService.this;
		}

		@Override
		public boolean isNew(final Object model)
		{
			return ((ItemModel) model).getPk() == null;
		}

		@Override
		public boolean isModified(final Object model, final String attribute)
		{
			return ((ItemModel) model).internals().dirtyAttributes().contains(attribute);
		}
	}

	/** テストで使う: 型の情報を見たいとき。 */
	public Map<String, AttributeDef> attributesOf(final String typeCode)
	{
		return ts().allAttributes(ts().getItemType(typeCode));
	}
}
