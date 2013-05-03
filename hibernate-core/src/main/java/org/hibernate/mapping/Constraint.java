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
package org.hibernate.mapping;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.StringHelper;

/**
 * A relational constraint.
 *
 * @author Gavin King
 * @author Brett Meyer
 */
public abstract class Constraint implements RelationalModel, Serializable {

	private String name;
	private final ArrayList<Column> columns = new ArrayList<Column>();
	private Table table;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * If a constraint is not explicitly named, this is called to generate
	 * a unique hash using the table and column names.
	 * Static so the name can be generated prior to creating the Constraint.
	 * They're cached, keyed by name, in multiple locations.
	 */
	public static String generateName(String prefix, Table table, Column... columns) {
		// Use a concatenation that guarantees uniqueness, even if identical names
		// exist between all table and column identifiers.

		StringBuilder sb = new StringBuilder( "table`" + table.getName() + "`" );

		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		Column[] alphabeticalColumns = columns.clone();
		Arrays.sort( alphabeticalColumns, new ColumnComparator() );
		for ( Column column : alphabeticalColumns ) {
			String columnName = column == null ? "" : column.getName();
			sb.append( "column`" + columnName + "`" );
		}
		return prefix + StringHelper.md5HashBase35( sb.toString() );
	}

	/**
	 * Helper method for the few occasions where the UK isn't cached.
	 */
	public void generateName() {
		name = generateName( generatedConstraintNamePrefix(), table, columns.toArray( new Column[columns.size()] ) );
	}
	
	private static class ColumnComparator implements Comparator<Column> {
		public int compare(Column col1, Column col2) {
			return col1.getName().compareTo( col2.getName() );
		}
	} 

	public void addColumn(Column column) {
		if ( !columns.contains( column ) ) columns.add( column );
	}

	public void addColumns(Iterator columnIterator) {
		while ( columnIterator.hasNext() ) {
			Selectable col = (Selectable) columnIterator.next();
			if ( !col.isFormula() ) addColumn( (Column) col );
		}
	}

	/**
	 * @param column
	 * @return true if this constraint already contains a column with same name.
	 */
	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public Column getColumn(int i) {
		return  columns.get( i );
	}
	//todo duplicated method, remove one
	public Iterator<Column> getColumnIterator() {
		return columns.iterator();
	}

	public Iterator<Column> columnIterator() {
		return columns.iterator();
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public boolean isGenerated(Dialect dialect) {
		return true;
	}

	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		if ( isGenerated( dialect ) ) {
			return new StringBuilder()
					.append( "alter table " )
					.append( getTable().getQualifiedName( dialect, defaultCatalog, defaultSchema ) )
					.append( " drop constraint " )
					.append( dialect.quote( getName() ) )
					.toString();
		}
		else {
			return null;
		}
	}

	public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) {
		if ( isGenerated( dialect ) ) {
			String constraintString = sqlConstraintString( dialect, getName(), defaultCatalog, defaultSchema );
			StringBuilder buf = new StringBuilder( "alter table " )
					.append( getTable().getQualifiedName( dialect, defaultCatalog, defaultSchema ) )
					.append( constraintString );
			return buf.toString();
		}
		else {
			return null;
		}
	}

	public List getColumns() {
		return columns;
	}

	public abstract String sqlConstraintString(Dialect d, String constraintName, String defaultCatalog,
											   String defaultSchema);

	public String toString() {
		return getClass().getName() + '(' + getTable().getName() + getColumns() + ") as " + name;
	}
	
	public abstract String generatedConstraintNamePrefix();
}
