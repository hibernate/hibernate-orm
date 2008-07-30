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
package org.hibernate.hql.ast.tree;

import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.antlr.SqlTokenTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a top-level AST node representing an HQL delete statement.
 *
 * @author Steve Ebersole
 */
public class DeleteStatement extends AbstractRestrictableStatement {

	private static final Logger log = LoggerFactory.getLogger( DeleteStatement.class );

	/**
	 * @see org.hibernate.hql.ast.tree.Statement#getStatementType()
	 */
	public int getStatementType() {
		return HqlSqlTokenTypes.DELETE;
	}

	/**
	 * @see org.hibernate.hql.ast.tree.Statement#needsExecutor()
	 */
	public boolean needsExecutor() {
		return true;
	}

	protected int getWhereClauseParentTokenType() {
		return SqlTokenTypes.FROM;
	}

	protected Logger getLog() {
		return log;
	}
}
