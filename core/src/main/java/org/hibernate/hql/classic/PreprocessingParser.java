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
package org.hibernate.hql.classic;

import org.hibernate.QueryException;
import org.hibernate.hql.CollectionProperties;
import org.hibernate.util.StringHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PreprocessingParser implements Parser {

	private static final Set HQL_OPERATORS;

	static {
		HQL_OPERATORS = new HashSet();
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
	private StringBuffer quotedString;
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
				quotedString = new StringBuffer( 20 ).append( token );
			}
			quoted = !quoted;
		}
		if ( quoted ) return;

		//ignore whitespace
		if ( ParserHelper.isWhitespace( token ) ) return;

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
			String prop = CollectionProperties.getNormalizedPropertyName( token.toLowerCase() );
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
			if ( HQL_OPERATORS.contains( doubleToken.toLowerCase() ) ) {
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
		if ( lastToken != null ) parser.token( lastToken, q );
		parser.end( q );
		lastToken = null;
		currentCollectionProp = null;
	}

}






