/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmConstantEnum;
import org.hibernate.query.sqm.tree.expression.SqmConstantFieldReference;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteralBigDecimal;
import org.hibernate.query.sqm.tree.expression.SqmLiteralBigInteger;
import org.hibernate.query.sqm.tree.expression.SqmLiteralCharacter;
import org.hibernate.query.sqm.tree.expression.SqmLiteralDate;
import org.hibernate.query.sqm.tree.expression.SqmLiteralDouble;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmLiteralFalse;
import org.hibernate.query.sqm.tree.expression.SqmLiteralFloat;
import org.hibernate.query.sqm.tree.expression.SqmLiteralInteger;
import org.hibernate.query.sqm.tree.expression.SqmLiteralLong;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmLiteralString;
import org.hibernate.query.sqm.tree.expression.SqmLiteralTime;
import org.hibernate.query.sqm.tree.expression.SqmLiteralTimestamp;
import org.hibernate.query.sqm.tree.expression.SqmLiteralTrue;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSubQuery;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.domain.AbstractSpecificSqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityIdentifierReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityTypeExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmMapEntryBinding;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceAny;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEntity;
import org.hibernate.query.sqm.tree.expression.function.SqmAbsFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmBitLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCoalesceFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmConcatFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountStarFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentDateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimeFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimestampFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLocateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLowerFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmModFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmNullifFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSqrtFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSubstringFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmUpperFunction;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
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
import org.hibernate.sql.ast.produce.ordering.internal.SqmColumnReference;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({"unchecked", "WeakerAccess"})
public class BaseSemanticQueryWalker<T> implements SemanticQueryWalker<T> {
	private final SessionFactoryImplementor sessionFactory;

