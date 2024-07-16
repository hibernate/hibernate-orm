/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
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
public class InformixSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public InformixSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
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
	protected void visitSqlSelections(SelectClause selectClause) {
		if ( supportsSkipFirstClause() ) {
			renderSkipFirstClause( (QuerySpec) getQueryPartStack().getCurrent() );
		}
		else {
			renderFirstClause( (QuerySpec) getQueryPartStack().getCurrent() );
		}
		if ( selectClause.isDistinct() ) {
			appendSql( "distinct " );
		}
		super.visitSqlSelections( selectClause );
	}

	@Override
	protected void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression, boolean inSelect) {
		if ( inSelect ) {
			withParameterRenderingMode(
					SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS,
					() -> super.visitCaseSearchedExpression( caseSearchedExpression, inSelect )
			);
		}
		else {
			super.visitCaseSearchedExpression( caseSearchedExpression, inSelect );
		}
	}

	@Override
	protected void renderSelectExpression(Expression expression) {
		renderSelectExpressionWithCastedOrInlinedPlainParameters( expression );
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
	protected String getDual() {
		return "(select 0 from systables where tabid=1)";
	}

	@Override
	protected String getFromDualForSelectOnly() {
		return " from " + getDual() + " dual";
	}

	@Override
	protected void renderNull(Literal literal) {
		if ( getParameterRenderingMode() == SqlAstNodeRenderingMode.NO_UNTYPED ) {
			renderCasted( literal );
		}
		else {
			int sqlType = literal.getExpressionType().getSingleJdbcMapping().getJdbcType().getJdbcTypeCode();
			String nullString = getDialect().getSelectClauseNullString( sqlType, getSessionFactory().getTypeConfiguration() );
			appendSql( nullString );
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
}
