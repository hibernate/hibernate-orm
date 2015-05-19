/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.classic.ParserHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;

/**
 * Provides query splitting methods, which were originally in QueryTranslator.
 *
 * @author josh
 */
public final class QuerySplitter {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QuerySplitter.class );

	private static final Set<String> BEFORE_CLASS_TOKENS = new HashSet<String>();
	private static final Set<String> NOT_AFTER_CLASS_TOKENS = new HashSet<String>();

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
		if ( tokens.length == 0 ) {
			// just especially for the trivial collection filter
			return new String[] { query };
		}
		ArrayList<String> placeholders = new ArrayList<String>();
		ArrayList<String[]> replacements = new ArrayList<String[]>();
		StringBuilder templateQuery = new StringBuilder( 40 );

		int start = getStartingPositionFor( tokens, templateQuery );
		int count = 0;
		String next;
		String last = tokens[start - 1].toLowerCase(Locale.ROOT);

		for ( int i = start; i < tokens.length; i++ ) {

			String token = tokens[i];

			if ( ParserHelper.isWhitespace( token ) ) {
				templateQuery.append( token );
				continue;
			}

			next = nextNonWhite( tokens, i ).toLowerCase(Locale.ROOT);

			boolean process = isJavaIdentifier( token ) &&
					isPossiblyClassName( last, next );

			last = token.toLowerCase(Locale.ROOT);

			if ( process ) {
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
		String[] results = StringHelper.multiply(
				templateQuery.toString(),
				placeholders.iterator(),
				replacements.iterator()
		);
		if ( results.length == 0 ) {
			LOG.noPersistentClassesFound( query );
		}
		return results;
	}

	private static String nextNonWhite(String[] tokens, int start) {
		for ( int i = start + 1; i < tokens.length; i++ ) {
			if ( !ParserHelper.isWhitespace( tokens[i] ) ) {
				return tokens[i];
			}
		}
		return tokens[tokens.length - 1];
	}

	private static int getStartingPositionFor(String[] tokens, StringBuilder templateQuery) {
		templateQuery.append( tokens[0] );
		if ( !"select".equals( tokens[0].toLowerCase(Locale.ROOT) ) ) {
			return 1;
		}

		// select-range is terminated by declaration of "from"
		for ( int i = 1; i < tokens.length; i++ ) {
			if ( "from".equals( tokens[i].toLowerCase(Locale.ROOT) ) ) {
				return i;
			}
			templateQuery.append( tokens[i] );
		}
		return tokens.length;
	}

	private static boolean isPossiblyClassName(String last, String next) {
		return "class".equals( last )
				|| ( BEFORE_CLASS_TOKENS.contains( last ) && !NOT_AFTER_CLASS_TOKENS.contains( next ) );
	}

	private static boolean isJavaIdentifier(String token) {
		return Character.isJavaIdentifierStart( token.charAt( 0 ) );
	}

	public static String getImportedClass(String name, SessionFactoryImplementor factory) {
		return factory.getImportedClassName( name );
	}
}
