/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.sql.exec.spi.JdbcOperation;

import java.util.List;

public class InterSystemsIRISSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	protected InterSystemsIRISSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		renderIntoIntoAndTable( tableInsert );
		appendSql( "default values" );
	}


	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete" );
		appendSql( " from " );
		renderDmlTargetTableExpression( statement.getTargetTable() );
		renderTableReferenceIdentificationVariable( statement.getTargetTable() );
	}


	@Override
	protected void renderTupleComparisonStandard(
			List<SqlSelection> lhsSelections,
			SqlTuple rhsTuple,
			ComparisonOperator operator
	) {

		if ( operator == ComparisonOperator.EQUAL || operator == ComparisonOperator.NOT_EQUAL ) {
			emulateTupleComparisonSelections( lhsSelections, rhsTuple, operator );
		}
		else {

			super.renderTupleComparisonStandard( lhsSelections, rhsTuple, operator );
		}
	}

	@SuppressWarnings("unchecked")
	protected void emulateTupleComparisonSelections(
			List<SqlSelection> lhsSelections,
			SqlTuple rhsTuple,
			ComparisonOperator operator
	) {
		final List<Expression> rhsExpressions = (List<Expression>) rhsTuple.getExpressions();

		if ( lhsSelections.size() != rhsExpressions.size() ) {
			throw new IllegalArgumentException( "Tuple size mismatch" );
		}

		final String joiner = ( operator == ComparisonOperator.EQUAL ) ? " and " : " or ";

		appendSql( OPEN_PARENTHESIS );
		for ( int i = 0; i < lhsSelections.size(); i++ ) {
			if ( i > 0 ) {
				appendSql( joiner );
			}

			lhsSelections.get( i ).getExpression().accept( this );
			appendSql( operator.sqlText() );
			rhsExpressions.get( i ).accept( this );
		}
		appendSql( CLOSE_PARENTHESIS );
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.QUERY_AND_VALUES_SELECT_LIST;
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		appendSql( "update" );
		append( WHITESPACE );
		renderDmlTargetTableExpression( updateStatement.getTargetTable() );
		renderTableReferenceIdentificationVariable( updateStatement.getTargetTable() );
	}


	@Override
	protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
		final DmlTargetColumnQualifierSupport qualifierSupport = getDialect().getDmlTargetColumnQualifierSupport();
		final MutationStatement currentDmlStatement;
		final String dmlAlias;

		if ( getClauseStack().getCurrent() != Clause.SET
			|| !( ( currentDmlStatement = getCurrentDmlStatement() ) instanceof InsertSelectStatement )
			|| ( dmlAlias = currentDmlStatement.getTargetTable().getIdentificationVariable() ) == null
			|| !dmlAlias.equals( columnReference.getQualifier() ) ) {
			return columnReference.getQualifier();
		}
		// Qualify the column reference with the table expression also when in subqueries
		else if ( qualifierSupport != DmlTargetColumnQualifierSupport.NONE || !getQueryPartStack().isEmpty() ) {
			return getCurrentDmlStatement().getTargetTable().getTableExpression();
		}
		else {
			return null;
		}
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
			appendSql( " \\ " );
			visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
		}
		else {
			super.visitBinaryArithmeticExpression( arithmeticExpression );
		}
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> request.fetchClauseType() != null
				&& request.fetchClauseType() != FetchClauseType.ROWS_ONLY
				&& !getDialect().getFetchClauseSupport().supports( request.fetchClauseType() )
				? new PaginationRenderingPlan.Window( true )
				: new PaginationRenderingPlan.OffsetFetch( true );
	}


	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		if ( statement.getFromClause().getRoots().isEmpty() ) {
			appendSql( " from " );
			renderDmlTargetTableExpression( statement.getTargetTable() );
			renderTableReferenceIdentificationVariable( statement.getTargetTable() );
		}
		else {
			visitFromClause( statement.getFromClause() );
		}
	}

}
