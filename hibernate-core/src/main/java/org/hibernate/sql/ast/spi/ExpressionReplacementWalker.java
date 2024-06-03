/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Collation;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.AggregateColumnWriteExpression;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.EmbeddableTypeLiteral;
import org.hibernate.sql.ast.tree.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.expression.Every;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.ModifiedSubQueryExpression;
import org.hibernate.sql.ast.tree.expression.NestedColumnReference;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.expression.Overflow;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.expression.UnparsedNumericLiteral;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.predicate.InArrayPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.predicate.ThruthnessPredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.internal.TableDeleteCustomSql;
import org.hibernate.sql.model.internal.TableDeleteStandard;
import org.hibernate.sql.model.internal.TableInsertCustomSql;
import org.hibernate.sql.model.internal.TableInsertStandard;
import org.hibernate.sql.model.internal.TableUpdateCustomSql;
import org.hibernate.sql.model.internal.TableUpdateStandard;

/**
 * A walker that allows to replace expressions.
 *
 * @author Christian Beikov
 */
public class ExpressionReplacementWalker implements SqlAstWalker {

	/**
	 * To avoid introducing a walker/visitor that returns an object,
	 * we use a heap variable to transfer the return value.
	 */
	private SqlAstNode returnedNode;

	public final <X extends SqlAstNode> X replaceExpressions(X expression) {
		expression.accept( this );
		//noinspection unchecked
		return (X) returnedNode;
	}

	protected <X extends SqlAstNode> X replaceExpression(X expression) {
		return expression;
	}

	private void doReplaceExpression(SqlAstNode expression) {
		returnedNode = replaceExpression( expression );
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		doReplaceExpression( columnReference );
	}

	@Override
	public void visitNestedColumnReference(NestedColumnReference nestedColumnReference) {
		doReplaceExpression( nestedColumnReference );
	}

	@Override
	public void visitAggregateColumnWriteExpression(AggregateColumnWriteExpression aggregateColumnWriteExpression) {
		doReplaceExpression( aggregateColumnWriteExpression );
	}

	@Override
	public void visitExtractUnit(ExtractUnit extractUnit) {
		doReplaceExpression( extractUnit );
	}

	@Override
	public void visitFormat(Format format) {
		doReplaceExpression( format );
	}

	@Override
	public void visitDistinct(Distinct distinct) {
		doReplaceExpression( distinct );
	}

	@Override
	public void visitOverflow(Overflow overflow) {
		doReplaceExpression( overflow );
	}

	@Override
	public void visitStar(Star star) {
		doReplaceExpression( star );
	}

	@Override
	public void visitTrimSpecification(TrimSpecification trimSpecification) {
		doReplaceExpression( trimSpecification );
	}

	@Override
	public void visitCastTarget(CastTarget castTarget) {
		doReplaceExpression( castTarget );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		doReplaceExpression( arithmeticExpression );
	}

