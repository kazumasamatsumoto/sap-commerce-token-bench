package de.hybris.bootstrap.beangenerator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * ant build の一部。各拡張の resources/&lt;拡張&gt;-beans.xml から、Data と WsDTO のクラス(と enum)を gensrc に作る。
 * 本物と同じく、同じ class の bean を別の拡張の beans.xml に書くと、属性が足される(マージ)。
 */
public final class BeanGenerator
{
	private final Map<String, BeanDef> beans = new LinkedHashMap<>();
	private final Map<String, List<String>> enums = new LinkedHashMap<>();

	private static final class BeanDef
	{
		String className;
		String superClass;
		String description;
		String declaredIn;
		final Map<String, String[]> properties = new LinkedHashMap<>();
	}

	public void read(final String extensionName, final File beansXml)
	{
		final Element root;
		try
		{
			root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(beansXml).getDocumentElement();
		}
		catch (final Exception e)
		{
			throw new IllegalStateException(extensionName + "-beans.xml を読めません: " + e.getMessage(), e);
		}
		final NodeList beanNodes = root.getElementsByTagName("bean");
		for (int i = 0; i < beanNodes.getLength(); i++)
		{
			final Element b = (Element) beanNodes.item(i);
			final String cls = b.getAttribute("class");
			final BeanDef def = beans.computeIfAbsent(cls, c -> {
				final BeanDef d = new BeanDef();
				d.className = c;
				d.declaredIn = extensionName;
				return d;
			});
			if (!b.getAttribute("extends").isEmpty())
			{
				def.superClass = b.getAttribute("extends");
			}
			final NodeList desc = b.getElementsByTagName("description");
			if (desc.getLength() > 0 && def.description == null)
			{
				def.description = desc.item(0).getTextContent().trim();
			}
			final NodeList props = b.getElementsByTagName("property");
			for (int p = 0; p < props.getLength(); p++)
			{
				final Element pe = (Element) props.item(p);
				final String name = pe.getAttribute("name");
				final String type = pe.getAttribute("type");
				final String[] existing = def.properties.get(name);
				if (existing != null && !existing[0].equals(type))
				{
					throw new IllegalStateException(extensionName + "-beans.xml: property '" + name + "' of " + cls + " is already declared in "
							+ existing[1] + " with type " + existing[0] + " (now " + type + ")");
				}
				def.properties.put(name, new String[] { type, extensionName });
			}
		}
		final NodeList enumNodes = root.getElementsByTagName("enum");
		for (int i = 0; i < enumNodes.getLength(); i++)
		{
			final Element e = (Element) enumNodes.item(i);
			final List<String> values = enums.computeIfAbsent(e.getAttribute("class"), c -> new ArrayList<>());
			final NodeList v = e.getElementsByTagName("value");
			for (int j = 0; j < v.getLength(); j++)
			{
				final String value = v.item(j).getTextContent().trim();
				if (!values.contains(value))
				{
					values.add(value);
				}
			}
		}
	}

	public int write(final Path out) throws IOException
	{
		for (final BeanDef b : beans.values())
		{
			writeBean(out, b);
		}
		for (final Map.Entry<String, List<String>> e : enums.entrySet())
		{
			writeEnum(out, e.getKey(), e.getValue());
		}
		return beans.size() + enums.size();
	}

	private void writeBean(final Path out, final BeanDef b) throws IOException
	{
		final String pkg = b.className.substring(0, b.className.lastIndexOf('.'));
		final String simple = b.className.substring(b.className.lastIndexOf('.') + 1);
		final StringBuilder s = new StringBuilder();
		s.append("/*\n * *-beans.xml から ant build が作ったファイル。直接書き換えない(次のビルドで消える)。\n */\n");
		s.append("package ").append(pkg).append(";\n\n");
		s.append("/**\n * ").append(b.description != null ? b.description : "Generated bean " + simple).append("\n * (first declared in ")
				.append(b.declaredIn).append("-beans.xml)\n */\n");
		s.append("@SuppressWarnings(\"all\")\npublic class ").append(simple);
		if (b.superClass != null)
		{
			s.append(" extends ").append(b.superClass);
		}
		s.append(" implements java.io.Serializable\n{\n\tprivate static final long serialVersionUID = 1L;\n\n");
		for (final Map.Entry<String, String[]> p : b.properties.entrySet())
		{
			s.append("\t/** <i>Generated property</i> for <code>").append(simple).append('.').append(p.getKey()).append("</code> (")
					.append(p.getValue()[1]).append("-beans.xml). */\n");
			s.append("\tprivate ").append(javaType(p.getValue()[0])).append(' ').append(p.getKey()).append(";\n\n");
		}
		s.append("\tpublic ").append(simple).append("()\n\t{\n\t\t// default constructor\n\t}\n\n");
		for (final Map.Entry<String, String[]> p : b.properties.entrySet())
		{
			final String type = javaType(p.getValue()[0]);
			final String cap = Character.toUpperCase(p.getKey().charAt(0)) + p.getKey().substring(1);
			s.append("\tpublic void set").append(cap).append("(final ").append(type).append(' ').append(p.getKey()).append(")\n\t{\n\t\tthis.")
					.append(p.getKey()).append(" = ").append(p.getKey()).append(";\n\t}\n\n");
			s.append("\tpublic ").append(type).append(' ').append("boolean".equals(type) ? "is" : "get").append(cap).append("()\n\t{\n\t\treturn ")
					.append(p.getKey()).append(";\n\t}\n\n");
		}
		s.append("}\n");
		write(out, pkg, simple, s.toString());
	}

	private void writeEnum(final Path out, final String className, final List<String> values) throws IOException
	{
		final String pkg = className.substring(0, className.lastIndexOf('.'));
		final String simple = className.substring(className.lastIndexOf('.') + 1);
		final String body = "/*\n * *-beans.xml から ant build が作ったファイル。\n */\npackage " + pkg + ";\n\npublic enum " + simple + "\n{\n\t"
				+ String.join(", ", values) + "\n}\n";
		write(out, pkg, simple, body);
	}

	private static String javaType(final String type)
	{
		final String t = type.trim();
		return switch (t)
		{
			case "String", "Integer", "Long", "Double", "Boolean", "Float", "Object" -> "java.lang." + t;
			default -> t.replace("<String>", "<java.lang.String>");
		};
	}

	private static void write(final Path out, final String pkg, final String name, final String content) throws IOException
	{
		final Path dir = out.resolve(pkg.replace('.', '/'));
		Files.createDirectories(dir);
		Files.writeString(dir.resolve(name + ".java"), content, StandardCharsets.UTF_8);
	}
}
