package de.hybris.platform.webservicescommons.mapping.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.mapping.config.FieldSetLevelMapping;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;

/**
 * DataMapper の本体。Data を一度 Map にして、fields で項目を絞ってから WsDTO にする。
 * 名前の違う項目(ProductSearchPageData.results → ProductSearchPageWsDTO.products など)は renames で読み替える。
 */
public class DefaultDataMapper implements DataMapper
{
	private static final Set<String> LEVELS = Set.of("BASIC", "DEFAULT", "FULL");

	private final ObjectMapper objectMapper;
	private final ObjectProvider<FieldSetLevelMapping> mappings;
	private final Map<Class<?>, Map<String, String>> renames = new HashMap<>();
	private volatile Map<Class<?>, Map<String, String>> levelCache;

	public DefaultDataMapper(final ObjectMapper objectMapper, final ObjectProvider<FieldSetLevelMapping> mappings)
	{
		this.objectMapper = objectMapper;
		this.mappings = mappings;
	}

	/** dtoClass の項目 dtoProperty の値は、Data の dataProperty から取る。 */
	public void addRename(final Class<?> dtoClass, final String dataProperty, final String dtoProperty)
	{
		renames.computeIfAbsent(dtoClass, k -> new HashMap<>()).put(dataProperty, dtoProperty);
	}

