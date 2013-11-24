/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, 2012 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.criteria;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Tuple;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.criteria.expression.BinaryArithmeticOperation;
import org.hibernate.jpa.criteria.expression.CoalesceExpression;
import org.hibernate.jpa.criteria.expression.CompoundSelectionImpl;
import org.hibernate.jpa.criteria.expression.ConcatExpression;
import org.hibernate.jpa.criteria.expression.LiteralExpression;
import org.hibernate.jpa.criteria.expression.NullLiteralExpression;
import org.hibernate.jpa.criteria.expression.NullifExpression;
import org.hibernate.jpa.criteria.expression.ParameterExpressionImpl;
import org.hibernate.jpa.criteria.expression.SearchedCaseExpression;
import org.hibernate.jpa.criteria.expression.SimpleCaseExpression;
import org.hibernate.jpa.criteria.expression.SizeOfCollectionExpression;
import org.hibernate.jpa.criteria.expression.SubqueryComparisonModifierExpression;
import org.hibernate.jpa.criteria.expression.UnaryArithmeticOperation;
import org.hibernate.jpa.criteria.expression.function.AbsFunction;
import org.hibernate.jpa.criteria.expression.function.AggregationFunction;
import org.hibernate.jpa.criteria.expression.function.BasicFunctionExpression;
import org.hibernate.jpa.criteria.expression.function.CurrentDateFunction;
import org.hibernate.jpa.criteria.expression.function.CurrentTimeFunction;
import org.hibernate.jpa.criteria.expression.function.CurrentTimestampFunction;
import org.hibernate.jpa.criteria.expression.function.LengthFunction;
import org.hibernate.jpa.criteria.expression.function.LocateFunction;
import org.hibernate.jpa.criteria.expression.function.LowerFunction;
import org.hibernate.jpa.criteria.expression.function.ParameterizedFunctionExpression;
import org.hibernate.jpa.criteria.expression.function.SqrtFunction;
import org.hibernate.jpa.criteria.expression.function.SubstringFunction;
import org.hibernate.jpa.criteria.expression.function.TrimFunction;
import org.hibernate.jpa.criteria.expression.function.UpperFunction;
import org.hibernate.jpa.criteria.path.PluralAttributePath;
import org.hibernate.jpa.criteria.path.RootImpl;
import org.hibernate.jpa.criteria.predicate.BetweenPredicate;
import org.hibernate.jpa.criteria.predicate.BooleanAssertionPredicate;
import org.hibernate.jpa.criteria.predicate.BooleanExpressionPredicate;
import org.hibernate.jpa.criteria.predicate.BooleanStaticAssertionPredicate;
import org.hibernate.jpa.criteria.predicate.ComparisonPredicate;
import org.hibernate.jpa.criteria.predicate.ComparisonPredicate.ComparisonOperator;
import org.hibernate.jpa.criteria.predicate.CompoundPredicate;
import org.hibernate.jpa.criteria.predicate.ExistsPredicate;
import org.hibernate.jpa.criteria.predicate.InPredicate;
import org.hibernate.jpa.criteria.predicate.IsEmptyPredicate;
import org.hibernate.jpa.criteria.predicate.LikePredicate;
import org.hibernate.jpa.criteria.predicate.MemberOfPredicate;
import org.hibernate.jpa.criteria.predicate.NullnessPredicate;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;

/**
 * Hibernate implementation of the JPA {@link CriteriaBuilder} contract.
 *
 * @author Steve Ebersole
 */
public class CriteriaBuilderImpl implements CriteriaBuilder, Serializable {
	private final EntityManagerFactoryImpl entityManagerFactory;

	public CriteriaBuilderImpl(EntityManagerFactoryImpl entityManagerFactory) {
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


	// Query builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public CriteriaQuery<Object> createQuery() {
		return new CriteriaQueryImpl<Object>( this, Object.class );
	}

	@Override
	public <T> CriteriaQuery<T> createQuery(Class<T> resultClass) {
		return new CriteriaQueryImpl<T>( this, resultClass );
	}

