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
import org.hibernate.sql.ast.SqlAstInsertTranslator;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * @author Steve Ebersole
 */
public class StandardSqlAstInsertTranslator
		extends AbstractSqlAstTranslator
		implements SqlAstInsertTranslator {
	public StandardSqlAstInsertTranslator(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	@Override
	public JdbcInsert translate(InsertStatement sqlAst) {
		appendSql( "insert into " );
		appendSql( sqlAst.getTargetTable().getTableExpression() );

		appendSql( " (" );
		boolean firstPass = true;

		final List<ColumnReference> targetColumnReferences = sqlAst.getTargetColumnReferences();
		if ( targetColumnReferences == null ) {
			renderImplicitTargetColumnSpec();
		}
		else {
			for (ColumnReference targetColumnReference : targetColumnReferences) {
				if (firstPass) {
					firstPass = false;
				}
				else {
					appendSql( ", " );
				}

				appendSql( targetColumnReference.getColumnExpression() );
			}
		}

		appendSql( ") " );

		if ( sqlAst.getSourceSelectStatement()!=null ) {
			visitQuerySpec( sqlAst.getSourceSelectStatement() );
		}
		else {
			appendSql("values");
			boolean firstTuple = true;
			for ( Values values : sqlAst.getValuesList() ) {
				if (firstTuple) {
					firstTuple = false;
				}
				else {
					appendSql(", ");
				}
				appendSql(" (");
				boolean firstExpr = true;
				for ( Expression expression : values.getExpressions() ) {
					if (firstExpr) {
						firstExpr = false;
					}
					else {
						appendSql(", ");
					}
					expression.accept( this );
				}
				appendSql(")");
			}
		}

		return new JdbcInsert() {
			@Override
			public String getSql() {
				return StandardSqlAstInsertTranslator.this.getSql();
			}

			@Override
			public List<JdbcParameterBinder> getParameterBinders() {
				return StandardSqlAstInsertTranslator.this.getParameterBinders();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return StandardSqlAstInsertTranslator.this.getAffectedTableNames();
			}
		};
	}

	private void renderImplicitTargetColumnSpec() {
	}

	@Override
	public JdbcInsert translate(CteStatement sqlAst) {
		assert sqlAst.getCteConsumer() instanceof InsertStatement;

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

		translate( (InsertStatement) sqlAst.getCteConsumer() );

		return new JdbcInsert() {
			@Override
			public String getSql() {
				return StandardSqlAstInsertTranslator.this.getSql();
			}

			@Override
			public List<JdbcParameterBinder> getParameterBinders() {
				return StandardSqlAstInsertTranslator.this.getParameterBinders();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return StandardSqlAstInsertTranslator.this.getAffectedTableNames();
			}
		};
	}
}
