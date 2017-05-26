/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.spi;

import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.AvgFunction;
import org.hibernate.sql.ast.tree.spi.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.spi.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.spi.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.spi.expression.CastFunction;
import org.hibernate.sql.ast.tree.spi.expression.CoalesceFunction;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.ConcatFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountStarFunction;
import org.hibernate.sql.ast.tree.spi.expression.GenericParameter;
import org.hibernate.sql.ast.tree.spi.expression.MaxFunction;
import org.hibernate.sql.ast.tree.spi.expression.MinFunction;
import org.hibernate.sql.ast.tree.spi.expression.NamedParameter;
import org.hibernate.sql.ast.tree.spi.expression.NonStandardFunction;
import org.hibernate.sql.ast.tree.spi.expression.NullifFunction;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.SumFunction;
import org.hibernate.sql.ast.tree.spi.expression.TrimFunction;
import org.hibernate.sql.ast.tree.spi.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.PluralAttributeElementReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.PluralAttributeIndexReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.SingularAttributeReference;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.ast.tree.spi.from.FromClause;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.spi.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.ast.tree.spi.sort.SortSpecification;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface SqlAstWalker {
	void visitQuerySpec(QuerySpec querySpec);

	void visitSortSpecification(SortSpecification sortSpecification);

	void visitLimitOffsetClause(QuerySpec querySpec);

	void visitSelectClause(SelectClause selectClause);

	void visitSelection(Selection selection);

	void visitSqlSelection(SqlSelection sqlSelection);

	void visitFromClause(FromClause fromClause);

	void visitTableSpace(TableSpace tableSpace);

	void visitTableGroup(TableGroup tableGroup);

	void visitTableGroupJoin(TableGroupJoin tableGroupJoin);

	void visitTableReference(TableReference tableReference);

	void visitTableReferenceJoin(TableReferenceJoin tableReferenceJoin);

	void visitSingularAttributeReference(SingularAttributeReference attributeExpression);

	void visitEntityExpression(EntityReference entityExpression);

	void visitPluralAttributeElement(PluralAttributeElementReference elementExpression);

	void visitPluralAttributeIndex(PluralAttributeIndexReference indexExpression);

	void visitColumnReference(ColumnReference columnReference);

	void visitAvgFunction(AvgFunction avgFunction);

	void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression);

	void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression);

	void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression);

	void visitColumnReferenceExpression(ColumnReference columnReference);

	void visitDynamicInstantiation(DynamicInstantiation<?> dynamicInstantiation);

	void visitCoalesceFunction(CoalesceFunction coalesceExpression);

	void visitConcatFunction(ConcatFunction concatExpression);

	void visitCountFunction(CountFunction countFunction);

	void visitCountStarFunction(CountStarFunction function);

	void visitMaxFunction(MaxFunction maxFunction);

	void visitMinFunction(MinFunction minFunction);

	void visitNamedParameter(NamedParameter namedParameter);

	void visitGenericParameter(GenericParameter parameter);

	void visitNonStandardFunctionExpression(NonStandardFunction functionExpression);

	void visitNullifFunction(NullifFunction function);

	void visitPositionalParameter(PositionalParameter positionalParameter);

	void visitQueryLiteral(QueryLiteral queryLiteral);

	void visitSumFunction(SumFunction sumFunction);

	void visitUnaryOperationExpression(UnaryOperation unaryOperationExpression);

	void visitBetweenPredicate(BetweenPredicate betweenPredicate);

	void visitFilterPredicate(FilterPredicate filterPredicate);

	void visitGroupedPredicate(GroupedPredicate groupedPredicate);

	void visitInListPredicate(InListPredicate inListPredicate);

	void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate);

	void visitJunction(Junction junction);

	void visitLikePredicate(LikePredicate likePredicate);

	void visitNegatedPredicate(NegatedPredicate negatedPredicate);

	void visitNullnessPredicate(NullnessPredicate nullnessPredicate);

	void visitRelationalPredicate(RelationalPredicate relationalPredicate);

	void visitSelfRenderingExpression(SelfRenderingExpression expression);

	void visitTrimFunction(TrimFunction trimFunction);

	void visitCastFunction(CastFunction castFunction);
}
