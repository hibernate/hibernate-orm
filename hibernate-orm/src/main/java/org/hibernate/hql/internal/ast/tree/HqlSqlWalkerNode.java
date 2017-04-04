/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.util.AliasGenerator;
import org.hibernate.hql.internal.ast.util.SessionFactoryHelper;

import antlr.ASTFactory;

/**
 * A semantic analysis node, that points back to the main analyzer.
 *
 * @author josh
 */
public class HqlSqlWalkerNode extends SqlNode implements InitializeableNode {
	/**
	 * A pointer back to the phase 2 processor.
	 */
	private HqlSqlWalker walker;

	public void initialize(Object param) {
		walker = (HqlSqlWalker) param;
	}

	public HqlSqlWalker getWalker() {
		return walker;
	}

	public SessionFactoryHelper getSessionFactoryHelper() {
		return walker.getSessionFactoryHelper();
	}

	public ASTFactory getASTFactory() {
		return walker.getASTFactory();
	}

	public AliasGenerator getAliasGenerator() {
		return walker.getAliasGenerator();
	}
}
