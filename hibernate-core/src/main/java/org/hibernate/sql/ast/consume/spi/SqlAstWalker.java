/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.assign.Assignment;
import org.hibernate.sql.ast.tree.spi.expression.AbsFunction;
import org.hibernate.sql.ast.tree.spi.expression.AvgFunction;
import org.hibernate.sql.ast.tree.spi.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.spi.expression.BitLengthFunction;
import org.hibernate.sql.ast.tree.spi.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.spi.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.spi.expression.CastFunction;
import org.hibernate.sql.ast.tree.spi.expression.CoalesceFunction;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.ConcatFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountFunction;
import org.hibernate.sql.ast.tree.spi.expression.CountStarFunction;
import org.hibernate.sql.ast.tree.spi.expression.CurrentDateFunction;
import org.hibernate.sql.ast.tree.spi.expression.CurrentTimeFunction;
import org.hibernate.sql.ast.tree.spi.expression.CurrentTimestampFunction;
import org.hibernate.sql.ast.tree.spi.expression.ExtractFunction;
import org.hibernate.sql.ast.tree.spi.expression.GenericParameter;
import org.hibernate.sql.ast.tree.spi.expression.LengthFunction;
import org.hibernate.sql.ast.tree.spi.expression.LocateFunction;
import org.hibernate.sql.ast.tree.spi.expression.LowerFunction;
import org.hibernate.sql.ast.tree.spi.expression.MaxFunction;
import org.hibernate.sql.ast.tree.spi.expression.MinFunction;
import org.hibernate.sql.ast.tree.spi.expression.ModFunction;
import org.hibernate.sql.ast.tree.spi.expression.NamedParameter;
import org.hibernate.sql.ast.tree.spi.expression.NonStandardFunction;
import org.hibernate.sql.ast.tree.spi.expression.NullifFunction;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.SqlTuple;
import org.hibernate.sql.ast.tree.spi.expression.SqrtFunction;
import org.hibernate.sql.ast.tree.spi.expression.SumFunction;
import org.hibernate.sql.ast.tree.spi.expression.TrimFunction;
import org.hibernate.sql.ast.tree.spi.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.spi.expression.UpperFunction;
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
import org.hibernate.sql.ast.tree.spi.sort.SortSpecification;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface SqlAstWalker {

	SessionFactoryImplementor getSessionFactory();

	void visitAssignment(Assignment assignment);

	void visitQuerySpec(QuerySpec querySpec);

	void visitSortSpecification(SortSpecification sortSpecification);

	void visitLimitOffsetClause(QuerySpec querySpec);

	void visitSelectClause(SelectClause selectClause);

	void visitSqlSelection(SqlSelection sqlSelection);

	void visitFromClause(FromClause fromClause);

	void visitTableSpace(TableSpace tableSpace);

	void visitTableGroup(TableGroup tableGroup);

	void visitTableGroupJoin(TableGroupJoin tableGroupJoin);

	void visitTableReference(TableReference tableReference);

	void visitTableReferenceJoin(TableReferenceJoin tableReferenceJoin);

//	void visitEntityExpression(EntityReference entityExpression);
//
//	void visitSingularAttributeReference(SingularAttributeReference attributeExpression);
//
//	void visitPluralAttribute(PluralAttributeReference pluralAttributeReference);
//
//	void visitPluralAttributeElement(PluralAttributeElementReference elementExpression);
//
//	void visitPluralAttributeIndex(PluralAttributeIndexReference indexExpression);

	void visitColumnReference(ColumnReference columnReference);

	void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression);

	void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression);

	void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression);

	void visitCoalesceFunction(CoalesceFunction coalesceExpression);

	void visitNamedParameter(NamedParameter namedParameter);

	void visitGenericParameter(GenericParameter parameter);

	void visitPositionalParameter(PositionalParameter positionalParameter);

	void visitQueryLiteral(QueryLiteral queryLiteral);

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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// functions

	void visitNonStandardFunctionExpression(NonStandardFunction function);

	void visitAbsFunction(AbsFunction function);

	void visitAvgFunction(AvgFunction function);

	void visitBitLengthFunction(BitLengthFunction function);

	void visitCastFunction(CastFunction function);

	void visitConcatFunction(ConcatFunction function);

	void visitCountFunction(CountFunction function);

	void visitCountStarFunction(CountStarFunction function);

	void visitCurrentDateFunction(CurrentDateFunction function);

	void visitCurrentTimeFunction(CurrentTimeFunction function);

	void visitCurrentTimestampFunction(CurrentTimestampFunction function);

	void visitTuple(SqlTuple tuple);

	void visitExtractFunction(ExtractFunction extractFunction);

	void visitLengthFunction(LengthFunction function);

	void visitLocateFunction(LocateFunction function);

	void visitLowerFunction(LowerFunction function);

	void visitMaxFunction(MaxFunction function);

	void visitMinFunction(MinFunction function);

	void visitModFunction(ModFunction function);

	void visitNullifFunction(NullifFunction function);

	void visitSqrtFunction(SqrtFunction function);

	void visitSumFunction(SumFunction function);

	void visitTrimFunction(TrimFunction function);

	void visitUpperFunction(UpperFunction function);

	void visitSqlSelectionExpression(SqlSelectionExpression expression);
}
