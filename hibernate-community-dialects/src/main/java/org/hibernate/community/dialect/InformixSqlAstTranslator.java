/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstTranslatorWithMerge;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.internal.TableInsertStandard;

/**
 * A SQL AST translator for Informix.
 *
 * @author Christian Beikov
 */
public class InformixSqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {

	public InformixSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected void visitQueryClauses(QuerySpec querySpec) {
		visitSelectClause( querySpec.getSelectClause() );
		visitFromClause( querySpec.getFromClause() );
		if ( !hasFrom( querySpec.getFromClause() )
				&& hasWhere( querySpec.getWhereClauseRestrictions() ) ) {
			append( " from " );
			append( getDual() );
		}
		visitWhereClause( querySpec.getWhereClauseRestrictions() );
		visitGroupByClause( querySpec, getDialect().getGroupBySelectItemReferenceStrategy() );
		visitHavingClause( querySpec );
		visitOrderBy( querySpec.getSortSpecifications() );
		visitOffsetFetchClause( querySpec );
	}

	@Override
	public void visitSelectClause(SelectClause selectClause) {
		getClauseStack().push( Clause.SELECT );

		try {
			appendSql( "select " );
			visitSqlSelections( selectClause );
			renderVirtualSelections( selectClause );
		}
		finally {
			getClauseStack().pop();
		}

	}

	@Override
	protected void renderSelectExpression(Expression expression) {
		renderSelectExpressionWithCastedOrInlinedPlainParameters( expression );
	}

	@Override
	protected void visitSqlSelections(SelectClause selectClause) {
		final QuerySpec querySpec = (QuerySpec) getQueryPartStack().getCurrent();
		if ( isRowsOnlyFetchClauseType( querySpec ) ) {
			if ( supportsSkipFirstClause() ) {
				renderSkipFirstClause( querySpec );
			}
			else {
				renderFirstClause( querySpec );
			}
		}
		if ( selectClause.isDistinct() ) {
			appendSql( "distinct " );
		}
		super.visitSqlSelections( selectClause );
	}

	@Override
	protected boolean needsRowsToSkip() {
		return !supportsSkipFirstClause();
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
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonEmulateIntersect( lhs, operator, rhs );
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

//	@Override
//	protected void renderNull(Literal literal) {
//		if ( getParameterRenderingMode() == SqlAstNodeRenderingMode.NO_UNTYPED ) {
//			renderCasted( literal );
//		}
//		else {
//			int sqlType = literal.getExpressionType().getSingleJdbcMapping().getJdbcType().getJdbcTypeCode();
//			String nullString = getDialect().getSelectClauseNullString( sqlType, getSessionFactory().getTypeConfiguration() );
//			appendSql( nullString );
//		}
//	}

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
	protected void visitConflictClause(ConflictClause conflictClause) {
		if ( conflictClause != null ) {
			if ( conflictClause.isDoUpdate() && conflictClause.getConstraintName() != null ) {
				throw new IllegalQueryOperationException( "Insert conflict 'do update' clause with constraint name is not supported" );
			}
		}
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
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		emulateValuesTableReferenceColumnAliasing( tableReference );
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		return useOffsetFetchClause( queryPart ) && getQueryPartForRowNumbering() != queryPart
			&& getDialect().supportsWindowFunctions() && !isRowsOnlyFetchClauseType( queryPart );
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
		if ( shouldEmulateFetchClause( querySpec ) ) {
			emulateFetchOffsetWithWindowFunctions( querySpec, true );
		}
		else {
			super.visitQuerySpec( querySpec );
		}
	}

	private void caseArgument(Expression expression) {
		// concatenation inside a case must be cast to varchar(255)
		// or we get a bunch of trailing whitespace
		final boolean concat =
				expression instanceof FunctionExpression fn
						&& fn.getFunctionName().equals( "concat" );
		if ( concat ) {
			append( "cast(" );
		}
		expression.accept( this );
		if ( concat ) {
			append( " as varchar(255))");
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
}
