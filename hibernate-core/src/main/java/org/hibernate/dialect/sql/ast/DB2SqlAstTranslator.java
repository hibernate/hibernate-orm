/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast;

import java.util.List;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.LockMode;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.JdbcParameterMetadata;
import org.hibernate.sql.ast.spi.SqlAstTranslatorWithMerge;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.internal.TableInsertStandard;
import org.hibernate.sql.model.internal.TableUpdateStandard;
import org.hibernate.type.SqlTypes;

import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * A SQL AST translator for DB2.
 *
 * @author Christian Beikov
 */
public class DB2SqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {

	// We have to track whether we are in a later query for applying lateral during window emulation
	private boolean inLateral;

	@Deprecated(forRemoval = true, since = "7.1")
	public DB2SqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	public DB2SqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, @Nullable JdbcParameterMetadata parameterInfo) {
		super( sessionFactory, statement, parameterInfo );
	}

	@Override
	protected boolean needsRecursiveKeywordInWithClause() {
		return false;
	}

	@Override
	protected void renderTableReferenceJoins(TableGroup tableGroup, LockMode lockMode, int swappedJoinIndex, boolean forceLeftJoin) {
		// When we are in a recursive CTE, we can't render joins on DB2...
		// See https://modern-sql.com/feature/with-recursive/db2/error-345-state-42836
		if ( isInRecursiveQueryPart() ) {
			final List<TableReferenceJoin> joins = tableGroup.getTableReferenceJoins();
			if ( joins == null || joins.isEmpty() ) {
				return;
			}

			for ( TableReferenceJoin tableJoin : joins ) {
				switch ( tableJoin.getJoinType() ) {
					case CROSS:
					case INNER:
						break;
					default:
						throw new UnsupportedOperationException( "Can't emulate '" + tableJoin.getJoinType().getText() + "join' in a DB2 recursive query part" );
				}
				appendSql( COMMA_SEPARATOR_CHAR );

				renderNamedTableReference( tableJoin.getJoinedTableReference(), lockMode );

				if ( tableJoin.getPredicate() != null && !tableJoin.getPredicate().isEmpty() ) {
					addAdditionalWherePredicate( tableJoin.getPredicate() );
				}
			}
		}
		else {
			super.renderTableReferenceJoins( tableGroup, lockMode, swappedJoinIndex, forceLeftJoin );
		}
	}

	@Override
	protected void renderTableGroupJoin(TableGroupJoin tableGroupJoin, List<TableGroupJoin> tableGroupJoinCollector) {
		if ( isInRecursiveQueryPart() ) {
			switch ( tableGroupJoin.getJoinType() ) {
				case CROSS:
				case INNER:
					break;
				default:
					throw new UnsupportedOperationException( "Can't emulate '" + tableGroupJoin.getJoinType().getText() + "join' in a DB2 recursive query part" );
			}
			appendSql( COMMA_SEPARATOR_CHAR );

			renderJoinedTableGroup( tableGroupJoin.getJoinedGroup(), null, tableGroupJoinCollector );
			if ( tableGroupJoin.getPredicate() != null && !tableGroupJoin.getPredicate().isEmpty() ) {
				addAdditionalWherePredicate( tableGroupJoin.getPredicate() );
			}
		}
		else {
			super.renderTableGroupJoin( tableGroupJoin, tableGroupJoinCollector );
		}
	}

	@Override
	protected void renderExpressionAsClauseItem(Expression expression) {
		expression.accept( this );
	}

	@Override
	protected void visitArithmeticOperand(Expression expression) {
		render( expression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
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

	// DB2 does not allow CASE expressions where all result arms contain plain parameters.
	// At least one result arm must provide some type context for inference,
	// so we cast the first result arm if we encounter this condition

	@Override
	protected void visitAnsiCaseSearchedExpression(
			CaseSearchedExpression caseSearchedExpression,
			Consumer<Expression> resultRenderer) {
		if ( getParameterRenderingMode() == SqlAstNodeRenderingMode.DEFAULT && areAllResultsParameters( caseSearchedExpression ) ) {
			final List<CaseSearchedExpression.WhenFragment> whenFragments = caseSearchedExpression.getWhenFragments();
			final Expression firstResult = whenFragments.get( 0 ).getResult();
			super.visitAnsiCaseSearchedExpression(
					caseSearchedExpression,
					e -> {
						if ( e == firstResult ) {
							renderCasted( e );
						}
						else {
							resultRenderer.accept( e );
						}
					}
			);
		}
		else {
			super.visitAnsiCaseSearchedExpression( caseSearchedExpression, resultRenderer );
		}
	}

	@Override
	protected void visitAnsiCaseSimpleExpression(
			CaseSimpleExpression caseSimpleExpression,
			Consumer<Expression> resultRenderer) {
		if ( getParameterRenderingMode() == SqlAstNodeRenderingMode.DEFAULT && areAllResultsParameters( caseSimpleExpression ) ) {
			final List<CaseSimpleExpression.WhenFragment> whenFragments = caseSimpleExpression.getWhenFragments();
			final Expression firstResult = whenFragments.get( 0 ).getResult();
			super.visitAnsiCaseSimpleExpression(
					caseSimpleExpression,
					e -> {
						if ( e == firstResult ) {
							renderCasted( e );
						}
						else {
							resultRenderer.accept( e );
						}
					}
			);
		}
		else {
			super.visitAnsiCaseSimpleExpression( caseSimpleExpression, resultRenderer );
		}
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		if ( getQueryPartForRowNumbering() == queryPart ) {
			return false;
		}
		// Percent fetches or ties fetches aren't supported in DB2
		return useOffsetFetchClause( queryPart ) && !isRowsOnlyFetchClauseType( queryPart );
	}

	protected boolean supportsOffsetClause() {
		return true;
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		final boolean oldLateral = inLateral;
		inLateral = tableReference.isLateral();
		super.visitQueryPartTableReference( tableReference );
		inLateral = oldLateral;
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
	public void renderNamedSetReturningFunction(String functionName, List<? extends SqlAstNode> sqlAstArguments, AnonymousTupleTableGroupProducer tupleType, String tableIdentifierVariable, SqlAstNodeRenderingMode argumentRenderingMode) {
		final ModelPart ordinalitySubPart = tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null );
		if ( ordinalitySubPart != null ) {
			appendSql( "lateral (select t.*, row_number() over() " );
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
	public void visitSelectStatement(SelectStatement statement) {
		if ( getQueryPartForRowNumbering() == statement.getQueryPart() && inLateral ) {
			appendSql( "lateral " );
		}
		super.visitSelectStatement( statement );
	}

	@Override
	protected void emulateFetchOffsetWithWindowFunctionsVisitQueryPart(QueryPart queryPart) {
		if ( inLateral ) {
			appendSql( "lateral " );
			final boolean oldLateral = inLateral;
			inLateral = false;
			super.emulateFetchOffsetWithWindowFunctionsVisitQueryPart( queryPart );
			inLateral = oldLateral;
		}
		else {
			super.emulateFetchOffsetWithWindowFunctionsVisitQueryPart( queryPart );
		}
	}

	private boolean shouldEmulateFetch(QueryPart queryPart) {
		return shouldEmulateFetchClause( queryPart )
			|| getQueryPartForRowNumbering() != queryPart && !supportsOffsetClause() && hasOffset( queryPart );
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		if ( shouldEmulateFetch( queryGroup ) ) {
			emulateFetchOffsetWithWindowFunctions( queryGroup, true );
		}
		else {
			super.visitQueryGroup( queryGroup );
		}
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		if ( shouldEmulateFetch( querySpec ) ) {
			emulateFetchOffsetWithWindowFunctions( querySpec, true );
		}
		else {
			super.visitQuerySpec( querySpec );
		}
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			if ( supportsOffsetClause() || !hasOffset( queryPart ) ) {
				renderOffsetFetchClause( queryPart, true );
			}
			else if ( queryPart.isRoot() && hasLimit() ) {
				renderFetch( getLimitParameter(), null, FetchClauseType.ROWS_ONLY );
			}
			else if ( queryPart.getFetchClauseExpression() != null ) {
				renderFetch( queryPart.getFetchClauseExpression(), null, queryPart.getFetchClauseType() );
			}
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
	protected void renderFetchExpression(Expression fetchExpression) {
		if ( supportsParameterOffsetFetchExpression() ) {
			super.renderFetchExpression( fetchExpression );
		}
		else {
			renderExpressionAsLiteral( fetchExpression, getJdbcParameterBindings() );
		}
	}

	@Override
	protected void visitDeleteStatementOnly(DeleteStatement statement) {
		final boolean closeWrapper = renderReturningClause( statement );
		super.visitDeleteStatementOnly( statement );
		if ( closeWrapper ) {
			appendSql( ')' );
		}
	}

	@Override
	protected void visitUpdateStatementOnly(UpdateStatement statement) {
		final boolean closeWrapper = renderReturningClause( statement );
		if ( supportsFromClauseInUpdate() || !hasNonTrivialFromClause( statement.getFromClause() ) ) {
			super.visitUpdateStatementOnly( statement );
		}
		else {
			if ( closeWrapper ) {
				// Merge statements can't be used in the `from final table( ... )` syntax
				visitUpdateStatementEmulateTupleSet( statement );
			}
			else {
				visitUpdateStatementEmulateMerge( statement );
			}
		}
		if ( closeWrapper ) {
			appendSql( ')' );
		}
	}

	protected boolean supportsFromClauseInUpdate() {
		return getDB2Version().isSameOrAfter( 11 );
	}

	@Override
	protected void visitInsertStatementOnly(InsertSelectStatement statement) {
		final boolean closeWrapper = renderReturningClause( statement );
		if ( statement.getConflictClause() == null || statement.getConflictClause().isDoNothing() ) {
			// Render plain insert statement and possibly run into unique constraint violation
			super.visitInsertStatementOnly( statement );
		}
		else {
			visitInsertStatementEmulateMerge( statement );
		}
		if ( closeWrapper ) {
			appendSql( ')' );
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
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		renderFromClauseExcludingDmlTargetReference( statement );
	}

	protected boolean renderReturningClause(MutationStatement statement) {
		final List<ColumnReference> returningColumns = statement.getReturningColumns();
		if ( isEmpty( returningColumns ) ) {
			return false;
		}
		appendSql( "select " );
		for ( int i = 0; i < returningColumns.size(); i++ ) {
			if ( i > 0 ) {
				appendSql( ", " );
			}
			appendSql( returningColumns.get( i ).getColumnExpression() );
		}
		if ( statement instanceof DeleteStatement ) {
			appendSql( " from old table (" );
		}
		else {
			appendSql( " from ");
			appendSql( getNewTableChangeModifier() );
			appendSql(" table (" );
		}
		return true;
	}

	protected String getNewTableChangeModifier() {
		// Use 'from new table' to also see data from triggers
		// See https://www.ibm.com/docs/en/db2/10.5?topic=clause-table-reference#:~:text=FOR%20sequence%20reference-,FINAL%20TABLE,-Specifies%20that%20the
		return "new";
	}

	@Override
	public void visitStandardTableInsert(TableInsertStandard tableInsert) {
		final List<ColumnReference> returningColumns = tableInsert.getReturningColumns();
		if ( isNotEmpty( returningColumns ) ) {
			appendSql( "select " );

			for ( int i = 0; i < returningColumns.size(); i++ ) {
				if ( i > 0 ) {
					appendSql( ", " );
				}
				appendSql( returningColumns.get( i ).getColumnExpression() );
			}

			appendSql( " from ");
			appendSql( getNewTableChangeModifier() );
			appendSql(" table (" );
			super.visitStandardTableInsert( tableInsert );
			appendSql( ")" );
		}
		else {
			super.visitStandardTableInsert( tableInsert );
		}
	}

	@Override
	public void visitStandardTableUpdate(TableUpdateStandard tableUpdate) {
		final List<ColumnReference> returningColumns = tableUpdate.getReturningColumns();
		if ( isNotEmpty( returningColumns ) ) {
			appendSql( "select " );

			for ( int i = 0; i < returningColumns.size(); i++ ) {
				if ( i > 0 ) {
					appendSql( ", " );
				}
				appendSql( returningColumns.get( i ).getColumnExpression() );
			}

			appendSql( " from final table (" );
			super.visitStandardTableUpdate( tableUpdate );
			appendSql( ")" );
		}
		else {
			super.visitStandardTableUpdate( tableUpdate );
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		if ( getDB2Version().isSameOrAfter( 11, 1 ) ) {
			renderComparisonStandard( lhs, operator, rhs );
		}
		else {
			final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
			if ( lhsExpressionType != null && lhsExpressionType.getJdbcTypeCount() == 1
					&& lhsExpressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() == SqlTypes.SQLXML ) {
				// In DB2, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
				switch ( operator ) {
					case DISTINCT_FROM:
						appendSql( "decode(" );
						appendSql( "xmlserialize(" );
						lhs.accept( this );
						appendSql( " as varchar(32672))" );
						appendSql( ',' );
						appendSql( "xmlserialize(" );
						rhs.accept( this );
						appendSql( " as varchar(32672))" );
						appendSql( ",0,1)=1" );
						return;
					case NOT_DISTINCT_FROM:
						appendSql( "decode(" );
						appendSql( "xmlserialize(" );
						lhs.accept( this );
						appendSql( " as varchar(32672))" );
						appendSql( ',' );
						appendSql( "xmlserialize(" );
						rhs.accept( this );
						appendSql( " as varchar(32672))" );
						appendSql( ",0,1)=0" );
						return;
					case EQUAL:
					case NOT_EQUAL:
						appendSql( "xmlserialize(" );
						lhs.accept( this );
						appendSql( " as varchar(32672))" );
						appendSql( operator.sqlText() );
						appendSql( "xmlserialize(" );
						rhs.accept( this );
						appendSql( " as varchar(32672))" );
						return;
					default:
						// Fall through
						break;
				}
			}
			renderComparisonEmulateDecode( lhs, operator, rhs, SqlAstNodeRenderingMode.NO_UNTYPED );
		}
	}

	@Override
	protected void renderComparisonStandard(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		if ( lhsExpressionType != null && lhsExpressionType.getJdbcTypeCount() == 1
				&& lhsExpressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() == SqlTypes.SQLXML ) {
			// In DB2, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
			switch ( operator ) {
				case EQUAL:
				case NOT_DISTINCT_FROM:
				case NOT_EQUAL:
				case DISTINCT_FROM:
					appendSql( "xmlserialize(" );
					lhs.accept( this );
					appendSql( " as varchar(32672))" );
					appendSql( operator.sqlText() );
					appendSql( "xmlserialize(" );
					rhs.accept( this );
					appendSql( " as varchar(32672))" );
					return;
				default:
					// Fall through
					break;
			}
		}
		super.renderComparisonStandard( lhs, operator, rhs );
	}

	@Override
	protected void renderSelectExpression(Expression expression) {
		renderSelectExpressionWithCastedOrInlinedPlainParameters( expression );
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void visitReturningColumns(List<ColumnReference> returningColumns) {
		// For DB2 we use #renderReturningClause to render a wrapper around the DML statement
	}

	public DatabaseVersion getDB2Version() {
		return this.getDialect().getVersion();
	}

	protected boolean supportsParameterOffsetFetchExpression() {
		return getDB2Version().isSameOrAfter( 11 );
	}

}
