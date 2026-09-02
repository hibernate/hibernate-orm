/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.internal;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.jdbc.Expectation;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardPaginationRenderingSupport;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.creation.SqlAliasStemHelper;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.expression.UnparsedNumericLiteral;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.predicate.InArrayPredicate;
import org.hibernate.sql.ast.spi.query.predicate.LikePredicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.expression.Any;
import org.hibernate.sql.ast.spi.query.expression.Every;
import org.hibernate.sql.ast.spi.query.expression.ModifiedSubQueryExpression;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.LogicalTableUpdate;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;
import org.hibernate.sql.spi.mutation.jdbc.DeleteOrUpsertOperation;
import org.hibernate.sql.spi.mutation.jdbc.UpsertOperation;

/**
 * A SQL AST translator for Spanner.
 *
 * @author Christian Beikov
 */
public class SpannerSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public SpannerSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		throw new MappingException(
				String.format(
						"The INSERT statement for table [%s] contains no column, and this is not supported by [%s]",
						tableInsert.getMutatingTable().getTableId(),
						getDialect()
				)
		);
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return StandardPaginationRenderingSupport.LIMIT_OFFSET;
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.SPANNER;
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		if ( rhs instanceof Every || rhs instanceof Any ) {
			final boolean all = rhs instanceof Every;
			final SelectStatement subquery = all ? ( (Every) rhs ).getSubquery() : ( (Any) rhs ).getSubquery();

			final AbstractSqlAstTranslator.SubQueryRelationalRestrictionEmulationRenderer<Expression> singleRenderer = (lhsSelections, singleExpr, op) -> {
				lhsSelections.get( 0 ).getExpression().accept( this );
				appendSql( op.invert().sqlText() );
				singleExpr.accept( this );
			};

			emulateSubQueryRelationalRestrictionPredicate(
					null,
					all,
					subquery,
					lhs,
					singleRenderer,
					all ? operator.negated() : operator
			);
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
					}
					else if ( expression.getModifier() == ModifiedSubQueryExpression.Modifier.ANY ||
							expression.getModifier() == ModifiedSubQueryExpression.Modifier.SOME ) {
						// Emulate ANY
						lhs.accept( this );
						appendSql( operator.sqlText() );
						renderQuantifiedEmulationSubQuery( querySpec, operator.invert() );
					}
				}
			}
		}
		else {
			renderComparisonEmulateIntersect( lhs, operator, rhs );
		}
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void renderFetchFirstRow() {
		appendSql( " limit 1" );
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
	protected void renderSelectClause(SelectClause selectClause) {
		appendSql( "select " );
		if ( isRenderingCorrelatedDerivedTable() ) {
			appendSql( "as struct " );
		}
		if ( selectClause.isDistinct() ) {
			appendSql( "distinct " );
		}
		renderSelectItems( selectClause );
	}

	@Override
	protected void renderStandardTableUpdate(TableUpdateStandard tableUpdate) {
		visitSpannerTableUpdate( tableUpdate );
		if ( tableUpdate.getWhereFragment() != null ) {
			appendSql( " and (" );
			appendSql( tableUpdate.getWhereFragment() );
			appendSql( ")" );
		}
	}

	@Override
	public void visitOptionalTableUpdate(OptionalTableUpdate tableUpdate) {
		getClauseStack().push( Clause.UPDATE );
		try {
			visitSpannerTableUpdate( tableUpdate );
		}
		finally {
			getClauseStack().pop();
		}
	}

	/**
	 * Spanner requires table aliasing in UPDATE statements to disambiguate table and column names
	 * if they are identical (e.g., table 'Discount' and column 'discount').
	 * This method overrides standard Hibernate rendering to inject generated aliases.
	 */
	private void visitSpannerTableUpdate(LogicalTableUpdate<? extends MutationOperation> tableUpdate) {
		applySqlComment( tableUpdate.getMutationComment() );
		final String stem = SqlAliasStemHelper.INSTANCE.generateStemFromEntityName( tableUpdate.getMutatingTable().getTableName() );
		final String alias = stem + "1_0";

		appendSql( "update " );
		appendSql( tableUpdate.getMutatingTable().getTableName() );
		appendSql( " " + alias );
		registerAffectedTable( tableUpdate.getMutatingTable().getTableName() );

		getClauseStack().push( Clause.SET );
		try {
			appendSql( " set" );
			tableUpdate.forEachValueBinding( (columnPosition, columnValueBinding) -> {
				if ( columnPosition == 0 ) {
					appendSql( " " );
				}
				else {
					appendSql( "," );
				}
				appendSql( alias + "." );
				appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
				appendSql( "=" );
				columnValueBinding.getValueExpression().accept( this );
			} );
		}
		finally {
			getClauseStack().pop();
		}

		getClauseStack().push( Clause.WHERE );
		try {
			appendSql( " where" );
			tableUpdate.forEachKeyBinding( (position, columnValueBinding) -> {
				if ( position == 0 ) {
					appendSql( " " );
				}
				else {
					appendSql( " and " );
				}
				appendSql( alias + "." );
				appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
				appendSql( "=" );
				columnValueBinding.getValueExpression().accept( this );
			} );

			if ( tableUpdate.getNumberOfOptimisticLockBindings() > 0 ) {
				tableUpdate.forEachOptimisticLockBinding( (position, columnValueBinding) -> {
					appendSql( " and " );
					appendSql( alias + "." );
					appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
					if ( columnValueBinding.getValueExpression() == null
							|| columnValueBinding.getValueExpression().getFragment() == null ) {
						appendSql( " is null" );
					}
					else {
						appendSql( "=" );
						columnValueBinding.getValueExpression().accept( this );
					}
				} );
			}
		}
		finally {
			getClauseStack().pop();
		}
	}

	private void applySqlComment(String comment) {
		if ( getSessionFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			if ( comment != null ) {
				appendSql( "/* " );
				appendSql( org.hibernate.sql.spi.SqlComments.escape( comment ) );
				appendSql( " */" );
			}
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
	public void visitInArrayPredicate(InArrayPredicate inArrayPredicate) {
		inArrayPredicate.getTestExpression().accept( this );
		appendSql( " in unnest(" );
		inArrayPredicate.getArrayParameter().accept( this );
		appendSql( ')' );
	}

	@Override
	protected void renderLikePredicate(LikePredicate likePredicate) {
		// Spanner uses the backslash character as the default escape character
		if (likePredicate.getEscapeCharacter() == null) {
			renderBackslashEscapedLikePattern( likePredicate.getPattern(), likePredicate.getEscapeCharacter(), false );
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
			appendSql( ", '\\\\\\\\'), " );
			escapeCharacter.accept( this );
			appendSql( "||'%', '\\\\%'), " );
			escapeCharacter.accept( this );
			appendSql( "||'_', '\\\\_')" );
		}
	}

	@Override
	protected void renderEscapeCharacter(Expression escapeCharacter) {
		// Spanner doesn't support passing escape character
	}

	@Override
	protected void appendBackslashEscapedLikeLiteral(SqlAppender appender, String literal, boolean noBackslashEscapes) {
		appender.appendSql( '\'' );
		for ( int i = 0; i < literal.length(); i++ ) {
			final char c = literal.charAt( i );
			switch ( c ) {
				case '\'':
					appender.appendSql( '\\' );
					break;
				case '\\':
					appender.appendSql( "\\\\\\" );
					break;
			}
			appender.appendSql( c );
		}
		appender.appendSql( '\'' );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			// Spanner uses functional syntax: DIV(numerator, denominator)
			appendSql( "div(" );
			visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
			appendSql( "," );
			visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
			appendSql( ")" );
		}
		else {
			super.visitBinaryArithmeticExpression( arithmeticExpression );
		}
	}

	@Override
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.STANDARD;
	}

	@Override
	public <N extends Number> void visitUnparsedNumericLiteral(UnparsedNumericLiteral<N> literal) {
		final Class<?> javaTypeClass = literal.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass();
		if ( BigDecimal.class.isAssignableFrom( javaTypeClass )
			|| BigInteger.class.isAssignableFrom( javaTypeClass ) ) {
			appendSql( "NUMERIC '" );
			appendSql( literal.getUnparsedLiteralValue() );
			appendSql( "'" );
		}
		else {
			super.visitUnparsedNumericLiteral( literal );
		}
	}

	@Override
	protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
		// Cloud Spanner does not support target table aliases in INSERT statements,
		// so we must qualify column references with the actual table name to avoid ambiguity.
		if ( getClauseStack().findCurrentFirst( clause -> clause == Clause.CONFLICT ? Boolean.TRUE : null ) != null ) {
			if ( !"excluded".equalsIgnoreCase( columnReference.getQualifier() ) ) {
				final MutationStatement currentDmlStatement = getCurrentDmlStatement();
				if ( currentDmlStatement != null && currentDmlStatement.getTargetTable() != null ) {
					return currentDmlStatement.getTargetTable().getTableExpression();
				}
			}
		}
		return super.determineColumnReferenceQualifier( columnReference );
	}

	public MutationOperation createMergeOperation(OptionalTableUpdate optionalTableUpdate, boolean hasUpdatableBindings) {
		renderInsertOrUpdate( optionalTableUpdate, hasUpdatableBindings );

		final UpsertOperation upsertOperation = new UpsertOperation(
				optionalTableUpdate.getMutatingTable().getTableMapping(),
				optionalTableUpdate.getMutationTarget(),
				getSql(),
				hasUpdatableBindings ? new Expectation.RowCount() : new Expectation.OptionalRowCount(),
				getParameterBinders()
		);

		return new DeleteOrUpsertOperation(
				upsertOperation,
				optionalTableUpdate
		);
	}

	protected void renderInsertOrUpdate(OptionalTableUpdate optionalTableUpdate, boolean hasUpdatableBindings) {
		if ( hasUpdatableBindings ) {
			appendSql( "insert or update into " );
		}
		else {
			appendSql( "insert or ignore into " );
		}

		appendSql( optionalTableUpdate.getMutatingTable().getTableName() );
		appendSql( " (" );

		final List<ColumnValueBinding> keyBindings = optionalTableUpdate.getKeyBindings();
		char separator = ' ';
		for ( ColumnValueBinding keyBinding : keyBindings ) {
			appendSql( separator );
			appendSql( keyBinding.getColumnReference().getColumnExpression() );
			separator = ',';
		}

		optionalTableUpdate.forEachValueBinding( (columnPosition, columnValueBinding) -> {
			appendSql( ',' );
			appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
		} );

		appendSql( ") values (" );

		separator = ' ';
		for ( ColumnValueBinding keyBinding : keyBindings ) {
			appendSql( separator );
			keyBinding.getValueExpression().accept( this );
			separator = ',';
		}

		optionalTableUpdate.forEachValueBinding( (columnPosition, columnValueBinding) -> {
			appendSql( ',' );
			columnValueBinding.getValueExpression().accept( this );
		} );
		appendSql( ") " );
	}
}
