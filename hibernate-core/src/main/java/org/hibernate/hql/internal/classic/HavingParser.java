/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;


/**
 * Parses the having clause of a hibernate query and translates it to an
 * SQL having clause.
 */
public class HavingParser extends WhereParser {

	void appendToken(QueryTranslatorImpl q, String token) {
		q.appendHavingToken( token );
	}

}
