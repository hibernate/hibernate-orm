/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.spi;

import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.AggregateColumnWriteExpression;
import org.hibernate.sql.ast.tree.expression.AggregateFunctionExpression;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Collation;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.EmbeddableTypeLiteral;
import org.hibernate.sql.ast.tree.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.expression.Every;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.ModifiedSubQueryExpression;
import org.hibernate.sql.ast.tree.expression.NestedColumnReference;
import org.hibernate.sql.ast.tree.expression.OrderedSetAggregateFunctionExpression;
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
import org.hibernate.sql.ast.tree.insert.Values;
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
 * A simple walker that checks for aggregate functions.
 *
 * @author Christian Beikov
 */
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
		if ( expression instanceof FunctionExpression ) {
			final FunctionExpression functionExpression = (FunctionExpression) expression;
			for ( SqlAstNode argument : functionExpression.getArguments() ) {
				argument.accept( this );
			}
			if ( expression instanceof AggregateFunctionExpression ) {
				final AggregateFunctionExpression aggregateFunctionExpression = (AggregateFunctionExpression) expression;
				if ( aggregateFunctionExpression.getFilter() != null ) {
					aggregateFunctionExpression.getFilter().accept( this );
				}
				if ( expression instanceof OrderedSetAggregateFunctionExpression ) {
					for ( SortSpecification specification : ( (OrderedSetAggregateFunctionExpression) expression ).getWithinGroup() ) {
						specification.accept( this );
					}
				}
			}
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
