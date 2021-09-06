/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.spi;

import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.AggregateFunctionExpression;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Collate;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.expression.Every;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.NullnessLiteral;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;

/**
 * A simple walker that checks for aggregate functions.
 *
 * @author Christian Beikov
 */
class AggregateFunctionChecker implements SqlAstWalker {

	private static final AggregateFunctionChecker INSTANCE = new AggregateFunctionChecker();

	private static class AggregateFunctionException extends RuntimeException {}

	public static boolean hasAggregateFunctions(QuerySpec querySpec) {
		try {
			querySpec.getSelectClause().accept( INSTANCE );
			querySpec.visitSortSpecifications( INSTANCE::visitSortSpecification );
			return false;
		}
		catch (AggregateFunctionException ex) {
			return true;
		}
	}

	@Override
	public void visitAny(Any any) {
		throw new AggregateFunctionException();
	}

	@Override
	public void visitEvery(Every every) {
		throw new AggregateFunctionException();
	}

	@Override
	public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
		if ( expression instanceof AggregateFunctionExpression ) {
			throw new AggregateFunctionException();
		}
		else if ( expression instanceof FunctionExpression ) {
			for ( SqlAstNode argument : ( (FunctionExpression) expression ).getArguments() ) {
				argument.accept( this );
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
	public void visitCollate(Collate collate) {
		collate.getExpression().accept( this );
	}

	@Override
	public void visitUnaryOperationExpression(UnaryOperation unaryOperationExpression) {
		unaryOperationExpression.getOperand().accept( this );
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
	public void visitRelationalPredicate(ComparisonPredicate comparisonPredicate) {
		comparisonPredicate.getLeftHandExpression().accept( this );
		comparisonPredicate.getRightHandExpression().accept( this );
	}

	@Override
	public void visitSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate) {
		selfRenderingPredicate.getSelfRenderingExpression().accept( this );
	}

	// --- to ignore ---

	@Override
	public void visitSelectStatement(SelectStatement statement) {
	}

	@Override
	public void visitDeleteStatement(DeleteStatement statement) {
	}

	@Override
	public void visitUpdateStatement(UpdateStatement statement) {
	}

	@Override
	public void visitInsertStatement(InsertStatement statement) {
	}

	@Override
	public void visitAssignment(Assignment assignment) {
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
	}

	@Override
	public void visitExtractUnit(ExtractUnit extractUnit) {
	}

	@Override
	public void visitFormat(Format format) {
	}

	@Override
	public void visitDistinct(Distinct distinct) {
	}

	@Override
	public void visitStar(Star star) {
	}

	@Override
	public void visitOffsetFetchClause(QueryPart querySpec) {
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
	public void visitDuration(Duration duration) {
	}

	@Override
	public void visitConversion(Conversion conversion) {
	}

	@Override
	public void visitInListPredicate(InListPredicate inListPredicate) {
	}

	@Override
	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
	}

	@Override
	public void visitExistsPredicate(ExistsPredicate existsPredicate) {
	}

	@Override
	public void visitFilterPredicate(FilterPredicate filterPredicate) {
	}

	@Override
	public void visitParameter(JdbcParameter jdbcParameter) {
	}

	@Override
	public void visitJdbcLiteral(JdbcLiteral jdbcLiteral) {
	}

	@Override
	public void visitQueryLiteral(QueryLiteral queryLiteral) {
	}

	@Override
	public void visitNullnessLiteral(NullnessLiteral nullnessLiteral) {
	}

	@Override
	public void visitSummarization(Summarization every) {
	}

	@Override
	public void visitEntityTypeLiteral(EntityTypeLiteral expression) {
	}

	@Override
	public void visitSqlSelectionExpression(SqlSelectionExpression expression) {
	}

	@Override
	public void visitTableReference(TableReference tableReference) {
	}

	@Override
	public void visitTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
	}

	@Override
	public void visitFromClause(FromClause fromClause) {
	}

	@Override
	public void visitTableGroup(TableGroup tableGroup) {
	}

	@Override
	public void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
	}
}
