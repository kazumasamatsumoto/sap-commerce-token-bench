package de.hybris.bootstrap.typesystem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * items.xml を読んで、型(itemtype)、属性、関連(relation)、enum、コレクション型をまとめたもの。
 * ビルドのとき(Model を作る)と、動かすとき(表の形と FlexibleSearch の読み替え)の両方で使う。
 * 本物の Commerce の型の仕組みのうち、計測台で使う部分だけを持つ。
 */
public final class TypeSystem
{
	public static final String COLUMN_PREFIX = "p_";

	private final Map<String, ItemTypeDef> itemTypes = new LinkedHashMap<>();
	private final Map<String, EnumTypeDef> enumTypes = new LinkedHashMap<>();
	private final Map<String, RelationDef> relations = new LinkedHashMap<>();
	private final Map<String, CollectionTypeDef> collectionTypes = new LinkedHashMap<>();

	/** 1 つの拡張の items.xml。packageRoot は extensioninfo.xml の coremodule packageroot。 */
	public record Source(String extensionName, String packageRoot, InputStream xml)
	{
	}

	public static TypeSystem load(final List<Source> sources)
	{
		final TypeSystem ts = new TypeSystem();
		for (final Source s : sources)
		{
			ts.read(s);
		}
		ts.validate();
		return ts;
	}

	// ------------------------------------------------------------------ 読み込み

	private void read(final Source source)
	{
		final Document doc;
		try
		{
			final DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			f.setNamespaceAware(false);
			doc = f.newDocumentBuilder().parse(source.xml());
		}
		catch (final Exception e)
		{
			throw new TypeSystemException(source.extensionName() + "-items.xml を読めません: " + e.getMessage(), e);
		}
		final Element root = doc.getDocumentElement();
		for (final Element e : descendants(root, "collectiontype"))
		{
			final CollectionTypeDef c = new CollectionTypeDef(e.getAttribute("code"), e.getAttribute("elementtype"),
					"set".equalsIgnoreCase(e.getAttribute("type")) ? "set" : "list");
			collectionTypes.put(key(c.code()), c);
		}
		for (final Element e : descendants(root, "enumtype"))
		{
			readEnum(e, source);
		}
		for (final Element e : descendants(root, "itemtype"))
		{
			readItemType(e, source);
		}
		for (final Element e : descendants(root, "relation"))
		{
			readRelation(e, source);
		}
	}

	private void readEnum(final Element e, final Source source)
	{
		final String code = e.getAttribute("code");
		final boolean autocreate = !"false".equals(e.getAttribute("autocreate"));
		EnumTypeDef def = enumTypes.get(key(code));
		if (def == null)
		{
			if (!autocreate)
			{
				throw new TypeSystemException(source.extensionName() + "-items.xml: enum '" + code
						+ "' は autocreate=\"false\" ですが、元の定義がどの拡張にもありません");
			}
			// package="..." は疑似プラットフォームだけが使う(本物の標準拡張ごとの置き場所に合わせるため)。
			final String pkg = e.getAttribute("package").isEmpty() ? packageFor(source, "enums") : e.getAttribute("package");
			def = new EnumTypeDef(code, pkg, source.extensionName(),
					"true".equals(e.getAttribute("dynamic")));
			enumTypes.put(key(code), def);
		}
		for (final Element v : children(e, "value"))
		{
			if (!def.values.contains(v.getAttribute("code")))
			{
				def.values.add(v.getAttribute("code"));
			}
		}
	}

