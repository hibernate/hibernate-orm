/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.internal;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorWithOnDuplicateKeyUpdate;
import java.util.ArrayDeque;
import java.util.List;

import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.query.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
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
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.dialect.sql.ast.spi.FullJoinEmulation;

/**
 * A SQL AST translator for MariaDB.
 *
 * @author Christian Beikov
 * @author Yoobin Yoon
 */
public class MariaDBSqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithOnDuplicateKeyUpdate<T> {

	private final MariaDBDialect dialect;
	private final ArrayDeque<FullJoinEmulation> fullJoinEmulations = new ArrayDeque<>();

	public MariaDBSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request, MariaDBDialect dialect) {
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
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_VALUES_FUNCTION;
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		if ( usesSingleTableDml( statement ) ) {
			appendSql( "delete from " );
			appendSql( statement.getTargetTable().getTableExpression() );
			registerAffectedTable( statement.getTargetTable() );
		}
		else {
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
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		if ( usesSingleTableDml( updateStatement ) ) {
			appendSql( "update " );
			appendSql( updateStatement.getTargetTable().getTableExpression() );
			registerAffectedTable( updateStatement.getTargetTable() );
		}
		else if ( updateStatement.getFromClause().getRoots().isEmpty() ) {
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
		if ( getClauseStack().getCurrent() != Clause.INSERT && !usesSingleTableDml( getCurrentDmlStatement() ) ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
		final DmlTargetColumnQualifierSupport qualifierSupport = getDialect().getDmlTargetColumnQualifierSupport();
		final String dmlAlias;
		// Since MariaDB does not support aliasing the insert target table,
		// we must detect column reference that are used in the conflict clause
		// and use the table expression as qualifier instead
		if ( getClauseStack().getCurrent() != Clause.SET
				|| !( getCurrentDmlStatement() instanceof InsertSelectStatement insertSelectStatement )
				|| ( dmlAlias = insertSelectStatement.getTargetTable().getIdentificationVariable() ) == null
				|| !dmlAlias.equals( columnReference.getQualifier() ) ) {
			final MutationStatement currentStatement = getCurrentDmlStatement();
			if ( currentStatement != null && usesSingleTableDml( currentStatement ) && columnReference.getQualifier() != null ) {
				final NamedTableReference targetTable = currentStatement.getTargetTable();
				final String targetTableName = targetTable.getTableExpression();
				final String qualifier = columnReference.getQualifier();
				final String targetAlias = targetTable.getIdentificationVariable();
				if ( ( targetAlias != null && qualifier.equals( targetAlias ) ) || qualifier.equals( targetTableName ) ) {
					return !getQueryPartStack().isEmpty() ? targetTableName : null;
				}
			}
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
	protected void visitRecursivePath(Expression recursivePath, int sizeEstimate) {
		// MariaDB determines the type and size of a column in a recursive CTE based on the expression of the non-recursive part
		// Due to that, we have to cast the path in the non-recursive path to a varchar of appropriate size to avoid data truncation errors
		if ( sizeEstimate == -1 ) {
			super.visitRecursivePath( recursivePath, sizeEstimate );
		}
		else {
			appendSql( "cast(" );
			recursivePath.accept( this );
			appendSql( " as char(" );
			appendSql( sizeEstimate );
			appendSql( "))" );
		}
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
						&& hasWindowFunctionSupport()
								? new PaginationRenderingPlan.Window( true )
								: new PaginationRenderingPlan.CombinedLimit();
	}

	@Override
	protected boolean shouldEmulateLateralWithIntersect(QueryPart queryPart) {
		// Intersect emulation requires nested correlation when no simple query grouping is possible
		// and the query has an offset/fetch clause, so we have to disable the emulation in this case,
		// because nested correlation is not supported though
		return getDialect().getSetOperationSupport()
				.supports( SetOperationSupport.Capability.SIMPLE_QUERY_GROUPING )
			|| !queryPart.hasOffsetOrFetchClause();
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
	protected void renderSelectClause(SelectClause selectClause) {
		if ( !currentFullJoinEmulationHelper().renderSelectClauseIfNeeded( selectClause ) ) {
			super.renderSelectClause( selectClause );
		}
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.QUERY_SELECT_LIST;
	}

	@Override
	protected void visitOrderBy(List<SortSpecification> sortSpecifications) {
		currentFullJoinEmulationHelper().renderOrderByIfNeeded( getCurrentQueryPart(), sortSpecifications, super::visitOrderBy );
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		if ( lhsExpressionType != null && lhsExpressionType.getJdbcTypeCount() == 1
				&& lhsExpressionType.getSingleJdbcMapping().getJdbcType().isJson()
				&& getDialect().getVersion().isSameOrAfter( 10, 7 ) ) {
			switch ( operator ) {
				case DISTINCT_FROM:
					appendSql( "case when json_equals(" );
					lhs.accept( this );
					appendSql( ',' );
					rhs.accept( this );
					appendSql( ")=1 or " );
					lhs.accept( this );
					appendSql( " is null and " );
					rhs.accept( this );
					appendSql( " is null then 0 else 1 end=1" );
					break;
				case NOT_DISTINCT_FROM:
					appendSql( "case when json_equals(" );
					lhs.accept( this );
					appendSql( ',' );
					rhs.accept( this );
					appendSql( ")=1 or " );
					lhs.accept( this );
					appendSql( " is null and " );
					rhs.accept( this );
					appendSql( " is null then 0 else 1 end=0" );
					break;
				case NOT_EQUAL:
					appendSql( "json_equals(" );
					lhs.accept( this );
					appendSql( ',' );
					rhs.accept( this );
					appendSql( ")=0" );
					break;
				case EQUAL:
					appendSql( "json_equals(" );
					lhs.accept( this );
					appendSql( ',' );
					rhs.accept( this );
					appendSql( ")=1" );
					break;
				default:
					renderComparisonDistinctOperator( lhs, operator, rhs );
					break;
			}
		}
		else {
			renderComparisonDistinctOperator( lhs, operator, rhs );
		}
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
					getDialect().isNoBackslashEscapesEnabled()
			);
		}
		else {
			appendSql( getDialect().getLowercaseFunction() );
			appendSql( OPEN_PARENTHESIS );
			likePredicate.getMatchExpression().accept( this );
			appendSql( CLOSE_PARENTHESIS );
			if ( likePredicate.isNegated() ) {
				appendSql( " not" );
			}
			appendSql( " like " );
			appendSql( getDialect().getLowercaseFunction() );
			appendSql( OPEN_PARENTHESIS );
			renderBackslashEscapedLikePattern(
					likePredicate.getPattern(),
					likePredicate.getEscapeCharacter(),
					getDialect().isNoBackslashEscapesEnabled()
			);
			appendSql( CLOSE_PARENTHESIS );
		}
		if ( likePredicate.getEscapeCharacter() != null ) {
			appendSql( " escape " );
			likePredicate.getEscapeCharacter().accept( this );
		}
	}

	@Override
	protected MariaDBDialect getDialect() {
		return dialect;
	}

	private boolean hasWindowFunctionSupport() {
		return getDialect().getWindowFunctionSupport()
				.supports( WindowFunctionSupport.Feature.WINDOW_FUNCTIONS );
	}

	@Override
	protected void renderStringContainsExactlyPredicate(Expression haystack, Expression needle) {
		// MariaDB can't cope with NUL characters in the position function, so we use a like predicate instead
		haystack.accept( this );
		appendSql( " like concat('%',replace(replace(replace(" );
		needle.accept( this );
		appendSql( ",'~','~~'),'?','~?'),'%','~%'),'%') escape '~'" );
	}

	/*
		Upsert Template: (for an entity WITHOUT @Version)
			INSERT INTO employees (id, name, salary, version)
				VALUES (?, ?, ?, ?)
			ON DUPLICATE KEY UPDATE
				name = values(name),
				salary = values(salary)
	*/
	@Override
	protected void renderUpdateValue(ColumnValueBinding columnValueBinding) {
		appendSql( "values(" );
		appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
		appendSql( ")" );
	}

	@Override
	protected void renderAssignmentColumn(ColumnReference column) {
		column.appendColumnForWrite(
				this,
				getAffectedTableNames().size() > 1 && !(getStatement() instanceof InsertSelectStatement)
						? determineColumnReferenceQualifier( column )
						: null );
	}

	private boolean usesSingleTableDml(MutationStatement statement) {
		// As of MariaDB 11.1, the self-join rewrite optimization can handle this, so no need force single table DML
		return getDialect().getVersion().isBefore( 11, 1 ) && hasTargetTableCorrelation( statement );
	}

	private boolean needsDmlSubqueryWrapper() {
		final Statement statement = getStatement();
		// As of MariaDB 11.1, the self-join rewrite optimization can handle this, so no need for the wrapper
		return getDialect().getVersion().isBefore( 11, 1 )
				&& statement instanceof AbstractUpdateOrDeleteStatement updateOrDeleteStatement
				&& !hasNestedOrTargetTableCorrelation( updateOrDeleteStatement );
	}

	@Override
	protected void renderSelectStatement(SelectStatement statement) {
		final boolean needsParenthesis = !statement.getQueryPart().isRoot();
		if ( needsParenthesis && needsDmlSubqueryWrapper() ) {
			appendSql( OPEN_PARENTHESIS );
			appendSql( "select * from " );
			super.renderSelectStatement( statement );
			appendSql( " _sub_" );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			super.renderSelectStatement( statement );
		}
	}

	@Override
	protected <X extends Expression> void renderRelationalEmulationSubQuery(
			QuerySpec subQuery,
			X lhsTuple,
			SubQueryRelationalRestrictionEmulationRenderer<X> renderer,
			ComparisonOperator tupleComparisonOperator) {
		if ( needsDmlSubqueryWrapper() ) {
			appendSql( OPEN_PARENTHESIS );
			appendSql( "select * from " );
			super.renderRelationalEmulationSubQuery( subQuery, lhsTuple, renderer, tupleComparisonOperator );
			appendSql( " _sub_" );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			super.renderRelationalEmulationSubQuery( subQuery, lhsTuple, renderer, tupleComparisonOperator );
		}
	}

	@Override
	protected void renderQuantifiedEmulationSubQuery(
			QuerySpec subQuery,
			ComparisonOperator tupleComparisonOperator) {
		if ( needsDmlSubqueryWrapper() ) {
			appendSql( OPEN_PARENTHESIS );
			appendSql( "select * from " );
			super.renderQuantifiedEmulationSubQuery( subQuery, tupleComparisonOperator );
			appendSql( " _sub_" );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			super.renderQuantifiedEmulationSubQuery( subQuery, tupleComparisonOperator );
		}
	}

	@Override
	protected void renderFetchFirstRow() {
		appendSql( " limit 1" );
	}

}
