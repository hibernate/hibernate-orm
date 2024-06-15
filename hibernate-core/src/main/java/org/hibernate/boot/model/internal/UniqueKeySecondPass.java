/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/**
 * @author Emmanuel Bernard
 */
public class UniqueKeySecondPass implements SecondPass {
	private final String indexName;
	private final MetadataBuildingContext buildingContext;
	private final AnnotatedColumn column;

	/**
	 * Build an index if unique is false or a Unique Key if unique is true
	 */
	public UniqueKeySecondPass(String indexName, AnnotatedColumn column, MetadataBuildingContext buildingContext) {
		this.indexName = indexName;
		this.column = column;
		this.buildingContext = buildingContext;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		if ( column != null ) {
			final AnnotatedColumns annotatedColumns = column.getParent();
			final Table table = annotatedColumns.getTable();
			final PropertyHolder propertyHolder = annotatedColumns.getPropertyHolder();
			final String entityName =
					propertyHolder.isComponent()
							? propertyHolder.getPersistentClass().getEntityName()
							: propertyHolder.getEntityName();
			final String propertyName = annotatedColumns.getPropertyName();
			final Property property = persistentClasses.get( entityName ).getProperty( propertyName );
			addConstraintToProperty( property, table );
		}
	}

	private void addConstraintToProperty(Property property, Table table) {
		if ( property.getValue() instanceof Component ) {
			final Component component = (Component) property.getValue();
			final List<Column> columns = new ArrayList<>();
			for ( Selectable selectable: component.getSelectables() ) {
				if ( selectable instanceof Column ) {
					columns.add( (Column) selectable );
				}
			}
			addConstraintToColumns( columns, table );
		}
		else {
			addConstraintToColumn( column.getMappingColumn(), table );
		}
	}

	private void addConstraintToColumn(Column mappingColumn, Table table) {
		final String columnName =
				buildingContext.getMetadataCollector()
						.getLogicalColumnName( table, mappingColumn.getQuotedName() );
		final Column column = table.getColumn( buildingContext.getMetadataCollector(), columnName );
		if ( column == null ) {
			throw new AnnotationException(
					"Table '" + table.getName() + "' has no column named '" + columnName
							+ "' matching the column specified in '@Index'"
			);
		}
		table.getOrCreateUniqueKey( indexName ).addColumn( column );
	}

	private void addConstraintToColumns(List<Column> columns, Table table) {
		final UniqueKey uniqueKey = table.getOrCreateUniqueKey( indexName );
		for ( Column column : columns ) {
			uniqueKey.addColumn( column );
		}
	}
}
