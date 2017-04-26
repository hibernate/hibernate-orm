/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.expression.BinaryArithmeticSqmExpression;
import org.hibernate.query.sqm.tree.expression.CaseSearchedSqmExpression;
import org.hibernate.query.sqm.tree.expression.CaseSimpleSqmExpression;
import org.hibernate.query.sqm.tree.expression.CoalesceSqmExpression;
import org.hibernate.query.sqm.tree.expression.CollectionSizeSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConcatSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConstantEnumSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConstantFieldSqmExpression;
import org.hibernate.query.sqm.tree.expression.EntityTypeLiteralSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralBigDecimalSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralBigIntegerSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralCharacterSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralDoubleSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralFalseSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralFloatSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralIntegerSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralLongSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralNullSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralStringSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralTrueSqmExpression;
import org.hibernate.query.sqm.tree.expression.NamedParameterSqmExpression;
import org.hibernate.query.sqm.tree.expression.NullifSqmExpression;
import org.hibernate.query.sqm.tree.expression.ParameterizedEntityTypeSqmExpression;
import org.hibernate.query.sqm.tree.expression.PositionalParameterSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SubQuerySqmExpression;
import org.hibernate.query.sqm.tree.expression.UnaryOperationSqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.AbstractSpecificSqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityIdentifierReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityTypeSqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmMapEntryBinding;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.expression.function.AvgFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CastFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.ConcatFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CountFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CountStarFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.GenericFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.LowerFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.MaxFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.MinFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SubstringFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SumFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.TrimFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.UpperFunctionSqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.order.SqmOrderByClause;
import org.hibernate.query.sqm.tree.order.SqmSortSpecification;
import org.hibernate.query.sqm.tree.paging.SqmLimitOffsetClause;
import org.hibernate.query.sqm.tree.predicate.AndSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BetweenSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BooleanExpressionSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.EmptinessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.GroupedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InListSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InSubQuerySqmPredicate;
import org.hibernate.query.sqm.tree.predicate.LikeSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.MemberOfSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NegatedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NullnessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.OrSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.RelationalSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.set.SqmAssignment;
import org.hibernate.query.sqm.tree.set.SqmSetClause;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({"unchecked", "WeakerAccess"})
public class BaseSemanticQueryWalker<T> implements SemanticQueryWalker<T> {
	@Override
	public T visitStatement(SqmStatement statement) {
		return statement.accept( this );
	}

	@Override
	public T visitSelectStatement(SqmSelectStatement statement) {
		visitQuerySpec( statement.getQuerySpec() );
		return (T) statement;
	}

	@Override
	public T visitUpdateStatement(SqmUpdateStatement statement) {
		visitRootEntityFromElement( statement.getEntityFromElement() );
		visitSetClause( statement.getSetClause() );
		visitWhereClause( statement.getWhereClause() );
		return (T) statement;
	}

	@Override
	public T visitSetClause(SqmSetClause setClause) {
		for ( SqmAssignment assignment : setClause.getAssignments() ) {
			visitAssignment( assignment );
		}
		return (T) setClause;
	}

	@Override
	public T visitAssignment(SqmAssignment assignment) {
		visitAttributeReferenceExpression( assignment.getStateField() );
		assignment.getStateField().accept( this );
		return (T) assignment;
	}

	@Override
	public T visitInsertSelectStatement(SqmInsertSelectStatement statement) {
		visitRootEntityFromElement( statement.getInsertTarget() );
		for ( SqmSingularAttributeReference stateField : statement.getStateFields() ) {
			stateField.accept( this );
		}
		visitQuerySpec( statement.getSelectQuery() );
		return (T) statement;
	}

	@Override
	public T visitDeleteStatement(SqmDeleteStatement statement) {
		visitRootEntityFromElement( statement.getEntityFromElement() );
		visitWhereClause( statement.getWhereClause() );
		return (T) statement;
	}

	@Override
	public T visitQuerySpec(SqmQuerySpec querySpec) {
		visitFromClause( querySpec.getFromClause() );
		visitSelectClause( querySpec.getSelectClause() );
		visitWhereClause( querySpec.getWhereClause() );
		visitOrderByClause( querySpec.getOrderByClause() );
		visitLimitOffsetClause( querySpec.getLimitOffsetClause() );
		return (T) querySpec;
	}

