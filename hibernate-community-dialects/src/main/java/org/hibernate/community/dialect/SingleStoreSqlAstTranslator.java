/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Every;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for SingleStore.
 *
 * @author Oleksandr Yeliseiev
 */
public class SingleStoreSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private static final int MAX_CHAR_SIZE = 8192;
	private final SingleStoreDialect dialect;

	public SingleStoreSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, SingleStoreDialect dialect) {
		super( sessionFactory, statement );
		this.dialect = dialect;
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
	protected void visitInsertSource(InsertSelectStatement statement) {
		if ( statement.getSourceSelectStatement() != null ) {
			if ( statement.getConflictClause() != null ) {
				final List<ColumnReference> targetColumnReferences = statement.getTargetColumns();
				final List<String> columnNames = new ArrayList<>( targetColumnReferences.size() );
				for ( ColumnReference targetColumnReference : targetColumnReferences ) {
					columnNames.add( targetColumnReference.getColumnExpression() );
				}
				appendSql( "select * from " );
				emulateQueryPartTableReferenceColumnAliasing( new QueryPartTableReference(
						new SelectStatement( statement.getSourceSelectStatement() ),
						"excluded",
						columnNames,
						false,
						getSessionFactory()
				) );
			}
			else {
				statement.getSourceSelectStatement().accept( this );
			}
		}
		else {
			visitValuesList( statement.getValuesList() );
		}
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		final Statement currentStatement;
		if ( "excluded".equals(
				columnReference.getQualifier() ) && (currentStatement = getStatementStack().getCurrent()) instanceof InsertSelectStatement && ((InsertSelectStatement) currentStatement).getSourceSelectStatement() == null ) {
			// Accessing the excluded row for an insert-values statement in the conflict clause requires the values qualifier
			appendSql( "values(" );
			columnReference.appendReadExpression( this, null );
			append( ')' );
		}
		else {
			super.visitColumnReference( columnReference );
		}
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete" );
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.DELETE );
			renderTableReferenceIdentificationVariable( statement.getTargetTable() );
			if ( statement.getFromClause().getRoots().isEmpty() ) {
				appendSql( " from " );
				renderDmlTargetTableExpression( statement.getTargetTable() );
			}
			else {
				visitFromClause( statement.getFromClause() );
			}
		}
		finally {
			clauseStack.pop();
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
	protected void visitConflictClause(ConflictClause conflictClause) {
		visitOnDuplicateKeyConflictClause( conflictClause );
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

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		return useOffsetFetchClause(
				queryPart ) && getQueryPartForRowNumbering() != queryPart && supportsWindowFunctions() && !isRowsOnlyFetchClauseType(
				queryPart );
	}

	@Override
	protected boolean shouldEmulateLateralWithIntersect(QueryPart queryPart) {
		return getDialect().supportsSimpleQueryGrouping() || !queryPart.hasOffsetOrFetchClause();
	}

	//SingleStore doesn't support 'FOR UPDATE' clause with distributed joins
	@Override
	protected String getForUpdate() {
		return dialect.getForUpdateString();
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
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		emulateQueryPartTableReferenceColumnAliasing( tableReference );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			renderCombinedLimitClause( queryPart );
		}
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
	public SingleStoreDialect getDialect() {
		return dialect;
	}

	private boolean supportsWindowFunctions() {
		return true;
	}

	public static String getSqlType(CastTarget castTarget, SessionFactoryImplementor factory) {
		final String sqlType = getCastTypeName( castTarget, factory.getTypeConfiguration() );
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
							dialect.getDefaultDecimalPrecision() :
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
