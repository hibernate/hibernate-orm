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
import org.hibernate.sql.ast.SqlAstInsertSelectTranslator;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * @author Steve Ebersole
 */
public class StandardSqlAstInsertSelectTranslator
		extends AbstractSqlAstTranslator
		implements SqlAstInsertSelectTranslator {
	public StandardSqlAstInsertSelectTranslator(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	@Override
	public JdbcInsert translate(InsertSelectStatement sqlAst) {
		appendSql( "insert into " );
		appendSql( sqlAst.getTargetTable().getTableExpression() );

		appendSql( " (" );
		boolean firstPass = true;

		final List<ColumnReference> targetColumnReferences = sqlAst.getTargetColumnReferences();
		if ( targetColumnReferences == null ) {
			renderImplicitTargetColumnSpec();
		}
		else {
			for ( int i = 0; i < targetColumnReferences.size(); i++ ) {
				if ( firstPass ) {
					firstPass = false;
				}
				else {
					appendSql( ", " );
				}

				final ColumnReference columnReference = targetColumnReferences.get( i );
				appendSql( columnReference.getColumnExpression() );
			}
		}

		appendSql( ") " );

		visitQuerySpec( sqlAst.getSourceSelectStatement() );

		return new JdbcInsert() {
			@Override
			public String getSql() {
				return StandardSqlAstInsertSelectTranslator.this.getSql();
			}

			@Override
			public List<JdbcParameterBinder> getParameterBinders() {
				return StandardSqlAstInsertSelectTranslator.this.getParameterBinders();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return StandardSqlAstInsertSelectTranslator.this.getAffectedTableNames();
			}
		};
	}

	private void renderImplicitTargetColumnSpec() {
	}

	@Override
	public JdbcInsert translate(CteStatement sqlAst) {
		assert sqlAst.getCteConsumer() instanceof DeleteStatement;

		appendSql( "with " );
		appendSql( sqlAst.getCteLabel() );

		appendSql( " (" );

		String separator = "";

		for ( int i = 0; i < sqlAst.getCteTable().getCteColumns().size(); i++ ) {
			final CteColumn cteColumn = sqlAst.getCteTable().getCteColumns().get( i );
			appendSql( separator );
			appendSql( cteColumn.getColumnExpression() );
			separator = ", ";
		}

		appendSql( ") as (" );

		visitQuerySpec( sqlAst.getCteDefinition() );

		appendSql( ") " );

		translate( (InsertSelectStatement) sqlAst.getCteConsumer() );

		return new JdbcInsert() {
			@Override
			public String getSql() {
				return StandardSqlAstInsertSelectTranslator.this.getSql();
			}

			@Override
			public List<JdbcParameterBinder> getParameterBinders() {
				return StandardSqlAstInsertSelectTranslator.this.getParameterBinders();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return StandardSqlAstInsertSelectTranslator.this.getAffectedTableNames();
			}
		};
	}
}
