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
