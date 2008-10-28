//$Id$
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/**
 * @author Emmanuel Bernard
 */
public class IndexOrUniqueKeySecondPass implements SecondPass {
	private Table table;
	private final String indexName;
	private final String[] columns;
	private final ExtendedMappings mappings;
	private final Ejb3Column column;
	private final boolean unique;

	/**
	 * Build an index
	 */
	public IndexOrUniqueKeySecondPass(Table table, String indexName, String[] columns, ExtendedMappings mappings) {
		this.table = table;
		this.indexName = indexName;
		this.columns = columns;
		this.mappings = mappings;
		this.column = null;
		this.unique = false;
	}

	/**
	 * Build an index
	 */
	public IndexOrUniqueKeySecondPass(String indexName, Ejb3Column column, ExtendedMappings mappings) {
		this( indexName, column, mappings, false );
	}

	/**
	 * Build an index if unique is false or a Unique Key if unique is true
	 */
	public IndexOrUniqueKeySecondPass(String indexName, Ejb3Column column,
									  ExtendedMappings mappings, boolean unique) {
		this.indexName = indexName;
		this.column = column;
		this.columns = null;
		this.mappings = mappings;
		this.unique = unique;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		if ( columns != null ) {
			for (String columnName : columns) {
				addConstraintToColumn( columnName );
			}
		}
		if ( column != null ) {
			this.table = column.getTable();
			addConstraintToColumn( mappings.getLogicalColumnName( column.getMappingColumn().getQuotedName(), table ) );
		}
	}

	private void addConstraintToColumn(String columnName) {
		Column column = table.getColumn(
				new Column(
						mappings.getPhysicalColumnName( columnName, table )
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
