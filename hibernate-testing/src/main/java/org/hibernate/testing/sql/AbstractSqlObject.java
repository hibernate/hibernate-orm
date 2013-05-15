/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.sql;

import java.util.Collection;
import java.util.Iterator;

/**
 *
 */
public abstract class AbstractSqlObject implements SqlObject {

	protected static void collectionToString( StringBuilder builder, Collection< ? > collection ) {
		if ( collection.isEmpty() ) {
			return;
		}
		Iterator< ? > iter = collection.iterator();
		builder.append( iter.next() );
		while ( iter.hasNext() ) {
			builder.append( ", " ).append( iter.next() );
		}
	}

	protected static void collectionToStringInParentheses( StringBuilder builder, Collection< ? > collection ) {
		builder.append( '(' );
		if ( !collection.isEmpty() ) {
			builder.append( ' ' );
		}
		collectionToString( builder, collection );
		if ( !collection.isEmpty() ) {
			builder.append( ' ' );
		}
		builder.append( ')' );
	}

	private SqlObject parent;

	AbstractSqlObject( SqlObject parent ) {
		this.parent = parent;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.SqlObject#namespaceScope()
	 */
	@Override
	public final LocalScope localScope() {
		return this instanceof LocalScope ? ( LocalScope ) this : parent.localScope();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.SqlObject#localObjectInScope(java.lang.String, boolean)
	 */
	@Override
	public SqlObject localObjectInScope( String name, boolean ignoreColumns ) {
		LocalScope scope = localScope();
		SqlObject obj = scope.localObject( name, ignoreColumns );
		return obj == null && scope.parent() != null ? scope.parent().localObjectInScope( name, ignoreColumns ) : obj;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.SqlObject#parent()
	 */
	@Override
	public final SqlObject parent() {
		return parent;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.SqlObject#setParent(org.hibernate.testing.sql.SqlObject)
	 */
	@Override
	public final void setParent( SqlObject parent ) {
		this.parent = parent;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.SqlObject#statement()
	 */
	@Override
	public final Statement statement() {
		return this instanceof Statement ? ( Statement ) this : parent.statement();
	}
}
