package de.hybris.bootstrap.typesystem;

import de.hybris.bootstrap.typesystem.TypeSystem.AttributeDef;
import de.hybris.bootstrap.typesystem.TypeSystem.EnumTypeDef;
import de.hybris.bootstrap.typesystem.TypeSystem.ItemTypeDef;
import de.hybris.bootstrap.typesystem.TypeSystem.Kind;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/**
 * ant build の前半。各拡張の resources/<拡張>-items.xml を読み、gensrc に Model と enum のクラスを書く。
 * 使い方: ModelGenerator <gensrc> <拡張名>=<拡張のフォルダ> ...
 */
public final class ModelGenerator
{
	private final TypeSystem ts;
	private final Path out;

	private ModelGenerator(final TypeSystem ts, final Path out)
	{
		this.ts = ts;
		this.out = out;
	}

	public static void main(final String[] args) throws Exception
	{
		final Path out = Path.of(args[0]);
		final List<TypeSystem.Source> sources = new ArrayList<>();
		final List<String> itemsXml = new ArrayList<>();
		final List<InputStream> opened = new ArrayList<>();
		final List<String> allExtensions = new ArrayList<>();
		for (int i = 1; i < args.length; i++)
		{
			final String name = args[i].substring(0, args[i].indexOf('='));
			allExtensions.add(name);
			final File dir = new File(args[i].substring(args[i].indexOf('=') + 1));
			final File items = new File(dir, "resources/" + name + "-items.xml");
			if (!items.exists())
			{
				continue;
			}
			final InputStream in = new FileInputStream(items);
			opened.add(in);
			sources.add(new TypeSystem.Source(name, packageRoot(dir), in));
			itemsXml.add(name + "-items.xml");
		}
		final TypeSystem ts;
		try
		{
			ts = TypeSystem.load(sources);
		}
		catch (final TypeSystemException e)
		{
			System.err.println("[ycodegen] ERROR: " + e.getMessage());
			System.err.println("BUILD FAILED");
			System.exit(1);
			return;
		}
		finally
		{
			for (final InputStream in : opened)
			{
				in.close();
			}
		}
		deleteRecursively(out);
		final ModelGenerator g = new ModelGenerator(ts, out);
		int models = 0;
		for (final ItemTypeDef t : ts.getItemTypes())
		{
			if (t.generate && !"Item".equals(t.code))
			{
				g.writeModel(t);
				models++;
			}
		}
		for (final EnumTypeDef e : ts.getEnumTypes())
		{
			g.writeEnum(e);
		}
		g.writeRegistry(itemsXml, sources, allExtensions);
		System.out.println("[ycodegen] generated " + models + " models and " + ts.getEnumTypes().size() + " enums into " + out);

		final de.hybris.bootstrap.beangenerator.BeanGenerator beans = new de.hybris.bootstrap.beangenerator.BeanGenerator();
		for (int i = 1; i < args.length; i++)
		{
			final String name = args[i].substring(0, args[i].indexOf('='));
			final File beansXml = new File(args[i].substring(args[i].indexOf('=') + 1), "resources/" + name + "-beans.xml");
			if (beansXml.exists())
			{
				try
				{
					beans.read(name, beansXml);
				}
				catch (final IllegalStateException e)
				{
					System.err.println("[beangenerator] ERROR: " + e.getMessage());
					System.err.println("BUILD FAILED");
					System.exit(1);
				}
			}
		}
		System.out.println("[beangenerator] generated " + beans.write(out) + " beans and enums into " + out);
	}

