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
import org.hibernate.internal.util.StringHelper;

/**
 * Models a SQL <tt>INDEX</tt>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Index extends AbstractConstraint implements Constraint {
	protected Index(Table table, String name) {
		super( table, name );
	}


	@Override
	public String getExportIdentifier() {
		StringBuilder sb = new StringBuilder( getTable().getLoggableValueQualifier());
		sb.append( ".IDX" );
		for ( Column column : getColumns() ) {
			sb.append( '_' ).append( column.getColumnName().getName() );
		}
		return sb.toString();
	}

	public String[] sqlCreateStrings(Dialect dialect) {
		return new String[] {
				buildSqlCreateIndexString(
						dialect, getName(), getTable(), getColumns(), false
				)
		};
	}

	public static String buildSqlCreateIndexString(
			Dialect dialect,
			String name,
			TableSpecification table,
			Iterable<Column> columns,
			boolean unique
	) {
		//TODO handle supportsNotNullUnique=false, but such a case does not exist in the wild so far
		StringBuilder buf = new StringBuilder( "create" )
				.append( unique ?
						" unique" :
						"" )
				.append( " index " )
				.append( dialect.qualifyIndexName() ?
						name :
						StringHelper.unqualify( name ) )
				.append( " on " )
				.append( table.getQualifiedName( dialect ) )
				.append( " (" );
		boolean first = true;
		for ( Column column : columns ) {
			if ( first ) {
				first = false;
			}
			else {
				buf.append( ", " );
			}
			buf.append( ( column.getColumnName().encloseInQuotesIfQuoted( dialect ) ) );
		}
		buf.append( ")" );
		return buf.toString();
	}

	public String sqlConstraintStringInAlterTable(Dialect dialect) {
		StringBuilder buf = new StringBuilder( " index (" );
		boolean first = true;
		for ( Column column : getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				buf.append( ", " );
			}
			buf.append( column.getColumnName().encloseInQuotesIfQuoted( dialect ) );
		}
		return buf.append( ')' ).toString();
	}

	public String[] sqlDropStrings(Dialect dialect) {
		return new String[] {
				new StringBuilder( "drop index " )
				.append(
						StringHelper.qualify(
								getTable().getQualifiedName( dialect ),
								getName()
						)
				).toString()
		};
	}
}
