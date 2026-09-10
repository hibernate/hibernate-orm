/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstTranslatorWithMerge;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.expression.UnparsedNumericLiteral;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.InArrayPredicate;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcOperationQueryInsertImpl;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperationQueryInsert;
import org.hibernate.sql.model.internal.OptionalTableInsert;
import org.hibernate.sql.model.internal.TableInsertStandard;
import org.hibernate.type.SqlTypes;

/**
 * A SQL AST translator for GaussDB.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLSqlAstTranslator.
 */
public class GaussDBSqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {

	public GaussDBSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	public void visitInArrayPredicate(InArrayPredicate inArrayPredicate) {
		inArrayPredicate.getTestExpression().accept( this );
		appendSql( " = any (" );
		inArrayPredicate.getArrayParameter().accept( this );
		appendSql( ")" );
	}

	@Override
	public void visitStar(Star star) {
		// GaussDB M mode (MySQL-compatible) optimizer bug: a correlated scalar subquery using
		// count(*) in a comparison — e.g. HQL `size(collection) = N` renders as
		// `where (select count(*) from child where child.fk = parent.id) = ?` — raises a FATAL
		// "Failed on assertion in nlist.cpp function list_nth_cell line 671. list is NIL" and kills
		// the connection. Rendering count(1) instead of count(*) avoids the bug and is semantically
		// equivalent (counts all rows). A mode (openGauss PG kernel) keeps count(*).
		if ( getDialect() instanceof GaussDBDialect g && g.isMMode() ) {
			appendSql( '1' );
			return;
		}
		super.visitStar( star );
	}

	@Override
	public <N extends Number> void visitUnparsedNumericLiteral(UnparsedNumericLiteral<N> literal) {
		// GaussDB M mode rejects a numeric literal that starts with '.' (e.g. HQL `.001f` renders the
		// raw ".001" -> "syntax error at or near \".\""). Prepend "0" -> "0.001". A mode (PG kernel)
		// accepts it, so keep super.
		if ( getDialect() instanceof GaussDBDialect g && g.isMMode() ) {
			final String literalValue = literal.getUnparsedLiteralValue();
			if ( literalValue.charAt( 0 ) == '.' ) {
				appendSql( "0" );
			}
		}
		super.visitUnparsedNumericLiteral( literal );
	}

	@Override
	protected String getArrayContainsFunction() {
		return super.getArrayContainsFunction();
	}

