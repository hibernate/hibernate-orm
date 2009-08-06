/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.criteria;

import java.io.Serializable;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.persistence.criteria.QueryBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Subquery;
import javax.persistence.Tuple;

import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.hibernate.ejb.criteria.expression.CompoundSelectionImpl;
import org.hibernate.ejb.criteria.expression.ParameterExpressionImpl;
import org.hibernate.ejb.criteria.expression.LiteralExpression;
import org.hibernate.ejb.criteria.expression.function.AggregationFunction;
import org.hibernate.ejb.criteria.predicate.BooleanExpressionPredicate;
import org.hibernate.ejb.criteria.predicate.ExplicitTruthValueCheck;
import org.hibernate.ejb.criteria.predicate.TruthValue;
import org.hibernate.ejb.criteria.predicate.NullnessPredicate;
import org.hibernate.ejb.criteria.predicate.CompoundPredicate;
import org.hibernate.ejb.criteria.predicate.ComparisonPredicate;
import org.hibernate.ejb.criteria.predicate.InPredicate;
import org.hibernate.ejb.criteria.predicate.BetweenPredicate;
import static org.hibernate.ejb.criteria.predicate.ComparisonPredicate.ComparisonOperator;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class QueryBuilderImpl implements QueryBuilder, Serializable {
	private final EntityManagerFactoryImpl entityManagerFactory;

	public QueryBuilderImpl(EntityManagerFactoryImpl entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	/**
	 * Provides protected access to the underlying {@link EntityManagerFactoryImpl}.
	 *
	 * @return The underlying {@link EntityManagerFactoryImpl}
	 */
	public  EntityManagerFactoryImpl getEntityManagerFactory() {
		return entityManagerFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaQuery<Object> createQuery() {
		return new CriteriaQueryImpl<Object>( this, Object.class );
	}

	/**
	 * {@inheritDoc}
	 */
	public <T> CriteriaQuery<T> createQuery(Class<T> resultClass) {
		return new CriteriaQueryImpl<T>( this, resultClass );
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaQuery<Tuple> createTupleQuery() {
		return new CriteriaQueryImpl<Tuple>( this, Tuple.class );
	}

	/**
	 * Package-protected method to centralize checking of criteria query
	 * multiselects as defined by the
	 * {@link CriteriaQuery#multiselect(List)}  method.
	 *
	 * @param selections The selection varargs to check
	 *
	 * @throws IllegalArgumentException If, as per
	 * {@link CriteriaQuery#multiselect(List)} documentation,
	 * <i>&quot;An argument to the multiselect method must not be a tuple-
     * or array-valued compound selection item.&quot;</i>
	 */
	void checkMultiselect(List<Selection<?>> selections) {
		for ( Selection<?> selection : selections ) {
			if ( selection.isCompoundSelection() ) {
				if ( selection.getJavaType().isArray() ) {
					throw new IllegalArgumentException(
							"multiselect selections cannot contain " +
									"compound array-valued elements"
					);
				}
				if ( Tuple.class.isAssignableFrom( selection.getJavaType() ) ) {
					throw new IllegalArgumentException(
							"multiselect selections cannot contain " +
									"compound tuple-valued elements"
					);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public CompoundSelection<Tuple> tuple(Selection<?>... selections) {
		return tuple( Arrays.asList( selections ) );
	}

	/**
	 * Version of {@link #tuple(Selection[])} taking a list.
	 *
	 * @param selections List of selections.
	 *
	 * @return The tuple compound selection
	 */
	public CompoundSelection<Tuple> tuple(List<Selection<?>> selections) {
		checkMultiselect( selections );
		return new CompoundSelectionImpl<Tuple>( this, Tuple.class, selections );
	}

	/**
	 * {@inheritDoc}
	 */
	public CompoundSelection<Object[]> array(Selection<?>... selections) {
		return array( Arrays.asList( selections ) );
	}

	/**
	 * Version of {@link #array(Selection[])} taking a list of selections.
	 *
	 * @param selections List of selections.
	 *
	 * @return The array compound selection
	 */
	public CompoundSelection<Object[]> array(List<Selection<?>> selections) {
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
	public <Y> CompoundSelection<Y> array(Class<Y> type, List<Selection<?>> selections) {
		checkMultiselect( selections );
		return new CompoundSelectionImpl<Y>( this, type, selections );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y> CompoundSelection<Y> construct(Class<Y> result, Selection<?>... selections) {
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
	public <Y> CompoundSelection<Y> construct(Class<Y> result, List<Selection<?>> selections) {
		checkMultiselect( selections );
		return new CompoundSelectionImpl<Y>( this, result, selections );
	}


	// ordering ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public Order asc(Expression<?> x) {
		return new OrderImpl( x, true );
	}

	/**
	 * {@inheritDoc}
	 */
	public Order desc(Expression<?> x) {
		return new OrderImpl( x, false );
	}


	// predicates ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Predicate wrap(Expression<Boolean> expression) {
		if ( Predicate.class.isInstance( expression ) ) {
			return ( ( Predicate ) expression );
		}
		else {
			return new BooleanExpressionPredicate( this, expression );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate not(Expression<Boolean> expression) {
		return wrap( expression ).negate();
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate and(Expression<Boolean> x, Expression<Boolean> y) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.AND, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate or(Expression<Boolean> x, Expression<Boolean> y) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.OR, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate and(Predicate... restrictions) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.AND, restrictions );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate or(Predicate... restrictions) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.OR, restrictions );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate conjunction() {
		return new CompoundPredicate( this, Predicate.BooleanOperator.AND );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate disjunction() {
		return new CompoundPredicate( this, Predicate.BooleanOperator.OR );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate isTrue(Expression<Boolean> x) {
		return new ExplicitTruthValueCheck( this, x, TruthValue.TRUE );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate isFalse(Expression<Boolean> x) {
		return new ExplicitTruthValueCheck( this, x, TruthValue.FALSE );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate isNull(Expression<?> x) {
		return new NullnessPredicate( this, x );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate isNotNull(Expression<?> x) {
		return isNull( x ).negate();
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate equal(Expression<?> x, Expression<?> y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.EQUAL, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate notEqual(Expression<?> x, Expression<?> y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.NOT_EQUAL, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate equal(Expression<?> x, Object y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.EQUAL, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate notEqual(Expression<?> x, Object y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.NOT_EQUAL, x, y ).negate();
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y extends Comparable<Y>> Predicate greaterThan(Expression<? extends Y> x, Expression<? extends Y> y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y extends Comparable<Y>> Predicate lessThan(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y extends Comparable<Y>> Predicate greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y extends Comparable<Y>> Predicate lessThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y extends Comparable<Y>> Predicate greaterThan(
			Expression<? extends Y> x,
			Y y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y extends Comparable<Y>> Predicate lessThan(
			Expression<? extends Y> x,
			Y y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y extends Comparable<Y>> Predicate greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Y y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y extends Comparable<Y>> Predicate lessThanOrEqualTo(
			Expression<? extends Y> x,
			Y y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate gt(Expression<? extends Number> x, Expression<? extends Number> y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate lt(Expression<? extends Number> x, Expression<? extends Number> y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate ge(Expression<? extends Number> x, Expression<? extends Number> y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate le(Expression<? extends Number> x, Expression<? extends Number> y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate gt(Expression<? extends Number> x, Number y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate lt(Expression<? extends Number> x, Number y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate ge(Expression<? extends Number> x, Number y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate le(Expression<? extends Number> x, Number y) {
		//noinspection SuspiciousNameCombination
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y extends Comparable<Y>> Predicate between(
			Expression<? extends Y> expression,
			Y lowerBound,
			Y upperBound) {
		return new BetweenPredicate<Y>( this, expression, lowerBound, upperBound );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y extends Comparable<Y>> Predicate between(
			Expression<? extends Y> expression,
			Expression<? extends Y> lowerBound,
			Expression<? extends Y> upperBound) {
		return new BetweenPredicate<Y>( this, expression, lowerBound, upperBound );
	}

	/**
	 * {@inheritDoc}
	 */
	public <T> In<T> in(Expression<? extends T> expression) {
		return new InPredicate<T>( this, expression );
	}

	public <T> In<T> in(Expression<? extends T> expression, Expression<? extends T>... values) {
		return new InPredicate<T>( this, expression, values );
	}

	public <T> In<T> in(Expression<? extends T> expression, T... values) {
		return new InPredicate<T>( this, expression, values );
	}

	public <T> In<T> in(Expression<? extends T> expression, Collection<T> values) {
		return new InPredicate<T>( this, expression, values );
	}


	// parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public <T> ParameterExpression<T> parameter(Class<T> paramClass) {
		return new ParameterExpressionImpl<T>( this, paramClass );
	}

	/**
	 * {@inheritDoc}
	 */
	public <T> ParameterExpression<T> parameter(Class<T> paramClass, String name) {
		return new ParameterExpressionImpl<T>( this, paramClass, name );
	}

	/**
	 * {@inheritDoc}
	 */
	public <T> Expression<T> literal(T value) {
		return new LiteralExpression<T>( this, value );
	}


	// aggregate functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public <N extends Number> AggregationFunction.AVG avg(Expression<N> x) {
		return new AggregationFunction.AVG( this, x );
	}

	/**
	 * {@inheritDoc}
	 */
	public <N extends Number> AggregationFunction.SUM<N> sum(Expression<N> x) {
		return new AggregationFunction.SUM<N>( this, x );
	}

	/**
	 * {@inheritDoc}
	 */
	public <N extends Number> AggregationFunction.MAX<N> max(Expression<N> x) {
		return new AggregationFunction.MAX<N>( this, x );
	}

	/**
	 * {@inheritDoc}
	 */
	public <N extends Number> AggregationFunction.MIN<N> min(Expression<N> x) {
		return new AggregationFunction.MIN<N>( this, x );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X extends Comparable<X>> Expression<X> greatest(Expression<X> x) {
		return new AggregationFunction.GREATEST<X>( this, x );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X extends Comparable<X>> Expression<X> least(Expression<X> x) {
		return new AggregationFunction.LEAST<X>( this, x );
	}

	/**
	 * {@inheritDoc}
	 */
	public Expression<Long> count(Expression<?> x) {
		return new AggregationFunction.COUNT( this, x, false );
	}

	/**
	 * {@inheritDoc}
	 */
	public Expression<Long> countDistinct(Expression<?> x) {
		return new AggregationFunction.COUNT( this, x, true );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public Predicate exists(Subquery<?> subquery) {
		return null;
	}

	public <Y> Expression<Y> all(Subquery<Y> ySubquery) {
		return null;
	}

	public <Y> Expression<Y> some(Subquery<Y> ySubquery) {
		return null;
	}

	public <Y> Expression<Y> any(Subquery<Y> ySubquery) {
		return null;
	}

	public <N extends Number> Expression<N> neg(Expression<N> nExpression) {
		return null;
	}

	public <N extends Number> Expression<N> abs(Expression<N> nExpression) {
		return null;
	}

	public <N extends Number> Expression<N> sum(Expression<? extends N> expression, Expression<? extends N> expression1) {
		return null;
	}

	public <N extends Number> Expression<N> prod(Expression<? extends N> expression, Expression<? extends N> expression1) {
		return null;
	}

	public <N extends Number> Expression<N> diff(Expression<? extends N> expression, Expression<? extends N> expression1) {
		return null;
	}

	public <N extends Number> Expression<N> sum(Expression<? extends N> expression, N n) {
		return null;
	}

	public <N extends Number> Expression<N> prod(Expression<? extends N> expression, N n) {
		return null;
	}

	public <N extends Number> Expression<N> diff(Expression<? extends N> expression, N n) {
		return null;
	}

	public <N extends Number> Expression<N> sum(N n, Expression<? extends N> expression) {
		return null;
	}

	public <N extends Number> Expression<N> prod(N n, Expression<? extends N> expression) {
		return null;
	}

	public <N extends Number> Expression<N> diff(N n, Expression<? extends N> expression) {
		return null;
	}

	public Expression<Number> quot(Expression<? extends Number> expression, Expression<? extends Number> expression1) {
		return null;
	}

	public Expression<Number> quot(Expression<? extends Number> expression, Number number) {
		return null;
	}

	public Expression<Number> quot(Number number, Expression<? extends Number> expression) {
		return null;
	}

	public Expression<Integer> mod(Expression<Integer> integerExpression, Expression<Integer> integerExpression1) {
		return null;
	}

	public Expression<Integer> mod(Expression<Integer> integerExpression, Integer integer) {
		return null;
	}

	public Expression<Integer> mod(Integer integer, Expression<Integer> integerExpression) {
		return null;
	}

	public Expression<Double> sqrt(Expression<? extends Number> expression) {
		return null;
	}

	public Expression<Long> toLong(Expression<? extends Number> expression) {
		return null;
	}

	public Expression<Integer> toInteger(Expression<? extends Number> expression) {
		return null;
	}

	public Expression<Float> toFloat(Expression<? extends Number> expression) {
		return null;
	}

	public Expression<Double> toDouble(Expression<? extends Number> expression) {
		return null;
	}

	public Expression<BigDecimal> toBigDecimal(Expression<? extends Number> expression) {
		return null;
	}

	public Expression<BigInteger> toBigInteger(Expression<? extends Number> expression) {
		return null;
	}

	public Expression<String> toString(Expression<Character> characterExpression) {
		return null;
	}

	public <C extends Collection<?>> Predicate isEmpty(Expression<C> cExpression) {
		return null;
	}

	public <C extends Collection<?>> Predicate isNotEmpty(Expression<C> cExpression) {
		return null;
	}

	public <C extends Collection<?>> Expression<Integer> size(C c) {
		return null;
	}

	public <C extends Collection<?>> Expression<Integer> size(Expression<C> cExpression) {
		return null;
	}

	public <E, C extends Collection<E>> Predicate isMember(E e, Expression<C> cExpression) {
		return null;
	}

	public <E, C extends Collection<E>> Predicate isNotMember(E e, Expression<C> cExpression) {
		return null;
	}

	public <E, C extends Collection<E>> Predicate isMember(Expression<E> eExpression, Expression<C> cExpression) {
		return null;
	}

	public <E, C extends Collection<E>> Predicate isNotMember(Expression<E> eExpression, Expression<C> cExpression) {
		return null;
	}

	public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map) {
		return null;
	}

	public <K, M extends Map<K, ?>> Expression<Set<K>> keys(M m) {
		return null;
	}

	public Predicate like(Expression<String> stringExpression, Expression<String> stringExpression1) {
		return null;
	}

	public Predicate like(Expression<String> stringExpression, Expression<String> stringExpression1, Expression<Character> characterExpression) {
		return null;
	}

	public Predicate like(Expression<String> stringExpression, Expression<String> stringExpression1, char c) {
		return null;
	}

	public Predicate like(Expression<String> stringExpression, String s) {
		return null;
	}

	public Predicate like(Expression<String> stringExpression, String s, Expression<Character> characterExpression) {
		return null;
	}

	public Predicate like(Expression<String> stringExpression, String s, char c) {
		return null;
	}

	public Predicate notLike(Expression<String> stringExpression, Expression<String> stringExpression1) {
		return null;
	}

	public Predicate notLike(Expression<String> stringExpression, Expression<String> stringExpression1, Expression<Character> characterExpression) {
		return null;
	}

	public Predicate notLike(Expression<String> stringExpression, Expression<String> stringExpression1, char c) {
		return null;
	}

	public Predicate notLike(Expression<String> stringExpression, String s) {
		return null;
	}

	public Predicate notLike(Expression<String> stringExpression, String s, Expression<Character> characterExpression) {
		return null;
	}

	public Predicate notLike(Expression<String> stringExpression, String s, char c) {
		return null;
	}

	public Expression<String> concat(Expression<String> stringExpression, Expression<String> stringExpression1) {
		return null;
	}

	public Expression<String> concat(Expression<String> stringExpression, String s) {
		return null;
	}

	public Expression<String> concat(String s, Expression<String> stringExpression) {
		return null;
	}

	public Expression<String> substring(Expression<String> stringExpression, Expression<Integer> integerExpression) {
		return null;
	}

	public Expression<String> substring(Expression<String> stringExpression, int i) {
		return null;
	}

	public Expression<String> substring(Expression<String> stringExpression, Expression<Integer> integerExpression, Expression<Integer> integerExpression1) {
		return null;
	}

	public Expression<String> substring(Expression<String> stringExpression, int i, int i1) {
		return null;
	}

	public Expression<String> trim(Expression<String> stringExpression) {
		return null;
	}

	public Expression<String> trim(Trimspec trimspec, Expression<String> stringExpression) {
		return null;
	}

	public Expression<String> trim(Expression<Character> characterExpression, Expression<String> stringExpression) {
		return null;
	}

	public Expression<String> trim(Trimspec trimspec, Expression<Character> characterExpression, Expression<String> stringExpression) {
		return null;
	}

	public Expression<String> trim(char c, Expression<String> stringExpression) {
		return null;
	}

	public Expression<String> trim(Trimspec trimspec, char c, Expression<String> stringExpression) {
		return null;
	}

	public Expression<String> lower(Expression<String> stringExpression) {
		return null;
	}

	public Expression<String> upper(Expression<String> stringExpression) {
		return null;
	}

	public Expression<Integer> length(Expression<String> stringExpression) {
		return null;
	}

	public Expression<Integer> locate(Expression<String> stringExpression, Expression<String> stringExpression1) {
		return null;
	}

	public Expression<Integer> locate(Expression<String> stringExpression, Expression<String> stringExpression1, Expression<Integer> integerExpression) {
		return null;
	}

	public Expression<Integer> locate(Expression<String> stringExpression, String s) {
		return null;
	}

	public Expression<Integer> locate(Expression<String> stringExpression, String s, int i) {
		return null;
	}

	public Expression<java.sql.Date> currentDate() {
		return null;
	}

	public Expression<java.sql.Timestamp> currentTimestamp() {
		return null;
	}

	public Expression<java.sql.Time> currentTime() {
		return null;
	}

	public <Y> Expression<Y> coalesce(Expression<? extends Y> expression, Expression<? extends Y> expression1) {
		return null;
	}

	public <Y> Expression<Y> coalesce(Expression<? extends Y> expression, Y y) {
		return null;
	}

	public <Y> Expression<Y> nullif(Expression<Y> yExpression, Expression<?> expression) {
		return null;
	}

	public <Y> Expression<Y> nullif(Expression<Y> yExpression, Y y) {
		return null;
	}

	public <T> Coalesce<T> coalesce() {
		return null;
	}

	public <C, R> SimpleCase<C, R> selectCase(Expression<? extends C> expression) {
		return null;
	}

	public <R> Case<R> selectCase() {
		return null;
	}

	public <T> Expression<T> function(String s, Class<T> tClass, Expression<?>... expressions) {
		return null;
	}
}
