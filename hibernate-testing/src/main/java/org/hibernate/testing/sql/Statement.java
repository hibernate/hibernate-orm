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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class Statement extends AbstractSqlObject implements LocalScope {

	private Map< String, Set< SqlObject > > localObjectsByName = new HashMap< String, Set< SqlObject > >();

	public Statement( SqlObject parent ) {
		super( parent );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.LocalScope#localObject(java.lang.String)
	 */
	@Override
	public SqlObject localObject( String unquotedName, boolean ignoreColumns ) {
		Set< SqlObject > set = localObjectsByName.get( unquotedName.toLowerCase() );
		if ( set == null ) {
			return null;
		}
		for ( SqlObject obj : set ) {
			if ( !ignoreColumns
					|| ( !( obj instanceof Column ) && ( !( obj instanceof Alias ) || !( ( ( Alias ) obj ).reference.referent instanceof Column ) ) ) ) {
				return obj;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.LocalScope#mapLocalObjectByName(java.lang.String, org.hibernate.testing.sql.SqlObject)
	 */
	@Override
	public void mapLocalObjectByName( String unquotedName, SqlObject object ) {
		unquotedName = unquotedName.toLowerCase();
		Set< SqlObject > set = localObjectsByName.get( unquotedName );
		if ( set == null ) {
			set = new HashSet< SqlObject >();
			localObjectsByName.put( unquotedName, set );
		}
		if ( !set.add( object ) ) {
			throw new IllegalStateException( "A SQL object already exists named " + unquotedName + " within " + this );
		}
	}
}
