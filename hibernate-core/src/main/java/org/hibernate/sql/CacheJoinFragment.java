/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;
import org.hibernate.AssertionFailure;

/**
 * A Cach&eacute; dialect join.  Differs from ANSI only in that full outer join
 * is not supported.
 *
 * @author Jeff Miller
 * @author Jonathan Levinson
 */
public class CacheJoinFragment extends ANSIJoinFragment {

	public void addJoin(String rhsTableName, String rhsAlias, String[] lhsColumns, String[] rhsColumns, JoinType joinType, String on) {
		if ( joinType == JoinType.FULL_JOIN ) {
			throw new AssertionFailure( "Cache does not support full outer joins" );
		}
		super.addJoin( rhsTableName, rhsAlias, lhsColumns, rhsColumns, joinType, on );
	}

}
