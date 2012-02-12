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
package org.hibernate.hql.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.classic.ParserHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;

/**
 * Provides query splitting methods, which were originally in QueryTranslator.
 * <br>
 * TODO: This will need to be refactored at some point.
 *
 * @author josh
 */
public final class QuerySplitter {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, QuerySplitter.class.getName());

	private static final Set BEFORE_CLASS_TOKENS = new HashSet();
	private static final Set NOT_AFTER_CLASS_TOKENS = new HashSet();

	static {
		BEFORE_CLASS_TOKENS.add( "from" );
		BEFORE_CLASS_TOKENS.add( "delete" );
		BEFORE_CLASS_TOKENS.add( "update" );
		//beforeClassTokens.add("new"); DEFINITELY DON'T HAVE THIS!!
		BEFORE_CLASS_TOKENS.add( "," );
		NOT_AFTER_CLASS_TOKENS.add( "in" );
		//notAfterClassTokens.add(",");
		NOT_AFTER_CLASS_TOKENS.add( "from" );
		NOT_AFTER_CLASS_TOKENS.add( ")" );
	}

	/**
	 * Private empty constructor.
	 * (or else checkstyle says: 'warning: Utility classes should not have a public or default constructor.')
	 */
	private QuerySplitter() {
	}

	/**
	 * Handle Hibernate "implicit" polymorphism, by translating the query string into
	 * several "concrete" queries against mapped classes.
	 */
	public static String[] concreteQueries(String query, SessionFactoryImplementor factory) throws MappingException {

		//scan the query string for class names appearing in the from clause and replace
		//with all persistent implementors of the class/interface, returning multiple
		//query strings (make sure we don't pick up a class in the select clause!)

		//TODO: this is one of the ugliest and most fragile pieces of code in Hibernate....

		String[] tokens = StringHelper.split( StringHelper.WHITESPACE + "(),", query, true );
		if ( tokens.length == 0 ) return new String[]{query}; // just especially for the trivial collection filter
		ArrayList placeholders = new ArrayList();
		ArrayList replacements = new ArrayList();
		StringBuilder templateQuery = new StringBuilder( 40 );

		int start = getStartingPositionFor(tokens, templateQuery);
		int count = 0;
		String next = null;
		String last = tokens[start - 1].toLowerCase();

		for ( int i = start; i < tokens.length; i++ ) {

			String token = tokens[i];

			if ( ParserHelper.isWhitespace( token ) ) {
				templateQuery.append( token );
				continue;
			}

			next = nextNonWhite(tokens, i).toLowerCase();

			boolean process = isJavaIdentifier( token ) &&
					isPossiblyClassName( last, next );

			last = token.toLowerCase();

			if (process) {
				String importedClassName = getImportedClass( token, factory );
				if ( importedClassName != null ) {
					String[] implementors = factory.getImplementors( importedClassName );
					token = "$clazz" + count++ + "$";
					if ( implementors != null ) {
						placeholders.add( token );
						replacements.add( implementors );
					}
				}
			}

			templateQuery.append( token );

		}
		String[] results = StringHelper.multiply( templateQuery.toString(), placeholders.iterator(), replacements.iterator() );
		if ( results.length == 0 ) {
			LOG.noPersistentClassesFound( query );
		}
		return results;
	}
	
	private static String nextNonWhite(String[] tokens, int start) {
		for ( int i = start + 1; i < tokens.length; i++ ) {
			if ( !ParserHelper.isWhitespace( tokens[i] ) ) return tokens[i];
		}
		return tokens[tokens.length - 1];
	}
	
	private static int getStartingPositionFor(String[] tokens, StringBuilder templateQuery) {
		templateQuery.append( tokens[0] );
		if ( !"select".equals( tokens[0].toLowerCase() ) ) return 1;

		// select-range is terminated by declaration of "from"
		for (int i = 1; i < tokens.length; i++ ) {
			if ( "from".equals( tokens[i].toLowerCase() ) ) return i;
			templateQuery.append( tokens[i] );
		}
		return tokens.length;
	}

	private static boolean isPossiblyClassName(String last, String next) {
		return "class".equals( last ) || (
				BEFORE_CLASS_TOKENS.contains( last ) &&
				!NOT_AFTER_CLASS_TOKENS.contains( next )
			);
	}

	private static boolean isJavaIdentifier(String token) {
		return Character.isJavaIdentifierStart( token.charAt( 0 ) );
	}

	public static String getImportedClass(String name, SessionFactoryImplementor factory) {
		return factory.getImportedClassName( name );
	}
}
