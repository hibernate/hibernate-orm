/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sql.ast;

import org.hibernate.dialect.sql.ast.PostgreSQLSqlAstTranslator;
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
		} else {
			super.renderSelectExpression(expression);
		}
	}
}
