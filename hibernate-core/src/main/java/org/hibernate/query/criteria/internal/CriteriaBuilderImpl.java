/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import javax.persistence.Tuple;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.criteria.CriteriaBuilderException;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaAttributeJoinImplementor;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaCollectionJoinImplementor;
import org.hibernate.query.criteria.JpaCompoundPredicate;
import org.hibernate.query.criteria.JpaExpressionImplementor;
import org.hibernate.query.criteria.JpaInImplementor;
import org.hibernate.query.criteria.JpaListJoinImplementor;
import org.hibernate.query.criteria.JpaMapJoinImplementor;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaPathImplementor;
import org.hibernate.query.criteria.JpaPredicateImplementor;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.hibernate.query.criteria.JpaSetJoinImplementor;
import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.query.criteria.internal.expression.BinaryArithmeticOperation;
import org.hibernate.query.criteria.internal.expression.CoalesceExpression;
import org.hibernate.query.criteria.internal.expression.ConcatExpression;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.hibernate.query.criteria.internal.expression.NullLiteralExpression;
import org.hibernate.query.criteria.internal.expression.NullifExpression;
import org.hibernate.query.criteria.internal.expression.ParameterExpressionImpl;
import org.hibernate.query.criteria.internal.expression.SearchedCaseExpression;
import org.hibernate.query.criteria.internal.expression.SimpleCaseExpression;
import org.hibernate.query.criteria.internal.expression.SizeOfPluralAttributeExpression;
import org.hibernate.query.criteria.internal.expression.SubqueryComparisonModifierExpression;
import org.hibernate.query.criteria.internal.expression.UnaryArithmeticOperation;
import org.hibernate.query.criteria.internal.expression.function.AbsFunction;
import org.hibernate.query.criteria.internal.expression.function.AggregationFunction;
import org.hibernate.query.criteria.internal.expression.function.BasicFunctionExpression;
import org.hibernate.query.criteria.internal.expression.function.CurrentDateFunction;
import org.hibernate.query.criteria.internal.expression.function.CurrentTimeFunction;
import org.hibernate.query.criteria.internal.expression.function.CurrentTimestampFunction;
import org.hibernate.query.criteria.internal.expression.function.LengthFunction;
import org.hibernate.query.criteria.internal.expression.function.LocateFunction;
import org.hibernate.query.criteria.internal.expression.function.LowerFunction;
import org.hibernate.query.criteria.internal.expression.function.ParameterizedFunctionExpression;
import org.hibernate.query.criteria.internal.expression.function.SqrtFunction;
import org.hibernate.query.criteria.internal.expression.function.SubstringFunction;
import org.hibernate.query.criteria.internal.expression.function.TrimFunction;
import org.hibernate.query.criteria.internal.expression.function.UpperFunction;
import org.hibernate.query.criteria.internal.path.PluralAttributePath;
import org.hibernate.query.criteria.internal.path.RootImpl;
import org.hibernate.query.criteria.internal.predicate.BetweenPredicate;
import org.hibernate.query.criteria.internal.predicate.BooleanAssertionPredicate;
import org.hibernate.query.criteria.internal.predicate.BooleanExpressionPredicate;
import org.hibernate.query.criteria.internal.predicate.BooleanStaticAssertionPredicate;
import org.hibernate.query.criteria.internal.predicate.ComparisonPredicate;
import org.hibernate.query.criteria.internal.predicate.ComparisonPredicate.ComparisonOperator;
import org.hibernate.query.criteria.internal.predicate.ExistsPredicate;
import org.hibernate.query.criteria.internal.predicate.InPredicate;
import org.hibernate.query.criteria.internal.predicate.IsEmptyPredicate;
import org.hibernate.query.criteria.internal.predicate.LikePredicate;
import org.hibernate.query.criteria.internal.predicate.MemberOfPredicate;
import org.hibernate.query.criteria.internal.predicate.NullnessPredicate;
import org.hibernate.query.criteria.internal.selection.ArrayJpaSelectionImpl;
import org.hibernate.query.criteria.internal.selection.DynamicInstantiationImpl;
import org.hibernate.query.criteria.internal.selection.TupleJpaSelectionImpl;
import org.hibernate.sqm.parser.criteria.tree.JpaCriteriaQuery;
import org.hibernate.sqm.parser.criteria.tree.JpaExpression;
import org.hibernate.sqm.parser.criteria.tree.from.JpaRoot;
import org.hibernate.sqm.parser.criteria.tree.select.JpaCompoundSelection;

/**
 * Hibernate implementation of the JPA {@link CriteriaBuilder} contract.
 *
 * @author Steve Ebersole
 */
public class CriteriaBuilderImpl implements HibernateCriteriaBuilder, Serializable {
	private final SessionFactoryImpl sessionFactory;

	public CriteriaBuilderImpl(SessionFactoryImpl sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Provides protected access to the underlying {@link SessionFactoryImpl}.
	 *
	 * @return The underlying {@link SessionFactoryImpl}
	 */
	public SessionFactoryImplementor getEntityManagerFactory() {
		return sessionFactory;
	}


	// Query builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public JpaCriteriaQuery<Object> createQuery() {
		return new CriteriaQueryImpl<>( this, Object.class );
	}

