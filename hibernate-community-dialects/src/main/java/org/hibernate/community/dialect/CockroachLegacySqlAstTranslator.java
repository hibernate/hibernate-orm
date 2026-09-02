/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.Internal;
import org.hibernate.Locking;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.dialect.sql.ast.spi.PostgreSQLFamilySqlAstTranslator;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.cte.CteMaterialization;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InArrayPredicate;
import org.hibernate.sql.ast.spi.query.predicate.LikePredicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;

/**
 * A SQL AST translator for Cockroach.
 *
 * @author Christian Beikov
 */
public class CockroachLegacySqlAstTranslator<T extends JdbcOperation> extends PostgreSQLFamilySqlAstTranslator<T> {

	public CockroachLegacySqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		renderIntoIntoAndTable( tableInsert );
		appendSql( "default values" );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			appendSql( "floor" );
		}
		super.visitBinaryArithmeticExpression(arithmeticExpression);
	}

	@Override
	@Internal
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.STANDARD;
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		final Statement currentStatement = getStatementStack().getCurrent();
		if ( !( currentStatement instanceof UpdateStatement )
				|| !hasNonTrivialFromClause( ( (UpdateStatement) currentStatement ).getFromClause() ) ) {
			// For UPDATE statements we render a full FROM clause and a join condition to match target table rows,
			// but for that to work, we have to omit the alias for the target table reference here
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		renderFromClauseJoiningDmlTargetReference( statement );
	}

	@Override
	protected void renderExpressionAsClauseItem(Expression expression) {
		expression.accept( this );
	}

	@Override
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		if ( booleanExpressionPredicate.isNegated() ) {
			super.visitBooleanExpressionPredicate( booleanExpressionPredicate );
		}
		else {
			final boolean isNegated = booleanExpressionPredicate.isNegated();
			if ( isNegated ) {
				appendSql( "not (" );
			}
			booleanExpressionPredicate.getExpression().accept( this );
			if ( isNegated ) {
				appendSql( CLOSE_PARENTHESIS );
			}
		}
	}

	@Override
	protected void renderMaterializationHint(CteMaterialization materialization) {
		if ( getDialect().getVersion().isSameOrAfter( 20, 2 ) ) {
			if ( materialization == CteMaterialization.NOT_MATERIALIZED ) {
				appendSql( "not " );
			}
			appendSql( "materialized " );
		}
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			Locking.FollowOn followOnLocking) {
		// Support was added in 20.1: https://www.cockroachlabs.com/docs/v20.1/select-for-update.html
		if ( getDialect().getVersion().isBefore( 20, 1 ) ) {
			return LockStrategy.NONE;
		}
		return super.determineLockingStrategy( querySpec, followOnLocking );
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> request.fetchClauseType() != null
				&& request.fetchClauseType() != FetchClauseType.ROWS_ONLY
				? new PaginationRenderingPlan.Window( true )
				: new PaginationRenderingPlan.LimitOffset();
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
	public void visitLikePredicate(LikePredicate likePredicate) {
		// Custom implementation because CockroachDB uses backslash as default escape character
		likePredicate.getMatchExpression().accept( this );
		if ( likePredicate.isNegated() ) {
			appendSql( " not" );
		}
		if ( likePredicate.isCaseSensitive() ) {
			appendSql( " like " );
		}
		else {
			appendSql( WHITESPACE );
			appendSql( getDialect().getPredicateSupport().getCaseInsensitiveLikeOperator().orElseThrow() );
			appendSql( WHITESPACE );
		}
		likePredicate.getPattern().accept( this );
		if ( likePredicate.getEscapeCharacter() != null ) {
			appendSql( " escape " );
			likePredicate.getEscapeCharacter().accept( this );
		}
		else {
			appendSql( " escape ''" );
		}
	}

	@Override
	public void visitInArrayPredicate(InArrayPredicate inArrayPredicate) {
		inArrayPredicate.getTestExpression().accept( this );
		appendSql( " = any(" );
		inArrayPredicate.getArrayParameter().accept( this );
		appendSql( ')' );
	}
}
