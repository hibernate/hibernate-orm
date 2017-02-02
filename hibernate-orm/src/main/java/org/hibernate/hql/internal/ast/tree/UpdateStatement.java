/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import antlr.collections.AST;

/**
 * Defines a top-level AST node representing an HQL update statement.
 *
 * @author Steve Ebersole
 */
public class UpdateStatement extends AbstractRestrictableStatement {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( UpdateStatement.class );

	@Override
	public int getStatementType() {
		return SqlTokenTypes.UPDATE;
	}

	@Override
	public boolean needsExecutor() {
		return true;
	}

	@Override
	protected int getWhereClauseParentTokenType() {
		return SqlTokenTypes.SET;
	}

	@Override
	protected CoreMessageLogger getLog() {
		return LOG;
	}

	public AST getSetClause() {
		return ASTUtil.findTypeInChildren( this, HqlSqlTokenTypes.SET );
	}
}
