/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.expression.Every;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.ModifiedSubQueryExpression;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.exec.spi.JdbcOperation;

public class SpannerPostgreSQLSqlAstTranslator<T extends JdbcOperation> extends PostgreSQLSqlAstTranslator<T> {

	public SpannerPostgreSQLSqlAstTranslator(
			SessionFactoryImplementor sessionFactory, Statement statement) {
		super(sessionFactory, statement);
	}

	@Override
	protected void renderMaterializationHint(CteMaterialization materialization) {
		// NO-OP
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		appendSql(tableReference.getTableExpression());
		registerAffectedTable(tableReference);
		// ALWAYS render the alias for the target table since Spanner doesn't support
		// FROM in UPDATE
		final Clause currentClause = getClauseStack().getCurrent();
		if ( currentClause == Clause.UPDATE || currentClause == Clause.DELETE) {
			renderTableReferenceIdentificationVariable(tableReference);
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
	protected void renderLikePredicate(LikePredicate likePredicate) {
		// We need a custom implementation here because Spanner
		// uses the backslash character as default escape character
		if (likePredicate.getEscapeCharacter() == null) {
			renderBackslashEscapedLikePattern( likePredicate.getPattern(), likePredicate.getEscapeCharacter(), true );
		}
		else {
			renderLikePattern( likePredicate.getPattern(), likePredicate.getEscapeCharacter() );
		}
	}

	@Override
	protected void renderLikePattern(Expression pattern, Expression escapeCharacter) {
		if (escapeCharacter == null) {
			super.renderLikePattern( pattern, escapeCharacter );
		}
		else {
			appendSql( "replace(replace(replace(" );
			pattern.accept( this );
			appendSql( ", " );
			escapeCharacter.accept( this );
			appendSql( "||" );
			escapeCharacter.accept( this );
			appendSql( ", '\\\\'), " );
			escapeCharacter.accept( this );
			appendSql( "||'%', '\\%'), " );
			escapeCharacter.accept( this );
			appendSql( "||'_', '\\_')" );
		}
	}

	@Override
	protected void renderEscapeCharacter(Expression escapeCharacter) {
		// Spanner doesn't support passing escape character other than "\"
	}

	@Override
	protected void renderSelectExpression(Expression expression) {
		if (getStatement() instanceof InsertSelectStatement
				&& expression instanceof Literal) {
			renderCasted(expression);
		}
		else {
			super.renderSelectExpression(expression);
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		if ( operator == ComparisonOperator.DISTINCT_FROM || operator == ComparisonOperator.NOT_DISTINCT_FROM ) {
			renderComparisonEmulateCase( lhs, operator, rhs );
			return;
		}
		if ( rhs instanceof Every every ) {
			SelectStatement subquery = every.getSubquery();
			if ( subquery.getQueryPart() instanceof QuerySpec querySpec ) {
				// Emulate ALL
				if ( operator != ComparisonOperator.NOT_EQUAL && operator != ComparisonOperator.NOT_DISTINCT_FROM ) {
					lhs.accept( this );
					appendSql( operator.sqlText() );
					renderQuantifiedEmulationSubQuery( querySpec, operator );
					return;
				}
			}
		}
		else if ( rhs instanceof ModifiedSubQueryExpression expression ) {
			SelectStatement subquery = expression.getSubQuery();
			if ( subquery.getQueryPart() instanceof QuerySpec querySpec ) {
				if ( operator != ComparisonOperator.NOT_EQUAL && operator != ComparisonOperator.NOT_DISTINCT_FROM ) {
					if ( expression.getModifier() == ModifiedSubQueryExpression.Modifier.ALL ) {
						// Emulate ALL
						lhs.accept( this );
						appendSql( operator.sqlText() );
						renderQuantifiedEmulationSubQuery( querySpec, operator );
						return;
					}
					else if ( expression.getModifier() == ModifiedSubQueryExpression.Modifier.ANY || expression.getModifier() == ModifiedSubQueryExpression.Modifier.SOME ) {
						// Emulate ANY
						lhs.accept( this );
						appendSql( operator.sqlText() );
						renderQuantifiedEmulationSubQuery( querySpec, operator.invert() );
						return;
					}
				}
			}
		}
		else if ( rhs instanceof Any any ) {
			SelectStatement subquery = any.getSubquery();
			if ( subquery.getQueryPart() instanceof QuerySpec querySpec ) {
				// Emulate ANY
				if ( operator != ComparisonOperator.NOT_EQUAL && operator != ComparisonOperator.NOT_DISTINCT_FROM ) {
					lhs.accept( this );
					appendSql( operator.sqlText() );
					renderQuantifiedEmulationSubQuery( querySpec, operator.invert() );
					return;
				}
			}
		}
		super.renderComparison(lhs, operator, rhs);
	}

	@Override
	public void visitUnaryOperationExpression(UnaryOperation unaryOperationExpression) {
		// Spanner PostgreSQL doesn't support unary plus, so we just render the operand
		if ( unaryOperationExpression.getOperator() == UnaryArithmeticOperator.UNARY_MINUS ) {
			appendSql( UnaryArithmeticOperator.UNARY_MINUS.getOperatorChar() );
		}
		unaryOperationExpression.getOperand().accept( this );
	}
}
