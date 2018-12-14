/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.criteria.JpaExpression;

/**
 * Models SQL aggregation functions (<tt>MIN</tt>, <tt>MAX</tt>, <tt>COUNT</tt>, etc).
 *
 * @author Steve Ebersole
 */
public abstract class AggregationFunction<T> extends AbstractStandardFunction<T> {
	private final  ExpressionImplementor argument;

	/**
	 * Constructs an aggregation function with a single literal argument.
	 *
	 * @param criteriaBuilder The query builder instance.
	 * @param returnType The function return type.
	 * @param functionName The name of the function.
	 * @param argument The literal argument
	 */
	public AggregationFunction(
			String functionName,
			Class<T> returnType,
			Object argument,
			CriteriaNodeBuilder criteriaBuilder) {
		this( functionName, returnType, criteriaBuilder.literal( argument ), criteriaBuilder );
	}

	/**
	 * Constructs an aggregation function with a single literal argument.
	 *
	 * @param criteriaBuilder The query builder instance.
	 * @param returnType The function return type.
	 * @param functionName The name of the function.
	 * @param argument The argument
	 */
	public AggregationFunction(
			String functionName,
			Class<T> returnType,
			ExpressionImplementor<?> argument,
			CriteriaNodeBuilder criteriaBuilder) {
		super( functionName, returnType, criteriaBuilder );
		this.argument = argument;
	}

	@Override
	public boolean isAggregator() {
		return true;
	}

	public ExpressionImplementor<?> getArgument() {
		return argument;
	}

	/**
	 * Implementation of a <tt>COUNT</tt> function providing convenience in construction.
	 * <p/>
	 * Parameterized as {@link Long} because thats what JPA states
	 * that the return from <tt>COUNT</tt> should be.
	 */
	public static class COUNT extends AggregationFunction<Long> {
		public static final String NAME = "count";

		private final boolean distinct;

		public COUNT(ExpressionImplementor<?> expression, boolean distinct, CriteriaNodeBuilder criteriaBuilder) {
			super( NAME, Long.class, expression, criteriaBuilder );
			this.distinct = distinct;
		}

		public boolean isDistinct() {
			return distinct;
		}

		@Override
		public <R> R accept(CriteriaVisitor visitor) {
			return visitor.visitCountFunction( this );
		}
	}

	/**
     * Implementation of a <tt>AVG</tt> function providing convenience in construction.
     * <p/>
     * Parameterized as {@link Double} because thats what JPA states that the return from <tt>AVG</tt> should be.
	 */
	public static class AVG extends AggregationFunction<Double> {
		public static final String NAME = "avg";

		public AVG(JpaExpression<? extends Number> expression, CriteriaNodeBuilder criteriaBuilder) {
			super( NAME, Double.class, expression, criteriaBuilder );
		}

		@Override
		public <R> R accept(CriteriaVisitor visitor) {
			return visitor.visitAvgFunction( this );
		}
	}

	/**
	 * Implementation of a <tt>SUM</tt> function providing convenience in construction.
	 * <p/>
	 * Parameterized as {@link Number N extends Number} because thats what JPA states
	 * that the return from <tt>SUM</tt> should be.
	 */
	public static class SUM<N extends Number> extends AggregationFunction<N> {
		public static final String NAME = "sum";

		@SuppressWarnings({ "unchecked" })
		public SUM(JpaExpression<N> expression, CriteriaNodeBuilder criteriaBuilder) {
			super( NAME, (Class<N>)expression.getJavaType(), expression, criteriaBuilder );
		}

		public SUM(JpaExpression<? extends Number> expression, Class<N> returnType, CriteriaNodeBuilder criteriaBuilder) {
			super( NAME, returnType, expression, criteriaBuilder );
		}

		@Override
		public <R> R accept(CriteriaVisitor visitor) {
			return visitor.visitSumFunction( this );
		}
	}

	/**
	 * Implementation of a <tt>MAX</tt> function providing convenience in construction.
	 * <p/>
	 * Parameterized as {@link Number N extends Number} because thats what JPA states
	 * that the return from <tt>MAX</tt> should be.
	 */
	public static class MAX<N extends Number> extends AggregationFunction<N> {
		public static final String NAME = "max";

		@SuppressWarnings({ "unchecked" })
		public MAX(JpaExpression<N> expression, CriteriaNodeBuilder criteriaBuilder) {
			super( NAME, (Class<N>) expression.getJavaType(), expression, criteriaBuilder );
		}

		@Override
		public <R> R accept(CriteriaVisitor visitor) {
			return visitor.visitMaxFunction( this );
		}
	}

	/**
	 * Implementation of a <tt>MIN</tt> function providing convenience in construction.
	 * <p/>
	 * Parameterized as {@link Number N extends Number} because thats what JPA states
	 * that the return from <tt>MIN</tt> should be.
	 */
	public static class MIN<N extends Number> extends AggregationFunction<N> {
		public static final String NAME = "min";

		@SuppressWarnings({ "unchecked" })
		public MIN(JpaExpression<N> expression, CriteriaNodeBuilder criteriaBuilder) {
			super( NAME, (Class<N>) expression.getJavaType(), expression, criteriaBuilder );
		}

		@Override
		public <R> R accept(CriteriaVisitor visitor) {
			return visitor.visitMinFunction( this );
		}
	}

	/**
	 * Models  the <tt>MIN</tt> function in terms of non-numeric expressions.
	 *
	 * @see MIN
	 */
	public static class LEAST<X extends Comparable<X>> extends AggregationFunction<X> {
		public static final String NAME = "min";

		@SuppressWarnings({ "unchecked" })
		public LEAST(JpaExpression<X> expression, CriteriaNodeBuilder criteriaBuilder) {
			super( NAME, (Class<X>) expression.getJavaType(), expression, criteriaBuilder );
		}

		@Override
		public <R> R accept(CriteriaVisitor visitor) {
			return visitor.visitLeastFunction( this );
		}
	}

	/**
	 * Models  the <tt>MAX</tt> function in terms of non-numeric expressions.
	 *
	 * @see MAX
	 */
	public static class GREATEST<X extends Comparable<X>> extends AggregationFunction<X> {
		public static final String NAME = "max";

		@SuppressWarnings({ "unchecked" })
		public GREATEST(JpaExpression<X> expression, CriteriaNodeBuilder builder) {
			super( NAME, (Class<X>) expression.getJavaType(), expression, builder );
		}

		@Override
		public <R> R accept(CriteriaVisitor visitor) {
			return visitor.visitGreatestFunction( this );
		}
	}
}
