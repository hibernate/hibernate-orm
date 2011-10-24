/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
	private final Mappings mappings;
	private final Ejb3Column column;
	private final boolean unique;

	/**
	 * Build an index
	 */
	public IndexOrUniqueKeySecondPass(Table table, String indexName, String[] columns, Mappings mappings) {
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
	public IndexOrUniqueKeySecondPass(String indexName, Ejb3Column column, Mappings mappings) {
		this( indexName, column, mappings, false );
	}

	/**
	 * Build an index if unique is false or a Unique Key if unique is true
	 */
	public IndexOrUniqueKeySecondPass(String indexName, Ejb3Column column, Mappings mappings, boolean unique) {
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
