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
package org.hibernate.hql.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.JoinFragment;

/**
 * Provides the SQL for collection subqueries.
 * <br>
 * Moved here from PathExpressionParser to make it re-useable.
 *
 * @author josh
 */
public final class CollectionSubqueryFactory {

	//TODO: refactor to .sql package

	private CollectionSubqueryFactory() {
	}

	public static String createCollectionSubquery(
			JoinSequence joinSequence,
	        Map enabledFilters,
	        String[] columns) {
		try {
			JoinFragment join = joinSequence.toJoinFragment( enabledFilters, true );
			return new StringBuilder( "select " )
					.append( StringHelper.join( ", ", columns ) )
					.append( " from " )
					.append( join.toFromFragmentString().substring( 2 ) )// remove initial ", "
					.append( " where " )
					.append( join.toWhereFragmentString().substring( 5 ) )// remove initial " and "
					.toString();
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
	}
}
