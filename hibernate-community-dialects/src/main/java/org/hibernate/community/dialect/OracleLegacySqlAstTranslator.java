/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.OracleArrayJdbcType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.common.FrameExclusion;
import org.hibernate.query.common.FrameKind;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.UnionTableGroup;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * A SQL AST translator for Oracle.
 *
 * @author Christian Beikov
 */
public class OracleLegacySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public OracleLegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected void visitInsertStatementOnly(InsertSelectStatement statement) {
		if ( statement.getConflictClause() == null || statement.getConflictClause().isDoNothing() ) {
			// Render plain insert statement and possibly run into unique constraint violation
			super.visitInsertStatementOnly( statement );
		}
		else {
			visitInsertStatementEmulateMerge( statement );
		}
	}

	@Override
	protected void visitUpdateStatementOnly(UpdateStatement statement) {
		if ( hasNonTrivialFromClause( statement.getFromClause() ) ) {
			visitUpdateStatementEmulateInlineView( statement );
		}
		else {
			renderUpdateClause( statement );
			renderSetClause( statement.getAssignments() );
			visitWhereClause( statement.getRestriction() );
			visitReturningColumns( statement.getReturningColumns() );
		}
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
	protected void visitConflictClause(ConflictClause conflictClause) {
		if ( conflictClause != null ) {
			if ( conflictClause.isDoUpdate() && conflictClause.getConstraintName() != null ) {
				throw new IllegalQueryOperationException( "Insert conflict 'do update' clause with constraint name is not supported" );
			}
		}
	}

	@Override
	protected boolean needsRecursiveKeywordInWithClause() {
		return false;
	}

	@Override
	protected boolean supportsWithClauseInSubquery() {
		// Oracle has some limitations, see ORA-32034, so we just report false here for simplicity
		return false;
	}

	@Override
	protected boolean supportsRecursiveSearchClause() {
		return true;
	}

	@Override
	protected boolean supportsRecursiveCycleClause() {
		return true;
	}

	@Override
	public void visitSqlSelection(SqlSelection sqlSelection) {
		if ( getCurrentCteStatement() != null ) {
			if ( getCurrentCteStatement().getMaterialization() == CteMaterialization.MATERIALIZED ) {
				appendSql( "/*+ materialize */ " );
			}
		}
		super.visitSqlSelection( sqlSelection );
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			ForUpdateClause forUpdateClause,
			Boolean followOnLocking) {
		LockStrategy strategy = super.determineLockingStrategy( querySpec, forUpdateClause, followOnLocking );
		final boolean followOnLockingDisabled = Boolean.FALSE.equals( followOnLocking );
		// Oracle also doesn't support locks with set operators
		// See https://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_10002.htm#i2066346
		if ( strategy != LockStrategy.FOLLOW_ON && isPartOfQueryGroup() ) {
			if ( followOnLockingDisabled ) {
				throw new IllegalQueryOperationException( "Locking with set operators is not supported" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		if ( strategy != LockStrategy.FOLLOW_ON && hasSetOperations( querySpec ) ) {
			if ( followOnLockingDisabled ) {
				throw new IllegalQueryOperationException( "Locking with set operators is not supported" );
			}
			strategy = LockStrategy.FOLLOW_ON;
		}
		if ( strategy != LockStrategy.FOLLOW_ON && needsLockingWrapper( querySpec ) && !canApplyLockingWrapper( querySpec ) ) {
			if ( followOnLockingDisabled ) {
				throw new IllegalQueryOperationException( "Locking with OFFSET/FETCH is not supported" );
			}
			strategy = LockStrategy.FOLLOW_ON;
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
	protected boolean supportsNestedSubqueryCorrelation() {
		// It seems it doesn't support it, at least on version 11
		return false;
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		if ( getQueryPartForRowNumbering() == queryPart ) {
			return false;
		}
		final boolean hasLimit = queryPart.isRoot() && hasLimit() || queryPart.getFetchClauseExpression() != null
				|| queryPart.getOffsetClauseExpression() != null;
		if ( !hasLimit ) {
			return false;
		}
		// Even if Oracle supports the OFFSET/FETCH clause, there are conditions where we still want to use the ROWNUM pagination
		if ( supportsOffsetFetchClause() ) {
			// Workaround an Oracle bug, segmentation fault for insert queries with a plain query group and fetch clause
			return queryPart instanceof QueryGroup && getClauseStack().isEmpty() && getStatement() instanceof InsertSelectStatement;
		}
		return true;
	}

	@Override
	protected void visitOrderBy(List<SortSpecification> sortSpecifications) {
		// If we have a query part for row numbering, there is no need to render the order by clause
		// as that is part of the row numbering window function already, by which we then order by in the outer query
		final QueryPart queryPartForRowNumbering = getQueryPartForRowNumbering();
		if ( queryPartForRowNumbering == null ) {
			renderOrderBy( true, sortSpecifications );
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
						renderOrderBy( true, sortSpecifications );
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
						renderOrderBy( true, sortSpecifications );
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
	protected void visitValuesList(List<Values> valuesList) {
		if ( valuesList.size() < 2 ) {
			visitValuesListStandard( valuesList );
		}
		else {
			// Oracle doesn't support a multi-values insert
			// So we render a select union emulation instead
			visitValuesListEmulateSelectUnion( valuesList );
		}
	}

	@Override
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		emulateValuesTableReferenceColumnAliasing( tableReference );
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		emulateQueryPartTableReferenceColumnAliasing( tableReference );
	}

	@Override
	public void visitFunctionTableReference(FunctionTableReference tableReference) {
		tableReference.getFunctionExpression().accept( this );
		if ( !tableReference.rendersIdentifierVariable() ) {
			renderDerivedTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	public void renderNamedSetReturningFunction(String functionName, List<? extends SqlAstNode> sqlAstArguments, AnonymousTupleTableGroupProducer tupleType, String tableIdentifierVariable, SqlAstNodeRenderingMode argumentRenderingMode) {
		final ModelPart ordinalitySubPart = tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null );
		if ( ordinalitySubPart != null ) {
			appendSql( "lateral (select t.*, rownum " );
			appendSql( ordinalitySubPart.asBasicValuedModelPart().getSelectionExpression() );
			appendSql( " from table(" );
			renderSimpleNamedFunction( functionName, sqlAstArguments, argumentRenderingMode );
			append( ") t)" );
		}
		else {
			appendSql( "table(" );
			super.renderNamedSetReturningFunction( functionName, sqlAstArguments, tupleType, tableIdentifierVariable, argumentRenderingMode );
			append( ')' );
		}
	}

	@Override
	protected void renderDerivedTableReference(DerivedTableReference tableReference) {
		if ( tableReference instanceof FunctionTableReference && tableReference.isLateral() ) {
			// No need for a lateral keyword for functions
			tableReference.accept( this );
		}
		else {
			super.renderDerivedTableReference( tableReference );
		}
	}

	@Override
	protected void renderDerivedTableReferenceIdentificationVariable(DerivedTableReference tableReference) {
		renderTableReferenceIdentificationVariable( tableReference );
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		if ( shouldEmulateFetchClause( queryGroup ) ) {
			emulateFetchOffsetWithWindowFunctions( queryGroup, true );
		}
		else {
			super.visitQueryGroup( queryGroup );
		}
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		final EntityIdentifierMapping identifierMappingForLockingWrapper = identifierMappingForLockingWrapper( querySpec );
		final Expression offsetExpression;
		final Expression fetchExpression;
		final FetchClauseType fetchClauseType;
		if ( querySpec.isRoot() && hasLimit() ) {
			prepareLimitOffsetParameters();
			offsetExpression = getOffsetParameter();
			fetchExpression = getLimitParameter();
			fetchClauseType = FetchClauseType.ROWS_ONLY;
		}
		else {
			offsetExpression = querySpec.getOffsetClauseExpression();
			fetchExpression = querySpec.getFetchClauseExpression();
			fetchClauseType = querySpec.getFetchClauseType();
		}
		if ( shouldEmulateFetchClause( querySpec ) ) {
			if ( identifierMappingForLockingWrapper == null ) {
				emulateFetchOffsetWithWindowFunctions(
						querySpec,
						offsetExpression,
						fetchExpression,
						fetchClauseType,
						true
				);
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
		else {
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
			subquery.getSelectClause().addSqlSelection( new SqlSelectionImpl( idColumnReference ) );
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
				final String arrayTypeName = ( (OracleArrayJdbcType) jdbcType ).getSqlTypeName();
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

	@Override
	protected boolean supportsDuplicateSelectItemsInQueryGroup() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntax() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInInList() {
		return getDialect().getVersion().isSameOrAfter( 8, 2 );
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInInSubQuery() {
		return getDialect().getVersion().isSameOrAfter( 9 );
	}

	private boolean supportsOffsetFetchClause() {
		return getDialect().supportsFetchClause( FetchClauseType.ROWS_ONLY );
	}

	@Override
	protected void visitSetAssignment(Assignment assignment) {
		final List<ColumnReference> columnReferences = assignment.getAssignable().getColumnReferences();
		if ( columnReferences.size() == 1 ) {
			columnReferences.get( 0 ).appendColumnForWrite( this );
			appendSql( '=' );
			final Expression assignedValue = assignment.getAssignedValue();
			final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( assignedValue );
			if ( sqlTuple != null ) {
				assert sqlTuple.getExpressions().size() == 1;
				sqlTuple.getExpressions().get( 0 ).accept( this );
			}
			else {
				assignedValue.accept( this );
			}
		}
		else {
			char separator = OPEN_PARENTHESIS;
			for ( ColumnReference columnReference : columnReferences ) {
				appendSql( separator );
				columnReference.appendColumnForWrite( this );
				separator = COMMA_SEPARATOR_CHAR;
			}
			appendSql( ")=" );
			assignment.getAssignedValue().accept( this );
		}
	}
}
