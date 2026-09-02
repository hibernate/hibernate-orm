/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.sql.ast.spi.query.predicate.SqlFragmentPredicate;
import org.hibernate.query.sqm.tree.spi.expression.Conversion;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.Any;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.CastTarget;
import org.hibernate.sql.ast.spi.query.expression.Collation;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.AggregateColumnWriteExpression;
import org.hibernate.sql.ast.spi.query.expression.Distinct;
import org.hibernate.sql.ast.spi.query.expression.Duration;
import org.hibernate.sql.ast.spi.query.expression.DurationUnit;
import org.hibernate.sql.ast.spi.query.expression.EmbeddableTypeLiteral;
import org.hibernate.sql.ast.spi.query.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.spi.query.expression.Every;
import org.hibernate.sql.ast.spi.query.expression.ExtractUnit;
import org.hibernate.sql.ast.spi.query.expression.Format;
import org.hibernate.sql.ast.spi.query.expression.JdbcLiteral;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.expression.ModifiedSubQueryExpression;
import org.hibernate.sql.ast.spi.query.expression.NestedColumnReference;
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
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.from.FunctionTableReference;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.QueryPartTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;
import org.hibernate.sql.ast.spi.query.from.ValuesTableReference;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
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
import org.hibernate.sql.ast.spi.query.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.spi.query.predicate.ThruthnessPredicate;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
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

import static org.hibernate.SPI.Role.USE;

/// Visitor callbacks for traversing the SQL AST.
///
/// This is a `USE` contract: SQL AST nodes call these methods from
/// [SqlAstNode#accept]. Dialect providers may invoke walker methods when
/// composing traversal, but should not implement this interface directly.
/// Custom SQL rendering belongs in a supported
/// [org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator] subclass, which
/// supplies traversal state and audited override points.
///
/// @since 8.0
/// @author Steve Ebersole
/// @author Andrea Boriero
@Incubating
@SPI(USE)
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
