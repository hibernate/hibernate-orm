// $Id: OrderByClause.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.ast.util.ASTUtil;

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
