/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.QueryException;
import org.hibernate.hql.internal.CollectionProperties;
import org.hibernate.internal.util.StringHelper;

/**
 *
 */
public class PreprocessingParser implements Parser {

	private static final Set<String> HQL_OPERATORS;

	static {
		HQL_OPERATORS = new HashSet<String>();
		HQL_OPERATORS.add( "<=" );
		HQL_OPERATORS.add( ">=" );
		HQL_OPERATORS.add( "=>" );
		HQL_OPERATORS.add( "=<" );
		HQL_OPERATORS.add( "!=" );
		HQL_OPERATORS.add( "<>" );
		HQL_OPERATORS.add( "!#" );
		HQL_OPERATORS.add( "!~" );
		HQL_OPERATORS.add( "!<" );
		HQL_OPERATORS.add( "!>" );
		HQL_OPERATORS.add( "is not" );
		HQL_OPERATORS.add( "not like" );
		HQL_OPERATORS.add( "not in" );
		HQL_OPERATORS.add( "not between" );
		HQL_OPERATORS.add( "not exists" );
	}

	private Map replacements;
	private boolean quoted;
	private StringBuilder quotedString;
	private ClauseParser parser = new ClauseParser();
	private String lastToken;
	private String currentCollectionProp;

	public PreprocessingParser(Map replacements) {
		this.replacements = replacements;
	}

	public void token(String token, QueryTranslatorImpl q) throws QueryException {

		//handle quoted strings
		if ( quoted ) {
			quotedString.append( token );
		}
		if ( "'".equals( token ) ) {
			if ( quoted ) {
				token = quotedString.toString();
			}
			else {
				quotedString = new StringBuilder( 20 ).append( token );
			}
			quoted = !quoted;
		}
		if ( quoted ) {
			return;
		}

		//ignore whitespace
		if ( ParserHelper.isWhitespace( token ) ) {
			return;
		}

		//do replacements
		String substoken = ( String ) replacements.get( token );
		token = ( substoken == null ) ? token : substoken;

		//handle HQL2 collection syntax
		if ( currentCollectionProp != null ) {
			if ( "(".equals( token ) ) {
				return;
			}
			else if ( ")".equals( token ) ) {
				currentCollectionProp = null;
				return;
			}
			else {
				token = StringHelper.qualify( token, currentCollectionProp );
			}
		}
		else {
			String prop = CollectionProperties.getNormalizedPropertyName( token.toLowerCase(Locale.ROOT) );
			if ( prop != null ) {
				currentCollectionProp = prop;
				return;
			}
		}


		//handle <=, >=, !=, is not, not between, not in
		if ( lastToken == null ) {
			lastToken = token;
		}
		else {
			String doubleToken = ( token.length() > 1 ) ?
					lastToken + ' ' + token :
					lastToken + token;
			if ( HQL_OPERATORS.contains( doubleToken.toLowerCase(Locale.ROOT) ) ) {
				parser.token( doubleToken, q );
				lastToken = null;
			}
			else {
				parser.token( lastToken, q );
				lastToken = token;
			}
		}

	}

	public void start(QueryTranslatorImpl q) throws QueryException {
		quoted = false;
		parser.start( q );
	}

	public void end(QueryTranslatorImpl q) throws QueryException {
		if ( lastToken != null ) {
			parser.token( lastToken, q );
		}
		parser.end( q );
		lastToken = null;
		currentCollectionProp = null;
	}

}

