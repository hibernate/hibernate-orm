/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.jpa;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.query.BindableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmExpressibleAccessor;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmTruthnessPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

/**
 * @author Steve Ebersole
 */
// todo (6.0) : how is this different from org.hibernate.query.sqm.internal.ParameterCollector?
public class ParameterCollector extends BaseSemanticQueryWalker {

	public static Set<SqmParameter<?>> collectParameters(SqmStatement<?> statement) {
		return collectParameters( statement, parameter -> {} );
	}

	public static Set<SqmParameter<?>> collectParameters(
			SqmStatement<?> statement,
			Consumer<SqmParameter<?>> consumer) {
		final ParameterCollector collector = new ParameterCollector( consumer );
		statement.accept( collector );
		return collector.parameterExpressions == null
				? Collections.emptySet()
				: collector.parameterExpressions;
	}

	private ParameterCollector(Consumer<SqmParameter<?>> consumer) {
		this.consumer = consumer;
	}

	private Set<SqmParameter<?>> parameterExpressions;
	private final Consumer<SqmParameter<?>> consumer;

	@Override
	public Object visitPositionalParameterExpression(SqmPositionalParameter<?> expression) {
		return visitParameter( expression );
	}

	@Override
	public Object visitNamedParameterExpression(SqmNamedParameter<?> expression) {
		return visitParameter( expression );
	}

	/**
	 * This is called while performing an in-flight parameter collection of parameters
	 * for {@link jakarta.persistence.criteria.CriteriaQuery#getParameters}. That method
	 * can be called multiple times and the parameters may have changed in between each
	 * call - therefore the parameters must be collected dynamically each time.
	 * This form simply returns the {@link JpaCriteriaParameter}.
	 *
	 * @see SqmSelectStatement#resolveParameters()
	 */
	@Override
	public SqmJpaCriteriaParameterWrapper<?> visitJpaCriteriaParameter(JpaCriteriaParameter<?> expression) {
		return visitParameter(
				new SqmJpaCriteriaParameterWrapper<>(
						getInferredParameterType( expression ),
						expression,
						expression.nodeBuilder()
				)
		);
	}

	@Override
	public Object visitFunction(SqmFunction<?> sqmFunction) {
		final SqmExpressibleAccessor<?> current = inferenceBasis;
		inferenceBasis = null;
		super.visitFunction( sqmFunction );
		inferenceBasis = current;
		return sqmFunction;
	}

	@Override
	public Object visitSetReturningFunction(SqmSetReturningFunction<?> sqmFunction) {
		final SqmExpressibleAccessor<?> current = inferenceBasis;
		inferenceBasis = null;
		super.visitSetReturningFunction( sqmFunction );
		inferenceBasis = current;
		return sqmFunction;
	}

	private <T> BindableType<T> getInferredParameterType(JpaCriteriaParameter<?> expression) {
		BindableType<?> parameterType = null;
		if ( inferenceBasis != null ) {
			final SqmExpressible<?> expressible = inferenceBasis.getExpressible();
			if ( expressible != null ) {
				parameterType = expressible;
			}
		}
		if ( parameterType == null ) {
			parameterType = expression.getHibernateType();
		}
		//noinspection unchecked
		return (BindableType<T>) parameterType;
	}

	private <T extends SqmParameter<?>> T visitParameter(T param) {
		if ( parameterExpressions == null ) {
			parameterExpressions = new HashSet<>();
		}
		parameterExpressions.add( param );
		consumer.accept( param );
		return param;
	}

	private <T> SqmJpaCriteriaParameterWrapper<T> visitParameter(SqmJpaCriteriaParameterWrapper<T> param) {
		if ( parameterExpressions == null ) {
			parameterExpressions = new HashSet<>();
		}
		parameterExpressions.add( param.getJpaCriteriaParameter() );
		consumer.accept( param );
		return param;
	}

	private SqmExpressibleAccessor<?> inferenceBasis;

	private <T> void withTypeInference(SqmExpressibleAccessor<T> inferenceBasis, SqmVisitableNode sqmVisitableNode) {
		SqmExpressibleAccessor<?> original = this.inferenceBasis;
		this.inferenceBasis = inferenceBasis;
		try {
			sqmVisitableNode.accept( this );
		}
		finally {
			this.inferenceBasis = original;
		}
	}

	@Override
	public Object visitSimpleCaseExpression(SqmCaseSimple<?, ?> expression) {
		final SqmExpressibleAccessor<?> inferenceSupplier = this.inferenceBasis;
		withTypeInference(
				() -> {
					for ( SqmCaseSimple.WhenFragment<?, ?> whenFragment : expression.getWhenFragments() ) {
						final SqmExpressible<?> resolved = whenFragment.getCheckValue().getExpressible();
						if ( resolved != null ) {
							return (SqmExpressible<?>) resolved;
						}
					}
					return null;
				},
				expression.getFixture()
		);
		SqmExpressibleAccessor<?> resolved = toExpressibleAccessor( expression );
		for ( SqmCaseSimple.WhenFragment<?, ?> whenFragment : expression.getWhenFragments() ) {
			withTypeInference(
					expression.getFixture(),
					whenFragment.getCheckValue()
			);
			withTypeInference(
					getInferenceBasis( inferenceSupplier, resolved ),
					whenFragment.getResult()
			);
			resolved = highestPrecedence( resolved, whenFragment.getResult() );
		}

		if ( expression.getOtherwise() != null ) {
			withTypeInference(
					getInferenceBasis( inferenceSupplier, resolved ),
					expression.getOtherwise()
			);
		}

		return expression;
	}

