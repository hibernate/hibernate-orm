/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.Locking;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardQueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardSetReturningFunctionRenderingSupport;
import org.hibernate.spi.Stack;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.common.FrameExclusion;
import org.hibernate.query.common.FrameKind;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.sql.ast.spi.query.cte.CteMaterialization;
import org.hibernate.sql.ast.spi.query.cte.CteStatement;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.FunctionExpression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.Over;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.SqlTupleContainer;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.UnionTableGroup;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.query.update.Assignment;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * A SQL AST translator for Oracle.
 *
 * @author Christian Beikov
 */
public class OracleLegacySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public OracleLegacySqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	@Internal
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.MERGE;
	}

	@Override
	protected QueryMutationRenderingSupport getQueryMutationRenderingSupport() {
		return StandardQueryMutationRenderingSupport.INLINE_VIEW;
	}

	@Override
	protected void renderMergeUpdateClause(List<Assignment> assignments, Predicate wherePredicate) {
		appendSql( " then update" );
		renderSetClause( assignments );
		visitWhereClause( wherePredicate );
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected void renderCteSelectHint(CteStatement cte) {
		if ( cte.getMaterialization() == CteMaterialization.MATERIALIZED ) {
			appendSql( "/*+ materialize */ " );
		}
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			Locking.FollowOn followOnStrategy) {
		if ( followOnStrategy == Locking.FollowOn.FORCE ) {
			return LockStrategy.FOLLOW_ON;
		}

		LockStrategy strategy = super.determineLockingStrategy( querySpec, followOnStrategy );

		// Oracle also doesn't support locks with set operators
		// See https://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_10002.htm#i2066346
		if ( strategy != LockStrategy.FOLLOW_ON && isPartOfQueryGroup() ) {
			if ( followOnStrategy == Locking.FollowOn.DISALLOW ) {
				throw new IllegalQueryOperationException( "Locking with set operators is not supported" );
			}
			else if ( followOnStrategy != Locking.FollowOn.IGNORE ) {
				strategy = LockStrategy.NONE;
			}
			else {
				strategy = LockStrategy.FOLLOW_ON;
			}
		}

		if ( strategy != LockStrategy.FOLLOW_ON && hasSetOperations( querySpec ) ) {
			if ( followOnStrategy == Locking.FollowOn.DISALLOW ) {
				throw new IllegalQueryOperationException( "Locking with set operators is not supported" );
			}
			else if ( followOnStrategy != Locking.FollowOn.IGNORE ) {
				strategy = LockStrategy.NONE;
			}
			else {
				strategy = LockStrategy.FOLLOW_ON;
			}
		}

		if ( strategy != LockStrategy.FOLLOW_ON && needsLockingWrapper( querySpec ) && !canApplyLockingWrapper( querySpec ) ) {
			if ( followOnStrategy == Locking.FollowOn.DISALLOW ) {
				throw new IllegalQueryOperationException( "Locking with OFFSET/FETCH is not supported" );
			}
			else if ( followOnStrategy != Locking.FollowOn.IGNORE ) {
				strategy = LockStrategy.NONE;
			}
			else {
				strategy = LockStrategy.FOLLOW_ON;
			}
		}

		return strategy;
	}

	private boolean hasSetOperations(QuerySpec querySpec) {
		return querySpec.getFromClause().queryTableGroups( group -> group instanceof UnionTableGroup ? group : null ) != null;
	}

	private boolean isPartOfQueryGroup() {
		return getQueryPartStack().findCurrentFirst( part -> part instanceof QueryGroup ? part : null ) != null;
	}

	@Override
	protected boolean shouldEmulateLateralWithIntersect(QueryPart queryPart) {
		// On Oracle 11 where there is no lateral support,
		// make sure we don't use intersect if the query has an offset/fetch clause
		return !queryPart.hasOffsetOrFetchClause();
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> {
			if ( !request.hasOffset() && !request.hasFetch() ) {
				return new PaginationRenderingPlan.OffsetFetch( true );
			}
			if ( !supportsOffsetFetchClause()
					// Work around an Oracle segmentation fault for insert queries with a plain query group and fetch clause.
					|| request.queryPart() instanceof QueryGroup
							&& getClauseStack().isEmpty()
							&& getStatement() instanceof InsertSelectStatement ) {
				return new PaginationRenderingPlan.Window( true );
			}
			return new PaginationRenderingPlan.OffsetFetch( true );
		};
	}

	@Override
	protected void visitOrderBy(List<SortSpecification> sortSpecifications) {
		// If we have a query part for row numbering, there is no need to render the order by clause
		// as that is part of the row numbering window function already, by which we then order by in the outer query
		final QueryPart queryPartForRowNumbering = getQueryPartForRowNumbering();
		if ( queryPartForRowNumbering == null ) {
			renderOrderByClause( sortSpecifications );
		}
		else {
			// This logic is tightly coupled to #emulateFetchOffsetWithWindowFunctions and #getFetchClauseTypeForRowNumbering
			// so that this is rendered when we end up in the special case for Oracle that renders a rownum filter
			if ( getFetchClauseTypeForRowNumbering( queryPartForRowNumbering ) == null ) {
				final QuerySpec querySpec = (QuerySpec) queryPartForRowNumbering;
				if ( querySpec.getOffsetClauseExpression() == null
						&& ( !querySpec.isRoot() || getOffsetParameter() == null ) ) {
					// When we enter here, we need to handle the special ROWNUM pagination
					if ( hasGroupingOrDistinct( querySpec ) || querySpec.getFromClause().hasJoins() ) {
						// When the query spec has joins, a group by, having or distinct clause,
						// we just need to render the order by clause, because the query is wrapped
						renderOrderByClause( sortSpecifications );
					}
					else {
						// Otherwise we need to render the ROWNUM pagination predicate in here
						final Predicate whereClauseRestrictions = querySpec.getWhereClauseRestrictions();
						if ( whereClauseRestrictions != null && !whereClauseRestrictions.isEmpty() ) {
							appendSql( " and " );
						}
						else {
							appendSql( " where " );
						}
						appendSql( "rownum<=" );
						final Stack<Clause> clauseStack = getClauseStack();
						clauseStack.push( Clause.WHERE );
						try {
							if ( querySpec.isRoot() && hasLimit() ) {
								getLimitParameter().accept( this );
							}
							else {
								querySpec.getFetchClauseExpression().accept( this );
							}
						}
						finally {
							clauseStack.pop();
						}
						renderOrderByClause( sortSpecifications );
						visitForUpdateClause( querySpec );
					}
				}
			}
		}
	}

	private boolean hasGroupingOrDistinct(QuerySpec querySpec) {
		return querySpec.getSelectClause().isDistinct()
				|| !querySpec.getGroupByClauseExpressions().isEmpty()
				|| querySpec.getHavingClauseRestrictions() != null;
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.ORACLE;
	}

	@Override
	protected SetReturningFunctionRenderingSupport getSetReturningFunctionRenderingSupport() {
		return StandardSetReturningFunctionRenderingSupport.ORACLE;
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		final EntityIdentifierMapping identifierMappingForLockingWrapper = identifierMappingForLockingWrapper( querySpec );
		final Expression offsetExpression;
		final Expression fetchExpression;
		final FetchClauseType fetchClauseType;
		if ( querySpec.isRoot() && hasLimit() ) {
			offsetExpression = getOffsetParameter();
			fetchExpression = getLimitParameter();
			fetchClauseType = FetchClauseType.ROWS_ONLY;
		}
		else {
			offsetExpression = querySpec.getOffsetClauseExpression();
			fetchExpression = querySpec.getFetchClauseExpression();
			fetchClauseType = querySpec.getFetchClauseType();
		}
		if ( identifierMappingForLockingWrapper == null ) {
			super.visitQuerySpec( querySpec );
		}
		else {
			super.visitQuerySpec(
					createLockingWrapper(
							querySpec,
							offsetExpression,
							fetchExpression,
							fetchClauseType,
							identifierMappingForLockingWrapper
					)
			);
			// Render the for update clause for the original query spec, because the locking wrapper is marked as non-root
			visitForUpdateClause( querySpec );
		}
	}

	private QuerySpec createLockingWrapper(
			QuerySpec querySpec,
			Expression offsetExpression,
			Expression fetchExpression,
			FetchClauseType fetchClauseType,
			EntityIdentifierMapping identifierMappingForLockingWrapper) {

		final TableGroup rootTableGroup = querySpec.getFromClause().getRoots().get( 0 );
		final List<ColumnReference> idColumnReferences = new ArrayList<>( identifierMappingForLockingWrapper.getJdbcTypeCount() );
		identifierMappingForLockingWrapper.forEachSelectable(
				0,
				(selectionIndex, selectableMapping) -> {
					idColumnReferences.add( new ColumnReference( rootTableGroup.getPrimaryTableReference(), selectableMapping ) );
				}
		);
		final Expression idExpression;
		if ( identifierMappingForLockingWrapper instanceof EmbeddableValuedModelPart ) {
			idExpression = new SqlTuple( idColumnReferences, identifierMappingForLockingWrapper );
		}
		else {
			idExpression = idColumnReferences.get( 0 );
		}
		final QuerySpec subquery = new QuerySpec( false, 1 );
		for ( ColumnReference idColumnReference : idColumnReferences ) {
			subquery.getSelectClause().addSqlSelection( idColumnReference );
		}
		subquery.getFromClause().addRoot( rootTableGroup );
		subquery.applyPredicate( querySpec.getWhereClauseRestrictions() );
		if ( querySpec.hasSortSpecifications() ) {
			for ( SortSpecification sortSpecification : querySpec.getSortSpecifications() ) {
				subquery.addSortSpecification( sortSpecification );
			}
		}
		subquery.setOffsetClauseExpression( offsetExpression );
		subquery.setFetchClauseExpression( fetchExpression, fetchClauseType );

		// Mark the query spec as non-root even if it might be the root, to avoid applying the pagination there
		final QuerySpec lockingWrapper = new QuerySpec( false, 1 );
		lockingWrapper.getFromClause().addRoot( rootTableGroup );
		for ( SqlSelection sqlSelection : querySpec.getSelectClause().getSqlSelections() ) {
			lockingWrapper.getSelectClause().addSqlSelection( sqlSelection );
		}
		lockingWrapper.applyPredicate( new InSubQueryPredicate( idExpression, subquery, false ) );
		return lockingWrapper;
	}

	private EntityIdentifierMapping identifierMappingForLockingWrapper(QuerySpec querySpec) {
		// We only need a locking wrapper for very simple queries
		if ( canApplyLockingWrapper( querySpec )
				// There must be the need for locking in this query
				&& needsLocking( querySpec )
				// The query uses some sort of pagination which makes the wrapper necessary
				&& needsLockingWrapper( querySpec )
				// The query may not have a group by, having and distinct clause, or use aggregate functions,
				// as these features will force the use of follow-on locking
				&& querySpec.getGroupByClauseExpressions().isEmpty()
				&& querySpec.getHavingClauseRestrictions() == null
				&& !querySpec.getSelectClause().isDistinct()
				&& !hasAggregateFunctions( querySpec ) ) {
			return ( (EntityMappingType) querySpec.getFromClause().getRoots().get( 0 ).getModelPart() ).getIdentifierMapping();
		}
		return null;
	}

	private boolean canApplyLockingWrapper(QuerySpec querySpec) {
		final FromClause fromClause;
		return querySpec.isRoot()
				// Must have a single root with no joins for an entity type
				&& ( fromClause = querySpec.getFromClause() ).getRoots().size() == 1
				&& !fromClause.hasJoins()
				&& fromClause.getRoots().get( 0 ).getModelPart() instanceof EntityMappingType;
	}

	private boolean needsLockingWrapper(QuerySpec querySpec) {
		return querySpec.getFetchClauseType() != FetchClauseType.ROWS_ONLY
				|| hasOffset( querySpec )
				|| hasLimit( querySpec );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			if ( supportsOffsetFetchClause() ) {
				if ( getQueryPartStack().depth() > 1 && queryPart.hasSortSpecifications()
						&& getQueryPartStack().peek( 1 ) instanceof QueryGroup
						&& ( queryPart.isRoot() && !hasLimit() || !queryPart.hasOffsetOrFetchClause() ) ) {
					// If the current query part has a query group parent, no offset/fetch clause, but an order by clause,
					// then we must render "offset 0 rows" as that is needed for the SQL to be valid
					appendSql( " offset 0 rows" );
				}
				else {
					renderOffsetFetchClause( queryPart, true );
				}
			}
			else {
				assertRowsOnlyFetchClauseType( queryPart );
			}
		}
	}

	@Override
	protected void renderRowNumber(SelectClause selectClause, QueryPart queryPart) {
		if ( !queryPart.hasSortSpecifications() ) {
			// Oracle doesn't allow an empty over clause for the row_number() function
			// For regular window function usage, we render a constant order by,
			// but since this is used for emulating limit/offset anyway, this is fine
			appendSql( "rownum" );
		}
		else {
			super.renderRowNumber( selectClause, queryPart );
		}
	}

	@Override
	public void visitOver(Over<?> over) {
		final Expression expression = over.getExpression();
		if ( expression instanceof FunctionExpression && "row_number".equals( ( (FunctionExpression) expression ).getFunctionName() ) ) {
			if ( over.getPartitions().isEmpty() && over.getOrderList().isEmpty()
					&& over.getStartKind() == FrameKind.UNBOUNDED_PRECEDING
					&& over.getEndKind() == FrameKind.CURRENT_ROW
					&& over.getExclusion() == FrameExclusion.NO_OTHERS ) {
				// Oracle doesn't allow an empty over clause for the row_number() function,
				// so we order by a constant
				append( "row_number() over(order by 1)" );
				return;
			}
		}
		super.visitOver( over );
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		if ( lhsExpressionType == null || lhsExpressionType.getJdbcTypeCount() != 1 ) {
			renderComparisonEmulateDecode( lhs, operator, rhs );
			return;
		}
		final JdbcType jdbcType = lhsExpressionType.getSingleJdbcMapping().getJdbcType();
		switch ( jdbcType.getDdlTypeCode() ) {
			case SqlTypes.SQLXML:
				// In Oracle, XMLTYPE is not "comparable", so we have to use the xmldiff function for this purpose
				switch ( operator ) {
					case EQUAL:
					case NOT_DISTINCT_FROM:
						appendSql( "0=" );
						break;
					case NOT_EQUAL:
					case DISTINCT_FROM:
						appendSql( "1=" );
						break;
					default:
						renderComparisonEmulateDecode( lhs, operator, rhs );
						return;
				}
				appendSql( "existsnode(xmldiff(" );
				lhs.accept( this );
				appendSql( ',' );
				rhs.accept( this );
				appendSql( "),'/*[local-name()=''xdiff'']/*')" );
				break;
			case SqlTypes.CLOB:
			case SqlTypes.NCLOB:
			case SqlTypes.BLOB:
				// In Oracle, BLOB, CLOB and NCLOB types are not "comparable",
				// so we have to use the dbms_lob.compare function for this purpose
				switch ( operator ) {
					case EQUAL:
						appendSql( "0=" );
						break;
					case NOT_EQUAL:
						appendSql( "-1=" );
						break;
					default:
						renderComparisonEmulateDecode( lhs, operator, rhs );
						return;
				}
				appendSql( "dbms_lob.compare(" );
				lhs.accept( this );
				appendSql( ',' );
				rhs.accept( this );
				appendSql( ')' );
				break;
			case SqlTypes.ARRAY:
				final String arrayTypeName = ( (SqlTypedJdbcType) jdbcType ).getSqlTypeName();
				switch ( operator ) {
					case DISTINCT_FROM:
					case NOT_DISTINCT_FROM:
						appendSql( arrayTypeName );
						appendSql( "_distinct(" );
						visitSqlSelectExpression( lhs );
						appendSql( ',' );
						visitSqlSelectExpression( rhs );
						appendSql( ")" );
						break;
					default:
						appendSql( arrayTypeName );
						appendSql( "_cmp(" );
						visitSqlSelectExpression( lhs );
						appendSql( ',' );
						visitSqlSelectExpression( rhs );
						appendSql( ")" );
						break;
				}
				switch ( operator ) {
					case DISTINCT_FROM:
						appendSql( "=1" );
						break;
					case NOT_DISTINCT_FROM:
						appendSql( "=0" );
						break;
					case EQUAL:
						appendSql( "=0" );
						break;
					case NOT_EQUAL:
						appendSql( "<>0" );
						break;
					case LESS_THAN:
						appendSql( "=-1" );
						break;
					case GREATER_THAN:
						appendSql( "=1" );
						break;
					case LESS_THAN_OR_EQUAL:
						appendSql( "<=0" );
						break;
					case GREATER_THAN_OR_EQUAL:
						appendSql( ">=0" );
						break;
				}
				break;
			default:
				renderComparisonEmulateDecode( lhs, operator, rhs );
				break;
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
	protected void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression, boolean inSelect) {
		// Oracle did not add support for CASE until 9i
		if ( getDialect().getVersion().isBefore( 9 ) ) {
			visitDecodeCaseSearchedExpression( caseSearchedExpression );
		}
		else {
			super.visitCaseSearchedExpression( caseSearchedExpression, inSelect );
		}
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "()" );
		}
		else if ( expression instanceof Summarization ) {
			Summarization summarization = (Summarization) expression;
			appendSql( summarization.getKind().sqlText() );
			appendSql( OPEN_PARENTHESIS );
			renderCommaSeparated( summarization.getGroupings() );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			expression.accept( this );
		}
	}

	private boolean supportsOffsetFetchClause() {
		return getDialect().getFetchClauseSupport().supports( FetchClauseType.ROWS_ONLY );
	}

	@Override
	protected void renderNull(Literal literal) {
		if ( getParameterRenderingMode() == SqlAstNodeRenderingMode.NO_UNTYPED ) {
			switch ( literal.getJdbcMapping().getJdbcType().getDdlTypeCode() ) {
				case SqlTypes.BLOB:
					appendSql( "to_blob(null)" );
					break;
				case SqlTypes.CLOB:
					appendSql( "to_clob(null)" );
					break;
				case SqlTypes.NCLOB:
					appendSql( "to_nclob(null)" );
					break;
				default:
					super.renderNull( literal );
					break;
			}
		}
		else {
			super.renderNull( literal );
		}
	}

	@Override
	protected void renderSetAssignment(Assignment assignment) {
		final List<ColumnReference> columnReferences = assignment.getAssignable().getColumnReferences();
		final Expression assignedValue = assignment.getAssignedValue();
		if ( columnReferences.size() == 1 ) {
			columnReferences.get( 0 ).appendColumnForWrite( this );
			appendSql( '=' );
			final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( assignedValue );
			if ( sqlTuple != null ) {
				assert sqlTuple.getExpressions().size() == 1;
				sqlTuple.getExpressions().get( 0 ).accept( this );
			}
			else {
				assignedValue.accept( this );
			}
		}
		else if ( assignedValue instanceof SelectStatement ) {
			char separator = OPEN_PARENTHESIS;
			for ( ColumnReference columnReference : columnReferences ) {
				appendSql( separator );
				columnReference.appendColumnForWrite( this );
				separator = COMMA_SEPARATOR_CHAR;
			}
			appendSql( ")=" );
			assignment.getAssignedValue().accept( this );
		}
		else {
			assert assignedValue instanceof SqlTupleContainer;
			final List<? extends Expression> expressions = ( (SqlTupleContainer) assignedValue ).getSqlTuple().getExpressions();
			columnReferences.get( 0 ).appendColumnForWrite( this, null );
			appendSql( '=' );
			expressions.get( 0 ).accept( this );
			for ( int i = 1; i < columnReferences.size(); i++ ) {
				appendSql( ',' );
				columnReferences.get( i ).appendColumnForWrite( this, null );
				appendSql( '=' );
				expressions.get( i ).accept( this );
			}
		}
	}
}
