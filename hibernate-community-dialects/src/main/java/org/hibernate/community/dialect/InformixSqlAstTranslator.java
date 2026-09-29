package org.hibernate.community.dialect;

import java.util.List;

import jakarta.annotation.Nullable;
import org.hibernate.Internal;
import org.hibernate.Locking;
import org.hibernate.SPI;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardQueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SelectItemReferenceStrategy;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.ast.spi.model.TenantIdColumnValueBinding;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.predicate.LikePredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.update.Assignable;
import org.hibernate.sql.ast.spi.query.update.Assignment;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorWithMerge;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.FunctionExpression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.sql.ast.spi.query.SqlAstTreeHelper.isNullLiteral;

/**
 * A SQL AST translator for Informix.
 *
 * @author Christian Beikov
 */
public class InformixSqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {

	public InformixSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	@SPI({ IMPLEMENT, SPI.Role.SUPPLY })
	protected SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.POSITION;
	}

	@Override
	protected void visitQueryClauses(QuerySpec querySpec) {
		visitSelectClause( querySpec.getSelectClause() );
		visitFromClause( querySpec.getFromClause() );
		if ( !hasFrom( querySpec.getFromClause() )
				&& ( hasWhere( querySpec.getWhereClauseRestrictions() ) || querySpec.hasSortSpecifications() || querySpec.hasOffsetOrFetchClause() )
				&& getDialect().getSingleRowTableSupport().getSelectOnlyFromClause().isBlank() ) {
			append( " from " );
			append( getSingleRowTableExpression() );
		}
		visitWhereClause( querySpec.getWhereClauseRestrictions() );
		visitGroupByClause( querySpec, getGroupBySelectItemReferenceStrategy() );
		visitHavingClause( querySpec );
		visitOrderBy( querySpec.getSortSpecifications() );
		visitOffsetFetchClause( querySpec );
	}

	@Override
	protected void renderSelectClause(SelectClause selectClause) {
		appendSql( "select " );
		final QuerySpec querySpec = (QuerySpec) getQueryPartStack().getCurrent();
		final PaginationRenderingPlan plan = determinePaginationRenderingPlan( querySpec );
		if ( plan instanceof PaginationRenderingPlan.SkipFirst ) {
			renderSkipFirstClause( querySpec );
		}
		else if ( plan instanceof PaginationRenderingPlan.First ) {
			renderFirstClause( querySpec );
		}
		if ( selectClause.isDistinct() ) {
			appendSql( "distinct " );
		}
		super.renderSelectItems( selectClause );
		renderVirtualSelections( selectClause );
	}

	@Override
	protected void renderSelectExpression(Expression expression) {
		renderSelectExpressionWithCastedOrInlinedPlainParameters( expression );
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> {
			if ( request.hasFetch() && request.fetchClauseType() != FetchClauseType.ROWS_ONLY ) {
				if ( getDialect().getWindowFunctionSupport()
						.supports( WindowFunctionSupport.Feature.WINDOW_FUNCTIONS ) ) {
					return new PaginationRenderingPlan.Window( true );
				}
				throw new IllegalArgumentException( "Can't emulate non-ROWS fetch clause without window functions" );
			}
			if ( request.queryPart() instanceof QueryGroup ) {
				return new PaginationRenderingPlan.FirstSkip();
			}
			return supportsSkipFirstClause()
					? new PaginationRenderingPlan.SkipFirst()
					: new PaginationRenderingPlan.First();
		};
	}

	@Override
	protected void renderFetchPlusOffsetExpression(
			Expression fetchClauseExpression,
			Expression offsetClauseExpression,
			int offset) {
		renderFetchPlusOffsetExpressionAsSingleParameter( fetchClauseExpression, offsetClauseExpression, offset );
	}

	@Override
	protected void renderFetchExpression(Expression fetchExpression) {
		if ( supportsParameterOffsetFetchExpression() ) {
			super.renderFetchExpression( fetchExpression );
		}
		else {
			renderExpressionAsLiteral( fetchExpression, getJdbcParameterBindings() );
		}
	}

	@Override
	protected void renderOffsetExpression(Expression offsetExpression) {
		if ( supportsParameterOffsetFetchExpression() ) {
			super.renderOffsetExpression( offsetExpression );
		}
		else {
			renderExpressionAsLiteral( offsetExpression, getJdbcParameterBindings() );
		}
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		// Informix only supports the SKIP clause in the top level query
		if ( !queryPart.isRoot() && queryPart.getOffsetClauseExpression() != null ) {
			throw new IllegalArgumentException( "Can't emulate offset clause in subquery" );
		}
		super.visitOffsetFetchClause( queryPart );
	}

	@Override
	protected void beforeQueryGroup(QueryGroup queryGroup, QueryPart currentQueryPart) {
		if ( queryGroup.isRoot() && queryGroup.hasOffsetOrFetchClause() ) {
			append( "select ");
			renderFirstSkipClause( queryGroup.getOffsetClauseExpression(),
					queryGroup.getFetchClauseExpression() );
			append(  "* from " );
			append( OPEN_PARENTHESIS );
		}
	}

	@Override
	protected void afterQueryGroup(QueryGroup queryGroup, QueryPart currentQueryPart) {
		if ( queryGroup.isRoot() && queryGroup.hasOffsetOrFetchClause() ) {
			append( CLOSE_PARENTHESIS );
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		switch ( operator ) {
			case DISTINCT_FROM:
			case NOT_DISTINCT_FROM:
				renderComparisonEmulateIntersect( lhs, operator, rhs );
				break;
			default:
				renderComparisonOperand( lhs );
				appendSql( operator.sqlText() );
				renderComparisonOperand( rhs );
				break;
		}
	}

	private void renderComparisonOperand(Expression expression) {
		if ( isNullLiteral( expression ) ) {
			renderCasted( expression );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected void renderLikePredicate(LikePredicate likePredicate) {
		// Informix uses the backslash character as the default escape character and doesn't support
		// an empty escape character to avoid escaping
		if (likePredicate.getEscapeCharacter() == null) {
			renderBackslashEscapedLikePattern( likePredicate.getPattern(), likePredicate.getEscapeCharacter(), true );
			appendSql( " escape '\\'" );
		}
		else {
			super.renderLikePredicate( likePredicate );
		}
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
		// We render an empty group instead of literals as some DBs don't support grouping by literals
		// Note that integer literals, which refer to select item positions, are handled in #visitGroupByClause
		if ( expression instanceof Literal ) {
			// Note that this depends on the SqmToSqlAstConverter to add a dummy table group
			appendSql( "dummy_.x" );
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
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		renderIntoIntoAndTable( tableInsert );
		appendSql( "values (0)" );
	}

	private boolean supportsParameterOffsetFetchExpression() {
		return getDialect().getVersion().isSameOrAfter( 11 );
	}

	private boolean supportsSkipFirstClause() {
		return getDialect().getVersion().isSameOrAfter( 11 );
	}

	@Override
	@Internal
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.MERGE;
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.VALUES_SELECT_LIST;
	}

	@Override
	protected void visitArithmeticOperand(Expression expression) {
		render( expression, SqlAstNodeRenderingMode.NO_UNTYPED );
	}

	private static boolean isConcatFunction(Expression expression) {
		return expression instanceof FunctionExpression fn
			&& fn.getFunctionName().equals( "concat" );
	}

	private void caseArgument(Expression expression) {
		if ( isConcatFunction( expression ) ) {
			// concatenation inside a case must be cast to varchar(255)
			// or we get a bunch of trailing whitespace
			append( "cast(" );
			expression.accept( this );
			append( " as varchar(255))");
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression, boolean inSelect) {
		visitAnsiCaseSearchedExpression( caseSearchedExpression, this::caseArgument );
	}

	@Override
	protected void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression, boolean inSelect) {
		visitAnsiCaseSimpleExpression( caseSimpleExpression, this::caseArgument );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			appendSql( "floor" );
		}
		super.visitBinaryArithmeticExpression( arithmeticExpression );
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if (getDialect().getVersion().isSameOrAfter( 12, 10 )) {
			if ( getClauseStack().getCurrent() != Clause.INSERT ) {
				renderTableReferenceIdentificationVariable( tableReference );
			}
		}
	}

	@Override
	protected QueryMutationRenderingSupport getQueryMutationRenderingSupport() {
		return StandardQueryMutationRenderingSupport.MERGE;
	}

	@Override
	protected LockStrategy determineLockingStrategy(QuerySpec querySpec, Locking.FollowOn followOnStrategy) {
		final LockStrategy lockStrategy = super.determineLockingStrategy( querySpec, followOnStrategy );
		final LockingClauseStrategy lockingClauseStrategy = getLockingClauseStrategy();
		if ( lockingClauseStrategy != null && lockingClauseStrategy.containsJoins() ) {
			// Informix does not allow FOR UPDATE when the query also contains joins
			if ( followOnStrategy == Locking.FollowOn.DISALLOW ) {
				throw new IllegalQueryOperationException( "Locking with joins is not supported" );
			}
			else if ( followOnStrategy == Locking.FollowOn.IGNORE ) {
				return LockStrategy.NONE;
			}
			else {
				return LockStrategy.FOLLOW_ON;
			}
		}
		else if ( querySpec.hasSortSpecifications() || hasOffset( querySpec ) || hasLimit( querySpec ) ) {
			// Informix does not allow FOR UPDATE when the query also contains an ORDER BY clause
			if ( followOnStrategy == Locking.FollowOn.DISALLOW ) {
				throw new IllegalQueryOperationException( "Locking with ORDER BY, OFFSET or FETCH is not supported" );
			}
			else if ( followOnStrategy == Locking.FollowOn.IGNORE ) {
				return LockStrategy.NONE;
			}
			else {
				return LockStrategy.FOLLOW_ON;
			}
		}
		else {
			return lockStrategy;
		}
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		// Informix does not like set operators on the top level select for an insert, so wrap it
		if ( getStatementStack().depth() == 1 && getStatement() instanceof InsertSelectStatement ) {
			appendSql( "select * from (" );
			super.visitQueryGroup( queryGroup );
			appendSql( ')' );
		}
		else {
			super.visitQueryGroup( queryGroup );
		}
	}

	@Override
	protected boolean supportsUnionInInsertTopLevelSelect() {
		return false;
	}

	@Override
	protected boolean renderTenantRestriction(ColumnValueBinding binding, String qualifier) {
		if ( binding instanceof TenantIdColumnValueBinding ) {
			binding.getColumnReference().appendReadExpression( this, qualifier );
			appendSql( "=coalesce((select " );
			renderCasted( binding.getValueExpression() );
			appendSql( ")," );
			binding.getColumnReference().appendReadExpression( this, qualifier );
			appendSql( ')' );
			return true;
		}
		return false;
	}

	@Override
	protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
		final String qualifier = super.determineColumnReferenceQualifier( columnReference );
		if ( qualifier == null && columnReference.getQualifier() == null
			&& getTranslationRequest() instanceof SqlAstTranslationRequest.ModelMutation<?> mutation ) {
			// To avoid potentially ambiguous column references, use the table expression as qualifier
			// for model mutations
			return mutation.statement().getTableName();
		}
		else {
			return qualifier;
		}
	}

	@Override
	protected void renderMergeUsingQuery(OptionalTableUpdate optionalTableUpdate) {
		appendSql( "select s.* from (" );
		super.renderMergeUsingQuery( optionalTableUpdate );
		appendSql( ") s" );
		if ( optionalTableUpdate.getNumberOfOptimisticLockBindings() != 0 ) {
			appendSql( " left join " );
			appendSql( optionalTableUpdate.getMutatingTable().getTableName() );
			appendSql( " t " );
			renderMergeOn( optionalTableUpdate );

			appendSql( " where " );
			optionalTableUpdate.getKeyBindings().get( 0 ).getColumnReference().appendReadExpression( this, "t" );
			appendSql( " is null or " );
			String separator = "";
			for ( ColumnValueBinding optimisticLockBinding : optionalTableUpdate.getOptimisticLockBindings() ) {
				appendSql( separator );
				separator = " and ";
				if ( renderTenantRestriction( optimisticLockBinding, "t" ) ) {
					continue;
				}
				optimisticLockBinding.getColumnReference().appendReadExpression( this, "t" );
				appendSql( '=' );
				optimisticLockBinding.getValueExpression().accept( this );
			}
		}

		// There is no need to handle optional tables, since we can't model a delete along with an update in the
		// merge statement on Informix
		assert !optionalTableUpdate.getMutatingTable().isOptional();
//		if ( optionalTableUpdate.getMutatingTable().isOptional() ) {
//			appendSql( separator );
//			separator = "(";
//			for ( ColumnValueBinding valueBinding : optionalTableUpdate.getValueBindings() ) {
//				appendSql( separator );
//				appendSql( "s." );
//				appendSql( valueBinding.getColumnReference().getColumnExpression() );
//				appendSql( " is not null" );
//				separator = " or ";
//			}
//			appendSql( ')' );
//		}
	}

	@Override
	protected void renderMergeUpdate(OptionalTableUpdate optionalTableUpdate) {
		final List<ColumnValueBinding> valueBindings = optionalTableUpdate.getValueBindings();

		if ( valueBindings.stream().anyMatch( ColumnValueBinding::isAttributeUpdatable ) ) {
			appendSql( "when matched then update set" );
			char separatorChar = ' ';
			for ( ColumnValueBinding binding : valueBindings ) {
				if ( binding.isAttributeUpdatable() ) {
					appendSql( separatorChar );
					binding.getColumnReference().appendColumnForWrite( this, null );
					appendSql( '=' );
					binding.getColumnReference().appendColumnForWrite( this, "s" );
					separatorChar = COMMA_SEPARATOR_CHAR;
				}
			}
		}
	}

	@Override
	@SPI(IMPLEMENT)
	protected void renderMergeUpdateClause(List<Assignment> assignments, Predicate wherePredicate) {
		// Don't render the predicate here, but use a case expression in the set assignments instead
		appendSql( " then update" );
		renderSetClause( assignments );
	}

	@Override
	protected void renderAssignmentValue(
			Assignable assignable,
			@Nullable ColumnReference columnReference,
			Expression expression) {
		if ( getStatement() instanceof InsertSelectStatement insertSelectStatement
			&& insertSelectStatement.getConflictClause() != null
			&& insertSelectStatement.getConflictClause().getPredicate() != null ) {
			if ( columnReference != null ) {
				appendSql( "case when " );
				insertSelectStatement.getConflictClause().getPredicate().accept( this );
				appendSql( " then " );
				expression.accept( this );
				appendSql( " else " );
				columnReference.accept( this );
				appendSql( " end" );
			}
			else {
				// Default to this row value constructor rendering, even though Informix doesn't support row values
				appendSql( "case when " );
				insertSelectStatement.getConflictClause().getPredicate().accept( this );
				appendSql( " then " );
				expression.accept( this );
				appendSql( " else " );
				char separator = OPEN_PARENTHESIS;
				for ( ColumnReference reference : assignable.getColumnReferences() ) {
					appendSql( separator );
					renderAssignmentColumn( reference );
					separator = COMMA_SEPARATOR_CHAR;
				}
				appendSql( ") end" );
			}
		}
		else {
			super.renderAssignmentValue( assignable, columnReference, expression );
		}
	}
}
