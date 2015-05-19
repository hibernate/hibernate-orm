/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ASTUtil;

import antlr.collections.AST;

/**
 * Implementation of OrderByClause.
 *
 * @author Steve Ebersole
 */
public class OrderByClause extends HqlSqlWalkerNode implements HqlSqlTokenTypes {
	public void addOrderFragment(String orderByFragment) {
		AST fragment = ASTUtil.create( getASTFactory(), SQL_TOKEN, orderByFragment );
		if ( getFirstChild() == null ) {
			setFirstChild( fragment );
		}
		else {
			addChild( fragment );
		}
	}

}
