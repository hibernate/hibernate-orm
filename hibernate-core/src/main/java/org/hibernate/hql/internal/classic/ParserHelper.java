/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;

import java.util.BitSet;
import java.util.StringTokenizer;

import org.hibernate.QueryException;
import org.hibernate.internal.util.StringHelper;

public final class ParserHelper {

	public static final String HQL_VARIABLE_PREFIX = ":";

	public static final String HQL_SEPARATORS = " \n\r\f\t,()=<>&|+-=/*'^![]#~\\";
	public static final BitSet HQL_SEPARATORS_BITSET = new BitSet();

	static {
		for ( int i = 0; i < HQL_SEPARATORS.length(); i++ ) {
			HQL_SEPARATORS_BITSET.set( HQL_SEPARATORS.charAt( i ) );
		}
	}

	//NOTICE: no " or . since they are part of (compound) identifiers
	public static final String PATH_SEPARATORS = ".";

	public static boolean isWhitespace(String str) {
		return StringHelper.WHITESPACE.contains( str );
	}

	private ParserHelper() {
		//cannot instantiate
	}

	public static void parse(Parser p, String text, String seperators, QueryTranslatorImpl q) throws QueryException {
		StringTokenizer tokens = new StringTokenizer( text, seperators, true );
		p.start( q );
		while ( tokens.hasMoreElements() ) {
			p.token( tokens.nextToken(), q );
		}
		p.end( q );
	}
}
