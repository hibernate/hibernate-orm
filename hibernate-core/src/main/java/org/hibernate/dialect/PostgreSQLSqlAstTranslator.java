/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for PostgreSQL.
 *
 * @author Christian Beikov
 */
public class PostgreSQLSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public PostgreSQLSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
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
	protected void renderMaterializationHint(CteMaterialization materialization) {
		if ( getDialect().getVersion().isSameOrAfter( 12 ) ) {
			if ( materialization == CteMaterialization.NOT_MATERIALIZED ) {
				appendSql( "not " );
			}
			appendSql( "materialized " );
		}
	}

	@Override
	public boolean supportsFilterClause() {
		return getDialect().getVersion().isSameOrAfter( 9, 4 );
	}

	@Override
	protected String getForUpdate() {
		return getDialect().getVersion().isSameOrAfter( 9, 3 ) ? " for no key update" : " for update";
	}

	@Override
	protected String getForShare(int timeoutMillis) {
		// Note that `for key share` is inappropriate as that only means "prevent PK changes"
		return " for share";
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		if ( getQueryPartForRowNumbering() == queryPart || isRowsOnlyFetchClauseType( queryPart ) ) {
			return false;
		}
		return !getDialect().supportsFetchClause( queryPart.getFetchClauseType() );
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

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			if ( getDialect().supportsFetchClause( FetchClauseType.ROWS_ONLY ) ) {
				renderOffsetFetchClause( queryPart, true );
			}
			else {
				renderLimitOffsetClause( queryPart );
			}
		}
	}

	@Override
	protected void renderSearchClause(CteStatement cte) {
		// PostgreSQL does not support this, but it's just a hint anyway
	}

	@Override
	protected void renderCycleClause(CteStatement cte) {
		// PostgreSQL does not support this, but it can be emulated
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		// We render an empty group instead of literals as some DBs don't support grouping by literals
		// Note that integer literals, which refer to select item positions, are handled in #visitGroupByClause
		if ( expression instanceof Literal ) {
			if ( getDialect().getVersion().isSameOrAfter( 9, 5 ) ) {
				appendSql( "()" );
			}
			else {
				appendSql( "(select 1" );
				appendSql( getFromDualForSelectOnly() );
				appendSql( ')' );
			}
		}
		else if ( expression instanceof Summarization ) {
			Summarization summarization = (Summarization) expression;
			if ( getDialect().getVersion().isSameOrAfter( 9, 5 ) ) {
				appendSql( summarization.getKind().sqlText() );
				appendSql( OPEN_PARENTHESIS );
				renderCommaSeparated( summarization.getGroupings() );
				appendSql( CLOSE_PARENTHESIS );
			}
			else {
				// This could theoretically be emulated by rendering all grouping variations of the query and
				// connect them via union all but that's probably pretty inefficient and would have to happen
				// on the query spec level
				throw new UnsupportedOperationException( "Summarization is not supported by DBMS!" );
			}
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		// We need a custom implementation here because PostgreSQL
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
			appendSql( getDialect().getCaseInsensitiveLike() );
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

}