	@Override
	public void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
		doReplaceExpression( caseSearchedExpression );
	}

	@Override
	public void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression) {
		doReplaceExpression( caseSimpleExpression );
	}

	@Override
	public void visitAny(Any any) {
		doReplaceExpression( any );
	}

	@Override
	public void visitEvery(Every every) {
		doReplaceExpression( every );
	}

	@Override
	public void visitSummarization(Summarization every) {
		doReplaceExpression( every );
	}

	@Override
	public void visitOver(Over<?> over) {
		doReplaceExpression( over );
	}

	@Override
	public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
		doReplaceExpression( expression );
	}

	@Override
	public void visitSqlSelectionExpression(SqlSelectionExpression expression) {
		doReplaceExpression( expression );
	}

	@Override
	public void visitEntityTypeLiteral(EntityTypeLiteral expression) {
		doReplaceExpression( expression );
	}

	@Override
	public void visitEmbeddableTypeLiteral(EmbeddableTypeLiteral expression) {
		doReplaceExpression( expression );
	}

	@Override
	public void visitTuple(SqlTuple tuple) {
		doReplaceExpression( tuple );
	}

	@Override
	public void visitCollation(Collation collation) {
		doReplaceExpression( collation );
	}

	@Override
	public void visitParameter(JdbcParameter jdbcParameter) {
		doReplaceExpression( jdbcParameter );
	}

	@Override
	public void visitJdbcLiteral(JdbcLiteral<?> jdbcLiteral) {
		doReplaceExpression( jdbcLiteral );
	}

	@Override
	public void visitQueryLiteral(QueryLiteral<?> queryLiteral) {
		doReplaceExpression( queryLiteral );
	}

	@Override
	public <N extends Number> void visitUnparsedNumericLiteral(UnparsedNumericLiteral<N> literal) {
		doReplaceExpression( literal );
	}

	@Override
	public void visitUnaryOperationExpression(UnaryOperation unaryOperationExpression) {
		doReplaceExpression( unaryOperationExpression );
	}

	@Override
	public void visitModifiedSubQueryExpression(ModifiedSubQueryExpression expression) {
		doReplaceExpression( expression );
	}

	@Override
	public void visitDurationUnit(DurationUnit durationUnit) {
		doReplaceExpression( durationUnit );
	}

	@Override
	public void visitDuration(Duration duration) {
		doReplaceExpression( duration );
	}

	@Override
	public void visitConversion(Conversion conversion) {
		doReplaceExpression( conversion );
	}

	@Override
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		doReplaceExpression( booleanExpressionPredicate );
	}

	@Override
	public void visitSqlFragmentPredicate(SqlFragmentPredicate predicate) {
		doReplaceExpression( predicate );
	}

	@Override
	public void visitBetweenPredicate(BetweenPredicate betweenPredicate) {
		final Expression expression = replaceExpression( betweenPredicate.getExpression() );
		final Expression lowerBound = replaceExpression( betweenPredicate.getLowerBound() );
		final Expression upperBound = replaceExpression( betweenPredicate.getUpperBound() );
		if ( expression != betweenPredicate.getExpression()
				|| lowerBound != betweenPredicate.getLowerBound()
				|| upperBound != betweenPredicate.getUpperBound() ) {
			returnedNode = new BetweenPredicate(
					expression,
					lowerBound,
					upperBound,
					betweenPredicate.isNegated(),
					betweenPredicate.getExpressionType()
			);
		}
		else {
			returnedNode = betweenPredicate;
		}
	}

	@Override
	public void visitGroupedPredicate(GroupedPredicate groupedPredicate) {
		groupedPredicate.getSubPredicate().accept( this );
		if ( returnedNode != groupedPredicate.getSubPredicate() ) {
			returnedNode = new GroupedPredicate( (Predicate) returnedNode );
		}
		else {
			returnedNode = groupedPredicate;
		}
	}

	@Override
	public void visitInListPredicate(InListPredicate inListPredicate) {
		final Expression testExpression = replaceExpression( inListPredicate.getTestExpression() );
		List<Expression> items = null;
		final List<Expression> listExpressions = inListPredicate.getListExpressions();
		for ( int i = 0; i < listExpressions.size(); i++ ) {
			final Expression listExpression = listExpressions.get( i );
			final Expression newListExpression = replaceExpression( listExpression );
			if ( newListExpression != listExpression ) {
				if ( items == null ) {
					items = new ArrayList<>( listExpressions );
				}
				items.set( i, newListExpression );
			}
		}
		if ( testExpression != inListPredicate.getTestExpression() || items != null ) {
			returnedNode = new InListPredicate(
					testExpression,
					items == null ? listExpressions : items,
					inListPredicate.isNegated(),
					inListPredicate.getExpressionType()
			);
		}
		else {
			returnedNode = inListPredicate;
		}
	}

	@Override
	public void visitInArrayPredicate(InArrayPredicate inArrayPredicate) {
		final Expression replacedTestExpression = replaceExpression( inArrayPredicate.getTestExpression() );
		returnedNode = new InArrayPredicate( replacedTestExpression, inArrayPredicate.getArrayParameter() );
	}

	@Override
	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
		final Expression testExpression = replaceExpression( inSubQueryPredicate.getTestExpression() );
		final SelectStatement subQuery = replaceExpression( inSubQueryPredicate.getSubQuery() );
		if ( testExpression != inSubQueryPredicate.getTestExpression()
				|| subQuery != inSubQueryPredicate.getSubQuery() ) {
			returnedNode = new InSubQueryPredicate(
					testExpression,
					subQuery,
					inSubQueryPredicate.isNegated(),
					inSubQueryPredicate.getExpressionType()
			);
		}
		else {
			returnedNode = inSubQueryPredicate;
		}
	}

	@Override
	public void visitExistsPredicate(ExistsPredicate existsPredicate) {
		final SelectStatement selectStatement = replaceExpression( existsPredicate.getExpression() );
		if ( selectStatement != existsPredicate.getExpression() ) {
			returnedNode = new ExistsPredicate(
					selectStatement,
					existsPredicate.isNegated(),
					existsPredicate.getExpressionType()
			);
		}
		else {
			returnedNode = existsPredicate;
		}
	}

	@Override
	public void visitJunction(Junction junction) {
		final List<Predicate> predicates = junction.getPredicates();
		List<Predicate> newPredicates = null;
		for ( int i = 0; i < predicates.size(); i++ ) {
			predicates.get( i ).accept( this );
			if ( returnedNode != predicates.get( i ) ) {
				if ( newPredicates == null ) {
					newPredicates = new ArrayList<>( predicates );
				}
				newPredicates.set( i, (Predicate) returnedNode );
			}
		}
		if ( newPredicates != null ) {
			returnedNode = new Junction(
					junction.getNature(),
					newPredicates,
					junction.getExpressionType()
			);
		}
		else {
			returnedNode = junction;
		}
	}

	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		final Expression matchExpression = replaceExpression( likePredicate.getMatchExpression() );
		final Expression patternExpression = replaceExpression( likePredicate.getPattern() );
		final Expression escapeExpression = likePredicate.getEscapeCharacter() == null
				? null
				: replaceExpression( likePredicate.getEscapeCharacter() );
		if ( matchExpression != likePredicate.getMatchExpression()
				|| patternExpression != likePredicate.getPattern()
				|| escapeExpression != likePredicate.getEscapeCharacter() ) {
			returnedNode = new LikePredicate(
					matchExpression,
					patternExpression,
					escapeExpression,
					likePredicate.isNegated(),
					likePredicate.isCaseSensitive(),
					likePredicate.getExpressionType()
			);
		}
		else {
			returnedNode = likePredicate;
		}
	}

	@Override
	public void visitNegatedPredicate(NegatedPredicate negatedPredicate) {
		negatedPredicate.getPredicate().accept( this );
		if ( returnedNode != negatedPredicate.getPredicate() ) {
			returnedNode = new NegatedPredicate( (Predicate) returnedNode );
		}
		else {
			returnedNode = negatedPredicate;
		}
	}

	@Override
	public void visitNullnessPredicate(NullnessPredicate nullnessPredicate) {
		final Expression expression = replaceExpression( nullnessPredicate.getExpression() );
		if ( expression != nullnessPredicate.getExpression() ) {
			returnedNode = new NullnessPredicate(
					expression,
					nullnessPredicate.isNegated(),
					nullnessPredicate.getExpressionType()
			);
		}
		else {
			returnedNode = nullnessPredicate;
		}
	}

	@Override
	public void visitThruthnessPredicate(ThruthnessPredicate thruthnessPredicate) {
		final Expression expression = replaceExpression( thruthnessPredicate.getExpression() );
		if ( expression != thruthnessPredicate.getExpression() ) {
			returnedNode = new ThruthnessPredicate(
					expression,
					thruthnessPredicate.getBooleanValue(),
					thruthnessPredicate.isNegated(),
					thruthnessPredicate.getExpressionType()
			);
		}
		else {
			returnedNode = thruthnessPredicate;
		}
	}

	@Override
	public void visitRelationalPredicate(ComparisonPredicate comparisonPredicate) {
		final Expression lhs = replaceExpression( comparisonPredicate.getLeftHandExpression() );
		final Expression rhs = replaceExpression( comparisonPredicate.getRightHandExpression() );
		if ( lhs != comparisonPredicate.getLeftHandExpression()
				|| rhs != comparisonPredicate.getRightHandExpression() ) {
			returnedNode = new ComparisonPredicate(
					lhs,
					comparisonPredicate.getOperator(),
					rhs,
					comparisonPredicate.getExpressionType()
			);
		}
		else {
			returnedNode = comparisonPredicate;
		}
	}

	@Override
	public void visitSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate) {
		final SelfRenderingExpression selfRenderingExpression = replaceExpression( selfRenderingPredicate.getSelfRenderingExpression() );
		if ( selfRenderingExpression != selfRenderingPredicate.getSelfRenderingExpression() ) {
			returnedNode = new SelfRenderingPredicate(
					selfRenderingExpression
			);
		}
		else {
			returnedNode = selfRenderingPredicate;
		}
	}

	/* Unsupported */

	@Override
	public void visitSelectStatement(SelectStatement statement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitDeleteStatement(DeleteStatement statement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitUpdateStatement(UpdateStatement statement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitInsertStatement(InsertSelectStatement statement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitAssignment(Assignment assignment) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitSortSpecification(SortSpecification sortSpecification) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitOffsetFetchClause(QueryPart querySpec) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitSelectClause(SelectClause selectClause) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitSqlSelection(SqlSelection sqlSelection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitFromClause(FromClause fromClause) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitTableGroup(TableGroup tableGroup) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitNamedTableReference(NamedTableReference tableReference) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitFunctionTableReference(FunctionTableReference tableReference) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitFilterPredicate(FilterPredicate filterPredicate) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitFilterFragmentPredicate(FilterPredicate.FilterFragmentPredicate fragmentPredicate) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitStandardTableInsert(TableInsertStandard tableInsert) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitCustomTableInsert(TableInsertCustomSql tableInsert) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitStandardTableUpdate(TableUpdateStandard tableUpdate) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitOptionalTableUpdate(OptionalTableUpdate tableUpdate) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitCustomTableUpdate(TableUpdateCustomSql tableUpdate) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitStandardTableDelete(TableDeleteStandard tableDelete) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitCustomTableDelete(TableDeleteCustomSql tableDelete) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitColumnWriteFragment(ColumnWriteFragment columnWriteFragment) {
		throw new UnsupportedOperationException();
	}
}
