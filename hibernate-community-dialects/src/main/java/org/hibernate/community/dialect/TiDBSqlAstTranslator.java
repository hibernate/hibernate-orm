/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.Internal;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorWithOnDuplicateKeyUpdate;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.dialect.sql.ast.spi.FullJoinEmulation;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.LikePredicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;

import java.util.ArrayDeque;

/**
 * A SQL AST translator for TiDB.
 *
 * @author Christian Beikov
 * @author Cong Wang
 */
public class TiDBSqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithOnDuplicateKeyUpdate<T> {

	private final TiDBDialect dialect;
	private final ArrayDeque<FullJoinEmulation> fullJoinEmulations = new ArrayDeque<>();

	public TiDBSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request, TiDBDialect dialect) {
		super( request );
		this.dialect = dialect;
		this.fullJoinEmulations.push( new FullJoinEmulation( this ) );
	}

	private FullJoinEmulation currentFullJoinEmulationHelper() {
		return fullJoinEmulations.getFirst();
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			appendSql( OPEN_PARENTHESIS );
			visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
			appendSql( " div " );
			visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			super.visitBinaryArithmeticExpression(arithmeticExpression);
		}
	}

	@Override
	@Internal
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_VALUES_FUNCTION;
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete" );
		renderTableReferenceIdentificationVariable( statement.getTargetTable() );
		if ( statement.getFromClause().getRoots().isEmpty() ) {
			appendSql( " from " );
			renderDmlTargetTableExpression( statement.getTargetTable() );
		}
		else {
			visitFromClause( statement.getFromClause() );
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
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
		final DmlTargetColumnQualifierSupport qualifierSupport = getDialect().getDmlTargetColumnQualifierSupport();
		final MutationStatement currentDmlStatement;
		final String dmlAlias;
		// Since TiDB does not support aliasing the insert target table,
		// we must detect column reference that are used in the conflict clause
		// and use the table expression as qualifier instead
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
	protected void renderExpressionAsClauseItem(Expression expression) {
		expression.accept( this );
	}

	@Override
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		final boolean isNegated = booleanExpressionPredicate.isNegated();
		if ( isNegated ) {
			appendSql( "not(" );
		}
		booleanExpressionPredicate.getExpression().accept( this );
		if ( isNegated ) {
			appendSql( CLOSE_PARENTHESIS );
		}
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> currentFullJoinEmulationHelper().isFullJoinEmulationQueryPart( request.queryPart() )
				? new PaginationRenderingPlan.None()
				: request.hasFetch()
						&& request.fetchClauseType() != FetchClauseType.ROWS_ONLY
						&& dialect.getWindowFunctionSupport()
								.supports( WindowFunctionSupport.Feature.WINDOW_FUNCTIONS )
								? new PaginationRenderingPlan.Window( true )
								: new PaginationRenderingPlan.CombinedLimit();
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		final var helper = currentFullJoinEmulationHelper();
		final boolean needsNestedHelper =
				helper.hasActiveFullJoinEmulation()
						&& !helper.isFullJoinEmulationQueryPart( querySpec );
		if ( needsNestedHelper ) {
			fullJoinEmulations.push( new FullJoinEmulation( this ) );
		}
		try {
			final var currentHelper = currentFullJoinEmulationHelper();
			if ( !currentHelper.renderFullJoinEmulationBranchIfNeeded( querySpec, super::visitQuerySpec )
					&& !currentHelper.emulateFullJoinWithUnionIfNeeded( querySpec ) ) {
				super.visitQuerySpec( querySpec );
			}
		}
		finally {
			if ( needsNestedHelper ) {
				fullJoinEmulations.pop();
			}
		}
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.QUERY_AND_VALUES_SELECT_LIST;
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonDistinctOperator( lhs, operator, rhs );
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "'0'" );
		}
		else if ( expression instanceof Summarization summarization ) {
			renderCommaSeparated( summarization.getGroupings() );
			appendSql( " with " );
			appendSql( summarization.getKind().sqlText() );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		if ( likePredicate.isCaseSensitive() ) {
			likePredicate.getMatchExpression().accept( this );
			if ( likePredicate.isNegated() ) {
				appendSql( " not" );
			}
			appendSql( " like " );
			renderBackslashEscapedLikePattern(
					likePredicate.getPattern(),
					likePredicate.getEscapeCharacter(),
					dialect.isNoBackslashEscapesEnabled()
			);
		}
		else {
			appendSql( dialect.getLowercaseFunction() );
			appendSql( OPEN_PARENTHESIS );
			likePredicate.getMatchExpression().accept( this );
			appendSql( CLOSE_PARENTHESIS );
			if ( likePredicate.isNegated() ) {
				appendSql( " not" );
			}
			appendSql( " like " );
			appendSql( dialect.getLowercaseFunction() );
			appendSql( OPEN_PARENTHESIS );
			renderBackslashEscapedLikePattern(
					likePredicate.getPattern(),
					likePredicate.getEscapeCharacter(),
					dialect.isNoBackslashEscapesEnabled()
			);
			appendSql( CLOSE_PARENTHESIS );
		}
		if ( likePredicate.getEscapeCharacter() != null ) {
			appendSql( " escape " );
			likePredicate.getEscapeCharacter().accept( this );
		}
	}

	@Override
	protected TiDBDialect getDialect() {
		return dialect;
	}

	@Override
	protected void renderStringContainsExactlyPredicate(Expression haystack, Expression needle) {
		// TiDB can't cope with NUL characters in the position function, so we use a like predicate instead
		haystack.accept( this );
		appendSql( " like concat('%',replace(replace(replace(" );
		needle.accept( this );
		appendSql( ",'~','~~'),'?','~?'),'%','~%'),'%') escape '~'" );
	}

	@Override
	protected void renderNewRowAlias() {
	}

	@Override
	protected void renderUpdateValue(ColumnValueBinding columnValueBinding) {
		appendSql( "values(" );
		appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
		appendSql( ")" );
	}
}