	@Override
	public <T> T map(final Object source, final Class<T> destinationClass)
	{
		return map(source, destinationClass, "DEFAULT");
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T map(final Object source, final Class<T> destinationClass, final String fields)
	{
		if (source == null)
		{
			return null;
		}
		final Object tree = objectMapper.convertValue(source, Object.class);
		final Object pruned = prune(tree, destinationClass, parse(fields == null || fields.isBlank() ? "DEFAULT" : fields, destinationClass, 0));
		return objectMapper.convertValue(pruned, destinationClass);
	}

	@Override
	public <T> T mapToData(final Object source, final Class<T> destinationClass)
	{
		final com.fasterxml.jackson.databind.ObjectMapper lenient = objectMapper.copy()
				.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return lenient.convertValue(source, destinationClass);
	}

	// ------------------------------------------------------------------ fields を読む

	/** 項目名 → 中の項目(中が無い単純な項目は null)。 */
	private Map<String, Map<String, Object>> parse(final String fields, final Class<?> dtoClass, final int depth)
	{
		if (depth > 8)
		{
			return new LinkedHashMap<>(); // 自分を中に持つ WsDTO(カテゴリの木など)で止まらなくならないように
		}
		final Map<String, Map<String, Object>> out = new LinkedHashMap<>();
		for (final String item : splitTopLevel(fields))
		{
			final String name = item.contains("(") ? item.substring(0, item.indexOf('(')).trim() : item.trim();
			final String inner = item.contains("(") ? item.substring(item.indexOf('(') + 1, item.lastIndexOf(')')) : null;
			if (name.isEmpty())
			{
				continue;
			}
			if (LEVELS.contains(name))
			{
				final String expanded = levelFor(dtoClass, name);
				if (expanded == null)
				{
					for (final PropertyDescriptor pd : properties(dtoClass))
					{
						out.putIfAbsent(pd.getName(), isDto(elementType(pd)) ? asObjectMap(parse(name, elementType(pd), depth + 1)) : null);
					}
				}
				else
				{
					parse(expanded, dtoClass, depth + 1).forEach((k, v) -> merge(out, k, v));
				}
				continue;
			}
			final PropertyDescriptor pd = property(dtoClass, name);
			if (pd == null)
			{
				continue; // 本物と同じく、知らない項目は黙って無視する
			}
			final Class<?> type = elementType(pd);
			if (inner != null)
			{
				merge(out, name, asObjectMap(parse(inner, type, depth + 1)));
			}
			else if (isDto(type))
			{
				merge(out, name, asObjectMap(parse("BASIC", type, depth + 1)));
			}
			else
			{
				out.putIfAbsent(name, null);
			}
		}
		return out;
	}

	/** 同じ項目が 2 回出てきたら(DEFAULT,entries(...) など)、中の項目を足し合わせる(本物と同じ)。 */
	@SuppressWarnings("unchecked")
	private static void merge(final Map<String, Map<String, Object>> out, final String name, final Map<String, Object> spec)
	{
		final Map<String, Object> existing = out.get(name);
		if (existing == null || spec == null)
		{
			if (spec != null || !out.containsKey(name))
			{
				out.put(name, spec);
			}
			return;
		}
		final Map<String, Object> merged = new LinkedHashMap<>(existing);
		spec.forEach((k, v) -> {
			if (merged.get(k) instanceof Map<?, ?> a && v instanceof Map<?, ?> b)
			{
				final Map<String, Map<String, Object>> tmp = new LinkedHashMap<>();
				tmp.put(k, (Map<String, Object>) a);
				merge(tmp, k, (Map<String, Object>) b);
				merged.put(k, tmp.get(k));
			}
			else if (v != null || !merged.containsKey(k))
			{
				merged.put(k, v);
			}
		});
		out.put(name, merged);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asObjectMap(final Map<String, Map<String, Object>> m)
	{
		return (Map<String, Object>) (Map<?, ?>) m;
	}

	private String levelFor(final Class<?> dtoClass, final String level)
	{
		Map<Class<?>, Map<String, String>> cache = levelCache;
		if (cache == null)
		{
			cache = new HashMap<>();
			for (final FieldSetLevelMapping m : mappings.orderedStream().toList())
			{
				final Map<String, String> levels = cache.computeIfAbsent(m.getDtoClass(), k -> new HashMap<>());
				m.getLevelMapping().forEach((k, v) -> levels.merge(k, v, (a, b) -> a + "," + b));
			}
			levelCache = cache;
		}
		final Map<String, String> levels = cache.get(dtoClass);
		return levels == null ? null : levels.get(level);
	}

	private static List<String> splitTopLevel(final String s)
	{
		final List<String> out = new ArrayList<>();
		int depth = 0;
		int start = 0;
		for (int i = 0; i < s.length(); i++)
		{
			final char c = s.charAt(i);
			if (c == '(')
			{
				depth++;
			}
			else if (c == ')')
			{
				depth--;
			}
			else if (c == ',' && depth == 0)
			{
				out.add(s.substring(start, i));
				start = i + 1;
			}
		}
		out.add(s.substring(start));
		return out;
	}

	// ------------------------------------------------------------------ 絞る

	@SuppressWarnings("unchecked")
	private Object prune(final Object tree, final Class<?> dtoClass, final Map<String, ?> spec)
	{
		if (tree instanceof Collection<?> c)
		{
			final List<Object> out = new ArrayList<>();
			for (final Object o : c)
			{
				out.add(prune(o, dtoClass, spec));
			}
			return out;
		}
		if (!(tree instanceof Map<?, ?> map))
		{
			return tree;
		}
		final Map<String, Object> data = new LinkedHashMap<>((Map<String, Object>) map);
		final Map<String, String> r = renames.get(dtoClass);
		if (r != null)
		{
			r.forEach((from, to) -> {
				if (data.containsKey(from))
				{
					data.put(to, data.remove(from));
				}
			});
		}
		final Map<String, Object> out = new LinkedHashMap<>();
		for (final Map.Entry<String, ?> e : spec.entrySet())
		{
			if (!data.containsKey(e.getKey()) || data.get(e.getKey()) == null)
			{
				continue;
			}
			final Object value = data.get(e.getKey());
			if (e.getValue() == null)
			{
				out.put(e.getKey(), value);
			}
			else
			{
				final PropertyDescriptor pd = property(dtoClass, e.getKey());
				out.put(e.getKey(), prune(value, elementType(pd), (Map<String, ?>) e.getValue()));
			}
		}
		// Map の項目(CMS コンポーネントの otherProperties など)は中を絞らない
		for (final PropertyDescriptor pd : properties(dtoClass))
		{
			if (Map.class.isAssignableFrom(pd.getPropertyType()) && data.containsKey(pd.getName()) && spec.containsKey(pd.getName()))
			{
				out.put(pd.getName(), data.get(pd.getName()));
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ 型を見る

	private static List<PropertyDescriptor> properties(final Class<?> c)
	{
		try
		{
			final List<PropertyDescriptor> out = new ArrayList<>();
			for (final PropertyDescriptor pd : Introspector.getBeanInfo(c, Object.class).getPropertyDescriptors())
			{
				if (pd.getReadMethod() != null)
				{
					out.add(pd);
				}
			}
			return out;
		}
		catch (final Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static PropertyDescriptor property(final Class<?> c, final String name)
	{
		return properties(c).stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
	}

	private static Class<?> elementType(final PropertyDescriptor pd)
	{
		final Type t = pd.getReadMethod().getGenericReturnType();
		if (t instanceof ParameterizedType pt && Collection.class.isAssignableFrom(pd.getPropertyType()))
		{
			final Type arg = pt.getActualTypeArguments()[0];
			return arg instanceof Class<?> c ? c : Object.class;
		}
		return pd.getPropertyType();
	}

	private static boolean isDto(final Class<?> c)
	{
		return c.getName().endsWith("WsDTO");
	}
}
