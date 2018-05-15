/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedIndex;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.model.relational.MappedUniqueKey;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * @author Emmanuel Bernard
 */
public class IndexOrUniqueKeySecondPass implements SecondPass {
	private MappedTable table;
	private final String indexName;
	private final String[] columns;
	private final MetadataBuildingContext buildingContext;
	private final Ejb3Column column;
	private final boolean unique;

	/**
	 * Build an index
	 */
	public IndexOrUniqueKeySecondPass(MappedTable table, String indexName, String[] columns, MetadataBuildingContext buildingContext) {
		this.table = table;
		this.indexName = indexName;
		this.columns = columns;
		this.buildingContext = buildingContext;
		this.column = null;
		this.unique = false;
	}


	/**
	 * Build an index
	 */
	public IndexOrUniqueKeySecondPass(String indexName, Ejb3Column column, MetadataBuildingContext buildingContext) {
		this( indexName, column, buildingContext, false );
	}

	/**
	 * Build an index if unique is false or a Unique Key if unique is true
	 */
	public IndexOrUniqueKeySecondPass(String indexName, Ejb3Column column, MetadataBuildingContext buildingContext, boolean unique) {
		this.indexName = indexName;
		this.column = column;
		this.columns = null;
		this.buildingContext = buildingContext;
		this.unique = unique;
	}

	@Override
	public void doSecondPass(Map persistentClasses) throws MappingException {
		if ( columns != null ) {
			for ( int i = 0; i < columns.length; i++ ) {
				addConstraintToColumn( columns[i] );
			}
		}
		if ( column != null ) {
			this.table = column.getTable();

			final PropertyHolder propertyHolder = column.getPropertyHolder();

			String entityName = ( propertyHolder.isComponent() ) ?
					propertyHolder.getPersistentClass().getEntityName() :
					propertyHolder.getEntityName();

			final PersistentClass persistentClass = (PersistentClass) persistentClasses.get( entityName );
			final Property property = persistentClass.getProperty( column.getPropertyName() );

			if ( property.getValue() instanceof Component ) {
				final Component component = (Component) property.getValue();

				final List<Column> columns = component.getMappedColumns().stream()
						.filter( Column.class::isInstance )
						.map(Column.class::cast )
						.collect( Collectors.toList() );
				addConstraintToColumns( columns );
			}
			else {
				addConstraintToColumn(
						 column.getMappingColumn().getQuotedName() )
				;
			}
		}
	}

	private void addConstraintToColumn(final String columnName ) {
		MappedColumn column = table.getColumn( new Column( table.getNameIdentifier(), columnName, false ) );
		if ( column == null ) {
			throw new AnnotationException(
					"@Index references a unknown column: " + columnName
			);
		}
		if ( unique ) {
			table.getOrCreateUniqueKey( indexName ).addColumn( (Column) column );
		}
		else {
			table.getOrCreateIndex( indexName ).addColumn( (Column) column );
		}
	}

	private void addConstraintToColumns(List<Column> columns) {
		if ( unique ) {
			MappedUniqueKey uniqueKey = table.getOrCreateUniqueKey( indexName );
			for ( Column column : columns ) {
				uniqueKey.addColumn( column );
			}
		}
		else {
			MappedIndex index = table.getOrCreateIndex( indexName );
			for ( Column column : columns ) {
				index.addColumn( column );
			}
		}
	}
}
