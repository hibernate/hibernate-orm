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
package org.hibernate.hql.ast.util;

import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.util.StringHelper;

import antlr.ASTFactory;
import antlr.collections.AST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utility methods for paths.
 *
 * @author josh
 */
public final class PathHelper {

	private static final Logger log = LoggerFactory.getLogger( PathHelper.class );

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
