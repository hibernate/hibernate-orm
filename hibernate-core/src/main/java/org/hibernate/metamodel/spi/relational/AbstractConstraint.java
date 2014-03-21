/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.spi.relational;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;

/**
 * Support for writing {@link Constraint} implementations
 *
 * @todo do we need to support defining these on particular schemas/catalogs?
 *
 * @author Steve Ebersole
 * @author Gail Badner
 * @author Brett Meyer
 */
public abstract class AbstractConstraint implements Constraint {
	private TableSpecification table;
	private Identifier name;
	private final Map<Identifier, Column> columnMap = new LinkedHashMap<Identifier, Column>();
	private final Map<Column, String> columnOrderMap = new HashMap<Column, String>();

	protected AbstractConstraint(TableSpecification table, Identifier name) {
		this.table = table;
		this.name = name;
	}

	@Override
	public TableSpecification getTable() {
		return table;
	}
	
	public void setTable( TableSpecification table ) {
		this.table = table;
	}

	/**
	 * Returns the constraint name, or null if the name has not been set.
	 *
	 * @return the constraint name, or null if the name has not been set
	 */
	public Identifier getName() {
		return name;
	}

	/**
	 * Sets a constraint name that is unique across
	 * all database objects.
	 *
	 * @param name - the unique constraint name; must be non-null.
	 *
	 * @throws IllegalArgumentException if name is null.
	 * @throws IllegalStateException if this constraint already has a non-null name.
	 */
	public void setName(Identifier name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "name must be non-null." );
		}
		if ( this.name != null ) {
			throw new IllegalStateException(
					String.format(
							"This constraint already has a name (%s) and cannot be renamed to (%s).",
							this.name,
							name
					)
			);
		}
		this.name = name;
	}

	public int columnListId() {
		return table.columnListId( getColumns() );
	}

	public int columnListAlphabeticalId() {
		List<Column> alphabeticalColumns = new ArrayList<Column>( columnMap.values() );
		Collections.sort( alphabeticalColumns, alphabeticalColumnComparator );
		return table.columnListId( Collections.unmodifiableList( alphabeticalColumns ) );
	}

	public List<Column> getColumns() {
		return Collections.unmodifiableList( new ArrayList<Column>( columnMap.values() ) );
	}
	
	public String getColumnNames() {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (Column column : columnMap.values()) {
			sb.append( column.getColumnName().getText() );
			sb.append( sep );
			sep = ", ";
		}
		return sb.toString();
	}

	public int getColumnSpan() {
		return columnMap.size();
	}
	
	public boolean hasColumn(Column column) {
		return columnMap.containsKey( column.getColumnName() );
	}
	
	public boolean hasColumn(String columnName) {
		for ( Identifier key : columnMap.keySet() ) {
			if ( key.getText().equals( columnName ) ) return true;
		}
		return false;
	}

	protected Map<Identifier, Column> internalColumnAccess() {
		return columnMap;
	}

	public void addColumn(Column column) {
		internalAddColumn( column );
	}

	protected void internalAddColumn(Column column) {
//		if ( column.getTable() != getTable() ) {
//			throw new AssertionFailure(
//					String.format(
//							"Unable to add column to constraint; tables [%s, %s] did not match",
//							column.getTable().toLoggableString(),
//							getTable().toLoggableString()
//					)
//			);
//		}
		columnMap.put( column.getColumnName(), column );
	}

	public void addColumn(Column column, String order) {
		addColumn( column );
		if ( StringHelper.isNotEmpty( order ) ) {
			columnOrderMap.put( column, order );
		}
	}
	
	public boolean hasOrdering(Column column) {
		return columnOrderMap.containsKey( column );
	}
	
	public String getOrdering(Column column) {
		return columnOrderMap.get( column );
	}
	
	private final static AlphabeticalColumnComparator alphabeticalColumnComparator = new AlphabeticalColumnComparator();
	private static class AlphabeticalColumnComparator implements Comparator<Column> {
		@Override
		public int compare(Column c1, Column c2) {
			return c1.getColumnName().compareTo( c2.getColumnName() );
		}
	}
}
