/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.query.sqm.function.SelfRenderingAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingOrderedSetAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingWindowFunctionSqlAstExpression;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.AliasedExpression;
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
		final var newExpression = replaceExpression( expression );
		if ( newExpression != expression ) {
			return newExpression;
		}
		else {
			expression.accept( this );
			//noinspection unchecked
			final X result = (X) returnedNode;
			returnedNode = null;
			return result;
		}
	}

	protected <X extends SqlAstNode> X replaceExpression(X expression) {
		return expression;
	}

	private void doReplaceExpression(SqlAstNode expression) {
		assert isLeafExpression( expression );
		returnedNode = replaceExpression( expression );
	}

	protected boolean isLeafExpression(SqlAstNode expression) {
		return expression instanceof ColumnReference
				|| expression instanceof AggregateColumnWriteExpression
				|| expression instanceof ExtractUnit
				|| expression instanceof Format
				|| expression instanceof Star
				|| expression instanceof TrimSpecification
				|| expression instanceof CastTarget
				|| expression instanceof SqlSelectionExpression
				|| expression instanceof EntityTypeLiteral
				|| expression instanceof EmbeddableTypeLiteral
				|| expression instanceof Collation
				|| expression instanceof JdbcParameter
				|| expression instanceof JdbcLiteral<?>
				|| expression instanceof QueryLiteral<?>
				|| expression instanceof UnparsedNumericLiteral<?>
				|| expression instanceof DurationUnit
				|| expression instanceof SqlFragmentPredicate
				|| expression instanceof SelfRenderingExpression
					&& !(expression instanceof AliasedExpression)
					&& !(expression instanceof SelfRenderingFunctionSqlAstExpression);
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
		final Expression expression = replaceExpressions( distinct.getExpression() );
		if ( expression != distinct.getExpression() ) {
			returnedNode = new Distinct( expression );
		}
		else {
			returnedNode = distinct;
		}
	}

	@Override
	public void visitOverflow(Overflow overflow) {
		final Expression separatorExpression = replaceExpressions( overflow.getSeparatorExpression() );
		final Expression fillerExpression = replaceExpressions( overflow.getFillerExpression() );
		if ( separatorExpression != overflow.getSeparatorExpression()
			|| fillerExpression != overflow.getFillerExpression() ) {
			returnedNode = new Overflow( separatorExpression, fillerExpression, overflow.isWithCount() );
		}
		else {
			returnedNode = overflow;
		}
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
		final Expression lhsExpression = replaceExpressions( arithmeticExpression.getLeftHandOperand() );
		final Expression rhsExpression = replaceExpressions( arithmeticExpression.getRightHandOperand() );
		if ( lhsExpression != arithmeticExpression.getLeftHandOperand()
			|| rhsExpression != arithmeticExpression.getRightHandOperand() ) {
			returnedNode = new BinaryArithmeticExpression(
					lhsExpression,
					arithmeticExpression.getOperator(),
					rhsExpression,
					arithmeticExpression.getExpressionType()
			);
		}
		else {
			returnedNode = arithmeticExpression;
		}
	}

	@Override
	public void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
		final var fragments = caseSearchedExpression.getWhenFragments();
		List<CaseSearchedExpression.WhenFragment> newFragments = null;
		for ( int i = 0; i < fragments.size(); i++ ) {
			final var fragment = fragments.get( i );
			final var predicate = fragment.getPredicate();
			final var result = fragment.getResult();
			final var newPred = replaceExpressions( predicate );
			final var newResult = replaceExpressions( result );
			if ( newPred != predicate || newResult != result ) {
				if ( newFragments == null ) {
					newFragments = new ArrayList<>( fragments );
				}
				newFragments.set( i, new CaseSearchedExpression.WhenFragment( newPred, newResult ) );
			}
		}
		final var originalOtherwise = caseSearchedExpression.getOtherwise();
		final var newOtherwise =
				originalOtherwise == null
						? null
						: replaceExpressions( originalOtherwise );
		if ( newFragments != null || newOtherwise != originalOtherwise ) {
			returnedNode = new CaseSearchedExpression(
					caseSearchedExpression.getExpressionType(),
					newFragments != null ? newFragments : fragments,
					newOtherwise
			);
		}
		else {
			returnedNode = caseSearchedExpression;
		}
	}

	@Override
	public void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression) {
		final var newFixture = replaceExpressions( caseSimpleExpression.getFixture() );
		final var fragments = caseSimpleExpression.getWhenFragments();
		List<CaseSimpleExpression.WhenFragment> newFragments = null;
		for ( int i = 0; i < fragments.size(); i++ ) {
			final var fragment = fragments.get( i );
			final var checkValue = fragment.getCheckValue();
			final var result = fragment.getResult();
			final var newCheck = replaceExpressions( checkValue );
			final var newResult = replaceExpressions( result );
			if ( newCheck != checkValue || newResult != result ) {
				if ( newFragments == null ) {
					newFragments = new ArrayList<>( fragments );
				}
				newFragments.set( i, new CaseSimpleExpression.WhenFragment( newCheck, newResult ) );
			}
		}
		final var originalOtherwise = caseSimpleExpression.getOtherwise();
		final var newOtherwise =
				originalOtherwise == null
						? null
						: replaceExpressions( originalOtherwise );
		if ( newFixture != caseSimpleExpression.getFixture()
			|| newFragments != null
			|| newOtherwise != originalOtherwise ) {
			returnedNode = new CaseSimpleExpression(
					caseSimpleExpression.getExpressionType(),
					newFixture,
					newFragments != null ? newFragments : fragments,
					newOtherwise
			);
		}
		else {
			returnedNode = caseSimpleExpression;
		}
	}

	@Override
	public void visitAny(Any any) {
		final SelectStatement subquery = replaceExpressions( any.getSubquery() );
		if ( subquery != any.getSubquery() ) {
			returnedNode = new Any( subquery, any.getExpressionType() );
		}
		else {
			returnedNode = any;
		}
	}

	@Override
	public void visitEvery(Every every) {
		final SelectStatement subquery = replaceExpressions( every.getSubquery() );
		if ( subquery != every.getSubquery() ) {
			returnedNode = new Every( subquery, every.getExpressionType() );
		}
		else {
			returnedNode = every;
		}
	}

	@Override
	public void visitSummarization(Summarization summarization) {
		final var groupings = summarization.getGroupings();
		List<Expression> newGroupings = null;
		for ( int i = 0; i < groupings.size(); i++ ) {
			final var grouping = groupings.get( i );
			final var newGrouping = replaceExpressions( grouping );
			if ( newGrouping != grouping ) {
				if ( newGroupings == null ) {
					newGroupings = new ArrayList<>( groupings );
				}
				newGroupings.set( i, newGrouping );
			}
		}
		if ( newGroupings != null ) {
			returnedNode = new Summarization( summarization.getKind(), newGroupings );
		}
		else {
			returnedNode = summarization;
		}
	}

	@Override
	public void visitOver(Over<?> over) {
		final var expression = over.getExpression();
		final var newExpression = replaceExpressions( expression );
		final var partitions = over.getPartitions();
		List<Expression> newPartitions = null;
		for ( int i = 0; i < partitions.size(); i++ ) {
			final var partition = partitions.get( i );
			final var newPartition = replaceExpressions( partition );
			if ( newPartition != partition ) {
				if ( newPartitions == null ) {
					newPartitions = new ArrayList<>( partitions );
				}
				newPartitions.set( i, newPartition );
			}
		}
		final var orderList = over.getOrderList();
		List<SortSpecification> newOrderList = null;
		for ( int i = 0; i < orderList.size(); i++ ) {
			final var sortSpecification = orderList.get( i );
			final var newSortSpecification = replaceExpressions( sortSpecification );
			if ( newSortSpecification != sortSpecification ) {
				if ( newOrderList == null ) {
					newOrderList = new ArrayList<>( orderList );
				}
				newOrderList.set( i, newSortSpecification );
			}
		}
		final var startExpression = over.getStartExpression();
		final var newStartExpression = replaceExpressions( startExpression );
		final var endExpression = over.getEndExpression();
		final var newEndExpression = replaceExpressions( endExpression );
		if ( expression != newExpression
			|| newPartitions != null
			|| newOrderList != null
			|| newStartExpression != startExpression
			|| newEndExpression != endExpression ) {
			returnedNode = new Over<>(
					newExpression,
					newPartitions != null ? newPartitions : partitions,
					newOrderList != null ? newOrderList : orderList,
					over.getMode(),
					over.getStartKind(),
					newStartExpression,
					over.getEndKind(),
					newEndExpression,
					over.getExclusion()
			);
		}
		else {
			returnedNode = over;
		}
	}

	@Override
	public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
		if ( expression instanceof AliasedExpression aliasExpression ) {
			final var aliasedExpression = aliasExpression.getExpression();
			final var newExpression = replaceExpressions( aliasedExpression );
			if ( aliasedExpression != newExpression ) {
				returnedNode = new AliasedExpression( newExpression, aliasExpression.getAlias() );
			}
			else {
				returnedNode = aliasedExpression;
			}
		}
		else if ( expression instanceof SelfRenderingFunctionSqlAstExpression<?> functionExpression ) {
			final var arguments = functionExpression.getArguments();
			List<SqlAstNode> newArguments = null;
			for ( int i = 0; i < arguments.size(); i++ ) {
				final var argument = arguments.get( i );
				final var newArgument = replaceExpressions( argument );
				if ( newArgument != newArguments ) {
					if ( newArguments == null ) {
						newArguments = new ArrayList<>( arguments );
					}
					newArguments.set( i, newArgument );
				}
			}
			if ( expression instanceof SelfRenderingAggregateFunctionSqlAstExpression<?> aggregate ) {
				final var filter = aggregate.getFilter();
				final var newFilter = replaceExpressions( filter );
				if ( expression instanceof SelfRenderingOrderedSetAggregateFunctionSqlAstExpression<?> setAggregate ) {
					final var withinGroup = setAggregate.getWithinGroup();
					List<SortSpecification> newWithinGroup = null;
					for ( int i = 0; i < withinGroup.size(); i++ ) {
						final var sortSpecification = withinGroup.get( i );
						final var newSortSpecification = replaceExpressions( sortSpecification );
						if ( newSortSpecification != sortSpecification ) {
							if ( newWithinGroup == null ) {
								newWithinGroup = new ArrayList<>( withinGroup );
							}
							newWithinGroup.set( i, newSortSpecification );
						}
					}
					if ( newArguments != null || newFilter != filter || newWithinGroup != null ) {
						returnedNode = new SelfRenderingOrderedSetAggregateFunctionSqlAstExpression<>(
								functionExpression.getFunctionName(),
								functionExpression.getFunctionRenderer(),
								newArguments != null ? newArguments : arguments,
								newFilter,
								newWithinGroup != null ? newWithinGroup : withinGroup,
								functionExpression.getType(),
								functionExpression.getExpressible()
						);
					}
					else {
						returnedNode = functionExpression;
					}
				}
				else {
					if ( newArguments != null || newFilter != filter ) {
						returnedNode = new SelfRenderingAggregateFunctionSqlAstExpression<>(
								functionExpression.getFunctionName(),
								functionExpression.getFunctionRenderer(),
								newArguments != null ? newArguments : arguments,
								newFilter,
								functionExpression.getType(),
								functionExpression.getExpressible()
						);
					}
					else {
						returnedNode = functionExpression;
					}
				}
			}
			else if ( expression instanceof SelfRenderingWindowFunctionSqlAstExpression<?> window ) {
				final var filter = window.getFilter();
				final var newFilter = replaceExpressions( filter );
				if ( newArguments != null || newFilter != filter ) {
					returnedNode = new SelfRenderingWindowFunctionSqlAstExpression<>(
							functionExpression.getFunctionName(),
							functionExpression.getFunctionRenderer(),
							newArguments != null ? newArguments : arguments,
							newFilter,
							window.getRespectNulls(),
							window.getFromFirst(),
							functionExpression.getType(),
							functionExpression.getExpressible()
					);
				}
				else {
					returnedNode = functionExpression;
				}
			}
			else {
				if ( newArguments != null ) {
					returnedNode = new SelfRenderingFunctionSqlAstExpression<>(
							functionExpression.getFunctionName(),
							functionExpression.getFunctionRenderer(),
							newArguments,
							functionExpression.getType(),
							functionExpression.getExpressible()
					);
				}
				else {
					returnedNode = functionExpression;
				}
			}
		}
		else {
			doReplaceExpression( expression );
		}
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
		final var expressions = tuple.getExpressions();
		List<Expression> newExpressions = null;
		for ( int i = 0; i < expressions.size(); i++ ) {
			final var expression = expressions.get( i );
			final var newExpression = replaceExpressions( expression );
			if ( newExpression != expression ) {
				if ( newExpressions == null ) {
					newExpressions = new ArrayList<>( expressions );
				}
				newExpressions.set( i, newExpression );
			}
		}
		if ( newExpressions != null ) {
			returnedNode = new SqlTuple( newExpressions,  tuple.getExpressionType() );
		}
		else {
			returnedNode = tuple;
		}
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
		final var operand = unaryOperationExpression.getOperand();
		final var newOperand = replaceExpressions( operand );
		if ( newOperand != operand ) {
			returnedNode = new UnaryOperation(
					unaryOperationExpression.getOperator(),
					newOperand,
					(BasicValuedMapping) unaryOperationExpression.getExpressionType()
			);
		}
		else {
			returnedNode = unaryOperationExpression;
		}
	}

	@Override
	public void visitModifiedSubQueryExpression(ModifiedSubQueryExpression expression) {
		final SelectStatement subquery = replaceExpressions( expression.getSubQuery() );
		if ( subquery != expression.getSubQuery() ) {
			returnedNode = new ModifiedSubQueryExpression( subquery, expression.getModifier() );
		}
		else {
			returnedNode = expression;
		}
	}

	@Override
	public void visitDurationUnit(DurationUnit durationUnit) {
		doReplaceExpression( durationUnit );
	}

	@Override
	public void visitDuration(Duration duration) {
		final var magnitude = duration.getMagnitude();
		final var newMagnitude = replaceExpressions( magnitude );
		if ( newMagnitude != magnitude ) {
			returnedNode = new Duration( newMagnitude, duration.getUnit(), duration.getExpressionType() );
		}
		else {
			returnedNode = duration;
		}
	}

	@Override
	public void visitConversion(Conversion conversion) {
		final var duration = conversion.getDuration();
		final var newDuration = replaceExpressions( duration );
		if ( newDuration != duration ) {
			returnedNode = new Conversion(
					newDuration,
					conversion.getUnit(),
					(BasicValuedMapping) conversion.getExpressionType()
			);
		}
		else {
			returnedNode = conversion;
		}
	}

	@Override
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		final var expression = booleanExpressionPredicate.getExpression();
		final var newExpressino = replaceExpressions( expression );
		if ( newExpressino != expression ) {
			returnedNode = new BooleanExpressionPredicate(
					newExpressino,
					booleanExpressionPredicate.isNegated(),
					booleanExpressionPredicate.getExpressionType()
			);
		}
		else {
			returnedNode = booleanExpressionPredicate;
		}
	}

	@Override
	public void visitSqlFragmentPredicate(SqlFragmentPredicate predicate) {
		doReplaceExpression( predicate );
	}

	@Override
	public void visitBetweenPredicate(BetweenPredicate betweenPredicate) {
		final Expression expression = replaceExpressions( betweenPredicate.getExpression() );
		final Expression lowerBound = replaceExpressions( betweenPredicate.getLowerBound() );
		final Expression upperBound = replaceExpressions( betweenPredicate.getUpperBound() );
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
		final Expression testExpression = replaceExpressions( inListPredicate.getTestExpression() );
		List<Expression> items = null;
		final List<Expression> listExpressions = inListPredicate.getListExpressions();
		for ( int i = 0; i < listExpressions.size(); i++ ) {
			final Expression listExpression = listExpressions.get( i );
			final Expression newListExpression = replaceExpressions( listExpression );
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
		final Expression replacedTestExpression = replaceExpressions( inArrayPredicate.getTestExpression() );
		returnedNode = new InArrayPredicate( replacedTestExpression, inArrayPredicate.getArrayParameter() );
	}

	@Override
	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
		final Expression testExpression = replaceExpressions( inSubQueryPredicate.getTestExpression() );
		final SelectStatement subQuery = replaceExpressions( inSubQueryPredicate.getSubQuery() );
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
		final SelectStatement selectStatement = replaceExpressions( existsPredicate.getExpression() );
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
		final Expression matchExpression = replaceExpressions( likePredicate.getMatchExpression() );
		final Expression patternExpression = replaceExpressions( likePredicate.getPattern() );
		final Expression escapeExpression = likePredicate.getEscapeCharacter() == null
				? null
				: replaceExpressions( likePredicate.getEscapeCharacter() );
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
		final Expression expression = replaceExpressions( nullnessPredicate.getExpression() );
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
		final Expression expression = replaceExpressions( thruthnessPredicate.getExpression() );
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
		final Expression lhs = replaceExpressions( comparisonPredicate.getLeftHandExpression() );
		final Expression rhs = replaceExpressions( comparisonPredicate.getRightHandExpression() );
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
		final SelfRenderingExpression selfRenderingExpression = replaceExpressions( selfRenderingPredicate.getSelfRenderingExpression() );
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
