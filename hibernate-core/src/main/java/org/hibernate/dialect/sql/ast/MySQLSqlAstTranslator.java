/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A SQL AST translator for MySQL.
 *
 * @author Christian Beikov
 */
public class MySQLSqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithOnDuplicateKeyUpdate<T> {

	/**
	 * On MySQL, 1GB or {@code 2^30 - 1} is the maximum size that a char value can be casted.
	 */
	private static final int MAX_CHAR_SIZE = (1 << 30) - 1;

	private final MySQLDialect dialect;

	public MySQLSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, MySQLDialect dialect) {
		super( sessionFactory, statement );
		this.dialect = dialect;
	}

	public static String getSqlType(CastTarget castTarget, SessionFactoryImplementor factory) {
		final String sqlType = getCastTypeName( castTarget, factory.getTypeConfiguration() );
		return getSqlType( castTarget, sqlType, factory.getJdbcServices().getDialect() );
	}

	//TODO: this is really, really bad since it circumvents the whole machinery we have in DdlType
	//      and in the Dialect for doing this in a unified way! These mappings should be held in
	//      the DdlTypes themselves and should be set up in registerColumnTypes(). Doing it here
	//      means we have problems distinguishing, say, the 'as Character' special case
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
					if ( ((MySQLDialect) dialect).getMySQLVersion().isSameOrAfter( 8, 0, 17 ) ) {
						return sqlType;
					}
					final int precision = castTarget.getPrecision() == null
							? dialect.getDefaultDecimalPrecision()
							: castTarget.getPrecision();
					final int scale = castTarget.getScale() == null ? Size.DEFAULT_SCALE : castTarget.getScale();
					return "decimal(" + precision + "," + scale + ")";
				case "char":
				case "varchar":
				case "nchar":
				case "nvarchar":
				case "text":
				case "mediumtext":
				case "longtext":
				case "enum":
					if ( castTarget.getLength() == null ) {
						// TODO: this is ugly and fragile, but could easily be handled in a DdlType
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
					return castTarget.getLength() == null
						? "binary"
						: "binary(" + castTarget.getLength() + ")";
			}
		}
		return sqlType;
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
			super.visitBinaryArithmeticExpression(arithmeticExpression);
		}
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
				emulateQueryPartTableReferenceColumnAliasing(
						new QueryPartTableReference(
								new SelectStatement( statement.getSourceSelectStatement() ),
								"excluded",
								columnNames,
								false,
								getSessionFactory()
						)
				);
			}
			else {
				statement.getSourceSelectStatement().accept( this );
			}
		}
		else {
			visitValuesList( statement.getValuesList() );
			if ( statement.getConflictClause() != null && getDialect().getMySQLVersion().isSameOrAfter( 8, 0, 19 ) ) {
				appendSql( " as excluded" );
				char separator = '(';
				for ( ColumnReference targetColumn : statement.getTargetColumns() ) {
					appendSql( separator );
					appendSql( targetColumn.getColumnExpression() );
					separator = ',';
				}
				appendSql( ')' );
			}
		}
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		if ( getDialect().getMySQLVersion().isBefore( 8, 0, 19 )
				&& "excluded".equals( columnReference.getQualifier() )
				&& getStatementStack().getCurrent() instanceof InsertSelectStatement insertSelectStatement
				&& insertSelectStatement.getSourceSelectStatement() == null ) {
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
		final String dmlAlias;
		// Since MySQL does not support aliasing the insert target table,
		// we must detect column reference that are used in the conflict clause
		// and use the table expression as qualifier instead
		if ( getClauseStack().getCurrent() != Clause.SET
				|| !( getCurrentDmlStatement() instanceof InsertSelectStatement insertSelectStatement )
				|| ( dmlAlias = insertSelectStatement.getTargetTable().getIdentificationVariable() ) == null
				|| !dmlAlias.equals( columnReference.getQualifier() ) ) {
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
	protected void renderDerivedTableReference(DerivedTableReference tableReference) {
		if ( tableReference instanceof FunctionTableReference && tableReference.isLateral() ) {
			// No need for a lateral keyword for functions
			tableReference.accept( this );
		}
		else {
			super.renderDerivedTableReference( tableReference );
		}
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
		else if ( expression instanceof Summarization summarization ) {
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
	public MySQLDialect getDialect() {
		return dialect;
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

	@Override
	protected void renderStringContainsExactlyPredicate(Expression haystack, Expression needle) {
		// MySQL can't cope with NUL characters in the position function, so we use a like predicate instead
		haystack.accept( this );
		appendSql( " like concat('%',replace(replace(replace(" );
		needle.accept( this );
		appendSql( ",'~','~~'),'?','~?'),'%','~%'),'%') escape '~'" );
	}
}
