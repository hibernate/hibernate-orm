/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Iterator;
import javax.persistence.JoinColumn;

import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Selectable;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public final class MetadataTools {
	private MetadataTools() {
	}

	public static Element addNativelyGeneratedId(
			Element parent, String name, String type,
			boolean useRevisionEntityWithNativeId) {
		final Element idMapping = parent.addElement( "id" );
		idMapping.addAttribute( "name", name ).addAttribute( "type", type );

		final Element generatorMapping = idMapping.addElement( "generator" );
		if ( useRevisionEntityWithNativeId ) {
			generatorMapping.addAttribute( "class", "native" );
		}
		else {
			generatorMapping.addAttribute( "class", "org.hibernate.envers.enhanced.OrderedSequenceGenerator" );
			generatorMapping.addElement( "param" ).addAttribute( "name", "sequence_name" ).setText(
					"REVISION_GENERATOR"
			);
			generatorMapping.addElement( "param" )
					.addAttribute( "name", "table_name" )
					.setText( "REVISION_GENERATOR" );
			generatorMapping.addElement( "param" ).addAttribute( "name", "initial_value" ).setText( "1" );
			generatorMapping.addElement( "param" ).addAttribute( "name", "increment_size" ).setText( "1" );
		}
//        generatorMapping.addAttribute("class", "sequence");
//        generatorMapping.addElement("param").addAttribute("name", "sequence").setText("custom");

		return idMapping;
	}

	public static Element addProperty(
			Element parent,
			String name,
			String type,
			boolean insertable,
			boolean updateable,
			boolean key) {
		final Element propMapping;
		if ( key ) {
			propMapping = parent.addElement( "key-property" );
		}
		else {
			propMapping = parent.addElement( "property" );
			propMapping.addAttribute( "insert", Boolean.toString( insertable ) );
			propMapping.addAttribute( "update", Boolean.toString( updateable ) );
		}

		propMapping.addAttribute( "name", name );

		if ( type != null ) {
			propMapping.addAttribute( "type", type );
		}

		return propMapping;
	}

	public static Element addProperty(Element parent, String name, String type, boolean insertable, boolean key) {
		return addProperty( parent, name, type, insertable, false, key );
	}

	public static Element addModifiedFlagProperty(Element parent, String propertyName, String suffix, String modifiedFlagName) {
		return addProperty(
				parent,
				(modifiedFlagName != null) ? modifiedFlagName : getModifiedFlagPropertyName( propertyName, suffix ),
				"boolean",
				true,
				false,
				false
		);
	}

	public static String getModifiedFlagPropertyName(String propertyName, String suffix) {
		return propertyName + suffix;
	}

	private static void addOrModifyAttribute(Element parent, String name, String value) {
		final Attribute attribute = parent.attribute( name );
		if ( attribute == null ) {
			parent.addAttribute( name, value );
		}
		else {
			attribute.setValue( value );
		}
	}

	/**
	 * Column name shall be wrapped with '`' signs if quotation required.
	 */
	public static Element addOrModifyColumn(Element parent, String name) {
		final Element columnMapping = parent.element( "column" );

		if ( columnMapping == null ) {
			return addColumn( parent, name, null, null, null, null, null, null );
		}

		if ( !StringTools.isEmpty( name ) ) {
			addOrModifyAttribute( columnMapping, "name", name );
		}

		return columnMapping;
	}

	/**
	 * Adds new <code>column</code> element. Method assumes that the value of <code>name</code> attribute is already
	 * wrapped with '`' signs if quotation required. It shall be invoked when column name is taken directly from configuration
	 * file and not from {@link org.hibernate.mapping.PersistentClass} descriptor.
	 */
	public static Element addColumn(
			Element parent,
			String name,
			Integer length,
			Integer scale,
			Integer precision,
			String sqlType,
			String customRead,
			String customWrite) {
		return addColumn( parent, name, length, scale, precision, sqlType, customRead, customWrite, false );
	}

	public static Element addColumn(
			Element parent,
			String name,
			Integer length,
			Integer scale,
			Integer precision,
			String sqlType,
			String customRead,
			String customWrite,
			boolean quoted) {
		final Element columnMapping = parent.addElement( "column" );

		columnMapping.addAttribute( "name", quoted ? "`" + name + "`" : name );
		if ( length != null ) {
			columnMapping.addAttribute( "length", length.toString() );
		}
		if ( scale != null ) {
			columnMapping.addAttribute( "scale", Integer.toString( scale ) );
		}
		if ( precision != null ) {
			columnMapping.addAttribute( "precision", Integer.toString( precision ) );
		}
		if ( !StringTools.isEmpty( sqlType ) ) {
			columnMapping.addAttribute( "sql-type", sqlType );
		}

		if ( !StringTools.isEmpty( customRead ) ) {
			columnMapping.addAttribute( "read", customRead );
		}
		if ( !StringTools.isEmpty( customWrite ) ) {
			columnMapping.addAttribute( "write", customWrite );
		}

		return columnMapping;
	}

	private static Element createEntityCommon(
			Document document,
			String type,
			AuditTableData auditTableData,
			String discriminatorValue,
			Boolean isAbstract) {
		final Element hibernateMapping = document.addElement( "hibernate-mapping" );
		hibernateMapping.addAttribute( "auto-import", "false" );

		final Element classMapping = hibernateMapping.addElement( type );

		if ( auditTableData.getAuditEntityName() != null ) {
			classMapping.addAttribute( "entity-name", auditTableData.getAuditEntityName() );
		}

		if ( discriminatorValue != null ) {
			classMapping.addAttribute( "discriminator-value", discriminatorValue );
		}

		if ( !StringTools.isEmpty( auditTableData.getAuditTableName() ) ) {
			classMapping.addAttribute( "table", auditTableData.getAuditTableName() );
		}

		if ( !StringTools.isEmpty( auditTableData.getSchema() ) ) {
			classMapping.addAttribute( "schema", auditTableData.getSchema() );
		}

		if ( !StringTools.isEmpty( auditTableData.getCatalog() ) ) {
			classMapping.addAttribute( "catalog", auditTableData.getCatalog() );
		}

		if ( isAbstract != null ) {
			classMapping.addAttribute( "abstract", isAbstract.toString() );
		}

		return classMapping;
	}

	public static Element createEntity(
			Document document,
			AuditTableData auditTableData,
			String discriminatorValue,
			Boolean isAbstract) {
		return createEntityCommon( document, "class", auditTableData, discriminatorValue, isAbstract );
	}

	public static Element createSubclassEntity(
			Document document,
			String subclassType,
			AuditTableData auditTableData,
			String extendsEntityName,
			String discriminatorValue,
			Boolean isAbstract) {
		final Element classMapping = createEntityCommon(
				document,
				subclassType,
				auditTableData,
				discriminatorValue,
				isAbstract
		);

		classMapping.addAttribute( "extends", extendsEntityName );

		return classMapping;
	}

	public static Element createJoin(
			Element parent,
			String tableName,
			String schema,
			String catalog) {
		final Element joinMapping = parent.addElement( "join" );

		joinMapping.addAttribute( "table", tableName );

		if ( !StringTools.isEmpty( schema ) ) {
			joinMapping.addAttribute( "schema", schema );
		}

		if ( !StringTools.isEmpty( catalog ) ) {
			joinMapping.addAttribute( "catalog", catalog );
		}

		return joinMapping;
	}

	public static void addColumns(Element anyMapping, Iterator selectables) {
		while ( selectables.hasNext() ) {
			final Selectable selectable = (Selectable) selectables.next();
			if ( selectable.isFormula() ) {
				throw new FormulaNotSupportedException();
			}
			addColumn( anyMapping, (Column) selectable );
		}
	}

	/**
	 * Adds <code>column</code> element with the following attributes (unless empty): <code>name</code>,
	 * <code>length</code>, <code>scale</code>, <code>precision</code>, <code>sql-type</code>, <code>read</code>
	 * and <code>write</code>.
	 *
	 * @param anyMapping Parent element.
	 * @param column Column descriptor.
	 */
	public static void addColumn(Element anyMapping, Column column) {
		addColumn(
				anyMapping,
				column.getName(),
				column.getLength(),
				column.getScale(),
				column.getPrecision(),
				column.getSqlType(),
				column.getCustomRead(),
				column.getCustomWrite(),
				column.isQuoted()
		);
	}

	@SuppressWarnings({"unchecked"})
	private static void changeNamesInColumnElement(Element element, ColumnNameIterator columnNameIterator) {
		final Iterator<Element> properties = element.elementIterator();
		while ( properties.hasNext() ) {
			final Element property = properties.next();

			if ( "column".equals( property.getName() ) ) {
				final Attribute nameAttr = property.attribute( "name" );
				if ( nameAttr != null ) {
					nameAttr.setText( columnNameIterator.next() );
				}
			}
		}
	}

	@SuppressWarnings({"unchecked"})
	public static void prefixNamesInPropertyElement(
			Element element,
			String prefix,
			ColumnNameIterator columnNameIterator,
			boolean changeToKey,
			boolean insertable) {
		final Iterator<Element> properties = element.elementIterator();
		while ( properties.hasNext() ) {
			final Element property = properties.next();

			if ( "property".equals( property.getName() ) || "many-to-one".equals( property.getName() ) ) {
				final Attribute nameAttr = property.attribute( "name" );
				if ( nameAttr != null ) {
					nameAttr.setText( prefix + nameAttr.getText() );
				}

				changeNamesInColumnElement( property, columnNameIterator );

				if ( changeToKey ) {
					property.setName( "key-" + property.getName() );
				}

				if ( "property".equals( property.getName() ) ) {
					final Attribute insert = property.attribute( "insert" );
					insert.setText( Boolean.toString( insertable ) );
				}
			}
		}
	}

	/**
	 * Adds <code>formula</code> element.
	 *
	 * @param element Parent element.
	 * @param formula Formula descriptor.
	 */
	public static void addFormula(Element element, Formula formula) {
		element.addElement( "formula" ).setText( formula.getText() );
	}

	/**
	 * Adds all <code>column</code> or <code>formula</code> elements.
	 *
	 * @param element Parent element.
	 * @param columnIterator Iterator pointing at {@link org.hibernate.mapping.Column} and/or
	 * {@link org.hibernate.mapping.Formula} objects.
	 */
	public static void addColumnsOrFormulas(Element element, Iterator columnIterator) {
		while ( columnIterator.hasNext() ) {
			final Object o = columnIterator.next();
			if ( o instanceof Column ) {
				addColumn( element, (Column) o );
			}
			else if ( o instanceof Formula ) {
				addFormula( element, (Formula) o );
			}
		}
	}

	/**
	 * An iterator over column names.
	 */
	public static abstract class ColumnNameIterator implements Iterator<String> {
	}

	public static ColumnNameIterator getColumnNameIterator(final Iterator<Selectable> selectableIterator) {
		return new ColumnNameIterator() {
			public boolean hasNext() {
				return selectableIterator.hasNext();
			}

			public String next() {
				final Selectable next = selectableIterator.next();
				if ( next.isFormula() ) {
					throw new FormulaNotSupportedException();
				}
				return ((Column) next).getName();
			}

			public void remove() {
				selectableIterator.remove();
			}
		};
	}

	public static ColumnNameIterator getColumnNameIterator(final JoinColumn[] joinColumns) {
		return new ColumnNameIterator() {
			int counter;

			public boolean hasNext() {
				return counter < joinColumns.length;
			}

			public String next() {
				return joinColumns[counter++].name();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