	private void readItemType(final Element e, final Source source)
	{
		final String code = e.getAttribute("code");
		final boolean autocreate = !"false".equals(e.getAttribute("autocreate"));
		ItemTypeDef def = itemTypes.get(key(code));
		if (autocreate)
		{
			if (def != null)
			{
				throw new TypeSystemException(source.extensionName() + "-items.xml: 型 '" + code + "' は " + def.extensionName
						+ " で定義済みです。既存の型に属性を足すときは autocreate=\"false\" generate=\"false\" にします");
			}
			final String jalo = e.getAttribute("jaloclass");
			def = new ItemTypeDef(code, emptyToNull(e.getAttribute("extends")), source.extensionName(),
					modelPackageFor(source, jalo), "true".equals(e.getAttribute("abstract")),
					!"false".equals(e.getAttribute("generate")));
			itemTypes.put(key(code), def);
			final Element dep = child(e, "deployment");
			if (dep != null)
			{
				def.table = dep.getAttribute("table").toLowerCase(Locale.ROOT);
				def.typecodeNumber = dep.getAttribute("typecode");
			}
			final Element desc = child(e, "description");
			def.description = desc == null ? null : desc.getTextContent().trim();
		}
		else if (def == null)
		{
			throw new TypeSystemException(source.extensionName() + "-items.xml: 型 '" + code
					+ "' は autocreate=\"false\" ですが、元の定義がどの拡張にもありません(拡張の読み込み順か、型の名前を確かめてください)");
		}
		final Element attrs = child(e, "attributes");
		if (attrs != null)
		{
			for (final Element a : children(attrs, "attribute"))
			{
				readAttribute(def, a, source);
			}
		}
	}

	private void readAttribute(final ItemTypeDef owner, final Element a, final Source source)
	{
		final String qualifier = a.getAttribute("qualifier");
		final boolean redeclare = "true".equals(a.getAttribute("redeclare"));
		if (owner.attributes.containsKey(key(qualifier)) && !redeclare)
		{
			throw new TypeSystemException(source.extensionName() + "-items.xml: 型 '" + owner.code + "' の属性 '" + qualifier
					+ "' が二重に定義されています");
		}
		final AttributeDef def = new AttributeDef(qualifier, a.getAttribute("type"), owner.code, source.extensionName());
		final Element persistence = child(a, "persistence");
		if (persistence != null)
		{
			def.persistence = persistence.getAttribute("type");
			def.attributeHandler = emptyToNull(persistence.getAttribute("attributeHandler"));
		}
		final Element modifiers = child(a, "modifiers");
		if (modifiers != null)
		{
			def.optional = !"false".equals(modifiers.getAttribute("optional"));
			def.unique = "true".equals(modifiers.getAttribute("unique"));
			def.writable = !"false".equals(modifiers.getAttribute("write"));
		}
		final Element defaultValue = child(a, "defaultvalue");
		def.defaultValue = defaultValue == null ? null : defaultValue.getTextContent().trim();
		final Element desc = child(a, "description");
		def.description = desc == null ? null : desc.getTextContent().trim();
		if ("dynamic".equals(def.persistence) && def.attributeHandler == null)
		{
			throw new TypeSystemException(source.extensionName() + "-items.xml: 動的属性 '" + owner.code + "." + qualifier
					+ "' に attributeHandler がありません");
		}
		owner.attributes.put(key(qualifier), def);
	}

	private void readRelation(final Element e, final Source source)
	{
		final String code = e.getAttribute("code");
		final Element src = child(e, "sourceElement");
		final Element tgt = child(e, "targetElement");
		if (src == null || tgt == null)
		{
			throw new TypeSystemException(source.extensionName() + "-items.xml: relation '" + code + "' に sourceElement か targetElement がありません");
		}
		final RelationDef rel = new RelationDef(code, source.extensionName(), end(src), end(tgt));
		final Element dep = child(e, "deployment");
		rel.table = dep == null ? null : dep.getAttribute("table").toLowerCase(Locale.ROOT);
		if (rel.manyToMany() && rel.table == null)
		{
			throw new TypeSystemException(source.extensionName() + "-items.xml: 多対多の relation '" + code + "' には deployment(表の名前)が必要です");
		}
		relations.put(key(code), rel);
		// sourceElement の qualifier は target 側の型の属性になる(本物と同じ向き)。
		attachRelationAttribute(rel, rel.source, rel.target, true, source);
		attachRelationAttribute(rel, rel.target, rel.source, false, source);
	}

	private void attachRelationAttribute(final RelationDef rel, final RelationEnd end, final RelationEnd other,
			final boolean sourceSide, final Source source)
	{
		if (end.qualifier == null || end.qualifier.isEmpty())
		{
			return;
		}
		final ItemTypeDef owner = itemTypes.get(key(other.type));
		if (owner == null)
		{
			throw new TypeSystemException(source.extensionName() + "-items.xml: relation '" + rel.code + "' の型 '" + other.type + "' がありません");
		}
		final AttributeDef def = new AttributeDef(end.qualifier, end.type, owner.code, source.extensionName());
		def.relation = rel;
		def.relationSourceSide = sourceSide;
		def.collection = "many".equals(end.cardinality);
		def.collectionKind = end.collectionType;
		def.optional = true;
		owner.attributes.put(key(end.qualifier), def);
	}