	@Override
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		renderIntoIntoAndTable( tableInsert );
		// Delegate to the dialect so M mode (MySQL-compatible) renders `() values ()`
		// instead of `default values`, which M mode rejects. A mode keeps `default values`.
		appendSql( getDialect().getNoColumnsInsertString() );
	}

	@Override
	protected JdbcOperationQueryInsert translateInsert(InsertSelectStatement sqlAst) {
		visitInsertStatement( sqlAst );

		return new JdbcOperationQueryInsertImpl(
				getSql(),
				getParameterBinders(),
				getAffectedTableNames(),
				null
		);
	}

	@Override
	protected void renderTableReferenceIdentificationVariable(TableReference tableReference) {
		final String identificationVariable = tableReference.getIdentificationVariable();
		if ( identificationVariable != null ) {
			final Clause currentClause = getClauseStack().getCurrent();
			if ( currentClause == Clause.INSERT ) {
				if ( getDialect() instanceof GaussDBDialect g && g.isMMode() ) {
					// M mode (MySQL-compatible) rejects INSERT table aliases entirely
					// (both `insert into t as a` and `insert into t a` are syntax errors),
					// so emit nothing. A mode (Oracle-compatible) requires the "as" keyword.
					return;
				}
				// GaussDB A mode requires the "as" keyword for inserts
				appendSql( " as " );
			}
			else {
				append( WHITESPACE );
			}
			append( tableReference.getIdentificationVariable() );
		}
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
	protected void renderDeleteClause(DeleteStatement statement) {
		if ( getDialect() instanceof GaussDBDialect g && g.isMMode() ) {
			// M mode (MySQL-compatible) rejects `delete from t alias` (PostgreSQL style);
			// render MySQL-style `delete alias from t alias` instead. A mode keeps PG style.
			appendSql( "delete" );
			final var clauseStack = getClauseStack();
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
		else {
			super.renderDeleteClause( statement );
		}
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		if ( getDialect() instanceof GaussDBDialect g && g.isMMode()
				&& !updateStatement.getFromClause().getRoots().isEmpty() ) {
			// M mode (MySQL-compatible) rejects PG-style `update t set ... from t1 join t2 ...`.
			// Render MySQL-style `update t1 join t2 on ...` up front; renderFromClauseAfterUpdateSet
			// is then a no-op. A mode (openGauss PG kernel) keeps the base `update t set ... from`.
			appendSql( "update " );
			renderFromClauseSpaces( updateStatement.getFromClause() );
		}
		else {
			super.renderUpdateClause( updateStatement );
		}
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		if ( getDialect() instanceof GaussDBDialect g && g.isMMode() ) {
			// M mode renders the FROM clause up front in renderUpdateClause (MySQL style),
			// so there is nothing to append after SET. A mode keeps PG-style `... from t1 join t2`
			// plus the row-matching predicate via renderFromClauseJoiningDmlTargetReference.
			return;
		}
		renderFromClauseJoiningDmlTargetReference( statement );
	}

	@Override
	protected void visitConflictClause(ConflictClause conflictClause) {
		// GaussDB uses MySQL-style `ON DUPLICATE KEY UPDATE` for both M mode (MySQL-compatible) and A mode
		// (openGauss Oracle-compatible), because A mode does not support PostgreSQL's `ON CONFLICT` syntax.
		// Emulate DO NOTHING via `ON DUPLICATE KEY UPDATE col=col` (MySQL-standard) &mdash; `UPDATE NOTHING`
		// is not valid GaussDB/MySQL syntax and raises "syntax error at end of input".
		visitOnDuplicateKeyConflictClause( conflictClause );
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		if ( getDialect() instanceof GaussDBDialect g && g.isMMode()
				&& "excluded".equals( columnReference.getQualifier() )
				&& getStatementStack().getCurrent() instanceof InsertSelectStatement insertSelectStatement
				&& insertSelectStatement.getSourceSelectStatement() == null ) {
			// M mode (MySQL-compatible) has no `excluded` row alias for insert-values ON DUPLICATE KEY
			// UPDATE; reference the proposed value via `values(col)` (MySQL-standard). A mode (ON CONFLICT)
			// uses `excluded.col` and keeps the base behavior.
			appendSql( "values(" );
			columnReference.appendReadExpression( this, null );
			append( ')' );
		}
		else {
			super.visitColumnReference( columnReference );
		}
	}

	@Override
	protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
		if ( getDialect() instanceof GaussDBDialect g && g.isMMode() ) {
			// M mode (MySQL-compatible) does not alias the INSERT target table, so a column reference
			// qualified with the target table's identification variable (e.g. be1_0) in the ON DUPLICATE
			// KEY UPDATE SET clause has no resolvable table &mdash; GaussDB raises "Missing FROM-clause
			// entry for table be1_0". Re-qualify such references with the table expression (table name),
			// mirroring MySQLSqlAstTranslator. A mode (ON CONFLICT) keeps the base behavior.
			final DmlTargetColumnQualifierSupport qualifierSupport = getDialect().getDmlTargetColumnQualifierSupport();
			final String dmlAlias;
			if ( getClauseStack().getCurrent() != Clause.SET
					|| !( getCurrentDmlStatement() instanceof InsertSelectStatement insertSelectStatement )
					|| ( dmlAlias = insertSelectStatement.getTargetTable().getIdentificationVariable() ) == null
					|| !dmlAlias.equals( columnReference.getQualifier() ) ) {
				return columnReference.getQualifier();
			}
			else if ( qualifierSupport != DmlTargetColumnQualifierSupport.NONE || !getQueryPartStack().isEmpty() ) {
				return getCurrentDmlStatement().getTargetTable().getTableExpression();
			}
			else {
				return null;
			}
		}
		return super.determineColumnReferenceQualifier( columnReference );
	}

	@Override
	public void visitStandardTableInsert(TableInsertStandard tableInsert) {
		if ( tableInsert instanceof OptionalTableInsert
				&& getDialect() instanceof GaussDBDialect g && g.isMMode() ) {
			// M mode (MySQL-compatible): emulate DO NOTHING for optional (collection/secondary) tables via
			// `ON DUPLICATE KEY UPDATE col=col` (MySQL-standard). `UPDATE NOTHING` is not valid GaussDB/MySQL
			// syntax and raises "syntax error at end of input". The base visitStandardTableInsert rejects
			// OptionalTableInsert, so M mode overrides here. A mode does not build OptionalTableInsert — see
			// GaussDBDialect#createOptionalTableUpdateOperation (A mode falls back to the default
			// OptionalTableUpdateOperation, which uses a plain insert + catch unique-violation) — and thus
			// uses the base translator, which is correct because A mode does not support ON CONFLICT either.
			getCurrentClauseStack().push( Clause.INSERT );
			try {
				renderInsertInto( tableInsert );
				appendSql( " on duplicate key update " );
				final ColumnReference[] firstCol = new ColumnReference[1];
				tableInsert.forEachValueBinding( (columnPosition, columnValueBinding) -> {
					if ( columnPosition == 0 ) {
						firstCol[0] = columnValueBinding.getColumnReference();
					}
				} );
				appendSql( firstCol[0].getColumnExpression() );
				appendSql( '=' );
				visitColumnReference( firstCol[0] );
				if ( tableInsert.getNumberOfReturningColumns() > 0 ) {
					visitReturningColumns( tableInsert::getReturningColumns );
				}
			}
			finally {
				getCurrentClauseStack().pop();
			}
		}
		else {
			super.visitStandardTableInsert( tableInsert );
		}
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
		if ( getDialect() instanceof GaussDBDialect g && g.isMMode() ) {
			// M mode (MySQL-compatible) rejects the `materialized`/`not materialized` CTE hint
			// (syntax error); emit nothing so CTEs render as plain `as (select ...)`. A mode
			// (Oracle-compatible, openGauss PG kernel) keeps the PostgreSQL-style hint.
			return;
		}
		if ( materialization == CteMaterialization.NOT_MATERIALIZED ) {
			appendSql( "not " );
		}
		appendSql( "materialized " );
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
	protected void renderStandardCycleClause(CteStatement cte) {
		super.renderStandardCycleClause( cte );
		if ( cte.getCycleMarkColumn() != null && cte.getCyclePathColumn() == null && getDialect().supportsRecursiveCycleUsingClause() ) {
			appendSql( " using " );
			appendSql( determineCyclePathColumnName( cte ) );
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
		if ( getDialect() instanceof GaussDBDialect g && g.isMMode() ) {
			if ( likePredicate.isCaseSensitive() ) {
				// M mode's default collation is case-insensitive, so a case-sensitive LIKE would
				// match 'Prod%' against "product"/"PROD" as well. Force a case-sensitive comparison
				// with `collate "C"` so Hibernate's case-sensitive LIKE keeps its semantics.
				// renderEscapeCharacter forces `escape ''` when no explicit escape is given, to
				// disable backslash escaping (MySQL treats `\` as the escape; PG/Hibernate treat
				// it as a literal).
				likePredicate.getMatchExpression().accept( this );
				appendSql( " collate \"C\"" );
				if ( likePredicate.isNegated() ) {
					appendSql( " not" );
				}
				appendSql( " like " );
				likePredicate.getPattern().accept( this );
				renderEscapeCharacter( likePredicate.getEscapeCharacter() );
			}
			else {
				// Case-insensitive LIKE: M mode has no native ILIKE (supportsCaseInsensitiveLike()
				// returns false), so let the base translator emulate it via lower(col) like lower(pattern).
				// The lower()-folded operands are already case-insensitive, so no collate is needed.
				super.visitLikePredicate( likePredicate );
			}
			return;
		}
		// A mode (openGauss PG kernel): we need a custom implementation here because GaussDB
		// uses the backslash character as default escape character.
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
			appendSql( " escape ''''" );
		}
	}

	@Override
	protected void renderEscapeCharacter(Expression escapeCharacter) {
		if ( getDialect() instanceof GaussDBDialect g && g.isMMode() ) {
			// M mode: force an empty escape character to disable backslash escaping (MySQL default),
			// making the backslash a literal — matching Hibernate/PG semantics. An explicit escape
			// character is honored as-is.
			if ( escapeCharacter != null ) {
				appendSql( " escape " );
				escapeCharacter.accept( this );
			}
			else {
				appendSql( " escape ''''" );
			}
		}
		else {
			super.renderEscapeCharacter( escapeCharacter );
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
