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
package org.hibernate.sql;

import org.hibernate.dialect.Dialect;
import org.hibernate.HibernateException;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Implementation of InsertSelect.
 *
 * @author Steve Ebersole
 */
public class InsertSelect {

	private Dialect dialect;
	private String tableName;
	private String comment;
	private List columnNames = new ArrayList();
	private Select select;

	public InsertSelect(Dialect dialect) {
		this.dialect = dialect;
	}

	public InsertSelect setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public InsertSelect setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public InsertSelect addColumn(String columnName) {
		columnNames.add( columnName );
		return this;
	}

	public InsertSelect addColumns(String[] columnNames) {
		for ( int i = 0; i < columnNames.length; i++ ) {
			this.columnNames.add( columnNames[i] );
		}
		return this;
	}

	public InsertSelect setSelect(Select select) {
		this.select = select;
		return this;
	}

	public String toStatementString() {
		if ( tableName == null ) throw new HibernateException( "no table name defined for insert-select" );
		if ( select == null ) throw new HibernateException( "no select defined for insert-select" );

		StringBuffer buf = new StringBuffer( (columnNames.size() * 15) + tableName.length() + 10 );
		if ( comment!=null ) {
			buf.append( "/* " ).append( comment ).append( " */ " );
		}
		buf.append( "insert into " ).append( tableName );
		if ( !columnNames.isEmpty() ) {
			buf.append( " (" );
			Iterator itr = columnNames.iterator();
			while ( itr.hasNext() ) {
				buf.append( itr.next() );
				if ( itr.hasNext() ) {
					buf.append( ", " );
				}
			}
			buf.append( ")" );
		}
		buf.append( ' ' ).append( select.toStatementString() );
		return buf.toString();
	}
}