	@Override
	public CriteriaQuery<Tuple> createTupleQuery() {
		return new CriteriaQueryImpl<Tuple>( this, Tuple.class );
	}

	@Override
	public <T> CriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity) {
		return new CriteriaUpdateImpl<T>( this );
	}

	@Override
	public <T> CriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity) {
		return new CriteriaDeleteImpl<T>( this );
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
	 * <i>&quot;An argument to the multiselect method must not be a tuple-
     * or array-valued compound selection item.&quot;</i>
	 */
	void checkMultiselect(List<Selection<?>> selections) {
		final HashSet<String> aliases = new HashSet<String>( CollectionHelper.determineProperSizing( selections.size() ) );

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

	@Override
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

	@Override
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

	@Override
	public Order asc(Expression<?> x) {
		return new OrderImpl( x, true );
	}

	@Override
	public Order desc(Expression<?> x) {
		return new OrderImpl( x, false );
	}


	// predicates ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Predicate wrap(Expression<Boolean> expression) {
		if ( Predicate.class.isInstance( expression ) ) {
			return ( (Predicate) expression );
		}
		else if ( PathImplementor.class.isInstance( expression ) ) {
			return new BooleanAssertionPredicate( this, expression, Boolean.TRUE );
		}
		else {
			return new BooleanExpressionPredicate( this, expression );
		}
	}

	@Override
	public Predicate not(Expression<Boolean> expression) {
		return wrap( expression ).not();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Predicate and(Expression<Boolean> x, Expression<Boolean> y) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.AND, x, y );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Predicate or(Expression<Boolean> x, Expression<Boolean> y) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.OR, x, y );
	}

	@Override
	public Predicate and(Predicate... restrictions) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.AND, restrictions );
	}

	@Override
	public Predicate or(Predicate... restrictions) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.OR, restrictions );
	}

	@Override
	public Predicate conjunction() {
		return new CompoundPredicate( this, Predicate.BooleanOperator.AND );
	}

	@Override
	public Predicate disjunction() {
		return new CompoundPredicate( this, Predicate.BooleanOperator.OR );
	}

	@Override
	public Predicate isTrue(Expression<Boolean> expression) {
		if ( CompoundPredicate.class.isInstance( expression ) ) {
			final CompoundPredicate predicate = (CompoundPredicate) expression;
			if ( predicate.getExpressions().size() == 0 ) {
				return new BooleanStaticAssertionPredicate(
						this,
						predicate.getOperator() == Predicate.BooleanOperator.AND
				);
			}
			return predicate;
		}
		else if ( Predicate.class.isInstance( expression ) ) {
			return (Predicate) expression;
		}
		return new BooleanAssertionPredicate( this, expression, Boolean.TRUE );
	}

	@Override
	public Predicate isFalse(Expression<Boolean> expression) {
		if ( CompoundPredicate.class.isInstance( expression ) ) {
			final CompoundPredicate predicate = (CompoundPredicate) expression;
			if ( predicate.getExpressions().size() == 0 ) {
				return new BooleanStaticAssertionPredicate(
						this,
						predicate.getOperator() == Predicate.BooleanOperator.OR
				);
			}
			predicate.not();
			return predicate;
		}
		else if ( Predicate.class.isInstance( expression ) ) {
			final Predicate predicate = (Predicate) expression;
			predicate.not();
			return predicate;
		}
		return new BooleanAssertionPredicate( this, expression, Boolean.FALSE );
	}

	@Override
	public Predicate isNull(Expression<?> x) {
		return new NullnessPredicate( this, x );
	}

	@Override
	public Predicate isNotNull(Expression<?> x) {
		return isNull( x ).not();
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate equal(Expression<?> x, Expression<?> y) {
		return new ComparisonPredicate( this, ComparisonOperator.EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate notEqual(Expression<?> x, Expression<?> y) {
		return new ComparisonPredicate( this, ComparisonOperator.NOT_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate equal(Expression<?> x, Object y) {
		return new ComparisonPredicate( this, ComparisonOperator.EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate notEqual(Expression<?> x, Object y) {
		return new ComparisonPredicate( this, ComparisonOperator.NOT_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> x, Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> Predicate lessThan(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> Predicate greaterThan(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> Predicate lessThan(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public<Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate gt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate lt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate ge(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate le(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate gt(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate lt(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate ge(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	@SuppressWarnings("SuspiciousNameCombination")
	public Predicate le(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate between(
			Expression<? extends Y> expression,
			Y lowerBound,
			Y upperBound) {
		return new BetweenPredicate<Y>( this, expression, lowerBound, upperBound );
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate between(
			Expression<? extends Y> expression,
			Expression<? extends Y> lowerBound,
			Expression<? extends Y> upperBound) {
		return new BetweenPredicate<Y>( this, expression, lowerBound, upperBound );
	}

	@Override
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

	@Override
	public Predicate like(Expression<String> matchExpression, Expression<String> pattern) {
		return new LikePredicate( this, matchExpression, pattern );
	}

	@Override
	public Predicate like(Expression<String> matchExpression, Expression<String> pattern, Expression<Character> escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public Predicate like(Expression<String> matchExpression, Expression<String> pattern, char escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public Predicate like(Expression<String> matchExpression, String pattern) {
		return new LikePredicate( this, matchExpression, pattern );
	}

	@Override
	public Predicate like(Expression<String> matchExpression, String pattern, Expression<Character> escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public Predicate like(Expression<String> matchExpression, String pattern, char escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, Expression<String> pattern) {
		return like( matchExpression, pattern ).not();
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, Expression<String> pattern, Expression<Character> escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, Expression<String> pattern, char escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, String pattern) {
		return like( matchExpression, pattern ).not();
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, String pattern, Expression<Character> escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, String pattern, char escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}


	// parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <T> ParameterExpression<T> parameter(Class<T> paramClass) {
		return new ParameterExpressionImpl<T>( this, paramClass );
	}

	@Override
	public <T> ParameterExpression<T> parameter(Class<T> paramClass, String name) {
		return new ParameterExpressionImpl<T>( this, paramClass, name );
	}

	@Override
	public <T> Expression<T> literal(T value) {
		if ( value == null ) {
			throw new IllegalArgumentException( "literal value cannot be null" );
		}
		return new LiteralExpression<T>( this, value );
	}

	@Override
	public <T> Expression<T> nullLiteral(Class<T> resultClass) {
		return new NullLiteralExpression<T>( this, resultClass );
	}


	// aggregate functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <N extends Number> Expression<Double> avg(Expression<N> x) {
		return new AggregationFunction.AVG( this, x );
	}

	@Override
	public <N extends Number> Expression<N> sum(Expression<N> x) {
		return new AggregationFunction.SUM<N>( this, x );
	}

	@Override
	public Expression<Long> sumAsLong(Expression<Integer> x) {
		return new AggregationFunction.SUM<Long>( this, x, Long.class );
	}

	@Override
	public Expression<Double> sumAsDouble(Expression<Float> x) {
		return new AggregationFunction.SUM<Double>( this, x, Double.class );
	}

	@Override
	public <N extends Number> Expression<N> max(Expression<N> x) {
		return new AggregationFunction.MAX<N>( this, x );
	}

	@Override
	public <N extends Number> Expression<N> min(Expression<N> x) {
		return new AggregationFunction.MIN<N>( this, x );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X extends Comparable<? super X>> Expression<X> greatest(Expression<X> x) {
		return new AggregationFunction.GREATEST( this, x );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X extends Comparable<? super X>> Expression<X> least(Expression<X> x) {
		return new AggregationFunction.LEAST( this, x );
	}

	@Override
	public Expression<Long> count(Expression<?> x) {
		return new AggregationFunction.COUNT( this, x, false );
	}

	@Override
	public Expression<Long> countDistinct(Expression<?> x) {
		return new AggregationFunction.COUNT( this, x, true );
	}


	// other functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <T> Expression<T> function(String name, Class<T> returnType, Expression<?>... arguments) {
		return new ParameterizedFunctionExpression<T>( this, returnType, name, arguments );
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
		return new BasicFunctionExpression<T>( this, returnType, name );
	}

	@Override
	public <N extends Number> Expression<N> abs(Expression<N> expression) {
		return new AbsFunction<N>( this, expression );
	}

	@Override
	public Expression<Double> sqrt(Expression<? extends Number> expression) {
		return new SqrtFunction( this, expression );
	}

	@Override
	public Expression<java.sql.Date> currentDate() {
		return new CurrentDateFunction( this );
	}

	@Override
	public Expression<java.sql.Timestamp> currentTimestamp() {
		return new CurrentTimestampFunction( this );
	}

	@Override
	public Expression<java.sql.Time> currentTime() {
		return new CurrentTimeFunction( this );
	}

	@Override
	public Expression<String> substring(Expression<String> value, Expression<Integer> start) {
		return new SubstringFunction( this, value, start );
	}

	@Override
	public Expression<String> substring(Expression<String> value, int start) {
		return new SubstringFunction( this, value, start );
	}

	@Override
	public Expression<String> substring(Expression<String> value, Expression<Integer> start, Expression<Integer> length) {
		return new SubstringFunction( this, value, start, length );
	}

	@Override
	public Expression<String> substring(Expression<String> value, int start, int length) {
		return new SubstringFunction( this, value, start, length );
	}

	@Override
	public Expression<String> trim(Expression<String> trimSource ) {
		return new TrimFunction( this, trimSource );
	}

	@Override
	public Expression<String> trim(Trimspec trimspec, Expression<String> trimSource) {
		return new TrimFunction( this, trimspec, trimSource );
	}

	@Override
	public Expression<String> trim(Expression<Character> trimCharacter, Expression<String> trimSource) {
		return new TrimFunction( this, trimCharacter, trimSource );
	}

	@Override
	public Expression<String> trim(Trimspec trimspec, Expression<Character> trimCharacter, Expression<String> trimSource) {
		return new TrimFunction( this, trimspec, trimCharacter, trimSource );
	}

	@Override
	public Expression<String> trim(char trimCharacter, Expression<String> trimSource) {
		return new TrimFunction( this, trimCharacter, trimSource );
	}

	@Override
	public Expression<String> trim(Trimspec trimspec, char trimCharacter, Expression<String> trimSource) {
		return new TrimFunction( this, trimspec, trimCharacter, trimSource );
	}

	@Override
	public Expression<String> lower(Expression<String> value) {
		return new LowerFunction( this, value );
	}

	@Override
	public Expression<String> upper(Expression<String> value) {
		return new UpperFunction( this, value );
	}

	@Override
	public Expression<Integer> length(Expression<String> value) {
		return new LengthFunction( this, value );
	}

	@Override
	public Expression<Integer> locate(Expression<String> string, Expression<String> pattern) {
		return new LocateFunction( this, pattern, string );
	}

	@Override
	public Expression<Integer> locate(Expression<String> string, Expression<String> pattern, Expression<Integer> start) {
		return new LocateFunction( this, pattern, string, start );
	}

	@Override
	public Expression<Integer> locate(Expression<String> string, String pattern) {
		return new LocateFunction( this, pattern, string );
	}

	@Override
	public Expression<Integer> locate(Expression<String> string, String pattern, int start) {
		return new LocateFunction( this, pattern, string, start );
	}


	// arithmetic operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <N extends Number> Expression<N> neg(Expression<N> expression) {
		return new UnaryArithmeticOperation<N>(
				this,
				UnaryArithmeticOperation.Operation.UNARY_MINUS,
				expression
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> sum(Expression<? extends N> expression1, Expression<? extends N> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to sum() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression1.getJavaType(), expression2.getJavaType() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.ADD,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> prod(Expression<? extends N> expression1, Expression<? extends N> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to prod() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression1.getJavaType(), expression2.getJavaType() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.MULTIPLY,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> diff(Expression<? extends N> expression1, Expression<? extends N> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to diff() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression1.getJavaType(), expression2.getJavaType() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.SUBTRACT,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> sum(Expression<? extends N> expression, N n) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to sum() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression.getJavaType(), n.getClass() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.ADD,
				expression,
				n
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> prod(Expression<? extends N> expression, N n) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to prod() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression.getJavaType(), n.getClass() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.MULTIPLY,
				expression,
				n
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> diff(Expression<? extends N> expression, N n) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to diff() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression.getJavaType(), n.getClass() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.SUBTRACT,
				expression,
				n
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> sum(N n, Expression<? extends N> expression) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to sum() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( n.getClass(), expression.getJavaType() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.ADD,
				n,
				expression
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> prod(N n, Expression<? extends N> expression) {
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
	public <N extends Number> Expression<N> diff(N n, Expression<? extends N> expression) {
		if ( n == null || expression == null ) {
			throw new IllegalArgumentException( "arguments to diff() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( n.getClass(), expression.getJavaType() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.SUBTRACT,
				n,
				expression
		);
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public Expression<Number> quot(Expression<? extends Number> expression1, Expression<? extends Number> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to quot() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression1.getJavaType(), expression2.getJavaType(), true );

		return new BinaryArithmeticOperation<Number>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.DIVIDE,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public Expression<Number> quot(Expression<? extends Number> expression, Number number) {
		if ( expression == null || number == null ) {
			throw new IllegalArgumentException( "arguments to quot() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( expression.getJavaType(), number.getClass(), true );

		return new BinaryArithmeticOperation<Number>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.DIVIDE,
				expression,
				number
		);
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public Expression<Number> quot(Number number, Expression<? extends Number> expression) {
		if ( expression == null || number == null ) {
			throw new IllegalArgumentException( "arguments to quot() cannot be null" );
		}

		final Class resultType = BinaryArithmeticOperation.determineResultType( number.getClass(), expression.getJavaType(), true );

		return new BinaryArithmeticOperation<Number>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.DIVIDE,
				number,
				expression
		);
	}

	@Override
	public Expression<Integer> mod(Expression<Integer> expression1, Expression<Integer> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to mod() cannot be null" );
		}

		return new BinaryArithmeticOperation<Integer>(
				this,
				Integer.class,
				BinaryArithmeticOperation.Operation.MOD,
				expression1,
				expression2
		);
	}

	@Override
	public Expression<Integer> mod(Expression<Integer> expression, Integer integer) {
		if ( expression == null || integer == null ) {
			throw new IllegalArgumentException( "arguments to mod() cannot be null" );
		}

		return new BinaryArithmeticOperation<Integer>(
				this,
				Integer.class,
				BinaryArithmeticOperation.Operation.MOD,
				expression,
				integer
		);
	}

	@Override
	public Expression<Integer> mod(Integer integer, Expression<Integer> expression) {
		if ( integer == null || expression == null ) {
			throw new IllegalArgumentException( "arguments to mod() cannot be null" );
		}

		return new BinaryArithmeticOperation<Integer>(
				this,
				Integer.class,
				BinaryArithmeticOperation.Operation.MOD,
				integer,
				expression
		);
	}


	// casting ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public ExpressionImplementor<Long> toLong(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asLong();
	}

	@Override
	public ExpressionImplementor<Integer> toInteger(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asInteger();
	}

	@Override
	public ExpressionImplementor<Float> toFloat(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asFloat();
	}

	@Override
	public ExpressionImplementor<Double> toDouble(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asDouble();
	}

	@Override
	public ExpressionImplementor<BigDecimal> toBigDecimal(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asBigDecimal();
	}

	@Override
	public ExpressionImplementor<BigInteger> toBigInteger(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asBigInteger();
	}

	@Override
	public ExpressionImplementor<String> toString(Expression<Character> characterExpression) {
		return ( (ExpressionImplementor<Character>) characterExpression ).asString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T, V extends T> Join<X, V> treat(Join<X, T> join, Class<V> type) {
		return ( (JoinImplementor) join ).treatAs( type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T, E extends T> CollectionJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type) {
		return ( (CollectionJoinImplementor) join ).treatAs( type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T, E extends T> SetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type) {
		return ( (SetJoinImplementor) join ).treatAs( type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T, E extends T> ListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type) {
		return ( (ListJoinImplementor) join ).treatAs( type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, K, T, V extends T> MapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type) {
		return ( (MapJoinImplementor) join ).treatAs( type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T extends X> Path<T> treat(Path<X> path, Class<T> type) {
		return ( (PathImplementor) path ).treatAs( type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, T extends X> Root<T> treat(Root<X> root, Class<T> type) {
		return ( (RootImpl) root ).treatAs( type );
	}


	// subqueries ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Predicate exists(Subquery<?> subquery) {
		return new ExistsPredicate( this, subquery );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> Expression<Y> all(Subquery<Y> subquery) {
		return new SubqueryComparisonModifierExpression<Y>(
				this,
				(Class<Y>) subquery.getJavaType(),
				subquery,
				SubqueryComparisonModifierExpression.Modifier.ALL
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> Expression<Y> some(Subquery<Y> subquery) {
		return new SubqueryComparisonModifierExpression<Y>(
				this,
				(Class<Y>) subquery.getJavaType(),
				subquery,
				SubqueryComparisonModifierExpression.Modifier.SOME
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> Expression<Y> any(Subquery<Y> subquery) {
		return new SubqueryComparisonModifierExpression<Y>(
				this,
				(Class<Y>) subquery.getJavaType(),
				subquery,
				SubqueryComparisonModifierExpression.Modifier.ANY
		);
	}


	// miscellaneous expressions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SuppressWarnings({ "RedundantCast" })
	public <Y> Expression<Y> coalesce(Expression<? extends Y> exp1, Expression<? extends Y> exp2) {
		return coalesce( (Class<Y>) null, exp1, exp2 );
	}

	public <Y> Expression<Y> coalesce(Class<Y> type, Expression<? extends Y> exp1, Expression<? extends Y> exp2) {
		return new CoalesceExpression<Y>( this, type ).value( exp1 ).value( exp2 );
	}

	@Override
	@SuppressWarnings({ "RedundantCast" })
	public <Y> Expression<Y> coalesce(Expression<? extends Y> exp1, Y exp2) {
		return coalesce( (Class<Y>) null, exp1, exp2 );
	}

	public <Y> Expression<Y> coalesce(Class<Y> type, Expression<? extends Y> exp1, Y exp2) {
		return new CoalesceExpression<Y>( this, type ).value( exp1 ).value( exp2 );
	}

	@Override
	public <T> Coalesce<T> coalesce() {
		return coalesce( (Class<T>)null );
	}

	public <T> Coalesce<T> coalesce(Class<T> type) {
		return new CoalesceExpression<T>( this, type );
	}

	@Override
	public Expression<String> concat(Expression<String> string1, Expression<String> string2) {
		return new ConcatExpression( this, string1, string2 );
	}

	@Override
	public Expression<String> concat(Expression<String> string1, String string2) {
		return new ConcatExpression( this, string1, string2 );
	}

	@Override
	public Expression<String> concat(String string1, Expression<String> string2) {
		return new ConcatExpression( this, string1, string2 );
	}

	@Override
	public <Y> Expression<Y> nullif(Expression<Y> exp1, Expression<?> exp2) {
		return nullif( null, exp1, exp2 );
	}

	public <Y> Expression<Y> nullif(Class<Y> type, Expression<Y> exp1, Expression<?> exp2) {
		return new NullifExpression<Y>( this, type, exp1, exp2 );
	}

	@Override
	public <Y> Expression<Y> nullif(Expression<Y> exp1, Y exp2) {
		return nullif( null, exp1, exp2 );
	}

	public <Y> Expression<Y> nullif(Class<Y> type, Expression<Y> exp1, Y exp2) {
		return new NullifExpression<Y>( this, type, exp1, exp2 );
	}

	@Override
	public <C, R> SimpleCase<C, R> selectCase(Expression<? extends C> expression) {
		return selectCase( (Class<R>)null, expression );
	}

	public <C, R> SimpleCase<C, R> selectCase(Class<R> type, Expression<? extends C> expression) {
		return new SimpleCaseExpression<C, R>( this, type, expression );
	}

	@Override
	public <R> Case<R> selectCase() {
		return selectCase( (Class<R>)null );
	}

	public <R> Case<R> selectCase(Class<R> type) {
		return new SearchedCaseExpression<R>( this, type );
	}

	@Override
	public <C extends Collection<?>> Expression<Integer> size(C c) {
		int size = c == null ? 0 : c.size();
		return new LiteralExpression<Integer>(this, Integer.class, size);
	}

	@Override
	public <C extends Collection<?>> Expression<Integer> size(Expression<C> exp) {
		if ( LiteralExpression.class.isInstance(exp) ) {
			return size( ( (LiteralExpression<C>) exp ).getLiteral() );
		}
		else if ( PluralAttributePath.class.isInstance(exp) ) {
			return new SizeOfCollectionExpression<C>(this, (PluralAttributePath<C>) exp );
		}
		// TODO : what other specific types?  any?
		throw new IllegalArgumentException("unknown collection expression type [" + exp.getClass().getName() + "]" );
	}

	@Override
	public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map) {
		return new LiteralExpression<Collection<V>>( this, map.values() );
	}

	@Override
	public <K, M extends Map<K, ?>> Expression<Set<K>> keys(M map) {
		return new LiteralExpression<Set<K>>( this, map.keySet() );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <C extends Collection<?>> Predicate isEmpty(Expression<C> collectionExpression) {
		if ( PluralAttributePath.class.isInstance(collectionExpression) ) {
			return new IsEmptyPredicate( this, (PluralAttributePath<C>) collectionExpression );
		}
		// TODO : what other specific types?  any?
		throw new IllegalArgumentException(
				"unknown collection expression type [" + collectionExpression.getClass().getName() + "]"
		);
	}

	@Override
	public <C extends Collection<?>> Predicate isNotEmpty(Expression<C> collectionExpression) {
		return isEmpty( collectionExpression ).not();
	}

	@Override
	public <E, C extends Collection<E>> Predicate isMember(E e, Expression<C> collectionExpression) {
		if ( ! PluralAttributePath.class.isInstance( collectionExpression ) ) {
			throw new IllegalArgumentException(
					"unknown collection expression type [" + collectionExpression.getClass().getName() + "]"
			);
		}
		return new MemberOfPredicate<E, C>(
				this,
				e,
				(PluralAttributePath<C>)collectionExpression
		);
	}

	@Override
	public <E, C extends Collection<E>> Predicate isNotMember(E e, Expression<C> cExpression) {
		return isMember(e, cExpression).not();
	}

	@Override
	public <E, C extends Collection<E>> Predicate isMember(Expression<E> elementExpression, Expression<C> collectionExpression) {
		if ( ! PluralAttributePath.class.isInstance( collectionExpression ) ) {
			throw new IllegalArgumentException(
					"unknown collection expression type [" + collectionExpression.getClass().getName() + "]"
			);
		}
		return new MemberOfPredicate<E, C>(
				this,
				elementExpression,
				(PluralAttributePath<C>)collectionExpression
		);
	}

	@Override
	public <E, C extends Collection<E>> Predicate isNotMember(Expression<E> eExpression, Expression<C> cExpression) {
		return isMember(eExpression, cExpression).not();
	}
}