	@Override
	public Object visitSearchedCaseExpression(SqmCaseSearched<?> expression) {
		final SqmExpressibleAccessor<?> inferenceSupplier = this.inferenceBasis;
		SqmExpressibleAccessor<?> resolved = toExpressibleAccessor( expression );

		for ( SqmCaseSearched.WhenFragment<?> whenFragment : expression.getWhenFragments() ) {
			withTypeInference(
					null,
					whenFragment.getPredicate()
			);
			withTypeInference(
					getInferenceBasis( inferenceSupplier, resolved ),
					whenFragment.getResult()
			);
			resolved = highestPrecedence( resolved, whenFragment.getResult() );
		}

		if ( expression.getOtherwise() != null ) {
			withTypeInference(
					getInferenceBasis( inferenceSupplier, resolved ),
					expression.getOtherwise()
			);
		}

		return expression;
	}

	private static SqmExpressibleAccessor<?> getInferenceBasis(
			SqmExpressibleAccessor<?> inferenceSupplier, SqmExpressibleAccessor<?> resolved) {
//		return resolved == null && inferenceSupplier != null ? inferenceSupplier : resolved;
		return inferenceSupplier == null ? resolved : inferenceSupplier;
	}

	private SqmExpressibleAccessor<?> highestPrecedence(SqmExpressibleAccessor<?> type1, SqmExpressibleAccessor<?> type2) {
		if ( type1 == null ) {
			return type2;
		}
		if ( type2 == null ) {
			return type1;
		}

		if ( type1.getExpressible() != null ) {
			return type1;
		}

		if ( type2.getExpressible() != null ) {
			return type2;
		}

		return type1;
	}

	private <T> SqmExpressibleAccessor<T> toExpressibleAccessor(SqmExpression<T> expression) {
		final SqmExpressible<T> expressible = expression.getExpressible();
		return expressible == null ? null : () -> expressible;
	}

	@Override
	public Object visitIndexedPluralAccessPath(SqmIndexedCollectionAccessPath<?> path) {
		path.getLhs().accept( this );
		withTypeInference( path.getPluralAttribute().getIndexPathSource(), path.getSelectorExpression() );
		return path;
	}

	@Override
	public Object visitIsEmptyPredicate(SqmEmptinessPredicate predicate) {
		final SqmExpressibleAccessor<?> original = this.inferenceBasis;
		this.inferenceBasis = null;
		super.visitIsEmptyPredicate( predicate );
		this.inferenceBasis = original;
		return predicate;
	}

	@Override
	public Object visitIsNullPredicate(SqmNullnessPredicate predicate) {
		final SqmExpressibleAccessor<?> original = this.inferenceBasis;
		this.inferenceBasis = null;
		super.visitIsNullPredicate( predicate );
		this.inferenceBasis = original;
		return predicate;
	}

	@Override
	public Object visitIsTruePredicate(SqmTruthnessPredicate predicate) {
		final SqmExpressibleAccessor<?> original = this.inferenceBasis;
		this.inferenceBasis = null;
		super.visitIsTruePredicate( predicate );
		this.inferenceBasis = original;
		return predicate;
	}

	@Override
	public Object visitComparisonPredicate(SqmComparisonPredicate predicate) {
		withTypeInference( predicate.getRightHandExpression(), predicate.getLeftHandExpression() );
		withTypeInference( predicate.getLeftHandExpression(), predicate.getRightHandExpression() );
		return predicate;
	}

	@Override
	public Object visitBetweenPredicate(SqmBetweenPredicate predicate) {
		withTypeInference( predicate.getLowerBound(), predicate.getExpression() );
		withTypeInference( predicate.getExpression(), predicate.getLowerBound() );
		withTypeInference( predicate.getExpression(), predicate.getUpperBound() );
		return predicate;
	}

	@Override
	public Object visitLikePredicate(SqmLikePredicate predicate) {
		withTypeInference( predicate.getPattern(), predicate.getMatchExpression() );
		withTypeInference( predicate.getMatchExpression(), predicate.getPattern() );
		if ( predicate.getEscapeCharacter() != null ) {
			withTypeInference( predicate.getMatchExpression(), predicate.getEscapeCharacter() );
		}
		return predicate;
	}

	@Override
	public Object visitMemberOfPredicate(SqmMemberOfPredicate predicate) {
		withTypeInference( predicate.getPluralPath(), predicate.getLeftHandExpression() );
		predicate.getPluralPath().accept( this );
		return predicate;
	}

	@Override
	public Object visitInListPredicate(SqmInListPredicate<?> predicate) {
		final SqmExpression<?> firstListElement = predicate.getListExpressions().isEmpty()
				? null
				: predicate.getListExpressions().get( 0 );
		withTypeInference( firstListElement, predicate.getTestExpression() );
		for ( SqmExpression<?> expression : predicate.getListExpressions() ) {
			withTypeInference( predicate.getTestExpression(), expression );
		}
		return predicate;
	}

	@Override
	public Object visitInSubQueryPredicate(SqmInSubQueryPredicate<?> predicate) {
		withTypeInference( predicate.getSubQueryExpression(), predicate.getTestExpression() );
		withTypeInference( predicate.getTestExpression(), predicate.getSubQueryExpression() );
		return predicate;
	}
}
