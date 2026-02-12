/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.NestedOrTargetTableCorrelationVisitor;
import org.hibernate.sql.ast.tree.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcOperationQueryInsertImpl;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperationQueryInsert;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.ast.spi.FullJoinEmulationHelper;

/**
 * A SQL AST translator for MariaDB.
 *
 * @author Christian Beikov
 * @author Yoobin Yoon
 */
public class MariaDBSqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithOnDuplicateKeyUpdate<T> {

	private final MariaDBDialect dialect;
	private final FullJoinEmulationHelper fullJoinEmulationHelper;

	public MariaDBSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, MariaDBDialect dialect) {
		super( sessionFactory, statement );
		this.dialect = dialect;
		this.fullJoinEmulationHelper = new FullJoinEmulationHelper( this );
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
		}
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		if ( "excluded".equals( columnReference.getQualifier() )
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
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.DELETE );
			if ( usesSingleTableDml( statement ) ) {
				appendSql( "delete from " );
				appendSql( statement.getTargetTable().getTableExpression() );
				registerAffectedTable( statement.getTargetTable() );
			}
			else {
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
		}
		finally {
			clauseStack.pop();
		}
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		if ( usesSingleTableDml( updateStatement ) ) {
			appendSql( "update " );
			appendSql( updateStatement.getTargetTable().getTableExpression() );
			registerAffectedTable( updateStatement.getTargetTable() );
		}
		else if ( updateStatement.getFromClause().getRoots().isEmpty() ) {
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
		if ( getClauseStack().getCurrent() != Clause.INSERT && !usesSingleTableDml( getCurrentDmlStatement() ) ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected JdbcOperationQueryInsert translateInsert(InsertSelectStatement sqlAst) {
		visitInsertStatement( sqlAst );

		return new JdbcOperationQueryInsertImpl(
				getSql(),
				getParameterBinders(),
				getAffectedTableNames(),
				getUniqueConstraintNameThatMayFail(sqlAst)
		);
	}

	@Override
	protected void visitConflictClause(ConflictClause conflictClause) {
		visitOnDuplicateKeyConflictClause( conflictClause );
	}

	@Override
	protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
		final DmlTargetColumnQualifierSupport qualifierSupport = getDialect().getDmlTargetColumnQualifierSupport();
		final String dmlAlias;
		// Since MariaDB does not support aliasing the insert target table,
		// we must detect column reference that are used in the conflict clause
		// and use the table expression as qualifier instead
		if ( getClauseStack().getCurrent() != Clause.SET
				|| !( getCurrentDmlStatement() instanceof InsertSelectStatement insertSelectStatement )
				|| ( dmlAlias = insertSelectStatement.getTargetTable().getIdentificationVariable() ) == null
				|| !dmlAlias.equals( columnReference.getQualifier() ) ) {
			final MutationStatement currentStatement = getCurrentDmlStatement();
			if ( currentStatement != null && usesSingleTableDml( currentStatement ) && columnReference.getQualifier() != null ) {
				final NamedTableReference targetTable = currentStatement.getTargetTable();
				final String targetTableName = targetTable.getTableExpression();
				final String qualifier = columnReference.getQualifier();
				final String targetAlias = targetTable.getIdentificationVariable();
				if ( ( targetAlias != null && qualifier.equals( targetAlias ) ) || qualifier.equals( targetTableName ) ) {
					return !getQueryPartStack().isEmpty() ? targetTableName : null;
				}
			}
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
		// MariaDB determines the type and size of a column in a recursive CTE based on the expression of the non-recursive part
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

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		return useOffsetFetchClause( queryPart ) && getQueryPartForRowNumbering() != queryPart && supportsWindowFunctions() && !isRowsOnlyFetchClauseType( queryPart );
	}

	@Override
	protected boolean shouldEmulateLateralWithIntersect(QueryPart queryPart) {
		// Intersect emulation requires nested correlation when no simple query grouping is possible
		// and the query has an offset/fetch clause, so we have to disable the emulation in this case,
		// because nested correlation is not supported though
		return getDialect().supportsSimpleQueryGrouping() || !queryPart.hasOffsetOrFetchClause();
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
		if ( !fullJoinEmulationHelper.renderFullJoinEmulationBranchIfNeeded( querySpec, super::visitQuerySpec ) ) {
			if ( !fullJoinEmulationHelper.emulateFullJoinWithUnionIfNeeded( querySpec ) ) {
				if ( shouldEmulateFetchClause( querySpec ) ) {
					emulateFetchOffsetWithWindowFunctions( querySpec, true );
				}
				else {
					super.visitQuerySpec( querySpec );
				}
			}
		}
	}

	@Override
	public void visitSelectClause(SelectClause selectClause) {
		if ( !fullJoinEmulationHelper.renderSelectClauseIfNeeded( selectClause ) ) {
			super.visitSelectClause( selectClause );
		}
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		emulateQueryPartTableReferenceColumnAliasing( tableReference );
	}

	@Override
	protected void renderDerivedTableReferenceIdentificationVariable(DerivedTableReference tableReference) {
		renderTableReferenceIdentificationVariable( tableReference );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !fullJoinEmulationHelper.isFullJoinEmulationQueryPart( queryPart )
				&& !isRowNumberingCurrentQueryPart() ) {
			renderCombinedLimitClause( queryPart );
		}
	}

	@Override
	protected void visitOrderBy(List<SortSpecification> sortSpecifications) {
		fullJoinEmulationHelper.renderOrderByIfNeeded( getCurrentQueryPart(), sortSpecifications, super::visitOrderBy );
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		if ( lhsExpressionType != null && lhsExpressionType.getJdbcTypeCount() == 1
				&& lhsExpressionType.getSingleJdbcMapping().getJdbcType().isJson()
				&& getDialect().getVersion().isSameOrAfter( 10, 7 ) ) {
			switch ( operator ) {
				case DISTINCT_FROM:
					appendSql( "case when json_equals(" );
					lhs.accept( this );
					appendSql( ',' );
					rhs.accept( this );
					appendSql( ")=1 or " );
					lhs.accept( this );
					appendSql( " is null and " );
					rhs.accept( this );
					appendSql( " is null then 0 else 1 end=1" );
					break;
				case NOT_DISTINCT_FROM:
					appendSql( "case when json_equals(" );
					lhs.accept( this );
					appendSql( ',' );
					rhs.accept( this );
					appendSql( ")=1 or " );
					lhs.accept( this );
					appendSql( " is null and " );
					rhs.accept( this );
					appendSql( " is null then 0 else 1 end=0" );
					break;
				case NOT_EQUAL:
					appendSql( "json_equals(" );
					lhs.accept( this );
					appendSql( ',' );
					rhs.accept( this );
					appendSql( ")=0" );
					break;
				case EQUAL:
					appendSql( "json_equals(" );
					lhs.accept( this );
					appendSql( ',' );
					rhs.accept( this );
					appendSql( ")=1" );
					break;
				default:
					renderComparisonDistinctOperator( lhs, operator, rhs );
					break;
			}
		}
		else {
			renderComparisonDistinctOperator( lhs, operator, rhs );
		}
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

	@Override
	public MariaDBDialect getDialect() {
		return dialect;
	}

	private boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	protected void renderStringContainsExactlyPredicate(Expression haystack, Expression needle) {
		// MariaDB can't cope with NUL characters in the position function, so we use a like predicate instead
		haystack.accept( this );
		appendSql( " like concat('%',replace(replace(replace(" );
		needle.accept( this );
		appendSql( ",'~','~~'),'?','~?'),'%','~%'),'%') escape '~'" );
	}

	/*
		Upsert Template: (for an entity WITHOUT @Version)
			INSERT INTO employees (id, name, salary, version)
				VALUES (?, ?, ?, ?)
			ON DUPLICATE KEY UPDATE
				name = values(name),
				salary = values(salary)
	*/
	@Override
	protected void renderUpdateValue(ColumnValueBinding columnValueBinding) {
		appendSql( "values(" );
		appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
		appendSql( ")" );
	}

	@Override
	protected void appendAssignmentColumn(ColumnReference column) {
		column.appendColumnForWrite(
				this,
				getAffectedTableNames().size() > 1 && !(getStatement() instanceof InsertSelectStatement)
						? determineColumnReferenceQualifier( column )
						: null );
	}

	private boolean usesSingleTableDml(MutationStatement statement) {
		// As of MariaDB 11.1, the self-join rewrite optimization can handle this, so no need force single table DML
		return getDialect().getVersion().isBefore( 11, 1 ) && hasTargetTableCorrelation( statement );
	}

	private boolean needsDmlSubqueryWrapper() {
		final Statement statement = getStatement();
		// As of MariaDB 11.1, the self-join rewrite optimization can handle this, so no need for the wrapper
		return getDialect().getVersion().isBefore( 11, 1 )
				&& statement instanceof AbstractUpdateOrDeleteStatement updateOrDeleteStatement
				&& !NestedOrTargetTableCorrelationVisitor.hasCorrelation( updateOrDeleteStatement );
	}

	@Override
	public void visitSelectStatement(SelectStatement statement) {
		final boolean needsParenthesis = !statement.getQueryPart().isRoot();
		if ( needsParenthesis && needsDmlSubqueryWrapper() ) {
			appendSql( OPEN_PARENTHESIS );
			appendSql( "select * from " );
			super.visitSelectStatement( statement );
			appendSql( " _sub_" );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			super.visitSelectStatement( statement );
		}
	}

	@Override
	protected <X extends Expression> void renderRelationalEmulationSubQuery(
			QuerySpec subQuery,
			X lhsTuple,
			SubQueryRelationalRestrictionEmulationRenderer<X> renderer,
			ComparisonOperator tupleComparisonOperator) {
		if ( needsDmlSubqueryWrapper() ) {
			appendSql( OPEN_PARENTHESIS );
			appendSql( "select * from " );
			super.renderRelationalEmulationSubQuery( subQuery, lhsTuple, renderer, tupleComparisonOperator );
			appendSql( " _sub_" );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			super.renderRelationalEmulationSubQuery( subQuery, lhsTuple, renderer, tupleComparisonOperator );
		}
	}

	@Override
	protected void renderQuantifiedEmulationSubQuery(
			QuerySpec subQuery,
			ComparisonOperator tupleComparisonOperator) {
		if ( needsDmlSubqueryWrapper() ) {
			appendSql( OPEN_PARENTHESIS );
			appendSql( "select * from " );
			super.renderQuantifiedEmulationSubQuery( subQuery, tupleComparisonOperator );
			appendSql( " _sub_" );
			appendSql( CLOSE_PARENTHESIS );
		}
		else {
			super.renderQuantifiedEmulationSubQuery( subQuery, tupleComparisonOperator );
		}
	}

	@Override
	protected void renderFetchFirstRow() {
		appendSql( " limit 1" );
	}

}
