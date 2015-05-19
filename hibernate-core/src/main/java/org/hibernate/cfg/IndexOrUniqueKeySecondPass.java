/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/**
 * @author Emmanuel Bernard
 */
public class IndexOrUniqueKeySecondPass implements SecondPass {
	private Table table;
	private final String indexName;
	private final String[] columns;
	private final MetadataBuildingContext buildingContext;
	private final Ejb3Column column;
	private final boolean unique;

	/**
	 * Build an index
	 */
	public IndexOrUniqueKeySecondPass(Table table, String indexName, String[] columns, MetadataBuildingContext buildingContext) {
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
			addConstraintToColumn(
					buildingContext.getMetadataCollector()
							.getLogicalColumnName( table, column.getMappingColumn().getQuotedName() )
			);
		}
	}

	private void addConstraintToColumn(final String columnName ) {
		Column column = table.getColumn(
				new Column(
						buildingContext.getMetadataCollector().getPhysicalColumnName( table, columnName )
				)
		);
		if ( column == null ) {
			throw new AnnotationException(
					"@Index references a unknown column: " + columnName
			);
		}
		if ( unique )
			table.getOrCreateUniqueKey( indexName ).addColumn( column );
		else
			table.getOrCreateIndex( indexName ).addColumn( column );
	}
}
