/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.util.StringHelper;

/**
 * A relational unique key constraint
 *
 * @author Gavin King
 */
public class UniqueKey extends Constraint {

	public String sqlConstraintString(Dialect dialect) {
		StringBuffer buf = new StringBuffer( "unique (" );
		boolean hadNullableColumn = false;
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			Column column = (Column) iter.next();
			if ( !hadNullableColumn && column.isNullable() ) {
				hadNullableColumn = true;
			}
			buf.append( column.getQuotedName( dialect ) );
			if ( iter.hasNext() ) {
				buf.append( ", " );
			}
		}
		//do not add unique constraint on DB not supporting unique and nullable columns
		return !hadNullableColumn || dialect.supportsNotNullUnique() ?
				buf.append( ')' ).toString() :
				null;
	}

	public String sqlConstraintString(
			Dialect dialect,
			String constraintName,
			String defaultCatalog,
			String defaultSchema) {
		StringBuffer buf = new StringBuffer(
				dialect.getAddPrimaryKeyConstraintString( constraintName )
		).append( '(' );
		Iterator iter = getColumnIterator();
		boolean nullable = false;
		while ( iter.hasNext() ) {
			Column column = (Column) iter.next();
			if ( !nullable && column.isNullable() ) nullable = true;
			buf.append( column.getQuotedName( dialect ) );
			if ( iter.hasNext() ) buf.append( ", " );
		}
		return !nullable || dialect.supportsNotNullUnique() ?
				StringHelper.replace( buf.append( ')' ).toString(), "primary key", "unique" ) :
				//TODO: improve this hack!
				null;
	}

	public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) {
		if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
			return super.sqlCreateString( dialect, p, defaultCatalog, defaultSchema );
		}
		else {
			return Index.buildSqlCreateIndexString( dialect, getName(), getTable(), getColumnIterator(), true,
					defaultCatalog, defaultSchema );
		}
	}

	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
			return super.sqlDropString( dialect, defaultCatalog, defaultSchema );
		}
		else {
			return Index.buildSqlDropIndexString( dialect, getTable(), getName(), defaultCatalog, defaultSchema );
		}
	}

	public boolean isGenerated(Dialect dialect) {
		if ( dialect.supportsNotNullUnique() ) return true;
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			if ( ( (Column) iter.next() ).isNullable() ) {
				return false;
			}
		}
		return true;
	}

}
