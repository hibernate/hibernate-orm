/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.internal;

import org.hibernate.MappingException;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardQueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardSetReturningFunctionRenderingSupport;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;

/**
 * An SQL AST translator for HANA.
 *
 * @author Christian Beikov
 */
public class HANASqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public HANASqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			appendSql( "cast(" );
			visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
			appendSql( arithmeticExpression.getOperator().getOperatorSqlTextString() );
			visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
			appendSql( " as int)" );
		}
		else {
			super.visitBinaryArithmeticExpression( arithmeticExpression );
		}
	}

	@Override
	protected void visitArithmeticOperand(Expression expression) {
		render( expression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
	}

	private boolean isHanaCloud() {
		return ( (HANADialect) getDialect() ).isCloud();
	}

	@Override
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.MERGE;
	}

	@Override
	protected QueryMutationRenderingSupport getQueryMutationRenderingSupport() {
		return isHanaCloud()
				? StandardQueryMutationRenderingSupport.MERGE
				: StandardQueryMutationRenderingSupport.STANDARD;
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		// HANA Cloud does not support the FROM clause in UPDATE statements
		if ( isHanaCloud() ) {
			super.renderUpdateClause( updateStatement );
		}
		else {
			appendSql( "update" );
			renderTableReferenceIdentificationVariable( updateStatement.getTargetTable() );
		}
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		// HANA Cloud does not support the FROM clause in UPDATE statements
		if ( !isHanaCloud() ) {
			if ( statement.getFromClause().getRoots().isEmpty() ) {
				appendSql( " from " );
				renderDmlTargetTableExpression( statement.getTargetTable() );
			}
			else {
				visitFromClause( statement.getFromClause() );
			}
		}
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> request.fetchClauseType() != null
				&& request.fetchClauseType() != FetchClauseType.ROWS_ONLY
				? new PaginationRenderingPlan.Window( true )
				: new PaginationRenderingPlan.LimitOffset();
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.HANA;
	}

	@Override
	protected SetReturningFunctionRenderingSupport getSetReturningFunctionRenderingSupport() {
		return StandardSetReturningFunctionRenderingSupport.HANA;
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		// In SAP HANA, LOBs are not "comparable", so we have to use a like predicate for comparison
		final boolean isLob = isLob( lhs.getExpressionType() );
		if ( operator == ComparisonOperator.DISTINCT_FROM || operator == ComparisonOperator.NOT_DISTINCT_FROM ) {
			if ( isLob ) {
				switch ( operator ) {
					case DISTINCT_FROM:
						appendSql( "case when " );
						lhs.accept( this );
						appendSql( " like " );
						rhs.accept( this );
						appendSql( " or " );
						lhs.accept( this );
						appendSql( " is null and " );
						rhs.accept( this );
						appendSql( " is null then 0 else 1 end=1" );
						return;
					case NOT_DISTINCT_FROM:
						appendSql( "case when " );
						lhs.accept( this );
						appendSql( " like " );
						rhs.accept( this );
						appendSql( " or " );
						lhs.accept( this );
						appendSql( " is null and " );
						rhs.accept( this );
						appendSql( " is null then 0 else 1 end=0" );
						return;
					default:
						// Fall through
						break;
				}
			}
			// HANA does not support plain parameters in the select clause of the intersect emulation
			withParameterRenderingMode(
					SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER,
					() -> renderComparisonEmulateIntersect( lhs, operator, rhs )
			);
		}
		else {
			if ( isLob ) {
				switch ( operator ) {
					case EQUAL:
						lhs.accept( this );
						appendSql( " like " );
						rhs.accept( this );
						return;
					case NOT_EQUAL:
						lhs.accept( this );
						appendSql( " not like " );
						rhs.accept( this );
						return;
					default:
						// Fall through
						break;
				}
			}
			renderComparisonStandard( lhs, operator, rhs );
		}
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "grouping sets (())" );
		}
		else if ( expression instanceof Summarization ) {
			throw new UnsupportedOperationException( "Summarization is not supported by DBMS" );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		throw new MappingException(
				String.format(
						"The INSERT statement for table [%s] contains no column, and this is not supported by [%s]",
						tableInsert.getMutatingTable().getTableId(),
						getDialect()
				)
		);
	}

}