	@Override
	public T visitFromClause(SqmFromClause fromClause) {
		for ( SqmFromElementSpace fromElementSpace : fromClause.getFromElementSpaces() ) {
			visitFromElementSpace( fromElementSpace );
		}
		return (T) fromClause;
	}

	@Override
	public T visitFromElementSpace(SqmFromElementSpace fromElementSpace) {
		visitRootEntityFromElement( fromElementSpace.getRoot() );
		for ( SqmJoin joinedFromElement : fromElementSpace.getJoins() ) {
			joinedFromElement.accept( this );
		}
		return (T) fromElementSpace;
	}

	@Override
	public T visitCrossJoinedFromElement(SqmCrossJoin joinedFromElement) {
		return (T) joinedFromElement;
	}

	@Override
	public T visitQualifiedEntityJoinFromElement(SqmEntityJoin joinedFromElement) {
		return (T) joinedFromElement;
	}

	@Override
	public T visitQualifiedAttributeJoinFromElement(SqmAttributeJoin joinedFromElement) {
		return (T) joinedFromElement;
	}

	@Override
	public T visitRootEntityFromElement(SqmRoot rootEntityFromElement) {
		return (T) rootEntityFromElement;
	}

	@Override
	public T visitSelectClause(SqmSelectClause selectClause) {
		for ( SqmSelection selection : selectClause.getSelections() ) {
			visitSelection( selection );
		}
		return (T) selectClause;
	}

	@Override
	public T visitSelection(SqmSelection selection) {
		selection.getExpression().accept( this );
		return (T) selection;
	}

	@Override
	public T visitDynamicInstantiation(SqmDynamicInstantiation dynamicInstantiation) {
		return (T) dynamicInstantiation;
	}

	@Override
	public T visitWhereClause(SqmWhereClause whereClause) {
		whereClause.getPredicate().accept( this );
		return (T) whereClause;
	}

