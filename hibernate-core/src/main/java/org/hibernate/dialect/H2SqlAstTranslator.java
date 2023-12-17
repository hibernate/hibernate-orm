/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.dialect.identity.H2IdentityColumnSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteTableGroup;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.internal.TableInsertStandard;

import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * A SQL AST translator for H2.
 *
 * @author Christian Beikov
 */
public class H2SqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {

	private boolean renderAsArray;

	public H2SqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	public void visitStandardTableInsert(TableInsertStandard tableInsert) {
		if ( isNotEmpty( tableInsert.getReturningColumns() ) ) {
			visitReturningInsertStatement( tableInsert );
		}
		else {
			super.visitStandardTableInsert( tableInsert );
		}
	}

	public void visitReturningInsertStatement(TableInsertStandard tableInsert) {
		assert tableInsert.getReturningColumns() != null
				&& !tableInsert.getReturningColumns().isEmpty();

		//TODO: This is a terrible way to solve this problem, please fix it!
		//      Not every "returning insert" statement has something to do
		//      with identity columns! (Nor is it an elegant implementation.)

		final H2IdentityColumnSupport  identitySupport = (H2IdentityColumnSupport) getSessionFactory()
				.getJdbcServices()
				.getDialect()
				.getIdentityColumnSupport();

		identitySupport.render(
				tableInsert,
				this::appendSql,
				(columnReference) -> columnReference.accept( this ),
				() -> super.visitStandardTableInsert( tableInsert ),
				getSessionFactory()
		);
	}

	@Override
	protected void visitReturningColumns(List<ColumnReference> returningColumns) {
		// do nothing - this is handled via `#visitReturningInsertStatement`
	}

	@Override
	public void visitCteContainer(CteContainer cteContainer) {
		// H2 has various bugs in different versions that make it impossible to use CTEs with parameters reliably
		withParameterRenderingMode(
				SqlAstNodeRenderingMode.INLINE_PARAMETERS,
				() -> super.visitCteContainer( cteContainer )
		);
	}

	@Override
	protected boolean needsCteInlining() {
		// CTEs in H2 are just so buggy, that we can't reliably use them
		return true;
	}

	@Override
	protected boolean shouldInlineCte(TableGroup tableGroup) {
		return tableGroup instanceof CteTableGroup
				&& !getCteStatement( tableGroup.getPrimaryTableReference().getTableId() ).isRecursive();
	}

	@Override
	protected boolean supportsWithClauseInSubquery() {
		return false;
	}

	@Override
	protected boolean supportsRowConstructor() {
		return true;
	}

	@Override
	protected boolean supportsArrayConstructor() {
		return true;
	}

	@Override
	protected String getArrayContainsFunction() {
		return "array_contains";
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
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( isRowsOnlyFetchClauseType( queryPart ) ) {
			if ( supportsOffsetFetchClause() ) {
				renderOffsetFetchClause( queryPart, true );
			}
			else {
				renderLimitOffsetClause( queryPart );
			}
		}
		else {
			if ( supportsOffsetFetchClausePercentWithTies() ) {
				renderOffsetFetchClause( queryPart, true );
			}
			else {
				// FETCH PERCENT and WITH TIES were introduced along with window functions
				throw new IllegalArgumentException( "Can't emulate fetch clause type: " + queryPart.getFetchClauseType() );
			}
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
	protected void visitSqlSelections(SelectClause selectClause) {
		final boolean renderAsArray = this.renderAsArray;
		this.renderAsArray = false;
		if ( renderAsArray ) {
			append( OPEN_PARENTHESIS );
		}
		super.visitSqlSelections( selectClause );
		if ( renderAsArray ) {
			append( CLOSE_PARENTHESIS );
		}
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "'0' || '0'" );
		}
		else if ( expression instanceof Summarization ) {
			// This could theoretically be emulated by rendering all grouping variations of the query and
			// connect them via union all but that's probably pretty inefficient and would have to happen
			// on the query spec level
			throw new UnsupportedOperationException( "Summarization is not supported by DBMS" );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		appendSql( OPEN_PARENTHESIS );
		render( arithmeticExpression.getLeftHandOperand(), SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		appendSql( arithmeticExpression.getOperator().getOperatorSqlTextString() );
		render( arithmeticExpression.getRightHandOperand(), SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		appendSql( CLOSE_PARENTHESIS );
	}

	@Override
	protected boolean renderPrimaryTableReference(TableGroup tableGroup, LockMode lockMode) {
		final TableReference tableRef = tableGroup.getPrimaryTableReference();
		// The H2 parser can't handle a sub-query as first element in a nested join
		// i.e. `join ( (select ...) alias join ... )`, so we have to introduce a dummy table reference
		if ( tableRef instanceof QueryPartTableReference || tableRef.getTableId().startsWith( "(select" ) ) {
			final boolean realTableGroup = tableGroup.isRealTableGroup()
					&& ( isNotEmpty( tableGroup.getTableReferenceJoins() )
					|| hasNestedTableGroupsToRender( tableGroup.getNestedTableGroupJoins() ) );
			if ( realTableGroup ) {
				appendSql( "dual cross join " );
			}
		}
		return super.renderPrimaryTableReference( tableGroup, lockMode );
	}

	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		super.visitLikePredicate( likePredicate );
		// Custom implementation because H2 uses backslash as the default escape character
		// We can override this by specifying an empty escape character
		// See http://www.h2database.com/html/grammar.html#like_predicate_right_hand_side
		if ( likePredicate.getEscapeCharacter() == null ) {
			appendSql( " escape ''" );
		}
	}

	@Override
	protected boolean supportsRowValueConstructorSyntax() {
		// Just a guess
		return true;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInInList() {
		// Just a guess
		return true;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		// Just a guess
		return true;
	}

	@Override
	protected boolean supportsRowValueConstructorDistinctFromSyntax() {
		return true;
	}

	@Override
	protected boolean supportsNullPrecedence() {
		// Support for nulls clause in listagg was added in 2.0
		return true;
	}

	@Override
	protected String getFromDual() {
		return " from dual";
	}


	private boolean supportsOffsetFetchClause() {
		return true;
	}

	private boolean supportsOffsetFetchClausePercentWithTies() {
		// Introduction of TIES clause https://github.com/h2database/h2database/commit/876e9fbe7baf11d01675bfe871aac2cf1b6104ce
		// Introduction of PERCENT support https://github.com/h2database/h2database/commit/f45913302e5f6ad149155a73763c0c59d8205849
		return true;
	}

	@Override
	protected boolean supportsJoinInMutationStatementSubquery() {
		return false;
	}
}