	private static RelationEnd end(final Element e)
	{
		final RelationEnd end = new RelationEnd();
		end.qualifier = e.getAttribute("qualifier");
		end.type = e.getAttribute("type");
		end.cardinality = "many".equals(e.getAttribute("cardinality")) ? "many" : "one";
		end.collectionType = emptyToNull(e.getAttribute("collectiontype"));
		end.ordered = "true".equals(e.getAttribute("ordered"));
		return end;
	}

	private void validate()
	{
		for (final ItemTypeDef t : itemTypes.values())
		{
			if (t.extendsCode != null && !itemTypes.containsKey(key(t.extendsCode)))
			{
				throw new TypeSystemException(t.extensionName + "-items.xml: 型 '" + t.code + "' の extends '" + t.extendsCode + "' がありません");
			}
			if (t.extendsCode == null && !"Item".equals(t.code))
			{
				t.extendsCode = "GenericItem";
			}
			for (final AttributeDef a : t.attributes.values())
			{
				if (a.relation == null && kindOf(a) == Kind.UNKNOWN)
				{
					throw new TypeSystemException(a.extensionName + "-items.xml: 属性 '" + t.code + "." + a.qualifier + "' の型 '" + a.type
							+ "' がありません(java の型、enum、itemtype、collectiontype のどれでもない)");
				}
			}
		}
		for (final ItemTypeDef t : itemTypes.values())
		{
			if (!t.isAbstract && tableOf(t) == null && !"Item".equals(t.code))
			{
				throw new TypeSystemException(t.extensionName + "-items.xml: 型 '" + t.code + "' に表がありません(自分か親に deployment が必要です)");
			}
		}
	}

	// ------------------------------------------------------------------ 引く

	public Collection<ItemTypeDef> getItemTypes()
	{
		return itemTypes.values();
	}

	public Collection<EnumTypeDef> getEnumTypes()
	{
		return enumTypes.values();
	}

	public Collection<RelationDef> getRelations()
	{
		return relations.values();
	}

	public Optional<ItemTypeDef> findItemType(final String code)
	{
		return Optional.ofNullable(code == null ? null : itemTypes.get(key(code)));
	}

	public ItemTypeDef getItemType(final String code)
	{
		return findItemType(code).orElseThrow(() -> new TypeSystemException("型 '" + code + "' がありません"));
	}

	public Optional<EnumTypeDef> findEnumType(final String code)
	{
		return Optional.ofNullable(enumTypes.get(key(code)));
	}

	public Optional<RelationDef> findRelation(final String code)
	{
		return Optional.ofNullable(relations.get(key(code)));
	}

	public Optional<CollectionTypeDef> findCollectionType(final String code)
	{
		return Optional.ofNullable(collectionTypes.get(key(code)));
	}

	public ItemTypeDef superType(final ItemTypeDef t)
	{
		return t.extendsCode == null ? null : itemTypes.get(key(t.extendsCode));
	}

	/** 自分と親たちの属性(親が先)。同じ名前は子の定義が勝つ。 */
	public Map<String, AttributeDef> allAttributes(final ItemTypeDef t)
	{
		final List<ItemTypeDef> chain = new ArrayList<>();
		for (ItemTypeDef c = t; c != null; c = superType(c))
		{
			chain.add(0, c);
		}
		final Map<String, AttributeDef> out = new LinkedHashMap<>();
		for (final ItemTypeDef c : chain)
		{
			out.putAll(c.attributes);
		}
		return out;
	}

	public Optional<AttributeDef> findAttribute(final ItemTypeDef t, final String qualifier)
	{
		return Optional.ofNullable(allAttributes(t).get(key(qualifier)));
	}

