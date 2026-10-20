package de.hybris.platform.lite.runtime;

import de.hybris.bootstrap.typesystem.TypeSystem;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.lite.generated.GeneratedTypeRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 起動したときに、ant build が作った GeneratedTypeRegistry の順で items.xml を読み直し、型の情報を持つ。
 * Model の中(遅延読み込みなど)からも引けるよう、static でも持つ。
 */
public final class TypeSystemHolder
{
	private static volatile TypeSystem typeSystem;

	private TypeSystemHolder()
	{
	}

	public static TypeSystem get()
	{
		TypeSystem ts = typeSystem;
		if (ts == null)
		{
			synchronized (TypeSystemHolder.class)
			{
				ts = typeSystem;
				if (ts == null)
				{
					ts = load();
					typeSystem = ts;
				}
			}
		}
		return ts;
	}

	public static TypeSystem getOrNull()
	{
		try
		{
			return get();
		}
		catch (final RuntimeException e)
		{
			return null;
		}
	}

	private static TypeSystem load()
	{
		final List<TypeSystem.Source> sources = new ArrayList<>();
		final List<InputStream> opened = new ArrayList<>();
		try
		{
			for (final String[] entry : GeneratedTypeRegistry.ITEMS_XML)
			{
				final InputStream in = TypeSystemHolder.class.getClassLoader().getResourceAsStream(entry[2]);
				if (in == null)
				{
					throw new IllegalStateException(entry[2] + " がクラスパスにありません。ant all をやり直してください");
				}
				opened.add(in);
				sources.add(new TypeSystem.Source(entry[0], entry[1], in));
			}
			return TypeSystem.load(sources);
		}
		finally
		{
			for (final InputStream in : opened)
			{
				try
				{
					in.close();
				}
				catch (final IOException ignored)
				{
					// 読み終わっているので無視してよい
				}
			}
		}
	}

	public static Class<? extends ItemModel> modelClass(final String typeCode)
	{
		final Class<? extends ItemModel> c = GeneratedTypeRegistry.MODEL_CLASSES.get(typeCode.toLowerCase(Locale.ROOT));
		if (c == null)
		{
			throw new IllegalArgumentException("型 '" + typeCode + "' の Model がありません(items.xml の generate=\"false\" か、ant all をしていない)");
		}
		return c;
	}

	public static Class<? extends Enum<?>> enumClass(final String enumCode)
	{
		return GeneratedTypeRegistry.ENUM_CLASSES.get(enumCode.toLowerCase(Locale.ROOT));
	}

	/** Model のクラスから型の code を引く(子のクラスでも自分の _TYPECODE)。 */
	public static String typeCodeOf(final Class<?> modelClass)
	{
		try
		{
			return (String) modelClass.getField("_TYPECODE").get(null);
		}
		catch (final ReflectiveOperationException e)
		{
			throw new IllegalArgumentException(modelClass + " は Model のクラスではありません");
		}
	}

	public static List<String> extensions()
	{
		return List.of(GeneratedTypeRegistry.EXTENSIONS);
	}
}
