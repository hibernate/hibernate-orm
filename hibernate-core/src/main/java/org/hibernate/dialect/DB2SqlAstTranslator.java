/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.util.List;

import org.hibernate.query.FetchClauseType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.NullnessLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
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
public class DB2SqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public DB2SqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Percent fetches or ties fetches aren't supported in DB2
		// According to LegacyDB2LimitHandler, variable limit also isn't supported before 11.1
		// Check if current query part is already row numbering to avoid infinite recursion
		return getQueryPartForRowNumbering() != queryPart && (
				useOffsetFetchClause( queryPart ) && !isRowsOnlyFetchClauseType( queryPart )
						|| getDialect().getVersion() < 1110 && ( queryPart.isRoot() && hasLimit() || !( queryPart.getFetchClauseExpression() instanceof Literal ) )
		);
	}

	protected boolean supportsOffsetClause() {
		return getDialect().getVersion() >= 1110;
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		final boolean emulateFetchClause = shouldEmulateFetchClause( queryGroup );
		if ( emulateFetchClause || hasOffset( queryGroup ) && !supportsOffsetClause() ) {
			emulateFetchOffsetWithWindowFunctions( queryGroup, emulateFetchClause );
		}
		else {
			super.visitQueryGroup( queryGroup );
		}
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		final boolean emulateFetchClause = shouldEmulateFetchClause( querySpec );
		if ( emulateFetchClause || hasOffset( querySpec ) && !supportsOffsetClause() ) {
			emulateFetchOffsetWithWindowFunctions( querySpec, emulateFetchClause );
		}
		else {
			super.visitQuerySpec( querySpec );
		}
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			if ( !hasOffset( queryPart ) || supportsOffsetClause() ) {
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
			separator = ", ";
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
		if ( getDialect().getVersion() >= 1110 ) {
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
		emulateTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "()" );
		}
		else if ( expression instanceof Summarization ) {
			Summarization summarization = (Summarization) expression;
			appendSql( summarization.getKind().name().toLowerCase() );
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

}
