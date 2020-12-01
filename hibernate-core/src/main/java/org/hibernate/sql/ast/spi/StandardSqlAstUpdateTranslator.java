/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.sql.ast.SqlAstUpdateTranslator;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
 * @author Steve Ebersole
 */
public class StandardSqlAstUpdateTranslator
		extends AbstractSqlAstTranslator
		implements SqlAstUpdateTranslator {
//	private final Dialect dialect;

	public StandardSqlAstUpdateTranslator(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );

		// todo (6.0) : use the Dialect to determine how to handle column references
		//		- specifically should they use the table-alias, the table-expression
		//			or neither for its qualifier
//		dialect = getSessionFactory().getJdbcServices().getJdbcEnvironment().getDialect();
	}

	private String updatingTableAlias;

	@Override
	public JdbcUpdate translate(UpdateStatement sqlAst) {
		try {
			updatingTableAlias = sqlAst.getTargetTable().getIdentificationVariable();

			appendSql( "update " );
			appendSql( sqlAst.getTargetTable().getTableExpression() );

			appendSql( " set " );
			boolean firstPass = true;
			for ( Assignment assignment : sqlAst.getAssignments() ) {
				if ( firstPass ) {
					firstPass = false;
				}
				else {
					appendSql( ", " );
				}

				final List<ColumnReference> columnReferences = assignment.getAssignable().getColumnReferences();
				if ( columnReferences.size() == 1 ) {
					columnReferences.get( 0 ).accept( this );
				}
				else {
					appendSql( " (" );
					for (ColumnReference columnReference : columnReferences) {
						columnReference.accept( this );
					}
					appendSql( ") " );
				}
				appendSql( " = " );
				assignment.getAssignedValue().accept( this );
			}

			if ( sqlAst.getRestriction() != null ) {
				appendSql( " where " );
				sqlAst.getRestriction().accept( this );
			}

			return new JdbcUpdate() {
				@Override
				public String getSql() {
					return StandardSqlAstUpdateTranslator.this.getSql();
				}

				@Override
				public List<JdbcParameterBinder> getParameterBinders() {
					return StandardSqlAstUpdateTranslator.this.getParameterBinders();
				}

				@Override
				public Set<FilterJdbcParameter> getFilterJdbcParameters() {
					return StandardSqlAstUpdateTranslator.this.getFilterJdbcParameters();
				}

				@Override
				public Set<String> getAffectedTableNames() {
					return StandardSqlAstUpdateTranslator.this.getAffectedTableNames();
				}
			};
		}
		finally {
			cleanup();
		}
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		if ( updatingTableAlias != null && updatingTableAlias.equals( columnReference.getQualifier() ) ) {
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
	public JdbcUpdate translate(CteStatement cteStatement) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
