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
package org.hibernate.metamodel.relational;

import org.hibernate.dialect.Dialect;

/**
 * Models a SQL <tt>INDEX</tt> defined as UNIQUE
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class UniqueKey extends AbstractConstraint implements Constraint {
	protected UniqueKey(Table table, String name) {
		super( table, name );
	}

	@Override
	public String getExportIdentifier() {
		StringBuilder sb = new StringBuilder( getTable().getLoggableValueQualifier() );
		sb.append( ".UK" );
		for ( Column column : getColumns() ) {
			sb.append( '_' ).append( column.getColumnName().getName() );
		}
		return sb.toString();
	}

	@Override
    public boolean isCreationVetoed(Dialect dialect) {
		if ( dialect.supportsNotNullUnique() ) {
			return false;
		}

		for ( Column column : getColumns() ) {
			if ( column.isNullable() ) {
				return true;
			}
		}
		return false;
	}

	public String sqlConstraintStringInCreateTable(Dialect dialect) {
		StringBuilder buf = new StringBuilder( "unique (" );
		boolean hadNullableColumn = false;
		boolean first = true;
		for ( Column column : getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				buf.append( ", " );
			}
			if ( !hadNullableColumn && column.isNullable() ) {
				hadNullableColumn = true;
			}
			buf.append( column.getColumnName().encloseInQuotesIfQuoted( dialect ) );
		}
		//do not add unique constraint on DB not supporting unique and nullable columns
		return !hadNullableColumn || dialect.supportsNotNullUnique() ?
				buf.append( ')' ).toString() :
				null;
	}

	@Override
    public String sqlConstraintStringInAlterTable(Dialect dialect) {
		StringBuilder buf = new StringBuilder(
				dialect.getAddUniqueConstraintString( getName() )
		).append( '(' );
		boolean nullable = false;
		boolean first = true;
		for ( Column column : getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				buf.append( ", " );
			}
			if ( !nullable && column.isNullable() ) {
				nullable = true;
			}
			buf.append( column.getColumnName().encloseInQuotesIfQuoted( dialect ) );
		}
		return !nullable || dialect.supportsNotNullUnique() ? buf.append( ')' ).toString() : null;
	}
}
