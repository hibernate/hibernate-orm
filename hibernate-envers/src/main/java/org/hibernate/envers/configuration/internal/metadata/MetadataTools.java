/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Iterator;
import java.util.List;
import javax.persistence.JoinColumn;

import org.hibernate.AssertionFailure;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.DerivedValue;
import org.hibernate.metamodel.spi.relational.Value;

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
			boolean useRevisionEntityWithNativeId,
			String idColumnName) {
		final Element idMapping = parent.addElement( "id" );
		idMapping.addAttribute( "name", name ).addAttribute( "type", type );
		MetadataTools.addColumn( idMapping, idColumnName, null, null, null, null, null, null, false );

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
		}

		propMapping.addAttribute( "name", name );

		if ( !key ) {
			propMapping.addAttribute( "insert", Boolean.toString( insertable ) );
			propMapping.addAttribute( "update", Boolean.toString( updateable ) );
		}
		if ( type != null ) {
			propMapping.addAttribute( "type", type );
		}

		return propMapping;
	}

	public static Element addProperty(Element parent, String name, String type, boolean insertable, boolean key) {
		return addProperty( parent, name, type, insertable, false, key );
	}

	public static Element addModifiedFlagProperty(Element parent, String propertyName, String suffix) {
		return addProperty(
				parent,
				getModifiedFlagPropertyName( propertyName, suffix ),
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
			Long length,
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
			Long length,
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

		if ( !"subclass".equals( type ) ) {
			if ( !StringTools.isEmpty( auditTableData.getAuditTableName() ) ) {
				classMapping.addAttribute( "table", auditTableData.getAuditTableName() );
			}

			if ( !StringTools.isEmpty( auditTableData.getSchema() ) ) {
				classMapping.addAttribute( "schema", auditTableData.getSchema() );
			}

			if ( !StringTools.isEmpty( auditTableData.getCatalog() ) ) {
				classMapping.addAttribute( "catalog", auditTableData.getCatalog() );
			}
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

	public static void addColumns(Element anyMapping, List<Column> columns) {
		for ( Column column : columns ) {
			addColumn( anyMapping, column );
		}
	}

	public static void addValuesAsColumns(Element anyMapping, List<Value> values) {
		for ( Value value : values ) {
			if ( Value.ValueType.DERIVED_VALUE.equals( value.getValueType() ) ) {
				throw new FormulaNotSupportedException();
			}
			addColumn( anyMapping, (Column) value );
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
				column.getColumnName().getText(),
				column.getSize().getLength(),
				column.getSize().getScale(),
				column.getSize().getPrecision(),
				column.getSqlType(),
				column.getReadFragment(),
				column.getWriteFragment(),
				column.getColumnName().isQuoted()
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
					// "insert" and "update" attributes are not allowed on key-many-to-one or key-property elements.
					property.remove( property.attribute( "insert" ) );
					property.remove( property.attribute( "update" ) );
				}
				else if ( "property".equals( property.getName() ) ) {
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
	public static void addFormula(Element element, DerivedValue formula) {
		element.addElement( "formula" ).setText( formula.getExpression() );
	}

	/**
	 * Adds all <code>column</code> or <code>formula</code> elements.
	 *
	 * @param element Parent element.
	 * @param values List of  {@link Column} and/or {@link DerivedValue} objects.
	 */
	public static void addColumnsOrFormulas(Element element, List<Value> values) {
		for ( Value value : values ) {
			if ( value.getValueType() == Value.ValueType.COLUMN ) {
				addColumn( element, (Column) value );
			}
			else if ( value.getValueType() == Value.ValueType.DERIVED_VALUE  ) {
				addFormula( element, (DerivedValue) value );
			}
			else {
				throw new AssertionFailure(
						String.format( "unknown type of value: %s", value.getValueType() )
				);
			}
		}
	}

	/**
	 * An iterator over column names.
	 */
	public static abstract class ColumnNameIterator implements Iterator<String> {
	}

	public static ColumnNameIterator getColumnNameIterator(final Iterator<Value> selectableIterator) {
		return new ColumnNameIterator() {
			public boolean hasNext() {
				return selectableIterator.hasNext();
			}

			public String next() {
				final Value next = selectableIterator.next();
				if ( next.getValueType() == Value.ValueType.DERIVED_VALUE ) {
					throw new FormulaNotSupportedException();
				}
				return ((Column) next).getColumnName().getText();
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
