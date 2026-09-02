/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.Internal;
import org.hibernate.SPI;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SelectItemReferenceStrategy;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.dialect.sql.ast.spi.PostgreSQLFamilySqlAstTranslator;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.cte.CteMaterialization;
import org.hibernate.sql.ast.spi.query.cte.CteStatement;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InArrayPredicate;
import org.hibernate.sql.ast.spi.query.predicate.LikePredicate;
import org.hibernate.sql.ast.spi.query.predicate.NullnessPredicate;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.type.SqlTypes;

/**
 * A SQL AST translator for GaussDB.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLSqlAstTranslator.
 */
public class GaussDBSqlAstTranslator<T extends JdbcOperation> extends PostgreSQLFamilySqlAstTranslator<T> {

	public GaussDBSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	@SPI({ SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
	protected SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.POSITION;
	}

	@Override
	public void visitInArrayPredicate(InArrayPredicate inArrayPredicate) {
		inArrayPredicate.getTestExpression().accept( this );
		appendSql( " = any (" );
		inArrayPredicate.getArrayParameter().accept( this );
		appendSql( ")" );
	}

	@Override
	protected String getArrayContainsFunction() {
		return super.getArrayContainsFunction();
	}

	@Override
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		renderIntoIntoAndTable( tableInsert );
		appendSql( "default values" );
	}

	@Override
	@Internal
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_NOTHING;
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		final Statement currentStatement = getStatementStack().getCurrent();
		if ( !( currentStatement instanceof UpdateStatement updateStatement )
			|| !hasNonTrivialFromClause( updateStatement.getFromClause() ) ) {
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
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		if ( lhsExpressionType != null && lhsExpressionType.getJdbcTypeCount() == 1
				&& lhsExpressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() == SqlTypes.SQLXML ) {
			// In GaussDB, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
			switch ( operator ) {
				case EQUAL:
				case NOT_DISTINCT_FROM:
				case NOT_EQUAL:
				case DISTINCT_FROM:
					appendSql( "cast(" );
					lhs.accept( this );
					appendSql( " as text)" );
					appendSql( operator.sqlText() );
					appendSql( "cast(" );
					rhs.accept( this );
					appendSql( " as text)" );
					return;
				default:
					// Fall through
					break;
			}
		}
		renderComparisonStandard( lhs, operator, rhs );
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
	public void visitNullnessPredicate(NullnessPredicate nullnessPredicate) {
		final Expression expression = nullnessPredicate.getExpression();
		final JdbcMappingContainer expressionType = expression.getExpressionType();
		if ( isStruct( expressionType ) ) {
			// Surprise, the null predicate checks if all components of the struct are null or not,
			// rather than the column itself, so we have to use the distinct from predicate to implement this instead
			expression.accept( this );
			if ( nullnessPredicate.isNegated() ) {
				appendSql( " is distinct from null" );
			}
			else {
				appendSql( " is not distinct from null" );
			}
		}
		else {
			super.visitNullnessPredicate( nullnessPredicate );
		}
	}

	@Override
	protected void renderMaterializationHint(CteMaterialization materialization) {
		if ( materialization == CteMaterialization.NOT_MATERIALIZED ) {
			appendSql( "not " );
		}
		appendSql( "materialized " );
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> request.fetchClauseType() != null
				&& request.fetchClauseType() != FetchClauseType.ROWS_ONLY
				&& !getDialect().getFetchClauseSupport().supports( request.fetchClauseType() )
				? new PaginationRenderingPlan.Window( true )
				: getDialect().getFetchClauseSupport().supports( FetchClauseType.ROWS_ONLY )
						? new PaginationRenderingPlan.OffsetFetch( true )
						: new PaginationRenderingPlan.LimitOffset();
	}

	@Override
	protected void renderCycleUsingClause(CteStatement cte) {
		if ( cte.getCyclePathColumn() == null ) {
			appendSql( " using " );
			appendSql( determineCyclePathColumnName( cte ) );
		}
		else {
			super.renderCycleUsingClause( cte );
		}
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		// We render an empty group instead of literals as some DBs don't support grouping by literals
		// Note that integer literals, which refer to select item positions, are handled in #visitGroupByClause
		if ( expression instanceof Literal ) {
			appendSql( "()" );
		}
		else if ( expression instanceof Summarization summarization ) {
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
	public void visitLikePredicate(LikePredicate likePredicate) {
		// We need a custom implementation here because GaussDB
		// uses the backslash character as default escape character
		// According to the documentation, we can overcome this by specifying an empty escape character
		// See https://www.postgresql.org/docs/current/functions-matching.html#FUNCTIONS-LIKE
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
			appendSql( " escape ''''" );
		}
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			appendSql( "floor" );
		}
		appendSql( OPEN_PARENTHESIS );
		visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
		appendSql( arithmeticExpression.getOperator().getOperatorSqlTextString() );
		visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
		appendSql( CLOSE_PARENTHESIS );
	}
}
