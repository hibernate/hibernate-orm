/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.predicate.InListPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.SqlTypes;

/**
 * A SQL AST translator for CUBRID.
 *
 * @author Christian Beikov
 */
public class CUBRIDSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public CUBRIDSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		//'limit' cannot express 'with ties' or a percentage, so emulate those with a row-numbering window
		return request -> request.hasFetch() && request.fetchClauseType() != FetchClauseType.ROWS_ONLY
				? new PaginationRenderingPlan.Window( true )
				: new PaginationRenderingPlan.CombinedLimit();
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		//CUBRID accepts a correlated derived table but not the 'lateral' keyword, and this is the
		//only standard profile that renders every lateral reference implicitly
		return StandardDerivedTableRenderingSupport.SQL_SERVER;
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		if ( isClob( lhs ) || isClob( rhs ) ) {
			renderClobComparison( lhs, operator, rhs );
		}
		else {
			//CUBRID has the null-safe '<=>' operator, so distinct-from needs no intersect emulation
			renderComparisonDistinctOperator( lhs, operator, rhs );
		}
	}

	//CUBRID compares a clob by locator, so two clobs holding the same text never compare equal,
	//and comparing a clob to a character type is rejected outright. Read both sides as characters.
	private void renderClobComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final boolean negated = operator == ComparisonOperator.DISTINCT_FROM;
		final String operatorText = switch ( operator ) {
			case DISTINCT_FROM, NOT_DISTINCT_FROM -> "<=>";
			default -> operator.sqlText();
		};
		if ( negated ) {
			appendSql( "not(" );
		}
		renderAsCharacterData( lhs );
		appendSql( operatorText );
		renderAsCharacterData( rhs );
		if ( negated ) {
			appendSql( CLOSE_PARENTHESIS );
		}
	}

	private void renderAsCharacterData(Expression expression) {
		if ( isClob( expression ) ) {
			appendSql( "clob_to_char(" );
			expression.accept( this );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			expression.accept( this );
		}
	}

	//every code below lands in a 'clob' column on CUBRID, including the long character types
	//XML degrades to, so all of them compare by locator rather than by content
	private static boolean isClob(Expression expression) {
		final JdbcMappingContainer expressionType = expression.getExpressionType();
		if ( expressionType == null || expressionType.getJdbcTypeCount() != 1 ) {
			return false;
		}
		return switch ( expressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() ) {
			case SqlTypes.CLOB, SqlTypes.NCLOB, SqlTypes.SQLXML,
					SqlTypes.LONG32VARCHAR, SqlTypes.LONG32NVARCHAR -> true;
			default -> false;
		};
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "'0' || '0'" );
		}
		else if ( expression instanceof Summarization ) {
			// This could theoretically be emulated by rendering all grouping variations of the query and
			// connect them via union all but that's probably pretty inefficient and would have to happen
			// on the query spec level
			throw new UnsupportedOperationException( "Summarization is not supported by DBMS!" );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete" );
		final var clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.DELETE );
			renderTableReferenceIdentificationVariable( statement.getTargetTable() );
			if ( statement.getFromClause().getRoots().isEmpty() ) {
				appendSql( " from " );
				renderDmlTargetTableExpression( statement.getTargetTable() );
			}
			else {
				visitFromClause( statement.getFromClause() );
			}
		}
		finally {
			clauseStack.pop();
		}
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		if ( updateStatement.getFromClause().getRoots().isEmpty() ) {
			super.renderUpdateClause( updateStatement );
		}
		else {
			appendSql( "update " );
			renderFromClauseSpaces( updateStatement.getFromClause() );
		}
	}

	@Override
	protected void renderAssignmentColumn(ColumnReference column) {
		column.appendColumnForWrite(
				this,
				getAffectedTableNames().size() > 1 && !( getStatement() instanceof InsertSelectStatement )
						? determineColumnReferenceQualifier( column )
						: null
		);
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	public void visitInListPredicate(InListPredicate inListPredicate) {
		final List<Expression> listExpressions = inListPredicate.getListExpressions();
		//CUBRID reads 'x in ((select ...))' as a list holding one scalar subquery and fails as soon as
		//the subquery returns more than one row, so render it as a plain in-subquery
		if ( listExpressions.size() == 1 && listExpressions.get( 0 ) instanceof SelectStatement subQuery ) {
			visitInSubQueryPredicate( new InSubQueryPredicate(
					inListPredicate.getTestExpression(),
					subQuery,
					inListPredicate.isNegated(),
					inListPredicate.getExpressionType()
			) );
		}
		else {
			super.visitInListPredicate( inListPredicate );
		}
	}
}
