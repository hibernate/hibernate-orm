/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;
import org.hibernate.QueryException;

public class SelectPathExpressionParser extends PathExpressionParser {

	public void end(QueryTranslatorImpl q) throws QueryException {
		if ( getCurrentProperty() != null && !q.isShallowQuery() ) {
			// "finish off" the join
			token( ".", q );
			token( null, q );
		}
		super.end( q );
	}

	protected void setExpectingCollectionIndex() throws QueryException {
		throw new QueryException( "illegal syntax near collection-valued path expression in select: "  + getCollectionName() );
	}

	public String getSelectName() {
		return getCurrentName();
	}
}