	public BaseSemanticQueryWalker(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public T visitSelectStatement(SqmSelectStatement statement) {
		visitQuerySpec( statement.getQuerySpec() );
		return (T) statement;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
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
		assignment.getStateField().accept( this );
		assignment.getValue().accept( this );
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
	public T visitQualifiedAttributeJoinFromElement(SqmNavigableJoin joinedFromElement) {
		return (T) joinedFromElement;
	}

	@Override
	public T visitRootEntityFromElement(SqmRoot rootEntityFromElement) {
		return (T) rootEntityFromElement;
	}

	@Override
	public T visitRootEntityReference(SqmEntityReference sqmEntityReference) {
		return (T) sqmEntityReference;
	}

	@Override
	public T visitSelectClause(SqmSelectClause selectClause) {
		for ( SqmSelection selection : selectClause.getSelections() ) {
			// todo (6.0) : add the ability for certain SqlSelections to be sort of "implicit"...
			//		- they do not get rendered into the SQL, but do have a SqlReader
			//
			// this is useful in 2 specific:
			///		1) literals : no need to even send those to the database - we could
			//			just have the SqlSelectionReader return us back the literal value
			//		2) `EmptySqlSelection` : if this ends up being important at all..
			visitSelection( selection );
		}
		return (T) selectClause;
	}

	@Override
	public T visitSelection(SqmSelection selection) {
		selection.getSelectableNode().accept( this );
		return (T) selection;
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
	public T visitPositionalParameterExpression(SqmPositionalParameter expression) {
		return (T) expression;
	}

	@Override
	public T visitNamedParameterExpression(SqmNamedParameter expression) {
		return (T) expression;
	}

	@Override
	public T visitEntityTypeLiteralExpression(SqmLiteralEntityType expression) {
		return (T) expression;
	}

	@Override
	public T visitEntityTypeExpression(SqmEntityTypeExpression expression) {
		return (T) expression;
	}

	@Override
	public T visitParameterizedEntityTypeExpression(SqmParameterizedEntityType expression) {
		return (T) expression;
	}

	@Override
	public T visitUnaryOperationExpression(SqmUnaryOperation sqmExpression) {
		sqmExpression.getOperand().accept( this );
		return (T) sqmExpression;
	}

	@Override
	public T visitGenericFunction(SqmGenericFunction function) {
		return (T) function;
	}

	@Override
	public T visitSqlAstFunctionProducer(SqlAstFunctionProducer sqlAstFunctionProducer) {
		return (T) sqlAstFunctionProducer;
	}

	@Override
	public T visitAbsFunction(SqmAbsFunction function) {
		return (T) function;
	}

	@Override
	public T visitAvgFunction(SqmAvgFunction function) {
		return (T) function;
	}

	@Override
	public T visitBitLengthFunction(SqmBitLengthFunction function) {
		return (T) function;
	}

	@Override
	public T visitCastFunction(SqmCastFunction function) {
		return (T) function;
	}

	@Override
	public T visitConcatFunction(SqmConcatFunction expression) {
		for ( SqmExpression argument : expression.getExpressions() ) {
			argument.accept( this );
		}
		return (T) expression;
	}

	@Override
	public T visitCountStarFunction(SqmCountStarFunction function) {
		return (T) function;
	}

	@Override
	public T visitCountFunction(SqmCountFunction function) {
		return (T) function;
	}

	@Override
	public T visitCurrentDateFunction(SqmCurrentDateFunction function) {
		return (T) function;
	}

	@Override
	public T visitCurrentTimeFunction(SqmCurrentTimeFunction function) {
		return (T) function;
	}

	@Override
	public T visitCurrentTimestampFunction(SqmCurrentTimestampFunction function) {
		return (T) function;
	}

	@Override
	public T visitExtractFunction(SqmExtractFunction function) {
		return (T) function;
	}

	@Override
	public T visitLengthFunction(SqmLengthFunction function) {
		return (T) function;
	}

	@Override
	public T visitLocateFunction(SqmLocateFunction function) {
		return (T) function;
	}

	@Override
	public T visitLowerFunction(SqmLowerFunction expression) {
		return (T) expression;
	}

	@Override
	public T visitMaxFunction(SqmMaxFunction function) {
		return (T) function;
	}

	@Override
	public T visitMinFunction(SqmMinFunction function) {
		return (T) function;
	}

	@Override
	public T visitModFunction(SqmModFunction function) {
		return (T) function;
	}

	@Override
	public T visitNullifFunction(SqmNullifFunction expression) {
		return (T) expression;
	}

	@Override
	public T visitSqrtFunction(SqmSqrtFunction function) {
		return (T) function;
	}

	@Override
	public T visitSubstringFunction(SqmSubstringFunction expression) {
		return (T) expression;
	}

	@Override
	public T visitSumFunction(SqmSumFunction function) {
		return (T) function;
	}

	@Override
	public T visitTrimFunction(SqmTrimFunction expression) {
		return (T) expression;
	}

	@Override
	public T visitUpperFunction(SqmUpperFunction expression) {
		return (T) expression;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions

	@Override
	public T visitPluralAttributeSizeFunction(SqmCollectionSize function) {
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
	public T visitLiteralStringExpression(SqmLiteralString expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralCharacterExpression(SqmLiteralCharacter expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralDoubleExpression(SqmLiteralDouble expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralIntegerExpression(SqmLiteralInteger expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralBigIntegerExpression(SqmLiteralBigInteger expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralBigDecimalExpression(SqmLiteralBigDecimal expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralFloatExpression(SqmLiteralFloat expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralLongExpression(SqmLiteralLong expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralTrueExpression(SqmLiteralTrue expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralFalseExpression(SqmLiteralFalse expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralNullExpression(SqmLiteralNull expression) {
		return (T) expression;
	}

	@Override
	public T visitLiteralTimestampExpression(SqmLiteralTimestamp literal) {
		return (T) literal;
	}

	@Override
	public T visitLiteralDateExpression(SqmLiteralDate literal) {
		return (T) literal;
	}

	@Override
	public T visitLiteralTimeExpression(SqmLiteralTime literal) {
		return (T) literal;
	}

	@Override
	public T visitConcatExpression(SqmConcat expression) {
		expression.getLeftHandOperand().accept( this );
		expression.getRightHandOperand().accept( this );
		return (T) expression;
	}

	@Override
	public T visitConstantEnumExpression(SqmConstantEnum expression) {
		return (T) expression;
	}

	@Override
	public T visitConstantFieldReference(SqmConstantFieldReference expression) {
		return (T) expression;
	}

	@Override
	public T visitBinaryArithmeticExpression(SqmBinaryArithmetic expression) {
		return (T) expression;
	}

	@Override
	public T visitSubQueryExpression(SqmSubQuery expression) {
		return (T) expression;
	}

	@Override
	public T visitSimpleCaseExpression(SqmCaseSimple expression) {
		return (T) expression;
	}

	@Override
	public T visitSearchedCaseExpression(SqmCaseSearched expression) {
		return (T) expression;
	}

	@Override
	public T visitExplicitColumnReference(SqmColumnReference sqmColumnReference) {
		return (T) sqmColumnReference;
	}

	@Override
	public T visitDynamicInstantiation(SqmDynamicInstantiation sqmDynamicInstantiation) {
		return (T) sqmDynamicInstantiation;
	}

	@Override
	public T visitCoalesceFunction(SqmCoalesceFunction expression) {
		return (T) expression;
	}

	@Override
	public T visitEntityIdentifierReference(SqmEntityIdentifierReference expression) {
		return (T) expression;
	}

	@Override
	public T visitBasicValuedSingularAttribute(SqmSingularAttributeReferenceBasic sqmAttributeReference) {
		return (T) sqmAttributeReference;
	}

	@Override
	public T visitEntityValuedSingularAttribute(SqmSingularAttributeReferenceEntity sqmAttributeReference) {
		return (T) sqmAttributeReference;
	}

	@Override
	public T visitEmbeddableValuedSingularAttribute(SqmSingularAttributeReferenceEmbedded sqmAttributeReference) {
		return (T) sqmAttributeReference;
	}

	@Override
	public T visitAnyValuedSingularAttribute(SqmSingularAttributeReferenceAny sqmAttributeReference) {
		return (T) sqmAttributeReference;
	}

	@Override
	public T visitPluralAttribute(SqmPluralAttributeReference reference) {
		return (T) reference;
	}
}
