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
import java.util.Iterator;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;

/**
 * A relational constraint.
 *
 * @author Gavin King
 */
public abstract class Constraint implements RelationalModel, Serializable {

	private String name;
	private final List columns = new ArrayList();
	private Table table;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Iterator getColumnIterator() {
		return columns.iterator();
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
		return (Column) columns.get( i );
	}

	public Iterator columnIterator() {
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
}
