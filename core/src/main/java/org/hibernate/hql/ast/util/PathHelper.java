// $Id: PathHelper.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.util;

import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.util.StringHelper;

import antlr.ASTFactory;
import antlr.collections.AST;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides utility methods for paths.
 *
 * @author josh Sep 14, 2004 8:16:29 AM
 */
public final class PathHelper {

	private static final Log log = LogFactory.getLog( PathHelper.class );

	private PathHelper() {
	}

	/**
	 * Turns a path into an AST.
	 *
	 * @param path    The path.
	 * @param factory The AST factory to use.
	 * @return An HQL AST representing the path.
	 */
	public static AST parsePath(String path, ASTFactory factory) {
		String[] identifiers = StringHelper.split( ".", path );
		AST lhs = null;
		for ( int i = 0; i < identifiers.length; i++ ) {
			String identifier = identifiers[i];
			AST child = ASTUtil.create( factory, HqlSqlTokenTypes.IDENT, identifier );
			if ( i == 0 ) {
				lhs = child;
			}
			else {
				lhs = ASTUtil.createBinarySubtree( factory, HqlSqlTokenTypes.DOT, ".", lhs, child );
			}
		}
		if ( log.isDebugEnabled() ) {
			log.debug( "parsePath() : " + path + " -> " + ASTUtil.getDebugString( lhs ) );
		}
		return lhs;
	}

	public static String getAlias(String path) {
		return StringHelper.root( path );
	}
}
