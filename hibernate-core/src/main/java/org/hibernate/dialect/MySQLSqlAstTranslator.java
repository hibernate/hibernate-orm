/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.util.Locale;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for MySQL.
 *
 * @author Christian Beikov
 */
public class MySQLSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public MySQLSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	/**
	 * @deprecated Use {@link #getSqlType(CastTarget, SessionFactoryImplementor)} instead
	 */
	@Deprecated(forRemoval = true)
	public static String getSqlType(CastTarget castTarget, Dialect dialect) {
		return getSqlType( castTarget, castTarget.getSqlType(), dialect );
	}

	public static String getSqlType(CastTarget castTarget, SessionFactoryImplementor factory) {
		final String sqlType = getCastTypeName( castTarget, factory.getTypeConfiguration() );
		return getSqlType( castTarget, sqlType, factory.getJdbcServices().getDialect() );
	}

	private static String getSqlType(CastTarget castTarget, String sqlType, Dialect dialect) {
		if ( sqlType != null ) {
			int parenthesesIndex = sqlType.indexOf( '(' );
			final String baseName = parenthesesIndex == -1 ? sqlType : sqlType.substring( 0, parenthesesIndex );
			switch ( baseName.toLowerCase( Locale.ROOT ) ) {
				case "bit":
					return "unsigned";
				case "tinyint":
				case "smallint":
				case "integer":
				case "bigint":
					return "signed";
				case "float":
				case "real":
				case "double precision":
					final int precision = castTarget.getPrecision() == null ?
							dialect.getDefaultDecimalPrecision() :
							castTarget.getPrecision();
					final int scale = castTarget.getScale() == null ? Size.DEFAULT_SCALE : castTarget.getScale();
					return "decimal(" + precision + "," + scale + ")";
				case "char":
				case "varchar":
				case "nchar":
				case "nvarchar":
					return castTarget.getLength() == null
							? "char"
							: ( "char(" + castTarget.getLength() + ")" );
				case "binary":
				case "varbinary":
					return castTarget.getLength() == null
						? "binary"
						: ( "binary(" + castTarget.getLength() + ")" );
			}
		}
		return sqlType;
	}

	@Override
	protected void renderExpressionAsClauseItem(Expression expression) {
		expression.accept( this );
	}

	@Override
	protected void visitRecursivePath(Expression recursivePath, int sizeEstimate) {
		// MySQL determines the type and size of a column in a recursive CTE based on the expression of the non-recursive part
		// Due to that, we have to cast the path in the non-recursive path to a varchar of appropriate size to avoid data truncation errors
		if ( sizeEstimate == -1 ) {
			super.visitRecursivePath( recursivePath, sizeEstimate );
		}
		else {
			appendSql( "cast(" );
			recursivePath.accept( this );
			appendSql( " as char(" );
			appendSql( sizeEstimate );
			appendSql( "))" );
		}
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
	protected String getForShare(int timeoutMillis) {
		return " for share";
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		return useOffsetFetchClause( queryPart ) && getQueryPartForRowNumbering() != queryPart
				&& getDialect().supportsWindowFunctions() && !isRowsOnlyFetchClauseType( queryPart );
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
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		emulateValuesTableReferenceColumnAliasing( tableReference );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			renderCombinedLimitClause( queryPart );
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		renderComparisonDistinctOperator( lhs, operator, rhs );
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "'0'" );
		}
		else if ( expression instanceof Summarization ) {
			Summarization summarization = (Summarization) expression;
			renderCommaSeparated( summarization.getGroupings() );
			appendSql( " with " );
			appendSql( summarization.getKind().sqlText() );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		// Custom implementation because MySQL uses backslash as the default escape character
		if ( getDialect().getVersion().isSameOrAfter( 8, 0, 24 ) ) {
			// From version 8.0.24 we can override this by specifying an empty escape character
			// See https://dev.mysql.com/doc/refman/8.0/en/string-comparison-functions.html#operator_like
			super.visitLikePredicate( likePredicate );
			if ( !getDialect().isNoBackslashEscapesEnabled() && likePredicate.getEscapeCharacter() == null ) {
				appendSql( " escape ''" );
			}
		}
		else {
			if ( likePredicate.isCaseSensitive() ) {
				likePredicate.getMatchExpression().accept( this );
				if ( likePredicate.isNegated() ) {
					appendSql( " not" );
				}
				appendSql( " like " );
				renderBackslashEscapedLikePattern(
						likePredicate.getPattern(),
						likePredicate.getEscapeCharacter(),
						getDialect().isNoBackslashEscapesEnabled()
				);
			}
			else {
				appendSql( getDialect().getLowercaseFunction() );
				appendSql( OPEN_PARENTHESIS );
				likePredicate.getMatchExpression().accept( this );
				appendSql( CLOSE_PARENTHESIS );
				if ( likePredicate.isNegated() ) {
					appendSql( " not" );
				}
				appendSql( " like " );
				appendSql( getDialect().getLowercaseFunction() );
				appendSql( OPEN_PARENTHESIS );
				renderBackslashEscapedLikePattern(
						likePredicate.getPattern(),
						likePredicate.getEscapeCharacter(),
						getDialect().isNoBackslashEscapesEnabled()
				);
				appendSql( CLOSE_PARENTHESIS );
			}
			if ( likePredicate.getEscapeCharacter() != null ) {
				appendSql( " escape " );
				likePredicate.getEscapeCharacter().accept( this );
			}
		}
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInSet() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return true;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	protected boolean supportsIntersect() {
		return false;
	}

	@Override
	protected boolean supportsDistinctFromPredicate() {
		// It supports a proprietary operator
		return true;
	}

	@Override
	protected boolean supportsSimpleQueryGrouping() {
		return true;
	}

	@Override
	protected boolean supportsNestedSubqueryCorrelation() {
		return false;
	}

	@Override
	protected boolean supportsWithClause() {
		return true;
	}

	@Override
	protected String getFromDual() {
		return " from dual";
	}

	@Override
	public MySQLDialect getDialect() {
		return (MySQLDialect) DialectDelegateWrapper.extractRealDialect( super.getDialect() );
	}

	@Override
	public void visitCastTarget(CastTarget castTarget) {
		String sqlType = getSqlType( castTarget, getSessionFactory() );
		if ( sqlType != null ) {
			appendSql( sqlType );
		}
		else {
			super.visitCastTarget( castTarget );
		}
	}
}
