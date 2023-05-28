/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.community.dialect;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.InArrayPredicate;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Cockroach.
 *
 * @author Christian Beikov
 */
public class CockroachLegacySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public CockroachLegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
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
	protected boolean supportsRowConstructor() {
		return true;
	}

	@Override
	protected boolean supportsArrayConstructor() {
		return true;
	}

	@Override
	protected String getForShare(int timeoutMillis) {
		return " for share";
	}

	@Override
	protected String getForUpdate() {
		return getDialect().getVersion().isBefore( 20, 1 ) ? "" : " for update";
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			ForUpdateClause forUpdateClause,
			Boolean followOnLocking) {
		// Support was added in 20.1: https://www.cockroachlabs.com/docs/v20.1/select-for-update.html
		if ( getDialect().getVersion().isBefore( 20, 1 ) ) {
			return LockStrategy.NONE;
		}
		return super.determineLockingStrategy( querySpec, forUpdateClause, followOnLocking );
	}

	@Override
	protected void renderForUpdateClause(QuerySpec querySpec, ForUpdateClause forUpdateClause) {
		// Support was added in 20.1: https://www.cockroachlabs.com/docs/v20.1/select-for-update.html
		if ( getDialect().getVersion().isBefore( 20, 1 ) ) {
			return;
		}
		super.renderForUpdateClause( querySpec, forUpdateClause );
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		return useOffsetFetchClause( queryPart ) && getQueryPartForRowNumbering() != queryPart
				&& !isRowsOnlyFetchClauseType( queryPart );
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
			renderLimitOffsetClause( queryPart );
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

	@Override
	protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	public void visitInArrayPredicate(InArrayPredicate inArrayPredicate) {
		inArrayPredicate.getTestExpression().accept( this );
		appendSql( " = any(" );
		inArrayPredicate.getArrayParameter().accept( this );
		appendSql( ')' );
	}
}
