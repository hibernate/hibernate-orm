/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import java.lang.reflect.Field;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmMaxElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMaxIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmMinElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMinIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFormat;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmRestrictedSubQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.function.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.function.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.function.SqmStar;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmOrPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmGroupByClause;
import org.hibernate.query.sqm.tree.select.SqmHavingClause;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.produce.spi.SqmFunction;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public class BaseSemanticQueryWalker<T> implements SemanticQueryWalker<T> {
	private final ServiceRegistry serviceRegistry;

	public BaseSemanticQueryWalker(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public T visitSelectStatement(SqmSelectStatement<?> statement) {
		visitQuerySpec( statement.getQuerySpec() );
		return (T) statement;
	}

	@Override
	public T visitUpdateStatement(SqmUpdateStatement<?> statement) {
		visitRootPath( statement.getTarget() );
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
		assignment.getTargetPath().accept( this );
		assignment.getValue().accept( this );
		return (T) assignment;
	}

	@Override
	public T visitInsertSelectStatement(SqmInsertSelectStatement<?> statement) {
		visitRootPath( statement.getTarget() );
		for ( SqmPath<?> stateField : statement.getInsertionTargetPaths() ) {
			stateField.accept( this );
		}
		visitQuerySpec( statement.getSelectQuerySpec() );
		return (T) statement;
	}

	@Override
	public T visitDeleteStatement(SqmDeleteStatement<?> statement) {
		visitRootPath( statement.getTarget() );
		visitWhereClause( statement.getWhereClause() );
		return (T) statement;
	}

	@Override
	public T visitQuerySpec(SqmQuerySpec querySpec) {
		visitFromClause( querySpec.getFromClause() );
		visitSelectClause( querySpec.getSelectClause() );
		visitWhereClause( querySpec.getWhereClause() );
		visitOrderByClause( querySpec.getOrderByClause() );
		visitOffsetExpression( querySpec.getOffsetExpression() );
		visitLimitExpression( querySpec.getLimitExpression() );
		return (T) querySpec;
	}

	@Override
	public T visitFromClause(SqmFromClause fromClause) {
		fromClause.visitRoots( this::visitRootPath );
		return (T) fromClause;
	}

	@Override
	public T visitRootPath(SqmRoot<?> sqmRoot) {
		sqmRoot.visitSqmJoins( sqmJoin -> ( (SqmJoin) sqmJoin ).accept( this ) );
		return (T) sqmRoot;
	}


	@Override
	public T visitCrossJoin(SqmCrossJoin<?> joinedFromElement) {
		joinedFromElement.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		return (T) joinedFromElement;
	}

	@Override
	public T visitQualifiedEntityJoin(SqmEntityJoin<?> joinedFromElement) {
		joinedFromElement.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		return (T) joinedFromElement;
	}

	@Override
	public T visitQualifiedAttributeJoin(SqmAttributeJoin<?,?> joinedFromElement) {
		joinedFromElement.visitSqmJoins( sqmJoin -> sqmJoin.accept( this ) );
		return (T) joinedFromElement;
	}

	@Override
	public T visitBasicValuedPath(SqmBasicValuedSimplePath path) {
		return (T) path;
	}

	@Override
	public T visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath path) {
		return (T) path;
	}

	@Override
	public T visitEntityValuedPath(SqmEntityValuedSimplePath path) {
		return (T) path;
	}

	@Override
	public T visitPluralValuedPath(SqmPluralValuedSimplePath path) {
		return (T) path;
	}

	@Override
	public T visitIndexedPluralAccessPath(SqmIndexedCollectionAccessPath path) {
		return (T) path;
	}

	@Override
	public T visitMaxElementPath(SqmMaxElementPath path) {
		return (T) path;
	}

	@Override
	public T visitMinElementPath(SqmMinElementPath path) {
		return (T) path;
	}

	@Override
	public T visitMaxIndexPath(SqmMaxIndexPath path) {
		return (T) path;
	}

	@Override
	public T visitMinIndexPath(SqmMinIndexPath path) {
		return (T) path;
	}

	@Override
	public T visitCorrelation(SqmCorrelation correlation) {
		return (T) correlation;
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
		if ( whereClause == null ) {
			return null;
		}

		whereClause.getPredicate().accept( this );
		return (T) whereClause;
	}

	@Override
	public T visitGroupedPredicate(SqmGroupedPredicate predicate) {
		predicate.getSubPredicate().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitAndPredicate(SqmAndPredicate predicate) {
		predicate.getLeftHandPredicate().accept( this );
		predicate.getRightHandPredicate().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitOrPredicate(SqmOrPredicate predicate) {
		predicate.getLeftHandPredicate().accept( this );
		predicate.getRightHandPredicate().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitComparisonPredicate(SqmComparisonPredicate predicate) {
		predicate.getLeftHandExpression().accept( this );
		predicate.getRightHandExpression().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitIsEmptyPredicate(SqmEmptinessPredicate predicate) {
		predicate.getPluralPath().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitIsNullPredicate(SqmNullnessPredicate predicate) {
		predicate.getExpression().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitBetweenPredicate(SqmBetweenPredicate predicate) {
		predicate.getExpression().accept( this );
		predicate.getLowerBound().accept( this );
		predicate.getUpperBound().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitLikePredicate(SqmLikePredicate predicate) {
		predicate.getMatchExpression().accept( this );
		predicate.getPattern().accept( this );
		predicate.getEscapeCharacter().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitMemberOfPredicate(SqmMemberOfPredicate predicate) {
		predicate.getPluralPath().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitNegatedPredicate(SqmNegatedPredicate predicate) {
		predicate.getWrappedPredicate().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitInListPredicate(SqmInListPredicate<?> predicate) {
		predicate.getTestExpression().accept( this );
		for ( SqmExpression expression : predicate.getListExpressions() ) {
			expression.accept( this );
		}
		return (T) predicate;
	}

	@Override
	public T visitInSubQueryPredicate(SqmInSubQueryPredicate<?> predicate) {
		predicate.getTestExpression().accept( this );
		predicate.getSubQueryExpression().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitBooleanExpressionPredicate(SqmBooleanExpressionPredicate predicate) {
		predicate.getBooleanExpression().accept( this );
		return (T) predicate;
	}

	@Override
	public T visitOrderByClause(SqmOrderByClause orderByClause) {
		if ( orderByClause == null ) {
			return null;
		}

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
	public T visitOffsetExpression(SqmExpression<?> expression) {
		if ( expression == null ) {
			return null;
		}

		return expression.accept( this );
	}

	@Override
	public T visitGroupByClause(SqmGroupByClause clause) {
		clause.visitGroupings( this::visitGrouping );
		return (T) clause;
	}

	@Override
	public T visitGrouping(SqmGroupByClause.SqmGrouping grouping) {
		grouping.getExpression().accept( this );
		return (T) grouping;
	}

	@Override
	public T visitHavingClause(SqmHavingClause clause) {
		clause.getPredicate().accept( this );
		return (T) clause;
	}

	@Override
	public T visitLimitExpression(SqmExpression<?> expression) {
		if ( expression == null ) {
			return null;
		}

		return expression.accept( this );
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
	public T visitCriteriaParameter(SqmCriteriaParameter expression) {
		return (T) expression;
	}

	@Override
	public T visitEntityTypeLiteralExpression(SqmLiteralEntityType expression) {
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
	public T visitFunction(SqmFunction sqmFunction) {
		return (T) sqmFunction;
	}

	@Override
	public T visitRestrictedSubQueryExpression(SqmRestrictedSubQueryExpression<?> sqmRestrictedSubQueryExpression) {
		return sqmRestrictedSubQueryExpression.getSubQuery().accept( this );
	}

	@Override
	public T visitExtractUnit(SqmExtractUnit extractUnit) {
		return (T) extractUnit;
	}

	@Override
	public T visitCastTarget(SqmCastTarget castTarget) {
		return (T) castTarget;
	}

	@Override
	public T visitTrimSpecification(SqmTrimSpecification trimSpecification) {
		return (T) trimSpecification;
	}

	@Override
	public T visitFormat(SqmFormat format) {
		return (T) format;
	}

	@Override
	public T visitDistinct(SqmDistinct distinct) {
		return (T) distinct;
	}

	@Override
	public T visitStar(SqmStar sqmStar) {
		return (T) sqmStar;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions


	@Override
	public T visitTreatedPath(SqmTreatedPath sqmTreatedPath) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public T visitPluralAttributeSizeFunction(SqmCollectionSize function) {
		return (T) function;
	}

	@Override
	public T visitMapEntryFunction(SqmMapEntryReference binding) {
		return (T) binding;
	}

	@Override
	public T visitLiteral(SqmLiteral literal) {
		return (T) literal;
	}

	@Override
	public T visitTuple(SqmTuple sqmTuple) {
		return (T) sqmTuple;
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
	public T visitSimpleCaseExpression(SqmCaseSimple<?,?> expression) {
		return (T) expression;
	}

	@Override
	public T visitSearchedCaseExpression(SqmCaseSearched<?> expression) {
		return (T) expression;
	}

	@Override
	public T visitDynamicInstantiation(SqmDynamicInstantiation<?> sqmDynamicInstantiation) {
		return (T) sqmDynamicInstantiation;
	}


	@Override
	public T visitFullyQualifiedClass(Class<?> namedClass) {
		throw new UnsupportedOperationException( "Not supported" );
	}

	@Override
	public T visitFullyQualifiedEnum(Enum<?> value) {
		throw new UnsupportedOperationException( "Not supported" );
	}

	@Override
	public T visitFullyQualifiedField(Field field) {
		throw new UnsupportedOperationException( "Not supported" );
	}

}
