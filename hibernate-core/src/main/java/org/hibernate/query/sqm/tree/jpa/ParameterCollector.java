/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
 * todo (6.0) : how is this different from {@link org.hibernate.query.sqm.internal.ParameterCollector}?
 *
 * @author Steve Ebersole
 */
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
	 * This is called while performing an inflight parameter collection of parameters
	 * for `CriteriaQuery#getParameters`.  That method can be called multiple times and
	 * the parameters may have changed in between each call - therefore the parameters
	 * must be collected dynamically each time.
	 *
	 * This form simply returns the JpaCriteriaParameter
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
		SqmExpressibleAccessor<?> current = inferenceBasis;
		inferenceBasis = null;
		super.visitFunction( sqmFunction );
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

	private void withTypeInference(SqmExpressibleAccessor<?> inferenceBasis, Runnable action) {
		SqmExpressibleAccessor<?> original = this.inferenceBasis;
		this.inferenceBasis = inferenceBasis;
		try {
			action.run();
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
							return (SqmExpressible<Object>) resolved;
						}
					}
					return null;
				},
				() -> expression.getFixture().accept( this )
		);
		SqmExpressibleAccessor<?> resolved = determineCurrentExpressible( expression );
		for ( SqmCaseSimple.WhenFragment<?, ?> whenFragment : expression.getWhenFragments() ) {
			withTypeInference(
					expression.getFixture(),
					() -> whenFragment.getCheckValue().accept( this )
			);
			withTypeInference(
					resolved == null && inferenceSupplier != null ? inferenceSupplier : resolved,
					() -> whenFragment.getResult().accept( this )
			);
			resolved = highestPrecedence( resolved, whenFragment.getResult() );
		}

		if ( expression.getOtherwise() != null ) {
			withTypeInference(
					resolved == null && inferenceSupplier != null ? inferenceSupplier : resolved,
					() -> expression.getOtherwise().accept( this )
			);
		}

		return expression;
	}

	@Override
	public Object visitSearchedCaseExpression(SqmCaseSearched<?> expression) {
		final SqmExpressibleAccessor<?> inferenceSupplier = this.inferenceBasis;
		SqmExpressibleAccessor<?> resolved = determineCurrentExpressible( expression );

		for ( SqmCaseSearched.WhenFragment<?> whenFragment : expression.getWhenFragments() ) {
			withTypeInference(
					null,
					() -> whenFragment.getPredicate().accept( this )
			);
			withTypeInference(
					resolved == null && inferenceSupplier != null ? inferenceSupplier : resolved,
					() -> whenFragment.getResult().accept( this )
			);
			resolved = highestPrecedence( resolved, whenFragment.getResult() );
		}

		if ( expression.getOtherwise() != null ) {
			withTypeInference(
					resolved == null && inferenceSupplier != null ? inferenceSupplier : resolved,
					() -> expression.getOtherwise().accept( this )
			);
		}

		return expression;
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

	private SqmExpressibleAccessor<?> determineCurrentExpressible(SqmExpression<?> expression) {
		if ( expression.getExpressible() != null ) {
			return () -> (SqmExpressible<Object>) expression.getExpressible();
		}
		return null;
	}

	@Override
	public Object visitIndexedPluralAccessPath(SqmIndexedCollectionAccessPath<?> path) {
		path.getLhs().accept( this );
		withTypeInference( path.getPluralAttribute().getIndexPathSource(), () -> path.getSelectorExpression().accept( this ) );
		return path;
	}

	@Override
	public Object visitIsEmptyPredicate(SqmEmptinessPredicate predicate) {
		withTypeInference( null, () -> super.visitIsEmptyPredicate( predicate ) );
		return predicate;
	}

	@Override
	public Object visitIsNullPredicate(SqmNullnessPredicate predicate) {
		withTypeInference( null, () -> super.visitIsNullPredicate( predicate ) );
		return predicate;
	}

	@Override
	public Object visitIsTruePredicate(SqmTruthnessPredicate predicate) {
		withTypeInference( null, () -> super.visitIsTruePredicate( predicate ) );
		return predicate;
	}

	@Override
	public Object visitComparisonPredicate(SqmComparisonPredicate predicate) {
		withTypeInference( predicate.getRightHandExpression(), () -> predicate.getLeftHandExpression().accept( this ) );
		withTypeInference( predicate.getLeftHandExpression(), () -> predicate.getRightHandExpression().accept( this ) );
		return predicate;
	}

	@Override
	public Object visitBetweenPredicate(SqmBetweenPredicate predicate) {
		withTypeInference( predicate.getLowerBound(), () -> predicate.getExpression().accept( this ) );
		withTypeInference(
				predicate.getExpression(),
				() -> {
					predicate.getLowerBound().accept( this );
					predicate.getUpperBound().accept( this );
				}
		);
		return predicate;
	}

	@Override
	public Object visitLikePredicate(SqmLikePredicate predicate) {
		withTypeInference( predicate.getPattern(), () -> predicate.getMatchExpression().accept( this ) );
		withTypeInference(
				predicate.getMatchExpression(),
				() -> {
					predicate.getPattern().accept( this );
					if ( predicate.getEscapeCharacter() != null ) {
						predicate.getEscapeCharacter().accept( this );
					}
				}
		);
		return predicate;
	}

	@Override
	public Object visitMemberOfPredicate(SqmMemberOfPredicate predicate) {
		withTypeInference( predicate.getPluralPath(), () -> predicate.getLeftHandExpression().accept( this ) );
		predicate.getPluralPath().accept( this );
		return predicate;
	}

	@Override
	public Object visitInListPredicate(SqmInListPredicate<?> predicate) {
		final SqmExpression<?> firstListElement = predicate.getListExpressions().isEmpty()
				? null
				: predicate.getListExpressions().get( 0 );
		withTypeInference( firstListElement, () -> predicate.getTestExpression().accept( this ) );
		withTypeInference(
				predicate.getTestExpression(),
				() -> {
					for ( SqmExpression<?> expression : predicate.getListExpressions() ) {
						expression.accept( this );
					}
				}
		);
		return predicate;
	}

	@Override
	public Object visitInSubQueryPredicate(SqmInSubQueryPredicate<?> predicate) {
		withTypeInference( predicate.getSubQueryExpression(), () -> predicate.getTestExpression().accept( this ) );
		withTypeInference( predicate.getTestExpression(), () -> predicate.getSubQueryExpression().accept( this ) );
		return predicate;
	}
}
