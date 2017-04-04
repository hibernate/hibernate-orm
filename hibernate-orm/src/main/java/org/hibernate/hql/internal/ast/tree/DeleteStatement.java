/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Defines a top-level AST node representing an HQL delete statement.
 *
 * @author Steve Ebersole
 */
public class DeleteStatement extends AbstractRestrictableStatement {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DeleteStatement.class );

	@Override
	public int getStatementType() {
		return HqlSqlTokenTypes.DELETE;
	}

	@Override
	public boolean needsExecutor() {
		return true;
	}

	@Override
	protected int getWhereClauseParentTokenType() {
		return SqlTokenTypes.FROM;
	}

	@Override
	protected CoreMessageLogger getLog() {
		return LOG;
	}
}
