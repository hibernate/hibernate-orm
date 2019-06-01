/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Conversion;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.GenericParameter;
import org.hibernate.sql.ast.tree.expression.NamedParameter;
import org.hibernate.sql.ast.tree.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.expression.domain.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.sort.SortSpecification;
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

	void visitRelationalPredicate(ComparisonPredicate comparisonPredicate);

	void visitSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate);

	void visitSelfRenderingExpression(SelfRenderingExpression expression);

	void visitSqlSelectionExpression(SqlSelectionExpression expression);

	void visitEntityTypeLiteral(EntityTypeLiteral expression);

	void visitTuple(SqlTuple tuple);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// functions

	void visitExtractUnit(ExtractUnit unit);

	void visitCastTarget(CastTarget castTarget);

	void visitTrimSpecification(TrimSpecification trimSpecification);

	void visitStar(Star star);

	void visitDistinct(Distinct distinct);

	void visitFormat(Format format);

	void visitConversion(Conversion conversion);

	void visitDuration(Duration duration);
}
