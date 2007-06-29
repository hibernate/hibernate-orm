//$Id$
package org.hibernate.hql.classic;


/**
 * Parses the having clause of a hibernate query and translates it to an
 * SQL having clause.
 */
public class HavingParser extends WhereParser {

	void appendToken(QueryTranslatorImpl q, String token) {
		q.appendHavingToken( token );
	}

}
