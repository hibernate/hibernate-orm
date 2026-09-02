/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.SPI;
import org.hibernate.sql.ast.spi.query.predicate.SqlFragmentPredicate;
import org.hibernate.query.sqm.tree.spi.expression.Conversion;
import org.hibernate.sql.ast.spi.query.cte.CteStatement;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.AggregateColumnWriteExpression;
import org.hibernate.sql.ast.spi.query.expression.AggregateFunctionExpression;
import org.hibernate.sql.ast.spi.query.expression.AliasedExpression;
import org.hibernate.sql.ast.spi.query.expression.Any;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.CastTarget;
import org.hibernate.sql.ast.spi.query.expression.Collation;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Distinct;
import org.hibernate.sql.ast.spi.query.expression.Duration;
import org.hibernate.sql.ast.spi.query.expression.DurationUnit;
import org.hibernate.sql.ast.spi.query.expression.EmbeddableTypeLiteral;
import org.hibernate.sql.ast.spi.query.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.spi.query.expression.Every;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.ExtractUnit;
import org.hibernate.sql.ast.spi.query.expression.Format;
import org.hibernate.sql.ast.spi.query.expression.FunctionExpression;
import org.hibernate.sql.ast.spi.query.expression.JdbcLiteral;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.expression.ModifiedSubQueryExpression;
import org.hibernate.sql.ast.spi.query.expression.NestedColumnReference;
import org.hibernate.sql.ast.spi.query.expression.OrderedSetAggregateFunctionExpression;
import org.hibernate.sql.ast.spi.query.expression.Over;
import org.hibernate.sql.ast.spi.query.expression.Overflow;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.spi.query.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Star;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.expression.TrimSpecification;
import org.hibernate.sql.ast.spi.query.expression.UnaryOperation;
import org.hibernate.sql.ast.spi.query.expression.UnparsedNumericLiteral;
import org.hibernate.sql.ast.spi.query.expression.WindowFunctionExpression;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.from.FunctionTableReference;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.QueryPartTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;
import org.hibernate.sql.ast.spi.query.from.ValuesTableReference;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.insert.Values;
import org.hibernate.sql.ast.spi.query.predicate.BetweenPredicate;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.ExistsPredicate;
import org.hibernate.sql.ast.spi.query.predicate.FilterPredicate;
import org.hibernate.sql.ast.spi.query.predicate.GroupedPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InArrayPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InListPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Junction;
import org.hibernate.sql.ast.spi.query.predicate.LikePredicate;
import org.hibernate.sql.ast.spi.query.predicate.NegatedPredicate;
import org.hibernate.sql.ast.spi.query.predicate.NullnessPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.spi.query.predicate.ThruthnessPredicate;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.query.update.Assignment;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.ast.spi.model.TableDeleteCustomSql;
import org.hibernate.sql.ast.spi.model.TableDeleteStandard;
import org.hibernate.sql.ast.spi.model.TableInsertCustomSql;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.sql.ast.spi.model.TableUpdateCustomSql;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;

