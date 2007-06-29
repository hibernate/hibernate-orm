//$Id: QuerySplitter.java 7646 2005-07-25 07:37:13Z oneovthafew $
package org.hibernate.hql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.classic.ParserHelper;
import org.hibernate.util.StringHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides query splitting methods, which were originally in QueryTranslator.
 * <br>
 * TODO: This will need to be refactored at some point.
 *
 * @author josh Mar 14, 2004 10:50:23 AM
 */
public final class QuerySplitter {

	private static final Log log = LogFactory.getLog( QuerySplitter.class );

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
		StringBuffer templateQuery = new StringBuffer( 40 );
		int count = 0;
		String last = null;
		int nextIndex = 0;
		String next = null;
		boolean isSelectClause = false;

		templateQuery.append( tokens[0] );
		if ( "select".equals( tokens[0].toLowerCase() ) ) isSelectClause = true;
        
		for ( int i = 1; i < tokens.length; i++ ) {

			//update last non-whitespace token, if necessary
			if ( !ParserHelper.isWhitespace( tokens[i - 1] ) ) last = tokens[i - 1].toLowerCase();

			// select-range is terminated by declaration of "from"
			if ( "from".equals( tokens[i].toLowerCase() ) ) isSelectClause = false;

			String token = tokens[i];
			if ( !ParserHelper.isWhitespace( token ) || last == null ) {

				//scan for next non-whitespace token
				if ( nextIndex <= i ) {
					for ( nextIndex = i + 1; nextIndex < tokens.length; nextIndex++ ) {
						next = tokens[nextIndex].toLowerCase();
						if ( !ParserHelper.isWhitespace( next ) ) break;
					}
				}

				boolean process = !isSelectClause && 
						isJavaIdentifier( token ) && 
						isPossiblyClassName( last, next );
						
				if (process) {
					String importedClassName = getImportedClass( token, factory );
					if ( importedClassName != null ) {
						String[] implementors = factory.getImplementors( importedClassName );
						String placeholder = "$clazz" + count++ + "$";
						if ( implementors != null ) {
							placeholders.add( placeholder );
							replacements.add( implementors );
						}
						token = placeholder; // Note this!!
					}
				}

			}

			templateQuery.append( token );

		}
		String[] results = StringHelper.multiply( templateQuery.toString(), placeholders.iterator(), replacements.iterator() );
		if ( results.length == 0 ) log.warn( "no persistent classes found for query class: " + query );
		return results;
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
