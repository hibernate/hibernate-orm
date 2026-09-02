/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;
import java.util.Locale;

import org.hibernate.Internal;
import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SelectItemReferenceStrategy;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.function.array.DdlTypeHelper;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.Any;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.CastTarget;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Every;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.LikePredicate;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for SingleStore.
 *
 * @author Oleksandr Yeliseiev
 */
public class SingleStoreSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private static final int MAX_CHAR_SIZE = 8192;
	private final SingleStoreDialect dialect;

	public SingleStoreSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request, SingleStoreDialect dialect) {
		super( request );
		this.dialect = dialect;
	}

	@Override
	@SPI({ SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
	protected SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.POSITION;
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			appendSql( OPEN_PARENTHESIS );
			visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
			appendSql( " div " );
			visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			super.visitBinaryArithmeticExpression( arithmeticExpression );
		}
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions, SqlTuple tuple, ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	@Internal
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_VALUES_FUNCTION;
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete" );
		renderTableReferenceIdentificationVariable( statement.getTargetTable() );
		if ( statement.getFromClause().getRoots().isEmpty() ) {
			appendSql( " from " );
			renderDmlTargetTableExpression( statement.getTargetTable() );
		}
		else {
			visitFromClause( statement.getFromClause() );
		}
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		if ( updateStatement.getFromClause().getRoots().isEmpty() ) {
			super.renderUpdateClause( updateStatement );
		}
		else {
			appendSql( "update " );
			renderFromClauseSpaces( updateStatement.getFromClause() );
		}
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
		final DmlTargetColumnQualifierSupport qualifierSupport = getDialect().getDmlTargetColumnQualifierSupport();
		final MutationStatement currentDmlStatement;
		final String dmlAlias;
		// Since SingleStore does not support aliasing the insert target table,
		// we must detect column reference that are used in the conflict clause
		// and use the table expression as qualifier instead
		if ( getClauseStack().getCurrent() != Clause.SET || !((currentDmlStatement = getCurrentDmlStatement()) instanceof InsertSelectStatement) || (dmlAlias = currentDmlStatement.getTargetTable()
				.getIdentificationVariable()) == null || !dmlAlias.equals( columnReference.getQualifier() ) ) {
			return columnReference.getQualifier();
		}
		// Qualify the column reference with the table expression also when in subqueries
		else if ( qualifierSupport != DmlTargetColumnQualifierSupport.NONE || !getQueryPartStack().isEmpty() ) {
			return getCurrentDmlStatement().getTargetTable().getTableExpression();
		}
		else {
			return null;
		}
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
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> request.hasFetch()
				&& request.fetchClauseType() != FetchClauseType.ROWS_ONLY
				&& hasWindowFunctionSupport()
						? new PaginationRenderingPlan.Window( true )
						: new PaginationRenderingPlan.CombinedLimit();
	}

	@Override
	protected boolean shouldEmulateLateralWithIntersect(QueryPart queryPart) {
		return getDialect().getSetOperationSupport()
				.supports( SetOperationSupport.Capability.SIMPLE_QUERY_GROUPING )
			|| !queryPart.hasOffsetOrFetchClause();
	}

	@Override
	public void visitAny(Any any) {
		throw new UnsupportedOperationException( "SingleStore doesn't support ANY clause" );
	}

	@Override
	public void visitEvery(Every every) {
		throw new UnsupportedOperationException( "SingleStore doesn't support ALL clause" );
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.QUERY_SELECT_LIST;
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		if ( operator == ComparisonOperator.DISTINCT_FROM || operator == ComparisonOperator.NOT_DISTINCT_FROM ) {
			renderDistinct( lhs, operator, rhs );
		}
		else {
			lhs.accept( this );
			appendSql( operator.sqlText() );
			rhs.accept( this );
		}
	}

	private void renderDistinct(Expression lhs, ComparisonOperator operator, Expression rhs) {
		appendSql( OPEN_PARENTHESIS );
		appendSql( "case when " );
		rhs.accept( this );
		appendSql( " is null then " );
		if ( operator == ComparisonOperator.DISTINCT_FROM ) {
			appendSql( OPEN_PARENTHESIS );
			lhs.accept( this );
			appendSql( " is not null) else (" );
			lhs.accept( this );
			appendSql( "!=" );
			rhs.accept( this );
			appendSql( " or " );
			lhs.accept( this );
			appendSql( " is null) end)" );
		}
		else {
			appendSql( OPEN_PARENTHESIS );
			lhs.accept( this );
			appendSql( " is null) else (" );
			lhs.accept( this );
			appendSql( "=" );
			rhs.accept( this );
			appendSql( ") end)" );
		}
	}

	@Override
	protected void emulateTupleComparison(
			final List<? extends SqlAstNode> lhsExpressions,
			final List<? extends SqlAstNode> rhsExpressions,
			ComparisonOperator operator,
			boolean indexOptimized) {
		if ( operator == ComparisonOperator.DISTINCT_FROM || operator == ComparisonOperator.NOT_DISTINCT_FROM ) {
			final int size = lhsExpressions.size();
			assert size == rhsExpressions.size();
			String separator = OPEN_PARENTHESIS + "";
			for ( int i = 0; i < size; i++ ) {
				appendSql( separator );
				renderDistinct( (Expression) lhsExpressions.get( i ), operator, (Expression) rhsExpressions.get( i ) );
				separator = ") and (";
			}
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			super.emulateTupleComparison( lhsExpressions, rhsExpressions, operator, indexOptimized );
		}
	}

	@Override
	protected void renderCombinedLimitClause(Expression offsetExpression, Expression fetchExpression) {
		if ( offsetExpression != null || fetchExpression != null ) {
			if ( getCurrentQueryPart() instanceof QueryGroup && (((QueryGroup) getCurrentQueryPart()).getSetOperator() == SetOperator.UNION || ((QueryGroup) getCurrentQueryPart()).getSetOperator() == SetOperator.UNION_ALL) ) {
				throw new UnsupportedOperationException(
						"SingleStore doesn't support UNION/UNION ALL with limit clause" );
			}
		}
		super.renderCombinedLimitClause( offsetExpression, fetchExpression );
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

	//SingleStore like is case insensitive
	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		appendSql( "cast( " );
		likePredicate.getMatchExpression().accept( this );
		appendSql( " as char) " );
		if ( likePredicate.isNegated() ) {
			appendSql( " not" );
		}
		appendSql( " like " );
		renderBackslashEscapedLikePattern( likePredicate.getPattern(), likePredicate.getEscapeCharacter(), false );
	}

	@Override
	protected void renderBackslashEscapedLikePattern(
			Expression pattern, Expression escapeCharacter, boolean noBackslashEscapes) {
		if ( escapeCharacter != null ) {
			appendSql( "replace" );
			appendSql( OPEN_PARENTHESIS );
			pattern.accept( this );
			appendSql( "," );
			escapeCharacter.accept( this );
			appendSql( ",'\\\\'" );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			// Since escape with empty or null character is ignored we need
			// four backslashes to render a single one in a like pattern
			if ( pattern instanceof Literal ) {
				Object literalValue = ((Literal) pattern).getLiteralValue();
				if ( literalValue == null ) {
					pattern.accept( this );
				}
				else {
					appendBackslashEscapedLikeLiteral( this, literalValue.toString(), false );
				}
			}
			else {
				appendSql( "replace" );
				appendSql( OPEN_PARENTHESIS );
				pattern.accept( this );
				appendSql( ",'\\\\','\\\\\\\\'" );
				appendSql( CLOSE_PARENTHESIS );
			}
		}
	}

	@Override
	protected SingleStoreDialect getDialect() {
		return dialect;
	}

	private boolean hasWindowFunctionSupport() {
		return getDialect().getWindowFunctionSupport()
				.supports( WindowFunctionSupport.Feature.WINDOW_FUNCTIONS );
	}

	public static String getSqlType(CastTarget castTarget, SessionFactoryImplementor factory) {
		final String sqlType = DdlTypeHelper.getCastTypeName( castTarget, factory.getTypeConfiguration() );
		return getSqlType( castTarget, sqlType, factory.getJdbcServices().getDialect() );
	}

	private static String getSqlType(CastTarget castTarget, String sqlType, Dialect dialect) {
		if ( sqlType != null ) {
			int parenthesesIndex = sqlType.indexOf( '(' );
			final String baseName = parenthesesIndex == -1 ? sqlType : sqlType.substring( 0, parenthesesIndex ).trim();
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
							dialect.getTypeSizingProfile().defaultDecimalPrecision() :
							castTarget.getPrecision();
					final int scale = castTarget.getScale() == null ? Size.DEFAULT_SCALE : castTarget.getScale();
					return "decimal(" + precision + "," + scale + ")";
				case "char":
				case "varchar":
				case "text":
				case "mediumtext":
				case "longtext":
				case "set":
				case "enum":
					if ( castTarget.getLength() == null ) {
						if ( castTarget.getJdbcMapping().getJdbcJavaType().getJavaType() == Character.class ) {
							return "char(1)";
						}
						else {
							return "char";
						}
					}
					return castTarget.getLength() > MAX_CHAR_SIZE ? "char" : "char(" + castTarget.getLength() + ")";
				case "binary":
				case "varbinary":
				case "mediumblob":
				case "longblob":
					return castTarget.getLength() == null ? "binary" : "binary(" + castTarget.getLength() + ")";
			}
		}
		return sqlType;
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
