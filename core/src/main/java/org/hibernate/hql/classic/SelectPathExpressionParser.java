//$Id: SelectPathExpressionParser.java 5861 2005-02-22 14:07:36Z oneovthafew $
package org.hibernate.hql.classic;

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