	@Override
	public <T> JpaCriteriaQuery<T> createQuery(Class<T> resultClass) {
		return new CriteriaQueryImpl<>( this, resultClass );
	}

	@Override
	public JpaCriteriaQuery<Tuple> createTupleQuery() {
		return new CriteriaQueryImpl<>( this, Tuple.class );
	}

	@Override
	public <T> CriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity) {
		return new CriteriaUpdateImpl<>( this );
	}

	@Override
	public <T> CriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity) {
		return new CriteriaDeleteImpl<>( this );
	}


	// selections ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Package-protected method to centralize checking of criteria query multi-selects as defined by the
	 * {@link CriteriaQuery#multiselect(List)}  method.
	 *
	 * @param selections The selection varargs to check
	 *
	 * @throws IllegalArgumentException If the selection items are not valid per {@link CriteriaQuery#multiselect}
	 * documentation.
	 * <i>&quot;.&quot;</i>
	 */
	@Override
	public void checkMultiSelect(List<Selection<?>> selections) {
		// especially check:
		//		"An argument to the multiselect method must not be a tuple- or array-valued compound selection item"
		//
		// although we may want to allow that as an extension for certain cases (like nested dynamic instantiations)...
		//		see HHH-11115

		final HashSet<String> aliases = new HashSet<>( CollectionHelper.determineProperSizing( selections.size() ) );

		for ( Selection<?> selection : selections ) {
			if ( selection.isCompoundSelection() ) {
				if ( selection.getJavaType().isArray() ) {
					throw new IllegalArgumentException(
							"Selection items in a multi-select cannot contain compound array-valued elements"
					);
				}
				if ( Tuple.class.isAssignableFrom( selection.getJavaType() ) ) {
					throw new IllegalArgumentException(
							"Selection items in a multi-select cannot contain compound tuple-valued elements"
					);
				}
			}
			if ( StringHelper.isNotEmpty( selection.getAlias() ) ) {
				boolean added = aliases.add( selection.getAlias() );
				if ( ! added ) {
					throw new IllegalArgumentException( "Multi-select expressions defined duplicate alias : " + selection.getAlias() );
				}
			}
		}
	}

	@Override
	public void checkIsJpaExpression(Selection<?> selection) {
		if ( selection instanceof JpaExpressionImplementor ) {
			return;
		}
		throw new CriteriaBuilderException(
				"Expecting javax.persistence.criteria.Selection to be " +
						"org.hibernate.query.criteria.JpaExpressionImplementor, but found " +
						selection.toString()
		);
	}

	@Override
	public void checkIsJpaExpression(Expression<?> expr) {
		if ( expr instanceof JpaExpression ) {
			return;
		}
		throw new CriteriaBuilderException(
				"Expecting javax.persistence.criteria.Expression to be " +
						"org.hibernate.query.criteria.JpaExpressionImplementor, but found " +
						expr.toString()
		);
	}

	@Override
	public  void checkIsJpaPredicate(Predicate predicate) {
		if ( predicate instanceof JpaPredicateImplementor ) {
			return;
		}
		throw new CriteriaBuilderException(
				"Expecting javax.persistence.criteria.Predicate to be " +
						"org.hibernate.query.criteria.JpaPredicateImplementor, but found " +
						predicate.toString()
		);
	}

	@Override
	public JpaCompoundSelection<Tuple> tuple(Selection<?>... selections) {
		return tuple( Arrays.asList( selections ) );
	}

	/**
	 * Version of {@link #tuple(Selection[])} taking a list.
	 *
	 * @param selections List of selections.
	 *
	 * @return The tuple compound selection
	 */
	public JpaCompoundSelection<Tuple> tuple(List<Selection<?>> selections) {
		checkMultiSelect( selections );
		return new TupleJpaSelectionImpl( this, Tuple.class, toExpressions( selections ) );
	}

	private List<JpaExpression<?>> toExpressions(List<Selection<?>> selections) {
		if ( selections == null || selections.isEmpty() ) {
			return Collections.emptyList();
		}

		final ArrayList<JpaExpression<?>> expressions = new ArrayList<>();
		for ( Selection<?> selection : selections ) {
			checkIsJpaExpression( selection );
			expressions.add( (JpaExpression) selection );
		}

		return expressions;
	}

	@Override
	public JpaCompoundSelection<Object[]> array(Selection<?>... selections) {
		return array( Arrays.asList( selections ) );
	}

	/**
	 * Version of {@link #array(Selection[])} taking a list of selections.
	 *
	 * @param selections List of selections.
	 *
	 * @return The array compound selection
	 */
	public JpaCompoundSelection<Object[]> array(List<Selection<?>> selections) {
		return array( Object[].class, selections );
	}

	/**
	 * Version of {@link #array(Selection[])} taking a list of selections,
	 * as well as the type of array.
	 *
	 * @param type The type of array
	 * @param selections List of selections.
	 *
	 * @return The array compound selection
	 */
	public <Y> JpaCompoundSelection<Y> array(Class<Y> type, List<Selection<?>> selections) {
		checkMultiSelect( selections );
		return new ArrayJpaSelectionImpl<>( this, type, toExpressions( selections ) );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> construct(Class<Y> result, Selection<?>... selections) {
		return construct( result, Arrays.asList( selections ) );
	}

	/**
	 * Version of {@link #construct(Class,Selection[])} taking the
	 * to-be-constructed type as well as a list of selections.
	 *
	 * @param result The result class to be constructed.
	 * @param selections The selections to use in the constructor call.
	 *
	 * @return The <b>view</b> compound selection.
	 */
	public <Y> JpaCompoundSelection<Y> construct(Class<Y> result, List<Selection<?>> selections) {
		checkMultiSelect( selections );
		return new DynamicInstantiationImpl<>( this, result, toExpressions( selections ) );
	}


	// ordering ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Order asc(Expression<?> x) {
		return new OrderImpl( x, true );
	}

	@Override
	public Order desc(Expression<?> x) {
		return new OrderImpl( x, false );
	}


	// predicates ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public JpaPredicateImplementor wrap(Expression<Boolean> expression) {
		if ( JpaPredicateImplementor.class.isInstance( expression ) ) {
			return ( (JpaPredicateImplementor) expression );
		}
		else if ( JpaPathImplementor.class.isInstance( expression ) ) {
			return new BooleanAssertionPredicate( this, expression, Boolean.TRUE );
		}
		else {
			return new BooleanExpressionPredicate( this, expression );
		}
	}

	@Override
	public JpaPredicateImplementor not(Expression<Boolean> expression) {
		return wrap( expression ).not();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaPredicateImplementor and(Expression<Boolean> x, Expression<Boolean> y) {
		return new JpaCompoundPredicate(
				this,
				Predicate.BooleanOperator.AND,
				Arrays.asList( wrap( x ), wrap( y ) )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaPredicateImplementor or(Expression<Boolean> x, Expression<Boolean> y) {
		return new JpaCompoundPredicate(
				this,
				Predicate.BooleanOperator.OR,
				Arrays.asList( wrap( x ), wrap( y ) )
		);
	}

	@Override
	public JpaPredicateImplementor and(Predicate... restrictions) {
		final JpaCompoundPredicate conjunction = new JpaCompoundPredicate( this, Predicate.BooleanOperator.AND );
		for ( Predicate restriction : restrictions ) {
			checkIsJpaPredicate( restriction );
			conjunction.applyPredicate( (JpaPredicateImplementor) restriction );
		}
		return conjunction;
	}

	@Override
	public JpaPredicateImplementor or(Predicate... restrictions) {
		final JpaCompoundPredicate disjunction = new JpaCompoundPredicate( this, Predicate.BooleanOperator.OR );
		for ( Predicate restriction : restrictions ) {
			checkIsJpaPredicate( restriction );
			disjunction.applyPredicate( (JpaPredicateImplementor) restriction );
		}
		return disjunction;
	}

	@Override
	public JpaPredicateImplementor conjunction() {
		return new JpaCompoundPredicate( this, Predicate.BooleanOperator.AND );
	}

	@Override
	public JpaPredicateImplementor disjunction() {
		return new JpaCompoundPredicate( this, Predicate.BooleanOperator.OR );
	}

	@Override
	public JpaPredicateImplementor isTrue(Expression<Boolean> expression) {
		if ( JpaCompoundPredicate.class.isInstance( expression ) ) {
			final JpaCompoundPredicate predicate = (JpaCompoundPredicate) expression;
			if ( predicate.getExpressions().size() == 0 ) {
				return new BooleanStaticAssertionPredicate(
						this,
						predicate.getOperator() == Predicate.BooleanOperator.AND
				);
			}
			return predicate;
		}
		else if ( JpaPredicateImplementor.class.isInstance( expression ) ) {
			return (JpaPredicateImplementor) expression;
		}

		return wrap( expression );
	}

	@Override
	public JpaPredicateImplementor isFalse(Expression<Boolean> expression) {
		if ( JpaCompoundPredicate.class.isInstance( expression ) ) {
			final JpaCompoundPredicate predicate = (JpaCompoundPredicate) expression;
			if ( predicate.getExpressions().size() == 0 ) {
				return new BooleanStaticAssertionPredicate(
						this,
						predicate.getOperator() == Predicate.BooleanOperator.OR
				);
			}
			predicate.not();
			return predicate;
		}
		else if ( JpaPredicateImplementor.class.isInstance( expression ) ) {
			final JpaPredicateImplementor predicate = (JpaPredicateImplementor) expression;
			predicate.not();
			return predicate;
		}
		return new BooleanAssertionPredicate( this, expression, Boolean.FALSE );
	}

	@Override
	public JpaPredicateImplementor isNull(Expression<?> x) {
		return new NullnessPredicate( this, x );
	}

	@Override
	public JpaPredicateImplementor isNotNull(Expression<?> x) {
		return isNull( x ).not();
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor equal(Expression<?> x, Expression<?> y) {
		return new ComparisonPredicate( this, ComparisonOperator.EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor notEqual(Expression<?> x, Expression<?> y) {
		return new ComparisonPredicate( this, ComparisonOperator.NOT_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor equal(Expression<?> x, Object y) {
		return new ComparisonPredicate( this, ComparisonOperator.EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor notEqual(Expression<?> x, Object y) {
		return new ComparisonPredicate( this, ComparisonOperator.NOT_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> JpaPredicateImplementor greaterThan(Expression<? extends Y> x, Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> JpaPredicateImplementor lessThan(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> JpaPredicateImplementor greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> JpaPredicateImplementor lessThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> JpaPredicateImplementor greaterThan(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> JpaPredicateImplementor lessThan(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> JpaPredicateImplementor greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public<Y extends Comparable<? super Y>> JpaPredicateImplementor lessThanOrEqualTo(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor gt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor lt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor ge(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor le(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor gt(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor lt(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor ge(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public JpaPredicateImplementor le(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicateImplementor between(
			Expression<? extends Y> expression,
			Y lowerBound,
			Y upperBound) {
		return new BetweenPredicate<>( this, expression, lowerBound, upperBound );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicateImplementor between(
			Expression<? extends Y> expression,
			Expression<? extends Y> lowerBound,
			Expression<? extends Y> upperBound) {
		return new BetweenPredicate<>( this, expression, lowerBound, upperBound );
	}

	@Override
	public <T> JpaInImplementor<T> in(Expression<? extends T> expression) {
		return new InPredicate<>( this, expression );
	}

	@Override
	public <T> JpaInImplementor<T> in(Expression<? extends T> expression, Expression<? extends T>... values) {
		return new InPredicate<>( this, expression, values );
	}

	@Override
	public <T> JpaInImplementor<T> in(Expression<? extends T> expression, T... values) {
		return new InPredicate<>( this, expression, values );
	}

	public <T> In<T> in(Expression<? extends T> expression, Collection<T> values) {
		return new InPredicate<>( this, expression, values );
	}

	@Override
	public JpaPredicateImplementor like(Expression<String> matchExpression, Expression<String> pattern) {
		return new LikePredicate( this, matchExpression, pattern );
	}

	@Override
	public JpaPredicateImplementor like(Expression<String> matchExpression, Expression<String> pattern, Expression<Character> escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public JpaPredicateImplementor like(Expression<String> matchExpression, Expression<String> pattern, char escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public JpaPredicateImplementor like(Expression<String> matchExpression, String pattern) {
		return new LikePredicate( this, matchExpression, pattern );
	}

	@Override
	public JpaPredicateImplementor like(Expression<String> matchExpression, String pattern, Expression<Character> escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public JpaPredicateImplementor like(Expression<String> matchExpression, String pattern, char escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public JpaPredicateImplementor notLike(Expression<String> matchExpression, Expression<String> pattern) {
		return like( matchExpression, pattern ).not();
	}

	@Override
	public JpaPredicateImplementor notLike(Expression<String> matchExpression, Expression<String> pattern, Expression<Character> escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}

	@Override
	public JpaPredicateImplementor notLike(Expression<String> matchExpression, Expression<String> pattern, char escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}

	@Override
	public JpaPredicateImplementor notLike(Expression<String> matchExpression, String pattern) {
		return like( matchExpression, pattern ).not();
	}

	@Override
	public JpaPredicateImplementor notLike(Expression<String> matchExpression, String pattern, Expression<Character> escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}

	@Override
	public JpaPredicateImplementor notLike(Expression<String> matchExpression, String pattern, char escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}


	// parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <T> JpaParameterExpression<T> parameter(Class<T> paramClass) {
		return new ParameterExpressionImpl<>(
				this,
				paramClass
		);
	}

	@Override
	public <T> JpaParameterExpression<T> parameter(Class<T> paramClass, String name) {
		return new ParameterExpressionImpl<>(
				this,
				paramClass,
				name
		);
	}

	@Override
	public <T> JpaExpressionImplementor<T> literal(T value) {
		if ( value == null ) {
			throw new IllegalArgumentException( "literal value cannot be null" );
		}
		return new LiteralExpression<>( this, value );
	}

	@Override
	public <T> JpaExpressionImplementor<T> nullLiteral(Class<T> resultClass) {
		return new NullLiteralExpression<>( this, resultClass );
	}


	// aggregate functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <N extends Number> JpaExpressionImplementor<Double> avg(Expression<N> x) {
		return new AggregationFunction.AVG( this, x );
	}

	@Override
	public <N extends Number> JpaExpressionImplementor<N> sum(Expression<N> x) {
		return new AggregationFunction.SUM<>( this, x );
	}

	@Override
	public JpaExpressionImplementor<Long> sumAsLong(Expression<Integer> x) {
		return new AggregationFunction.SUM<>( this, x, Long.class );
	}

	@Override
	public JpaExpressionImplementor<Double> sumAsDouble(Expression<Float> x) {
		return new AggregationFunction.SUM<>( this, x, Double.class );
	}

	@Override
	public <N extends Number> JpaExpressionImplementor<N> max(Expression<N> x) {
		return new AggregationFunction.MAX<>( this, x );
	}

	@Override
	public <N extends Number> JpaExpressionImplementor<N> min(Expression<N> x) {
		return new AggregationFunction.MIN<>( this, x );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X extends Comparable<? super X>> JpaExpressionImplementor<X> greatest(Expression<X> x) {
		return new AggregationFunction.GREATEST( this, x );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X extends Comparable<? super X>> JpaExpressionImplementor<X> least(Expression<X> x) {
		return new AggregationFunction.LEAST( this, x );
	}

	@Override
	public JpaExpressionImplementor<Long> count(Expression<?> x) {
		return new AggregationFunction.COUNT( this, x, false );
	}

	@Override
	public JpaExpressionImplementor<Long> countDistinct(Expression<?> x) {
		return new AggregationFunction.COUNT( this, x, true );
	}


	// other functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <T> JpaExpressionImplementor<T> function(String name, Class<T> returnType, Expression<?>... arguments) {
		return new ParameterizedFunctionExpression<>( this, returnType, name, arguments );
	}

	/**
	 * Create a reference to a function taking no params.
	 *
	 * @param name The function name.
	 * @param returnType The return type.
	 *
	 * @return The function expression
	 */
	public <T> Expression<T> function(String name, Class<T> returnType) {
		return new BasicFunctionExpression<>( this, returnType, name );
	}

	@Override
	public <N extends Number> JpaExpressionImplementor<N> abs(Expression<N> expression) {
		return new AbsFunction<>( this, expression );
	}

	@Override
	public JpaExpressionImplementor<Double> sqrt(Expression<? extends Number> expression) {
		return new SqrtFunction( this, expression );
	}

	@Override
	public JpaExpressionImplementor<java.sql.Date> currentDate() {
		return new CurrentDateFunction( this );
	}

	@Override
	public JpaExpressionImplementor<java.sql.Timestamp> currentTimestamp() {
		return new CurrentTimestampFunction( this );
	}

	@Override
	public JpaExpressionImplementor<java.sql.Time> currentTime() {
		return new CurrentTimeFunction( this );
	}

	@Override
	public JpaExpressionImplementor<String> substring(Expression<String> value, Expression<Integer> start) {
		return new SubstringFunction( this, value, start );
	}

	@Override
	public JpaExpressionImplementor<String> substring(Expression<String> value, int start) {
		return new SubstringFunction( this, value, start );
	}

	@Override
	public JpaExpressionImplementor<String> substring(Expression<String> value, Expression<Integer> start, Expression<Integer> length) {
		return new SubstringFunction( this, value, start, length );
	}

	@Override
	public JpaExpressionImplementor<String> substring(Expression<String> value, int start, int length) {
		return new SubstringFunction( this, value, start, length );
	}

	@Override
	public JpaExpressionImplementor<String> trim(Expression<String> trimSource ) {
		return new TrimFunction( this, trimSource );
	}

	@Override
	public JpaExpressionImplementor<String> trim(Trimspec trimspec, Expression<String> trimSource) {
		return new TrimFunction( this, trimspec, trimSource );
	}

	@Override
	public JpaExpressionImplementor<String> trim(Expression<Character> trimCharacter, Expression<String> trimSource) {
		return new TrimFunction( this, trimCharacter, trimSource );
	}

	@Override
	public JpaExpressionImplementor<String> trim(Trimspec trimspec, Expression<Character> trimCharacter, Expression<String> trimSource) {
		return new TrimFunction( this, trimspec, trimCharacter, trimSource );
	}

	@Override
	public JpaExpressionImplementor<String> trim(char trimCharacter, Expression<String> trimSource) {
		return new TrimFunction( this, trimCharacter, trimSource );
	}

	@Override
	public JpaExpressionImplementor<String> trim(Trimspec trimspec, char trimCharacter, Expression<String> trimSource) {
		return new TrimFunction( this, trimspec, trimCharacter, trimSource );
	}

	@Override
	public JpaExpressionImplementor<String> lower(Expression<String> value) {
		return new LowerFunction( this, value );
	}

	@Override
	public JpaExpressionImplementor<String> upper(Expression<String> value) {
		return new UpperFunction( this, value );
	}

	@Override
	public JpaExpressionImplementor<Integer> length(Expression<String> value) {
		return new LengthFunction( this, value );
	}

	@Override
	public JpaExpressionImplementor<Integer> locate(Expression<String> string, Expression<String> pattern) {
		return new LocateFunction( this, pattern, string );
	}

	@Override
	public JpaExpressionImplementor<Integer> locate(Expression<String> string, Expression<String> pattern, Expression<Integer> start) {
		return new LocateFunction( this, pattern, string, start );
	}

	@Override
	public JpaExpressionImplementor<Integer> locate(Expression<String> string, String pattern) {
		return new LocateFunction( this, pattern, string );
	}

	@Override
	public JpaExpressionImplementor<Integer> locate(Expression<String> string, String pattern, int start) {
		return new LocateFunction( this, pattern, string, start );
	}


	// arithmetic operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <N extends Number> JpaExpressionImplementor<N> neg(Expression<N> expression) {
		return new UnaryArithmeticOperation<>(
				this,
				UnaryArithmeticOperation.Operation.UNARY_MINUS,
				expression
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> JpaExpressionImplementor<N> sum(Expression<? extends N> expression1, Expression<? extends N> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to sum() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression1.getJavaType(), expression2.getJavaType() );

		return new BinaryArithmeticOperation<>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.ADD,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> JpaExpressionImplementor<N> prod(Expression<? extends N> expression1, Expression<? extends N> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to prod() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression1.getJavaType(), expression2.getJavaType() );

		return new BinaryArithmeticOperation<>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.MULTIPLY,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> JpaExpressionImplementor<N> diff(Expression<? extends N> expression1, Expression<? extends N> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to diff() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression1.getJavaType(), expression2.getJavaType() );

		return new BinaryArithmeticOperation<>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.SUBTRACT,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> JpaExpressionImplementor<N> sum(Expression<? extends N> expression, N n) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to sum() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression.getJavaType(), n.getClass() );

		return new BinaryArithmeticOperation<>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.ADD,
				expression,
				n
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> JpaExpressionImplementor<N> prod(Expression<? extends N> expression, N n) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to prod() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression.getJavaType(), n.getClass() );

		return new BinaryArithmeticOperation<>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.MULTIPLY,
				expression,
				n
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> JpaExpressionImplementor<N> diff(Expression<? extends N> expression, N n) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to diff() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression.getJavaType(), n.getClass() );

		return new BinaryArithmeticOperation<>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.SUBTRACT,
				expression,
				n
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> JpaExpressionImplementor<N> sum(N n, Expression<? extends N> expression) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to sum() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( n.getClass(), expression.getJavaType() );

		return new BinaryArithmeticOperation<>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.ADD,
				n,
				expression
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> JpaExpressionImplementor<N> prod(N n, Expression<? extends N> expression) {
		if ( n == null || expression == null ) {
			throw new IllegalArgumentException( "arguments to prod() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( n.getClass(), expression.getJavaType() );

		return (BinaryArithmeticOperation<N>) new BinaryArithmeticOperation(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.MULTIPLY,
				n,
				expression
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> JpaExpressionImplementor<N> diff(N n, Expression<? extends N> expression) {
		if ( n == null || expression == null ) {
			throw new IllegalArgumentException( "arguments to diff() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( n.getClass(), expression.getJavaType() );

		return new BinaryArithmeticOperation<>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.SUBTRACT,
				n,
				expression
		);
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public JpaExpressionImplementor<Number> quot(Expression<? extends Number> expression1, Expression<? extends Number> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to quot() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression1.getJavaType(), expression2.getJavaType(), true );

		return new BinaryArithmeticOperation<>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.DIVIDE,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public JpaExpressionImplementor<Number> quot(Expression<? extends Number> expression, Number number) {
		if ( expression == null || number == null ) {
			throw new IllegalArgumentException( "arguments to quot() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression.getJavaType(), number.getClass(), true );

		return new BinaryArithmeticOperation<>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.DIVIDE,
				expression,
				number
		);
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public JpaExpressionImplementor<Number> quot(Number number, Expression<? extends Number> expression) {
		if ( expression == null || number == null ) {
			throw new IllegalArgumentException( "arguments to quot() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( number.getClass(), expression.getJavaType(), true );

		return new BinaryArithmeticOperation<>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.DIVIDE,
				number,
				expression
		);
	}

	@Override
	public JpaExpressionImplementor<Integer> mod(Expression<Integer> expression1, Expression<Integer> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to mod() cannot be null" );
		}

		return new BinaryArithmeticOperation<>(
				this,
				Integer.class,
				BinaryArithmeticOperation.Operation.MOD,
				expression1,
				expression2
		);
	}

	@Override
	public JpaExpressionImplementor<Integer> mod(Expression<Integer> expression, Integer integer) {
		if ( expression == null || integer == null ) {
			throw new IllegalArgumentException( "arguments to mod() cannot be null" );
		}

		return new BinaryArithmeticOperation<>(
				this,
				Integer.class,
				BinaryArithmeticOperation.Operation.MOD,
				expression,
				integer
		);
	}

	@Override
	public JpaExpressionImplementor<Integer> mod(Integer integer, Expression<Integer> expression) {
		if ( integer == null || expression == null ) {
			throw new IllegalArgumentException( "arguments to mod() cannot be null" );
		}

		return new BinaryArithmeticOperation<>(
				this,
				Integer.class,
				BinaryArithmeticOperation.Operation.MOD,
				integer,
				expression
		);
	}


	// casting ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpressionImplementor<Long> toLong(Expression<? extends Number> expression) {
		return ( (JpaExpressionImplementor<? extends Number>) expression ).asLong();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpressionImplementor<Integer> toInteger(Expression<? extends Number> expression) {
		return ( (JpaExpressionImplementor<? extends Number>) expression ).asInteger();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpressionImplementor<Float> toFloat(Expression<? extends Number> expression) {
		return ( (JpaExpressionImplementor<? extends Number>) expression ).asFloat();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpressionImplementor<Double> toDouble(Expression<? extends Number> expression) {
		return ( (JpaExpressionImplementor<? extends Number>) expression ).asDouble();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpressionImplementor<BigDecimal> toBigDecimal(Expression<? extends Number> expression) {
		return ( (JpaExpressionImplementor<? extends Number>) expression ).asBigDecimal();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpressionImplementor<BigInteger> toBigInteger(Expression<? extends Number> expression) {
		return ( (JpaExpressionImplementor<? extends Number>) expression ).asBigInteger();
	}

	@Override
	public JpaExpressionImplementor<String> toString(Expression<Character> characterExpression) {
		return ( (JpaExpressionImplementor<Character>) characterExpression ).asString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T, V extends T> JpaAttributeJoinImplementor<X, V> treat(Join<X, T> join, Class<V> type) {
		return treat( join, type, (j, t) -> ((JpaAttributeJoinImplementor) j).treatAs( t ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T, E extends T> JpaCollectionJoinImplementor<X, E> treat(CollectionJoin<X, T> join, Class<E> type) {
		return treat( join, type, (j, t) -> ((JpaCollectionJoinImplementor) j).treatAs( t ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T, E extends T> JpaSetJoinImplementor<X, E> treat(SetJoin<X, T> join, Class<E> type) {
		return treat( join, type, (j, t) -> ((JpaSetJoinImplementor) j).treatAs( t ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T, E extends T> JpaListJoinImplementor<X, E> treat(ListJoin<X, T> join, Class<E> type) {
		return treat( join, type, (j, t) -> ((JpaListJoinImplementor) join).treatAs( type ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, K, T, V extends T> JpaMapJoinImplementor<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type) {
		return treat( join, type, (j, t) -> ((JpaMapJoinImplementor) join).treatAs( type ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T extends X> JpaPathImplementor<T> treat(Path<X> path, Class<T> type) {
		return ( (JpaPathImplementor) path ).treatAs( type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T extends X> JpaRoot<T> treat(Root<X> root, Class<T> type) {
		return ((RootImpl) root).treatAs( type );
	}

	// subqueries ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public JpaPredicateImplementor exists(Subquery<?> subquery) {
		return new ExistsPredicate( this, subquery );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> JpaExpressionImplementor<Y> all(Subquery<Y> subquery) {
		return new SubqueryComparisonModifierExpression<>(
				this,
				(Class<Y>) subquery.getJavaType(),
				subquery,
				SubqueryComparisonModifierExpression.Modifier.ALL
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> JpaExpressionImplementor<Y> some(Subquery<Y> subquery) {
		return new SubqueryComparisonModifierExpression<>(
				this,
				(Class<Y>) subquery.getJavaType(),
				subquery,
				SubqueryComparisonModifierExpression.Modifier.SOME
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> JpaExpressionImplementor<Y> any(Subquery<Y> subquery) {
		return new SubqueryComparisonModifierExpression<>(
				this,
				(Class<Y>) subquery.getJavaType(),
				subquery,
				SubqueryComparisonModifierExpression.Modifier.ANY
		);
	}


	// miscellaneous expressions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SuppressWarnings({ "RedundantCast" })
	public <Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> exp1, Expression<? extends Y> exp2) {
		return coalesce( (Class<Y>) null, exp1, exp2 );
	}

	public <Y> JpaCoalesce<Y> coalesce(Class<Y> type, Expression<? extends Y> exp1, Expression<? extends Y> exp2) {
		return new CoalesceExpression<>( this, type ).value( exp1 ).value( exp2 );
	}

	@Override
	@SuppressWarnings({ "RedundantCast" })
	public <Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> exp1, Y exp2) {
		return coalesce( (Class<Y>) null, exp1, exp2 );
	}

	public <Y> JpaCoalesce<Y> coalesce(Class<Y> type, Expression<? extends Y> exp1, Y exp2) {
		return new CoalesceExpression<>( this, type ).value( exp1 ).value( exp2 );
	}

	@Override
	public <T> JpaCoalesce<T> coalesce() {
		return coalesce( null );
	}

	public <T> JpaCoalesce<T> coalesce(Class<T> type) {
		return new CoalesceExpression<>( this, type );
	}

	@Override
	public JpaExpressionImplementor<String> concat(Expression<String> string1, Expression<String> string2) {
		return new ConcatExpression( this, string1, string2 );
	}

	@Override
	public JpaExpressionImplementor<String> concat(Expression<String> string1, String string2) {
		return new ConcatExpression( this, string1, string2 );
	}

	@Override
	public JpaExpressionImplementor<String> concat(String string1, Expression<String> string2) {
		return new ConcatExpression( this, string1, string2 );
	}

	@Override
	public <Y> JpaExpressionImplementor<Y> nullif(Expression<Y> exp1, Expression<?> exp2) {
		return nullif( null, exp1, exp2 );
	}

	public <Y> JpaExpressionImplementor<Y> nullif(Class<Y> type, Expression<Y> exp1, Expression<?> exp2) {
		return new NullifExpression<>( this, type, exp1, exp2 );
	}

	@Override
	public <Y> JpaExpressionImplementor<Y> nullif(Expression<Y> exp1, Y exp2) {
		return nullif( null, exp1, exp2 );
	}

	public <Y> JpaExpressionImplementor<Y> nullif(Class<Y> type, Expression<Y> exp1, Y exp2) {
		return new NullifExpression<>( this, type, exp1, exp2 );
	}

	@Override
	public <C, R> JpaSimpleCase<C, R> selectCase(Expression<? extends C> expression) {
		return selectCase( null, expression );
	}

	@SuppressWarnings("unchecked")
	public <C, R> JpaSimpleCase<C, R> selectCase(Class<R> type, Expression<? extends C> expression) {
		checkIsJpaExpression( expression );
		return new SimpleCaseExpression<>( this, type, (JpaExpressionImplementor<? extends C>) expression );
	}

	@Override
	public <R> JpaSearchedCase<R> selectCase() {
		return selectCase( (Class<R>)null );
	}

	public <R> JpaSearchedCase<R> selectCase(Class<R> type) {
		return new SearchedCaseExpression<>( this, type );
	}

	@Override
	public <C extends Collection<?>> JpaExpressionImplementor<Integer> size(C c) {
		int size = c == null ? 0 : c.size();
		return new LiteralExpression<>(this, Integer.class, size);
	}

	@Override
	public <C extends Collection<?>> JpaExpressionImplementor<Integer> size(Expression<C> exp) {
		if ( LiteralExpression.class.isInstance(exp) ) {
			return size( ( (LiteralExpression<C>) exp ).getLiteral() );
		}
		else if ( PluralAttributePath.class.isInstance(exp) ) {
			return new SizeOfPluralAttributeExpression( this, (PluralAttributePath<C>) exp );
		}
		// TODO : what other specific types?  any?
		throw new IllegalArgumentException("unknown collection expression type [" + exp.getClass().getName() + "]" );
	}

	@Override
	public <V, M extends Map<?, V>> JpaExpressionImplementor<Collection<V>> values(M map) {
		return new LiteralExpression<>( this, map.values() );
	}

	@Override
	public <K, M extends Map<K, ?>> JpaExpressionImplementor<Set<K>> keys(M map) {
		return new LiteralExpression<>( this, map.keySet() );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <C extends Collection<?>> JpaPredicateImplementor isEmpty(Expression<C> collectionExpression) {
		if ( PluralAttributePath.class.isInstance(collectionExpression) ) {
			return new IsEmptyPredicate( this, (PluralAttributePath<C>) collectionExpression );
		}
		// TODO : what other specific types?  any?
		throw new IllegalArgumentException(
				"unknown collection expression type [" + collectionExpression.getClass().getName() + "]"
		);
	}

	@Override
	public <C extends Collection<?>> JpaPredicateImplementor isNotEmpty(Expression<C> collectionExpression) {
		return isEmpty( collectionExpression ).not();
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicateImplementor isMember(E e, Expression<C> collectionExpression) {
		if ( ! PluralAttributePath.class.isInstance( collectionExpression ) ) {
			throw new IllegalArgumentException(
					"unknown collection expression type [" + collectionExpression.getClass().getName() + "]"
			);
		}
		return new MemberOfPredicate<>(
				this,
				e,
				(PluralAttributePath<C>) collectionExpression
		);
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicateImplementor isNotMember(E e, Expression<C> cExpression) {
		return isMember(e, cExpression).not();
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicateImplementor isMember(Expression<E> elementExpression, Expression<C> collectionExpression) {
		if ( ! PluralAttributePath.class.isInstance( collectionExpression ) ) {
			throw new IllegalArgumentException(
					"unknown collection expression type [" + collectionExpression.getClass().getName() + "]"
			);
		}
		return new MemberOfPredicate<>(
				this,
				elementExpression,
				(PluralAttributePath<C>) collectionExpression
		);
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicateImplementor isNotMember(Expression<E> eExpression, Expression<C> cExpression) {
		return isMember(eExpression, cExpression).not();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <M extends Map<?, ?>> JpaPredicateImplementor isMapEmpty(Expression<M> mapExpression) {
		if ( PluralAttributePath.class.isInstance( mapExpression ) ) {
			return new IsEmptyPredicate( this, (PluralAttributePath<M>) mapExpression );
		}
		// TODO : what other specific types?  any?
		throw new IllegalArgumentException(
				"unknown collection expression type [" + mapExpression.getClass().getName() + "]"
		);
	}

	@Override
	public <M extends Map<?, ?>> JpaPredicateImplementor isMapNotEmpty(Expression<M> mapExpression) {
		return isMapEmpty( mapExpression ).not();
	}

	@Override
	public <M extends Map<?, ?>> JpaExpressionImplementor<Integer> mapSize(Expression<M> mapExpression) {
		if ( LiteralExpression.class.isInstance( mapExpression ) ) {
			return mapSize( ( (LiteralExpression<M>) mapExpression ).getLiteral() );
		}
		else if ( PluralAttributePath.class.isInstance( mapExpression ) ) {
			return new SizeOfPluralAttributeExpression( this, (PluralAttributePath) mapExpression );
		}
		// TODO : what other specific types?  any?
		throw new IllegalArgumentException("unknown collection expression type [" + mapExpression.getClass().getName() + "]" );
	}

	@Override
	public <M extends Map<?, ?>> JpaExpressionImplementor<Integer> mapSize(M map) {
		int size = map == null ? 0 : map.size();
		return new LiteralExpression<>( this, Integer.class, size );
	}

	@SuppressWarnings("unchecked")
	private <X, T, V extends T, K extends JpaAttributeJoinImplementor> K treat(
			Join<X, T> join,
			Class<V> type,
			BiFunction<Join<X, T>, Class<V>, K> f) {
		final Set<Join<X, ?>> joins = join.getParent().getJoins();
		final K treatAs = f.apply( join, type );
		joins.add( treatAs );
		return treatAs;
	}
}
