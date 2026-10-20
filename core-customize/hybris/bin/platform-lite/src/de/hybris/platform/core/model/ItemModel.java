package de.hybris.platform.core.model;

import de.hybris.bootstrap.typesystem.TypeSystem;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.Registry;
import de.hybris.platform.lite.runtime.TypeSystemHolder;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * すべての Model の親。値は属性の名前ごとに持ち、変えた属性を覚えておいて、保存のときにその分だけ書く。
 * 参照先の Model や関連のコレクションは、初めて読んだときに DB から引く(遅延読み込み)。
 */
@SuppressWarnings("unchecked")
public class ItemModel
{
	public static final String _TYPECODE = "Item";
	public static final String PK = "pk";
	public static final String CREATIONTIME = "creationtime";
	public static final String MODIFIEDTIME = "modifiedtime";

	/** DB から読んだあと、初めて触ったときに値を作るもの。 */
	@FunctionalInterface
	public interface LazyValue extends Supplier<Object>
	{
	}

	private PK pk;
	private String itemtype;
	private Date creationtime;
	private Date modifiedtime;
	private boolean removed;
	private final Map<String, Object> values = new HashMap<>();
	private final Map<String, Map<String, String>> localizedValues = new HashMap<>();
	private final Set<String> dirtyAttributes = new HashSet<>();

	public ItemModel()
	{
		try
		{
			this.itemtype = (String) getClass().getField("_TYPECODE").get(null);
		}
		catch (final ReflectiveOperationException e)
		{
			this.itemtype = _TYPECODE;
		}
	}

	public PK getPk()
	{
		return pk;
	}

	public String getItemtype()
	{
		return itemtype;
	}

	public Date getCreationtime()
	{
		return creationtime;
	}

	public Date getModifiedtime()
	{
		return modifiedtime;
	}

	public void setModifiedtime(final Date modifiedtime)
	{
		this.modifiedtime = modifiedtime;
	}

	// ------------------------------------------------------------------ 生成されたクラスが使う

	protected Object getPersistentValue(final String qualifier)
	{
		Object value = values.get(qualifier);
		if (value instanceof LazyValue lazy)
		{
			value = lazy.get();
			values.put(qualifier, value);
		}
		if (value == null && !values.containsKey(qualifier))
		{
			value = emptyCollectionIfCollection(qualifier);
		}
		return value;
	}

	protected void setPersistentValue(final String qualifier, final Object value)
	{
		values.put(qualifier, value);
		dirtyAttributes.add(qualifier);
	}

	protected Object getLocalizedValue(final String qualifier, final Locale locale)
	{
		final Map<String, String> byLang = localizedValues.get(qualifier);
		if (byLang == null)
		{
			return null;
		}
		return byLang.get(languageOf(locale));
	}

	protected void setLocalizedValue(final String qualifier, final Locale locale, final Object value)
	{
		localizedValues.computeIfAbsent(qualifier, k -> new LinkedHashMap<>()).put(languageOf(locale), (String) value);
		dirtyAttributes.add(qualifier);
	}

	protected Object getDynamicValue(final String qualifier)
	{
		return handlerFor(qualifier).get(this);
	}

	protected void setDynamicValue(final String qualifier, final Object value)
	{
		handlerFor(qualifier).set(this, value);
	}

	private DynamicAttributeHandler<Object, ItemModel> handlerFor(final String qualifier)
	{
		final TypeSystem ts = TypeSystemHolder.get();
		final TypeSystem.AttributeDef def = ts.findAttribute(ts.getItemType(itemtype), qualifier)
				.orElseThrow(() -> new IllegalStateException("unknown attribute " + itemtype + "." + qualifier));
		return Registry.getApplicationContext().getBean(def.attributeHandler, DynamicAttributeHandler.class);
	}

	private Object emptyCollectionIfCollection(final String qualifier)
	{
		final TypeSystem ts = TypeSystemHolder.getOrNull();
		if (ts == null)
		{
			return null;
		}
		return ts.findItemType(itemtype).flatMap(t -> ts.findAttribute(t, qualifier))
				.filter(a -> ts.kindOf(a) == TypeSystem.Kind.COLLECTION)
				.map(a -> "set".equals(a.collectionKind) ? (Object) Collections.emptySet() : (Object) Collections.emptyList())
				.orElse(null);
	}

	private static String languageOf(final Locale locale)
	{
		if (locale != null)
		{
			return locale.getLanguage();
		}
		if (Registry.hasApplicationContext())
		{
			return Registry.getApplicationContext().getBean(I18NService.class).getCurrentLocale().getLanguage();
		}
		return "ja";
	}

	// ------------------------------------------------------------------ 疑似プラットフォームの中だけで使う(lite.persistence)

	public final ItemModelInternals internals()
	{
		return new ItemModelInternals();
	}

	/** 保存と読み込みのための入口。業務のコードからは使わない。 */
	public final class ItemModelInternals
	{
		public void setPk(final PK value)
		{
			pk = value;
		}

		public void setItemtype(final String value)
		{
			itemtype = value;
		}

		public void setTimestamps(final Date created, final Date modified)
		{
			creationtime = created;
			modifiedtime = modified;
		}

		public boolean isRemoved()
		{
			return removed;
		}

		public void markRemoved()
		{
			removed = true;
		}

		public Map<String, Object> rawValues()
		{
			return values;
		}

		public Map<String, Map<String, String>> localizedValues()
		{
			return localizedValues;
		}

		public Set<String> dirtyAttributes()
		{
			return dirtyAttributes;
		}

		public void loadValue(final String qualifier, final Object value)
		{
			values.put(qualifier, value);
		}

		public void loadLocalized(final String qualifier, final Map<String, String> byLang)
		{
			localizedValues.put(qualifier, byLang == null ? new LinkedHashMap<>() : new LinkedHashMap<>(byLang));
		}

		/** 遅延の値はそのまま返す(読み込みを起こさない)。 */
		public Object peek(final String qualifier)
		{
			return values.get(qualifier);
		}

		public Object get(final String qualifier)
		{
			return getPersistentValue(qualifier);
		}

		public void set(final String qualifier, final Object value)
		{
			setPersistentValue(qualifier, value);
		}

		public void clearDirty()
		{
			dirtyAttributes.clear();
		}

		public void clearValues()
		{
			values.clear();
			localizedValues.clear();
			dirtyAttributes.clear();
		}

		public List<String> loadedQualifiers()
		{
			return List.copyOf(values.keySet());
		}
	}

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass() || pk == null)
		{
			return false;
		}
		return pk.equals(((ItemModel) o).pk);
	}

	@Override
	public int hashCode()
	{
		return pk == null ? System.identityHashCode(this) : pk.hashCode();
	}

	@Override
	public String toString()
	{
		return itemtype + " (" + (pk == null ? "<unsaved>" : pk.toString()) + ")";
	}
}
