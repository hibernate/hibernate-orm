/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.community.dialect;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Summarization;
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
import org.hibernate.sql.model.internal.TableInsertStandard;
import org.hibernate.type.SqlTypes;

/**
 * A SQL AST translator for PostgreSQL.
 *
 * @author Christian Beikov
 */
public class PostgreSQLLegacySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public PostgreSQLLegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
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
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		renderIntoIntoAndTable( tableInsert );
		appendSql( "default values" );
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
				// PostgreSQL requires the "as" keyword for inserts
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
		if ( !( currentStatement instanceof UpdateStatement )
				|| !hasNonTrivialFromClause( ( (UpdateStatement) currentStatement ).getFromClause() ) ) {
			// For UPDATE statements we render a full FROM clause and a join condition to match target table rows,
			// but for that to work, we have to omit the alias for the target table reference here
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		renderFromClauseJoiningDmlTargetReference( statement );
	}

	@Override
	protected void visitConflictClause(ConflictClause conflictClause) {
		visitStandardConflictClause( conflictClause );
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
			// In PostgreSQL, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
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
		if ( getDialect().getVersion().isSameOrAfter( 12 ) ) {
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
	protected boolean supportsRecursiveSearchClause() {
		return getDialect().getVersion().isSameOrAfter( 14 );
	}

	@Override
	protected boolean supportsRecursiveCycleClause() {
		return getDialect().getVersion().isSameOrAfter( 14 );
	}

	@Override
	protected boolean supportsRecursiveCycleUsingClause() {
		return getDialect().getVersion().isSameOrAfter( 14 );
	}

	@Override
	protected void renderStandardCycleClause(CteStatement cte) {
		super.renderStandardCycleClause( cte );
		if ( cte.getCycleMarkColumn() != null && cte.getCyclePathColumn() == null && supportsRecursiveCycleUsingClause() ) {
			appendSql( " using " );
			appendSql( determineCyclePathColumnName( cte ) );
		}
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
				appendSql( "(select 1)" );
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
				throw new UnsupportedOperationException( "Summarization is not supported by DBMS" );
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

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		appendSql( OPEN_PARENTHESIS );
		visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
		appendSql( arithmeticExpression.getOperator().getOperatorSqlTextString() );
		visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
		appendSql( CLOSE_PARENTHESIS );
	}

}
