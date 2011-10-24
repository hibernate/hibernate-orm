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
package org.hibernate.hql.internal.ast.util;
import antlr.ASTFactory;
import antlr.collections.AST;

import org.hibernate.hql.internal.NameGenerator;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.tree.HqlSqlWalkerNode;

/**
 * Provides utility methods for dealing with arrays of SQL column names.
 *
 * @author josh
 */
public final class ColumnHelper {

	/**
	 * @deprecated (tell clover to filter this out)
	 */
	private ColumnHelper() {
	}

	public static void generateSingleScalarColumn(HqlSqlWalkerNode node, int i) {
		ASTFactory factory = node.getASTFactory();
		ASTUtil.createSibling( factory, SqlTokenTypes.SELECT_COLUMNS, " as " + NameGenerator.scalarName( i, 0 ), node );
	}

	/**
	 * Generates the scalar column AST nodes for a given array of SQL columns
	 */
	public static void generateScalarColumns(HqlSqlWalkerNode node, String sqlColumns[], int i) {
		if ( sqlColumns.length == 1 ) {
			generateSingleScalarColumn( node, i );
		}
		else {
			ASTFactory factory = node.getASTFactory();
			AST n = node;
			n.setText( sqlColumns[0] );	// Use the DOT node to emit the first column name.
			// Create the column names, folled by the column aliases.
			for ( int j = 0; j < sqlColumns.length; j++ ) {
				if ( j > 0 ) {
					n = ASTUtil.createSibling( factory, SqlTokenTypes.SQL_TOKEN, sqlColumns[j], n );
				}
				n = ASTUtil.createSibling( factory, SqlTokenTypes.SELECT_COLUMNS, " as " + NameGenerator.scalarName( i, j ), n );
			}
		}
	}
}
