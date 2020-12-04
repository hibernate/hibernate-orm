/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.SqlAstDeleteTranslator;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * @author Steve Ebersole
 */
public class StandardSqlAstDeleteTranslator extends AbstractSqlAstTranslator implements SqlAstDeleteTranslator {
	public StandardSqlAstDeleteTranslator(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	private String deletingTableAlias;

	@Override
	public JdbcDelete translate(DeleteStatement sqlAst) {
		try {
			deletingTableAlias = sqlAst.getTargetTable().getIdentificationVariable();
			// todo (6.0) : to support joins we need dialect support
			appendSql( "delete from " );
			final Stack<Clause> clauseStack = getClauseStack();
			try {
				clauseStack.push( Clause.DELETE );
				renderTableReference( sqlAst.getTargetTable() );
			}
			finally {
				clauseStack.pop();
			}

			if ( sqlAst.getRestriction() != null ) {
				try {
					clauseStack.push( Clause.WHERE );
					appendSql( " where " );
					sqlAst.getRestriction().accept( this );
				}
				finally {
					clauseStack.pop();
				}
			}

			return new JdbcDelete() {
				@Override
				public String getSql() {
					return StandardSqlAstDeleteTranslator.this.getSql();
				}

				@Override
				public List<JdbcParameterBinder> getParameterBinders() {
					return StandardSqlAstDeleteTranslator.this.getParameterBinders();
				}

				@Override
				public Set<String> getAffectedTableNames() {
					return StandardSqlAstDeleteTranslator.this.getAffectedTableNames();
				}

				@Override
				public Set<FilterJdbcParameter> getFilterJdbcParameters() {
					return StandardSqlAstDeleteTranslator.this.getFilterJdbcParameters();
				}
			};
		}
		finally {
			cleanup();
		}
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		if ( deletingTableAlias != null && deletingTableAlias.equals( columnReference.getQualifier() ) ) {
			// todo (6.0) : use the Dialect to determine how to handle column references
			//		- specifically should they use the table-alias, the table-expression
			//			or neither for its qualifier

			// for now, use the unqualified form
			appendSql( columnReference.getColumnExpression() );
		}
		else {
			super.visitColumnReference( columnReference );
		}
	}

	@Override
	public JdbcDelete translate(CteStatement sqlAst) {
		assert sqlAst.getCteConsumer() instanceof DeleteStatement;
		try {
			appendSql( "with " );
			appendSql( sqlAst.getCteLabel() );

			appendSql( " (" );

			String separator = "";

			for ( CteColumn cteColumn : sqlAst.getCteTable().getCteColumns() ) {
				appendSql( separator );
				appendSql( cteColumn.getColumnExpression() );
				separator = ", ";
			}

			appendSql( ") as (" );

			visitQuerySpec( sqlAst.getCteDefinition() );

			appendSql( ") " );

			translate( (DeleteStatement) sqlAst.getCteConsumer() );

			return new JdbcDelete() {
				@Override
				public String getSql() {
					return StandardSqlAstDeleteTranslator.this.getSql();
				}

				@Override
				public List<JdbcParameterBinder> getParameterBinders() {
					return StandardSqlAstDeleteTranslator.this.getParameterBinders();
				}

				@Override
				public Set<String> getAffectedTableNames() {
					return StandardSqlAstDeleteTranslator.this.getAffectedTableNames();
				}

				@Override
				public Set<FilterJdbcParameter> getFilterJdbcParameters() {
					return StandardSqlAstDeleteTranslator.this.getFilterJdbcParameters();
				}
			};
		}
		finally {
			cleanup();
		}
	}
}
