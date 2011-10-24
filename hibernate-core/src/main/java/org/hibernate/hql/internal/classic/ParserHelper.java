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
package org.hibernate.hql.internal.classic;
import java.util.StringTokenizer;

import org.hibernate.QueryException;
import org.hibernate.internal.util.StringHelper;

public final class ParserHelper {

	public static final String HQL_VARIABLE_PREFIX = ":";

	public static final String HQL_SEPARATORS = " \n\r\f\t,()=<>&|+-=/*'^![]#~\\";
	//NOTICE: no " or . since they are part of (compound) identifiers
	public static final String PATH_SEPARATORS = ".";

	public static boolean isWhitespace(String str) {
		return StringHelper.WHITESPACE.indexOf( str ) > -1;
	}

	private ParserHelper() {
		//cannot instantiate
	}

	public static void parse(Parser p, String text, String seperators, QueryTranslatorImpl q) throws QueryException {
		StringTokenizer tokens = new StringTokenizer( text, seperators, true );
		p.start( q );
		while ( tokens.hasMoreElements() ) p.token( tokens.nextToken(), q );
		p.end( q );
	}

}