	/** 型の行が入る表。自分に deployment が無ければ親の表に入る(本物と同じ)。 */
	public String tableOf(final ItemTypeDef t)
	{
		for (ItemTypeDef c = t; c != null; c = superType(c))
		{
			if (c.table != null)
			{
				return c.table;
			}
		}
		return null;
	}

	public boolean isAssignableFrom(final ItemTypeDef parent, final ItemTypeDef child)
	{
		for (ItemTypeDef c = child; c != null; c = superType(c))
		{
			if (c == parent)
			{
				return true;
			}
		}
		return false;
	}

	/** 自分と子孫の型(FlexibleSearch で {Product} が子の型も含むため)。 */
	public List<ItemTypeDef> withSubtypes(final ItemTypeDef t)
	{
		final List<ItemTypeDef> out = new ArrayList<>();
		for (final ItemTypeDef c : itemTypes.values())
		{
			if (isAssignableFrom(t, c))
			{
				out.add(c);
			}
		}
		return out;
	}

	/** 同じ表に入るすべての型の、列になる属性。 */
	public Map<String, AttributeDef> columnsOfTable(final String table)
	{
		final Map<String, AttributeDef> out = new LinkedHashMap<>();
		for (final ItemTypeDef t : itemTypes.values())
		{
			if (table.equals(tableOf(t)))
			{
				for (final AttributeDef a : allAttributes(t).values())
				{
					if (hasColumn(a))
					{
						out.putIfAbsent(columnName(a.qualifier), a);
					}
				}
			}
		}
		return out;
	}

	public List<String> tables()
	{
		final List<String> out = new ArrayList<>();
		for (final ItemTypeDef t : itemTypes.values())
		{
			final String table = tableOf(t);
			if (table != null && !out.contains(table))
			{
				out.add(table);
			}
		}
		return out;
	}

	/** 列を持つ属性: 普通の属性と、関連の「1」の側(相手の PK を持つ)。多の側と動的属性は列を持たない。 */
	public boolean hasColumn(final AttributeDef a)
	{
		// pk / creationtime / modifiedtime は、どの表にも決まった名前の列(pk / createdts / modifiedts)で持つ。
		if ("Item".equals(a.declaringType) || "dynamic".equals(a.persistence))
		{
			return false;
		}
		if (a.relation != null)
		{
			return !a.collection && !a.relation.manyToMany();
		}
		return true;
	}

	public static String columnName(final String qualifier)
	{
		return COLUMN_PREFIX + qualifier.toLowerCase(Locale.ROOT);
	}

	public enum Kind
	{
		STRING, INTEGER, LONG, DOUBLE, BOOLEAN, DATE, DECIMAL, LOCALIZED_STRING, ENUM, ITEM, COLLECTION, UNKNOWN
	}

	public Kind kindOf(final AttributeDef a)
	{
		if (a.relation != null)
		{
			return a.collection ? Kind.COLLECTION : Kind.ITEM;
		}
		String type = a.type;
		if (type.startsWith("localized:"))
		{
			return Kind.LOCALIZED_STRING;
		}
		switch (type)
		{
			case "java.lang.String":
				return Kind.STRING;
			case "java.lang.Integer":
			case "int":
				return Kind.INTEGER;
			case "java.lang.Long":
			case "long":
				return Kind.LONG;
			case "java.lang.Double":
			case "double":
			case "java.lang.Float":
			case "float":
				return Kind.DOUBLE;
			case "java.lang.Boolean":
			case "boolean":
				return Kind.BOOLEAN;
			case "java.util.Date":
				return Kind.DATE;
			case "java.math.BigDecimal":
				return Kind.DECIMAL;
			default:
				break;
		}
		if (enumTypes.containsKey(key(type)))
		{
			return Kind.ENUM;
		}
		if (itemTypes.containsKey(key(type)))
		{
			return Kind.ITEM;
		}
		if (collectionTypes.containsKey(key(type)))
		{
			return Kind.COLLECTION;
		}
		return Kind.UNKNOWN;
	}

	// ------------------------------------------------------------------ 道具

	static String key(final String code)
	{
		return code.toLowerCase(Locale.ROOT);
	}

	private static String emptyToNull(final String s)
	{
		return s == null || s.isEmpty() ? null : s;
	}

	private static String packageFor(final Source source, final String suffix)
	{
		return source.packageRoot() + "." + suffix;
	}