	private static String packageRoot(final File extensionDir) throws Exception
	{
		final Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new File(extensionDir, "extensioninfo.xml")).getDocumentElement();
		final Element core = (Element) root.getElementsByTagName("coremodule").item(0);
		if (core == null)
		{
			throw new IllegalStateException(extensionDir + "/extensioninfo.xml に coremodule がありません(items.xml を持つ拡張には必要です)");
		}
		return core.getAttribute("packageroot");
	}

	// ------------------------------------------------------------------ Model

	private void writeModel(final ItemTypeDef t) throws IOException
	{
		final ItemTypeDef parent = ts.superType(t);
		final String parentClass = parent == null || "Item".equals(parent.code) ? "de.hybris.platform.core.model.ItemModel"
				: parent.modelClassName();
		final StringBuilder b = new StringBuilder();
		b.append("/*\n * items.xml から ant build が作ったファイル。直接書き換えない(次のビルドで消える)。\n */\n");
		b.append("package ").append(t.modelPackage).append(";\n\n");
		b.append("/**\n * Generated model class for type ").append(t.code).append(" first defined at extension ").append(t.extensionName).append(".\n");
		if (t.description != null)
		{
			b.append(" * <p>").append(t.description).append("\n");
		}
		b.append(" */\n@SuppressWarnings(\"all\")\npublic class ").append(t.code).append("Model extends ").append(parentClass).append("\n{\n");
		b.append("\t/** <i>Generated model type code constant.</i> */\n");
		b.append("\tpublic static final String _TYPECODE = \"").append(t.code).append("\";\n\n");
		for (final AttributeDef a : t.attributes.values())
		{
			b.append("\t/** <i>Generated constant</i> - Attribute key of <code>").append(t.code).append('.').append(a.qualifier)
					.append("</code> attribute defined at extension <code>").append(a.extensionName).append("</code>. */\n");
			b.append("\tpublic static final String ").append(constant(a.qualifier)).append(" = \"").append(a.qualifier).append("\";\n\n");
		}
		b.append("\t/** <i>Generated constructor</i> - Default constructor for generic creation. */\n");
		b.append("\tpublic ").append(t.code).append("Model()\n\t{\n\t\tsuper();\n\t}\n\n");
		for (final AttributeDef a : t.attributes.values())
		{
			writeAccessors(b, t, a);
		}
		b.append("}\n");
		write(t.modelPackage, t.code + "Model", b.toString());
	}

	private void writeAccessors(final StringBuilder b, final ItemTypeDef t, final AttributeDef a)
	{
		final Kind kind = ts.kindOf(a);
		final String c = constant(a.qualifier);
		final String cap = Character.toUpperCase(a.qualifier.charAt(0)) + a.qualifier.substring(1);
		final String javaType = javaType(a, kind);
		final boolean primitive = isPrimitive(a.type);
		final String doc = "\t/**\n\t * <i>Generated method</i> - Getter of the <code>" + t.code + "." + a.qualifier + "</code> attribute defined at extension <code>"
				+ a.extensionName + "</code>.\n" + (a.description == null ? "" : "\t * " + a.description + "\n") + "\t */\n";
		final boolean dynamic = "dynamic".equals(a.persistence);
		final String read = dynamic ? "getDynamicValue(" + c + ")" : "getPersistentValue(" + c + ")";

		if (kind == Kind.LOCALIZED_STRING)
		{
			b.append(doc).append("\tpublic String get").append(cap).append("()\n\t{\n\t\treturn get").append(cap).append("(null);\n\t}\n\n");
			b.append(doc).append("\tpublic String get").append(cap).append("(final java.util.Locale loc)\n\t{\n\t\treturn (String) getLocalizedValue(")
					.append(c).append(", loc);\n\t}\n\n");
			if (a.writable)
			{
				b.append("\tpublic void set").append(cap).append("(final String value)\n\t{\n\t\tset").append(cap).append("(value, null);\n\t}\n\n");
				b.append("\tpublic void set").append(cap).append("(final String value, final java.util.Locale loc)\n\t{\n\t\tsetLocalizedValue(")
						.append(c).append(", loc, value);\n\t}\n\n");
			}
			return;
		}
		if (primitive)
		{
			final String boxed = boxed(a.type);
			final String prefix = "boolean".equals(a.type) ? "is" : "get";
			b.append(doc).append("\tpublic ").append(a.type).append(' ').append(prefix).append(cap).append("()\n\t{\n\t\tfinal ").append(boxed)
					.append(" value = (").append(boxed).append(") ").append(read).append(";\n\t\treturn value == null ? ")
					.append(primitiveDefault(a.type)).append(" : value;\n\t}\n\n");
			if (a.writable)
			{
				b.append("\tpublic void set").append(cap).append("(final ").append(a.type).append(" value)\n\t{\n\t\t")
						.append(dynamic ? "setDynamicValue(" : "setPersistentValue(").append(c).append(", value);\n\t}\n\n");
			}
			return;
		}
		b.append(doc).append("\tpublic ").append(javaType).append(" get").append(cap).append("()\n\t{\n\t\treturn (").append(javaType).append(") ")
				.append(read).append(";\n\t}\n\n");
		if (a.writable)
		{
			b.append("\tpublic void set").append(cap).append("(final ").append(javaType).append(" value)\n\t{\n\t\t")
					.append(dynamic ? "setDynamicValue(" : "setPersistentValue(").append(c).append(", value);\n\t}\n\n");
		}
	}

	private String javaType(final AttributeDef a, final Kind kind)
	{
		switch (kind)
		{
			case ENUM:
				return ts.findEnumType(a.type).orElseThrow().className();
			case ITEM:
				return ts.getItemType(a.type).modelClassName();
			case COLLECTION:
				if (a.relation != null)
				{
					final String element = ts.getItemType(a.type).modelClassName();
					return ("list".equals(a.collectionKind) ? "java.util.List<" : "set".equals(a.collectionKind) ? "java.util.Set<" : "java.util.Collection<")
							+ element + ">";
				}
				final TypeSystem.CollectionTypeDef ct = ts.findCollectionType(a.type).orElseThrow();
				final String element = ts.findEnumType(ct.elementType()).map(EnumTypeDef::className)
						.orElseGet(() -> ts.findItemType(ct.elementType()).map(ItemTypeDef::modelClassName).orElse(ct.elementType()));
				return ("set".equals(ct.kind()) ? "java.util.Set<" : "java.util.List<") + element + ">";
			default:
				return isPrimitive(a.type) ? boxed(a.type) : a.type;
		}
	}

	// ------------------------------------------------------------------ enum

	private void writeEnum(final EnumTypeDef e) throws IOException
	{
		final StringBuilder b = new StringBuilder();
		b.append("/*\n * items.xml から ant build が作ったファイル。直接書き換えない(次のビルドで消える)。\n */\n");
		b.append("package ").append(e.packageName).append(";\n\n");
		b.append("/** Generated enum ").append(e.code).append(" declared at extension ").append(e.extensionName).append(". */\n");
		b.append("public enum ").append(e.code).append(" implements de.hybris.platform.core.HybrisEnumValue\n{\n");
		for (int i = 0; i < e.values.size(); i++)
		{
			final String v = e.values.get(i);
			b.append("\t/** Generated enum value for ").append(e.code).append('.').append(v).append(" value defined at extension ")
					.append(e.extensionName).append(". */\n");
			b.append('\t').append(constant(v)).append("(\"").append(v).append("\")").append(i == e.values.size() - 1 ? ";" : ",").append("\n\n");
		}
		if (e.values.isEmpty())
		{
			b.append("\t;\n\n");
		}
		b.append("\tpublic static final String _TYPECODE = \"").append(e.code).append("\";\n\n");
		b.append("\tprivate final String code;\n\n");
		b.append("\t").append(e.code).append("(final String code)\n\t{\n\t\tthis.code = code;\n\t}\n\n");
		b.append("\t@Override\n\tpublic String getCode()\n\t{\n\t\treturn code;\n\t}\n\n");
		b.append("\t@Override\n\tpublic String getType()\n\t{\n\t\treturn _TYPECODE;\n\t}\n\n");
		b.append("\tpublic static ").append(e.code).append(" valueOfCode(final String code)\n\t{\n\t\tfor (final ").append(e.code)
				.append(" v : values())\n\t\t{\n\t\t\tif (v.code.equalsIgnoreCase(code))\n\t\t\t{\n\t\t\t\treturn v;\n\t\t\t}\n\t\t}\n")
				.append("\t\tthrow new IllegalArgumentException(\"No enum value '\" + code + \"' in ").append(e.code).append("\");\n\t}\n}\n");
		write(e.packageName, e.code, b.toString());
	}

	// ------------------------------------------------------------------ 型の一覧

	private void writeRegistry(final List<String> itemsXml, final List<TypeSystem.Source> sources, final List<String> allExtensions)
			throws IOException
	{
		final StringBuilder b = new StringBuilder();
		b.append("package de.hybris.platform.lite.generated;\n\n");
		b.append("/** ant build が作った、型と Model のクラスの対応。疑似プラットフォームが起動するときに読む。 */\n");
		b.append("public final class GeneratedTypeRegistry\n{\n");
		b.append("\t/** localextensions.xml の順の拡張。ImpEx の初期データもこの順に入れる。 */\n");
		b.append("\tpublic static final String[] EXTENSIONS = { ");
		for (final String name : allExtensions)
		{
			b.append('"').append(name).append("\", ");
		}
		b.append("};\n\n");
		b.append("\tpublic static final String[][] ITEMS_XML = {\n");
		for (int i = 0; i < itemsXml.size(); i++)
		{
			b.append("\t\t{ \"").append(sources.get(i).extensionName()).append("\", \"").append(sources.get(i).packageRoot()).append("\", \"")
					.append(itemsXml.get(i)).append("\" },\n");
		}
		b.append("\t};\n\n");
		b.append("\tpublic static final java.util.Map<String, Class<? extends de.hybris.platform.core.model.ItemModel>> MODEL_CLASSES = new java.util.LinkedHashMap<>();\n\n");
		b.append("\tpublic static final java.util.Map<String, Class<? extends Enum<?>>> ENUM_CLASSES = new java.util.LinkedHashMap<>();\n\n");
		b.append("\tstatic\n\t{\n");
		b.append("\t\tMODEL_CLASSES.put(\"item\", de.hybris.platform.core.model.ItemModel.class);\n");
		for (final ItemTypeDef t : ts.getItemTypes())
		{
			if (t.generate && !"Item".equals(t.code))
			{
				b.append("\t\tMODEL_CLASSES.put(\"").append(t.code.toLowerCase(Locale.ROOT)).append("\", ").append(t.modelClassName()).append(".class);\n");
			}
		}
		for (final EnumTypeDef e : ts.getEnumTypes())
		{
			b.append("\t\tENUM_CLASSES.put(\"").append(e.code.toLowerCase(Locale.ROOT)).append("\", ").append(e.className()).append(".class);\n");
		}
		b.append("\t}\n\n\tprivate GeneratedTypeRegistry()\n\t{\n\t}\n}\n");
		write("de.hybris.platform.lite.generated", "GeneratedTypeRegistry", b.toString());
	}

	// ------------------------------------------------------------------ 道具

	private void write(final String pkg, final String name, final String content) throws IOException
	{
		final Path dir = out.resolve(pkg.replace('.', '/'));
		Files.createDirectories(dir);
		Files.writeString(dir.resolve(name + ".java"), content, StandardCharsets.UTF_8);
	}

	static String constant(final String qualifier)
	{
		// 本物と同じく、区切りを入れずに大文字にする(catalogVersion → CATALOGVERSION)。
		return qualifier.replaceAll("[^A-Za-z0-9]", "_").toUpperCase(Locale.ROOT);
	}

	private static boolean isPrimitive(final String type)
	{
		return List.of("int", "long", "double", "float", "boolean").contains(type);
	}

	private static String boxed(final String type)
	{
		return switch (type)
		{
			case "int" -> "Integer";
			case "long" -> "Long";
			case "double" -> "Double";
			case "float" -> "Float";
			case "boolean" -> "Boolean";
			default -> type;
		};
	}

	private static String primitiveDefault(final String type)
	{
		return switch (type)
		{
			case "boolean" -> "false";
			case "double" -> "0.0d";
			case "float" -> "0.0f";
			case "long" -> "0L";
			default -> "0";
		};
	}

	private static void deleteRecursively(final Path dir) throws IOException
	{
		if (!Files.exists(dir))
		{
			return;
		}
		try (Stream<Path> walk = Files.walk(dir))
		{
			for (final Path p : walk.sorted(Comparator.reverseOrder()).toList())
			{
				Files.delete(p);
			}
		}
	}
}
