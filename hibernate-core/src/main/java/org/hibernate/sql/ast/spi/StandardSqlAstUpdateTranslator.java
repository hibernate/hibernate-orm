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
		extends AbstractSqlAstToJdbcOperationConverter
		implements SqlAstUpdateTranslator {
	public StandardSqlAstUpdateTranslator(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	@Override
	public JdbcUpdate translate(UpdateStatement sqlAst) {
		appendSql( "update " );
		appendSql( sqlAst.getTargetTable().getTableExpression() );

		appendSql( " set " );
		boolean firstPass = true;
		for ( int i = 0; i < sqlAst.getAssignments().size(); i++ ) {
			if ( firstPass ) {
				firstPass = false;
			}
			else {
				appendSql( ", " );
			}

			final Assignment assignment = sqlAst.getAssignments().get( i );
			assignment.getColumnReference().accept( this );
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
			public Set<String> getAffectedTableNames() {
				return StandardSqlAstUpdateTranslator.this.getAffectedTableNames();
			}
		};
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		super.visitColumnReference( columnReference );
	}

	@Override
	public JdbcUpdate translate(CteStatement cteStatement) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