	/** jaloclass の .jalo. を .model. に替えた場所(platform の型は core.model)。無ければ packageroot.model。 */
	private static String modelPackageFor(final Source source, final String jaloclass)
	{
		if (jaloclass == null || jaloclass.isEmpty())
		{
			return packageFor(source, "model");
		}
		final String pkg = jaloclass.substring(0, jaloclass.lastIndexOf('.'));
		if (pkg.startsWith("de.hybris.platform.jalo"))
		{
			return pkg.replace("de.hybris.platform.jalo", "de.hybris.platform.core.model");
		}
		return pkg.replace(".jalo", ".model");
	}

	private static List<Element> descendants(final Element root, final String name)
	{
		final List<Element> out = new ArrayList<>();
		final NodeList list = root.getElementsByTagName(name);
		for (int i = 0; i < list.getLength(); i++)
		{
			out.add((Element) list.item(i));
		}
		return out;
	}

	private static List<Element> children(final Element parent, final String name)
	{
		final List<Element> out = new ArrayList<>();
		for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n instanceof Element el && name.equals(el.getTagName()))
			{
				out.add(el);
			}
		}
		return out;
	}

	private static Element child(final Element parent, final String name)
	{
		final List<Element> list = children(parent, name);
		return list.isEmpty() ? null : list.get(0);
	}

	// ------------------------------------------------------------------ 型の定義

	public static final class ItemTypeDef
	{
		public final String code;
		public String extendsCode;
		public final String extensionName;
		public final String modelPackage;
		public final boolean isAbstract;
		public final boolean generate;
		public String table;
		public String typecodeNumber;
		public String description;
		public final Map<String, AttributeDef> attributes = new LinkedHashMap<>();

		ItemTypeDef(final String code, final String extendsCode, final String extensionName, final String modelPackage,
				final boolean isAbstract, final boolean generate)
		{
			this.code = code;
			this.extendsCode = extendsCode;
			this.extensionName = extensionName;
			this.modelPackage = modelPackage;
			this.isAbstract = isAbstract;
			this.generate = generate;
		}

		public String modelClassName()
		{
			return modelPackage + "." + code + "Model";
		}
	}

	public static final class AttributeDef
	{
		public final String qualifier;
		public final String type;
		public final String declaringType;
		public final String extensionName;
		public String persistence = "property";
		public String attributeHandler;
		public boolean optional = true;
		public boolean unique;
		public boolean writable = true;
		public String defaultValue;
		public String description;
		public RelationDef relation;
		public boolean relationSourceSide;
		public boolean collection;
		public String collectionKind;

		AttributeDef(final String qualifier, final String type, final String declaringType, final String extensionName)
		{
			this.qualifier = qualifier;
			this.type = type;
			this.declaringType = declaringType;
			this.extensionName = extensionName;
		}

		public boolean isLocalized()
		{
			return type.startsWith("localized:");
		}

		/** 関連の属性の相手の型。 */
		public String relatedType()
		{
			return type;
		}
	}

	public static final class RelationDef
	{
		public final String code;
		public final String extensionName;
		public final RelationEnd source;
		public final RelationEnd target;
		public String table;

		RelationDef(final String code, final String extensionName, final RelationEnd source, final RelationEnd target)
		{
			this.code = code;
			this.extensionName = extensionName;
			this.source = source;
			this.target = target;
		}

		public boolean manyToMany()
		{
			return "many".equals(source.cardinality) && "many".equals(target.cardinality);
		}
	}

	public static final class RelationEnd
	{
		public String qualifier;
		public String type;
		public String cardinality;
		public String collectionType;
		public boolean ordered;
	}

	public static final class EnumTypeDef
	{
		public final String code;
		public final String packageName;
		public final String extensionName;
		public final boolean dynamic;
		public final List<String> values = new ArrayList<>();

		EnumTypeDef(final String code, final String packageName, final String extensionName, final boolean dynamic)
		{
			this.code = code;
			this.packageName = packageName;
			this.extensionName = extensionName;
			this.dynamic = dynamic;
		}

		public String className()
		{
			return packageName + "." + code;
		}
	}

	public record CollectionTypeDef(String code, String elementType, String kind)
	{
	}
}
