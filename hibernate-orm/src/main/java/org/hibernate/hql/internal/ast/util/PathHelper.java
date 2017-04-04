/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.util;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

import antlr.ASTFactory;
import antlr.collections.AST;

/**
 * Provides utility methods for paths.
 *
 * @author josh
 */
public final class PathHelper {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, PathHelper.class.getName() );

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
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "parsePath() : %s -> %s", path, ASTUtil.getDebugString( lhs ) );
		}
		return lhs;
	}

	public static String getAlias(String path) {
		return StringHelper.root( path );
	}
}