/**
 * A simple walker that checks for aggregate functions.
 *
 * @author Christian Beikov
 */
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public class AbstractSqlAstWalker implements SqlAstWalker {

	@Override
	public void visitAny(Any any) {
		any.getSubquery().accept( this );
	}

	@Override
	public void visitEvery(Every every) {
		every.getSubquery().accept( this );
	}

	@Override
	public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
		if ( expression instanceof FunctionExpression functionExpression ) {
			for ( SqlAstNode argument : functionExpression.getArguments() ) {
				argument.accept( this );
			}
			if ( expression instanceof AggregateFunctionExpression aggregateFunctionExpression ) {
				if ( aggregateFunctionExpression.getFilter() != null ) {
					aggregateFunctionExpression.getFilter().accept( this );
				}
				if ( expression instanceof OrderedSetAggregateFunctionExpression orderedSetAggregateFunctionExpression ) {
					for ( SortSpecification specification : orderedSetAggregateFunctionExpression.getWithinGroup() ) {
						specification.accept( this );
					}
				}
			}
			else if ( expression instanceof WindowFunctionExpression windowFunctionExpression ) {
				if ( windowFunctionExpression.getFilter() != null ) {
					windowFunctionExpression.getFilter().accept( this );
				}
			}
		}
		else if ( expression instanceof AliasedExpression aliasedExpression ) {
			aliasedExpression.getExpression().accept( this );
		}
	}

	@Override
	public void visitSortSpecification(SortSpecification sortSpecification) {
		sortSpecification.getSortExpression().accept( this );
	}

	@Override
	public void visitSelectClause(SelectClause selectClause) {
		for ( SqlSelection sqlSelection : selectClause.getSqlSelections() ) {
			sqlSelection.accept( this );
		}
	}

	@Override
	public void visitSqlSelection(SqlSelection sqlSelection) {
		sqlSelection.getExpression().accept( this );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		arithmeticExpression.getLeftHandOperand().accept( this );
		arithmeticExpression.getRightHandOperand().accept( this );
	}

	@Override
	public void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
		for ( CaseSearchedExpression.WhenFragment whenFragment : caseSearchedExpression.getWhenFragments() ) {
			whenFragment.getPredicate().accept( this );
			whenFragment.getResult().accept( this );
		}
		if ( caseSearchedExpression.getOtherwise() != null ) {
			caseSearchedExpression.getOtherwise().accept( this );
		}
	}

	@Override
	public void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression) {
		caseSimpleExpression.getFixture().accept( this );
		for ( CaseSimpleExpression.WhenFragment whenFragment : caseSimpleExpression.getWhenFragments() ) {
			whenFragment.getCheckValue().accept( this );
			whenFragment.getResult().accept( this );
		}
		if ( caseSimpleExpression.getOtherwise() != null ) {
			caseSimpleExpression.getOtherwise().accept( this );
		}
	}

	@Override
	public void visitTuple(SqlTuple tuple) {
		for ( Expression expression : tuple.getExpressions() ) {
			expression.accept( this );
		}
	}

	@Override
	public void visitCollation(Collation collation) {
	}

	@Override
	public void visitUnaryOperationExpression(UnaryOperation unaryOperationExpression) {
		unaryOperationExpression.getOperand().accept( this );
	}

	@Override
	public void visitModifiedSubQueryExpression(ModifiedSubQueryExpression expression) {
		expression.getSubQuery().accept( this );
	}

	@Override
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		booleanExpressionPredicate.getExpression().accept( this );
	}

	@Override
	public void visitBetweenPredicate(BetweenPredicate betweenPredicate) {
		betweenPredicate.getExpression().accept( this );
		betweenPredicate.getLowerBound().accept( this );
		betweenPredicate.getUpperBound().accept( this );
	}

	@Override
	public void visitGroupedPredicate(GroupedPredicate groupedPredicate) {
		groupedPredicate.getSubPredicate().accept( this );
	}

	@Override
	public void visitJunction(Junction junction) {
		for ( Predicate predicate : junction.getPredicates() ) {
			predicate.accept( this );
		}
	}

	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		likePredicate.getMatchExpression().accept( this );
		if ( likePredicate.getEscapeCharacter() != null ) {
			likePredicate.getEscapeCharacter().accept( this );
		}
		likePredicate.getPattern().accept( this );
	}

	@Override
	public void visitNegatedPredicate(NegatedPredicate negatedPredicate) {
		negatedPredicate.getPredicate().accept( this );
	}

	@Override
	public void visitNullnessPredicate(NullnessPredicate nullnessPredicate) {
		nullnessPredicate.getExpression().accept( this );
	}

	@Override
	public void visitThruthnessPredicate(ThruthnessPredicate thruthnessPredicate) {
		thruthnessPredicate.getExpression().accept( this );
	}

	@Override
	public void visitRelationalPredicate(ComparisonPredicate comparisonPredicate) {
		comparisonPredicate.getLeftHandExpression().accept( this );
		comparisonPredicate.getRightHandExpression().accept( this );
	}

	@Override
	public void visitSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate) {
		selfRenderingPredicate.getSelfRenderingExpression().accept( this );
	}

	@Override
	public void visitOver(Over<?> over) {
		over.getExpression().accept( this );
		for ( Expression partition : over.getPartitions() ) {
			partition.accept( this );
		}
		for ( SortSpecification sortSpecification : over.getOrderList() ) {
			sortSpecification.accept( this );
		}
		if ( over.getStartExpression() != null ) {
			over.getStartExpression().accept( this );
		}
		if ( over.getEndExpression() != null ) {
			over.getEndExpression().accept( this );
		}
	}

	@Override
	public void visitSelectStatement(SelectStatement statement) {
		for ( CteStatement cteStatement : statement.getCteStatements().values() ) {
			cteStatement.getCteDefinition().accept( this );
		}
		statement.getQueryPart().accept( this );
	}

	@Override
	public void visitDeleteStatement(DeleteStatement statement) {
		for ( CteStatement cteStatement : statement.getCteStatements().values() ) {
			cteStatement.getCteDefinition().accept( this );
		}
		statement.getRestriction().accept( this );
	}

	@Override
	public void visitUpdateStatement(UpdateStatement statement) {
		for ( CteStatement cteStatement : statement.getCteStatements().values() ) {
			cteStatement.getCteDefinition().accept( this );
		}
		for ( Assignment assignment : statement.getAssignments() ) {
			assignment.accept( this );
		}
		statement.getRestriction().accept( this );
	}

	@Override
	public void visitInsertStatement(InsertSelectStatement statement) {
		for ( CteStatement cteStatement : statement.getCteStatements().values() ) {
			cteStatement.getCteDefinition().accept( this );
		}
		if ( statement.getSourceSelectStatement() != null ) {
			statement.getSourceSelectStatement().accept( this );
		}
		else if ( statement.getValuesList() != null ) {
			for ( Values values : statement.getValuesList() ) {
				for ( Expression expression : values.getExpressions() ) {
					expression.accept( this );
				}
			}
		}
	}

	@Override
	public void visitAssignment(Assignment assignment) {
		assignment.getAssignedValue().accept( this );
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		for ( QueryPart queryPart : queryGroup.getQueryParts() ) {
			queryPart.accept( this );
		}
		visitOffsetFetchClause( queryGroup );
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		querySpec.getSelectClause().accept( this );
		querySpec.getFromClause().accept( this );
		if ( querySpec.getWhereClauseRestrictions() != null ) {
			querySpec.getWhereClauseRestrictions().accept( this );
		}
		for ( Expression groupByClauseExpression : querySpec.getGroupByClauseExpressions() ) {
			groupByClauseExpression.accept( this );
		}
		if ( querySpec.getHavingClauseRestrictions() != null ) {
			querySpec.getHavingClauseRestrictions().accept( this );
		}
		visitOffsetFetchClause( querySpec );
	}

	@Override
	public void visitDistinct(Distinct distinct) {
		distinct.getExpression().accept( this );
	}

	@Override
	public void visitOverflow(Overflow overflow) {
		overflow.getSeparatorExpression().accept( this );
		if ( overflow.getFillerExpression() != null ) {
			overflow.getFillerExpression().accept( this );
		}
	}

	@Override
	public void visitOffsetFetchClause(QueryPart querySpec) {
		if ( querySpec.getSortSpecifications() != null ) {
			for ( SortSpecification sortSpecification : querySpec.getSortSpecifications() ) {
				sortSpecification.accept( this );
			}
		}
		if ( querySpec.getOffsetClauseExpression() != null ) {
			querySpec.getOffsetClauseExpression().accept( this );
		}
		if ( querySpec.getFetchClauseExpression() != null ) {
			querySpec.getFetchClauseExpression().accept( this );
		}
	}

	@Override
	public void visitDuration(Duration duration) {
		duration.getMagnitude().accept( this );
	}

	@Override
	public void visitConversion(Conversion conversion) {
		conversion.getDuration().accept( this );
	}

	@Override
	public void visitInListPredicate(InListPredicate inListPredicate) {
		inListPredicate.getTestExpression().accept( this );
		for ( Expression listExpression : inListPredicate.getListExpressions() ) {
			listExpression.accept( this );
		}
	}

	@Override
	public void visitInArrayPredicate(InArrayPredicate predicate) {
		predicate.getTestExpression().accept( this );
		predicate.getArrayParameter().accept( this );
	}

	@Override
	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
		inSubQueryPredicate.getTestExpression().accept( this );
		inSubQueryPredicate.getSubQuery().accept( this );
	}

	@Override
	public void visitExistsPredicate(ExistsPredicate existsPredicate) {
		existsPredicate.getExpression().accept( this );
	}

	@Override
	public void visitSummarization(Summarization every) {
		for ( Expression grouping : every.getGroupings() ) {
			grouping.accept( this );
		}
	}

	@Override
	public void visitSqlSelectionExpression(SqlSelectionExpression expression) {
		expression.accept( this );
	}

	@Override
	public void visitTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
		tableReferenceJoin.getJoinedTableReference().accept( this );
		if ( tableReferenceJoin.getPredicate() != null ) {
			tableReferenceJoin.getPredicate().accept( this );
		}
	}

	@Override
	public void visitFromClause(FromClause fromClause) {
		for ( TableGroup root : fromClause.getRoots() ) {
			root.accept( this );
		}
	}

	@Override
	public void visitTableGroup(TableGroup tableGroup) {
		tableGroup.getPrimaryTableReference().accept( this );
		for ( TableReferenceJoin tableReferenceJoin : tableGroup.getTableReferenceJoins() ) {
			tableReferenceJoin.accept( this );
		}
		for ( TableGroupJoin tableGroupJoin : tableGroup.getTableGroupJoins() ) {
			tableGroupJoin.accept( this );
		}
		for ( TableGroupJoin nestedTableGroupJoin : tableGroup.getNestedTableGroupJoins() ) {
			nestedTableGroupJoin.accept( this );
		}
	}

	@Override
	public void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
		final TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();
		if ( joinedGroup.isInitialized() ) {
			// Only process already initialized table groups to avoid
			// forced initialization of joined lazy table groups
			joinedGroup.accept( this );
		}
		if ( tableGroupJoin.getPredicate() != null ) {
			tableGroupJoin.getPredicate().accept( this );
		}
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
	}

	@Override
	public void visitNestedColumnReference(NestedColumnReference nestedColumnReference) {
	}

	@Override
	public void visitAggregateColumnWriteExpression(AggregateColumnWriteExpression aggregateColumnWriteExpression) {
	}

	@Override
	public void visitExtractUnit(ExtractUnit extractUnit) {
	}

	@Override
	public void visitFormat(Format format) {
	}

	@Override
	public void visitStar(Star star) {
	}

	@Override
	public void visitTrimSpecification(TrimSpecification trimSpecification) {
	}

	@Override
	public void visitCastTarget(CastTarget castTarget) {
	}

	@Override
	public void visitDurationUnit(DurationUnit durationUnit) {
	}

	@Override
	public void visitFilterPredicate(FilterPredicate filterPredicate) {
	}

	@Override
	public void visitFilterFragmentPredicate(FilterPredicate.FilterFragmentPredicate fragmentPredicate) {
	}

	@Override
	public void visitSqlFragmentPredicate(SqlFragmentPredicate predicate) {
	}

	@Override
	public void visitParameter(JdbcParameter jdbcParameter) {
	}

	@Override
	public void visitJdbcLiteral(JdbcLiteral<?> jdbcLiteral) {
	}

	@Override
	public void visitQueryLiteral(QueryLiteral<?> queryLiteral) {
	}

	@Override
	public <N extends Number> void visitUnparsedNumericLiteral(UnparsedNumericLiteral<N> literal) {
	}

	@Override
	public void visitEntityTypeLiteral(EntityTypeLiteral expression) {
	}

	@Override
	public void visitEmbeddableTypeLiteral(EmbeddableTypeLiteral expression) {
	}

	@Override
	public void visitNamedTableReference(NamedTableReference tableReference) {
	}

	@Override
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		for ( Values values : tableReference.getValuesList() ) {
			for ( Expression expression : values.getExpressions() ) {
				expression.accept( this );
			}
		}
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		tableReference.getStatement().accept( this );
	}

	@Override
	public void visitFunctionTableReference(FunctionTableReference tableReference) {
		for ( SqlAstNode argument : tableReference.getFunctionExpression().getArguments() ) {
			argument.accept( this );
		}
	}


	@Override
	public void visitStandardTableInsert(TableInsertStandard tableInsert) {
		tableInsert.getMutatingTable().accept( this );

		tableInsert.forEachValueBinding( (integer, columnValueBinding) -> {
			columnValueBinding.getColumnReference().accept( this );
			columnValueBinding.getValueExpression().accept( this );
		} );

		tableInsert.forEachReturningColumn( (integer, columnReference) -> {
			columnReference.accept( this );
		} );
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
	public void visitColumnWriteFragment(ColumnWriteFragment columnWriteFragment) {
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
}
