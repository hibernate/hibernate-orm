/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.sql.JoinFragment;

/**
 * Provides the SQL for collection subqueries.
 * <br>
 * Moved here from PathExpressionParser to make it re-useable.
 *
 * @author josh
 */
public final class CollectionSubqueryFactory {
	private CollectionSubqueryFactory() {
	}

	public static String createCollectionSubquery(
			JoinSequence joinSequence,
			Map enabledFilters,
			String[] columns) {
		try {
			JoinFragment join = joinSequence.toJoinFragment( enabledFilters, true );
			return "select " + String.join( ", ", columns )
					+ " from " + join.toFromFragmentString().substring( 2 )
					+ " where " + join.toWhereFragmentString().substring( 5 );
		}
		catch (MappingException me) {
			throw new QueryException( me );
		}
	}
}
