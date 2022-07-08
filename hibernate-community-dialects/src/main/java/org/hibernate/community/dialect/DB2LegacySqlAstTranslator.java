/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.community.dialect;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for DB2.
 *
 * @author Christian Beikov
 */
public class DB2LegacySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public DB2LegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected void renderExpressionAsClauseItem(Expression expression) {
		expression.accept( this );
	}

	@Override
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		if ( getDB2Version().isSameOrAfter( 11 ) ) {
			final boolean isNegated = booleanExpressionPredicate.isNegated();
			if ( isNegated ) {
				appendSql( "not(" );
			}
			booleanExpressionPredicate.getExpression().accept( this );
			if ( isNegated ) {
				appendSql( CLOSE_PARENTHESIS );
			}
		}
		else {
			super.visitBooleanExpressionPredicate( booleanExpressionPredicate );
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

	@Override
	protected String getForUpdate() {
		return " for read only with rs use and keep update locks";
	}

	@Override
	protected String getForShare(int timeoutMillis) {
		return " for read only with rs use and keep share locks";
	}

	@Override
	protected String getSkipLocked() {
		return " skip locked data";
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Percent fetches or ties fetches aren't supported in DB2
		// According to LegacyDB2LimitHandler, variable limit also isn't supported before 11.1
		// Check if current query part is already row numbering to avoid infinite recursion
		return getQueryPartForRowNumbering() != queryPart && (
				useOffsetFetchClause( queryPart ) && !isRowsOnlyFetchClauseType( queryPart )
						|| getDB2Version().isBefore( 11, 1 ) && ( queryPart.isRoot() && hasLimit() || !( queryPart.getFetchClauseExpression() instanceof Literal ) )
		);
	}

	protected boolean supportsOffsetClause() {
		return getDB2Version().isSameOrAfter( 11, 1 );
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		final boolean emulateFetchClause = shouldEmulateFetchClause( queryGroup );
		if ( emulateFetchClause || !supportsOffsetClause() && hasOffset( queryGroup ) ) {
			emulateFetchOffsetWithWindowFunctions( queryGroup, emulateFetchClause );
		}
		else {
			super.visitQueryGroup( queryGroup );
		}
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		final boolean emulateFetchClause = shouldEmulateFetchClause( querySpec );
		if ( emulateFetchClause || !supportsOffsetClause() && hasOffset( querySpec ) ) {
			emulateFetchOffsetWithWindowFunctions( querySpec, emulateFetchClause );
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
		super.visitUpdateStatementOnly( statement );
		if ( closeWrapper ) {
			appendSql( ')' );
		}
	}

	@Override
	protected void visitInsertStatementOnly(InsertStatement statement) {
		final boolean closeWrapper = renderReturningClause( statement );
		super.visitInsertStatementOnly( statement );
		if ( closeWrapper ) {
			appendSql( ')' );
		}
	}

	protected boolean renderReturningClause(MutationStatement statement) {
		final List<ColumnReference> returningColumns = statement.getReturningColumns();
		final int size = returningColumns.size();
		if ( size == 0 ) {
			return false;
		}
		appendSql( "select " );
		String separator = "";
		for ( int i = 0; i < size; i++ ) {
			appendSql( separator );
			appendSql( returningColumns.get( i ).getColumnExpression() );
			separator = ",";
		}
		if ( statement instanceof DeleteStatement ) {
			appendSql( " from old table (" );
		}
		else {
			appendSql( " from final table (" );
		}
		return true;
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		if ( getDB2Version().isSameOrAfter( 11, 1 ) ) {
			renderComparisonStandard( lhs, operator, rhs );
		}
		else {
			renderComparisonEmulateDecode( lhs, operator, rhs );
		}
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
	protected boolean supportsRowValueConstructorSyntax() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	protected String getFromDual() {
		return " from sysibm.dual";
	}

	@Override
	protected String getFromDualForSelectOnly() {
		return getFromDual();
	}

	@Override
	protected void visitReturningColumns(MutationStatement mutationStatement) {
		// For DB2 we use #renderReturningClause to render a wrapper around the DML statement
	}

	public DatabaseVersion getDB2Version() {
		return this.getDialect().getVersion();
	}

}
