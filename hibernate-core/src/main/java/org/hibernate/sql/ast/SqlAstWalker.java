/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import org.hibernate.Incubating;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.spi.SqlSelection;
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
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
@Incubating
public interface SqlAstWalker {

	void visitSelectStatement(SelectStatement statement);

	void visitDeleteStatement(DeleteStatement statement);

	void visitUpdateStatement(UpdateStatement statement);

	void visitInsertStatement(InsertSelectStatement statement);

	void visitAssignment(Assignment assignment);

	void visitQueryGroup(QueryGroup queryGroup);

	void visitQuerySpec(QuerySpec querySpec);

	void visitSortSpecification(SortSpecification sortSpecification);

	void visitOffsetFetchClause(QueryPart querySpec);

	void visitSelectClause(SelectClause selectClause);

	void visitSqlSelection(SqlSelection sqlSelection);

	void visitFromClause(FromClause fromClause);

	void visitTableGroup(TableGroup tableGroup);

	void visitTableGroupJoin(TableGroupJoin tableGroupJoin);

	void visitNamedTableReference(NamedTableReference tableReference);

	void visitValuesTableReference(ValuesTableReference tableReference);

	void visitQueryPartTableReference(QueryPartTableReference tableReference);

	void visitFunctionTableReference(FunctionTableReference tableReference);

	void visitTableReferenceJoin(TableReferenceJoin tableReferenceJoin);

	void visitColumnReference(ColumnReference columnReference);

	void visitNestedColumnReference(NestedColumnReference nestedColumnReference);

	void visitAggregateColumnWriteExpression(AggregateColumnWriteExpression aggregateColumnWriteExpression);

	void visitExtractUnit(ExtractUnit extractUnit);

	void visitFormat(Format format);

	void visitDistinct(Distinct distinct);

	void visitOverflow(Overflow overflow);

	void visitStar(Star star);

	void visitTrimSpecification(TrimSpecification trimSpecification);

	void visitCastTarget(CastTarget castTarget);

	void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression);

	void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression);

	void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression);

	void visitAny(Any any);

	void visitEvery(Every every);

	void visitSummarization(Summarization every);

	void visitOver(Over<?> over);

	void visitSelfRenderingExpression(SelfRenderingExpression expression);

	void visitSqlSelectionExpression(SqlSelectionExpression expression);

	void visitEntityTypeLiteral(EntityTypeLiteral expression);

	void visitEmbeddableTypeLiteral(EmbeddableTypeLiteral expression);

	void visitTuple(SqlTuple tuple);

	void visitCollation(Collation collation);

	void visitParameter(JdbcParameter jdbcParameter);

	void visitJdbcLiteral(JdbcLiteral<?> jdbcLiteral);

	void visitQueryLiteral(QueryLiteral<?> queryLiteral);

	<N extends Number> void visitUnparsedNumericLiteral(UnparsedNumericLiteral<N> literal);

	void visitUnaryOperationExpression(UnaryOperation unaryOperationExpression);

	void visitModifiedSubQueryExpression(ModifiedSubQueryExpression expression);

	void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate);

	void visitBetweenPredicate(BetweenPredicate betweenPredicate);

	void visitFilterPredicate(FilterPredicate filterPredicate);
	void visitFilterFragmentPredicate(FilterPredicate.FilterFragmentPredicate fragmentPredicate);
	void visitSqlFragmentPredicate(SqlFragmentPredicate predicate);

	void visitGroupedPredicate(GroupedPredicate groupedPredicate);

	void visitInListPredicate(InListPredicate inListPredicate);

	void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate);

	void visitInArrayPredicate(InArrayPredicate inArrayPredicate);

	void visitExistsPredicate(ExistsPredicate existsPredicate);

	void visitJunction(Junction junction);

	void visitLikePredicate(LikePredicate likePredicate);

	void visitNegatedPredicate(NegatedPredicate negatedPredicate);

	void visitNullnessPredicate(NullnessPredicate nullnessPredicate);

	void visitThruthnessPredicate(ThruthnessPredicate predicate);

	void visitRelationalPredicate(ComparisonPredicate comparisonPredicate);

	void visitSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate);

	void visitDurationUnit(DurationUnit durationUnit);

	void visitDuration(Duration duration);

	void visitConversion(Conversion conversion);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Model mutations

	void visitStandardTableInsert(TableInsertStandard tableInsert);

	void visitCustomTableInsert(TableInsertCustomSql tableInsert);

	void visitStandardTableDelete(TableDeleteStandard tableDelete);

	void visitCustomTableDelete(TableDeleteCustomSql tableDelete);

	void visitStandardTableUpdate(TableUpdateStandard tableUpdate);

	void visitOptionalTableUpdate(OptionalTableUpdate tableUpdate);

	void visitCustomTableUpdate(TableUpdateCustomSql tableUpdate);

	void visitColumnWriteFragment(ColumnWriteFragment columnWriteFragment);
}