	@Override
	public T visitGroupedPredicate(GroupedSqmPredicate predicate) {
		predicate.getSubPredicate().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitAndPredicate(AndSqmPredicate predicate) {
		predicate.getLeftHandPredicate().accept( this );
		predicate.getRightHandPredicate().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitOrPredicate(OrSqmPredicate predicate) {
		predicate.getLeftHandPredicate().accept( this );
		predicate.getRightHandPredicate().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitRelationalPredicate(RelationalSqmPredicate predicate) {
		predicate.getLeftHandExpression().accept( this );
		predicate.getRightHandExpression().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitIsEmptyPredicate(EmptinessSqmPredicate predicate) {
		predicate.getExpression().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitIsNullPredicate(NullnessSqmPredicate predicate) {
		predicate.getExpression().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitBetweenPredicate(BetweenSqmPredicate predicate) {
		predicate.getExpression().accept( this );
		predicate.getLowerBound().accept( this );
		predicate.getUpperBound().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitLikePredicate(LikeSqmPredicate predicate) {
		predicate.getMatchExpression().accept( this );
		predicate.getPattern().accept( this );
		predicate.getEscapeCharacter().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitMemberOfPredicate(MemberOfSqmPredicate predicate) {
		predicate.getPluralAttributeReference().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitNegatedPredicate(NegatedSqmPredicate predicate) {
		predicate.getWrappedPredicate().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitInListPredicate(InListSqmPredicate predicate) {
		predicate.getTestExpression().accept( this );
		for ( SqmExpression expression : predicate.getListExpressions() ) {
			expression.accept( this );
		}
		return (T) predicate;
	}

	@Override
	public T visitInSubQueryPredicate(InSubQuerySqmPredicate predicate) {
		predicate.getTestExpression().accept( this );
		predicate.getSubQueryExpression().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitBooleanExpressionPredicate(BooleanExpressionSqmPredicate predicate) {
		predicate.getBooleanExpression().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitOrderByClause(SqmOrderByClause orderByClause) {
		if ( orderByClause.getSortSpecifications() != null ) {
			for ( SqmSortSpecification sortSpecification : orderByClause.getSortSpecifications() ) {
				visitSortSpecification( sortSpecification );
			}
		}
		return (T) orderByClause;
	}

	@Override
	public T visitSortSpecification(SqmSortSpecification sortSpecification) {
		sortSpecification.getSortExpression().accept( this );
		return (T) sortSpecification;
	}

	@Override
	public T visitLimitOffsetClause(SqmLimitOffsetClause limitOffsetClause) {
		if ( limitOffsetClause != null ) {
			if ( limitOffsetClause.getLimitExpression() != null ) {
				limitOffsetClause.getLimitExpression().accept( this );
			}
			if ( limitOffsetClause.getOffsetExpression() != null ) {
				limitOffsetClause.getOffsetExpression().accept( this );
			}
		}
		return (T) limitOffsetClause;
	}

	@Override
	public T visitPositionalParameterExpression(PositionalParameterSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitNamedParameterExpression(NamedParameterSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitEntityTypeLiteralExpression(EntityTypeLiteralSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitEntityTypeExpression(SqmEntityTypeSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitParameterizedEntityTypeExpression(ParameterizedEntityTypeSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitUnaryOperationExpression(UnaryOperationSqmExpression expression) {
		expression.getOperand().accept( this );
		return (T) expression;
	}

	@Override
	public T visitAttributeReferenceExpression(SqmAttributeReference expression) {
		return (T) expression;
	}

	@Override
	public T visitGenericFunction(GenericFunctionSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitCastFunction(CastFunctionSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitAvgFunction(AvgFunctionSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitCountStarFunction(CountStarFunctionSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitCountFunction(CountFunctionSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitMaxFunction(MaxFunctionSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitMinFunction(MinFunctionSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitSumFunction(SumFunctionSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitPluralAttributeSizeFunction(CollectionSizeSqmExpression function) {
		return (T) function;
	}

	@Override
	public T visitPluralAttributeElementBinding(SqmCollectionElementReference binding) {
		return (T) binding;
	}

	@Override
	public T visitPluralAttributeIndexFunction(SqmCollectionIndexReference binding) {
		return (T) binding;
	}

	@Override
	public T visitMapKeyBinding(SqmCollectionIndexReference binding) {
		return (T) binding;
	}

	@Override
	public T visitMapEntryFunction(SqmMapEntryBinding binding) {
		return (T) binding;
	}

	@Override
	public T visitMaxElementBinding(SqmMaxElementReference binding) {
		return (T) binding;
	}

	@Override
	public T visitMinElementBinding(SqmMinElementReference binding) {
		return (T) binding;
	}

	@Override
	public T visitMaxIndexFunction(AbstractSpecificSqmCollectionIndexReference function) {
		return (T) function;
	}

	@Override
	public T visitMinIndexFunction(SqmMinIndexReferenceBasic function) {
		return (T) function;
	}

	@Override
	public T visitLiteralStringExpression(LiteralStringSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralCharacterExpression(LiteralCharacterSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralDoubleExpression(LiteralDoubleSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralIntegerExpression(LiteralIntegerSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralBigIntegerExpression(LiteralBigIntegerSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralBigDecimalExpression(LiteralBigDecimalSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralFloatExpression(LiteralFloatSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralLongExpression(LiteralLongSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralTrueExpression(LiteralTrueSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralFalseExpression(LiteralFalseSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralNullExpression(LiteralNullSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitConcatExpression(ConcatSqmExpression expression) {
		expression.getLeftHandOperand().accept( this );
		expression.getRightHandOperand().accept( this );
		return (T) expression;
	}

	@Override
	public T visitConcatFunction(ConcatFunctionSqmExpression expression) {
		for ( SqmExpression argument : expression.getExpressions() ) {
			argument.accept( this );
		}
		return (T) expression;
	}

	@Override
	public T visitConstantEnumExpression(ConstantEnumSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitConstantFieldExpression(ConstantFieldSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitBinaryArithmeticExpression(BinaryArithmeticSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitSubQueryExpression(SubQuerySqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitSimpleCaseExpression(CaseSimpleSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitSearchedCaseExpression(CaseSearchedSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitCoalesceExpression(CoalesceSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitNullifExpression(NullifSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitSubstringFunction(SubstringFunctionSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitTrimFunction(TrimFunctionSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitLowerFunction(LowerFunctionSqmExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitEntityIdentifierBinding(SqmEntityIdentifierReference expression) {
		return (T) expression;
	}

	@Override
	public T visitUpperFunction(UpperFunctionSqmExpression expression) {
		return (T) expression;
	}
}
