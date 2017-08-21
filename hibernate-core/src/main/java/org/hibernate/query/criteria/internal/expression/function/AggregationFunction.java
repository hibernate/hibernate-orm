/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression.function;

import java.io.Serializable;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Models SQL aggregation functions (<tt>MIN</tt>, <tt>MAX</tt>, <tt>COUNT</tt>, etc).
 *
 * @author Steve Ebersole
 */
public class AggregationFunction<T>
		extends ParameterizedFunctionExpression<T>
		implements Serializable {

	/**
	 * Constructs an aggregation function with a single literal argument.
	 *
	 * @param criteriaBuilder The query builder instance.
	 * @param returnType The function return type.
	 * @param functionName The name of the function.
	 * @param argument The literal argument
	 */
	@SuppressWarnings({ "unchecked" })
	public AggregationFunction(
			CriteriaBuilderImpl criteriaBuilder,
			JavaTypeDescriptor<T> returnType,
			String functionName,
			Object argument) {
		this( criteriaBuilder, returnType, functionName, new LiteralExpression( criteriaBuilder, argument ) );
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
			CriteriaBuilderImpl criteriaBuilder,
			JavaTypeDescriptor<T> returnType,
			String functionName,
			Expression<?> argument) {
		super( criteriaBuilder, returnType, functionName, argument );
	}

	public AggregationFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			String name,
			Expression<?> expression) {
		this(
				criteriaBuilder,
				criteriaBuilder.getSessionFactory()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( javaType ),
				name,
				expression
		);
	}

	@Override
	public boolean isAggregation() {
		return true;
	}

	@Override
	protected boolean isStandardJpaFunction() {
		return true;
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

		public COUNT(CriteriaBuilderImpl criteriaBuilder, Expression<?> expression, boolean distinct) {
			super( criteriaBuilder, Long.class, NAME , expression );
			this.distinct = distinct;
		}

		public boolean isDistinct() {
			return distinct;
		}

	}

	/**
     * Implementation of a <tt>AVG</tt> function providing convenience in construction.
     * <p/>
     * Parameterized as {@link Double} because thats what JPA states that the return from <tt>AVG</tt> should be.
	 */
	public static class AVG extends AggregationFunction<Double> {
		public static final String NAME = "avg";

		public AVG(CriteriaBuilderImpl criteriaBuilder, Expression<? extends Number> expression) {
			super( criteriaBuilder, Double.class, NAME, expression );
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
		public SUM(CriteriaBuilderImpl criteriaBuilder, Expression<N> expression) {
			super( criteriaBuilder, (Class<N>)expression.getJavaType(), NAME , expression);
		}

		public SUM(CriteriaBuilderImpl criteriaBuilder, Expression<? extends Number> expression, Class<N> returnType) {
			super( criteriaBuilder, returnType, NAME , expression);
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
		public MIN(CriteriaBuilderImpl criteriaBuilder, Expression<N> expression) {
			super( criteriaBuilder, ( Class<N> ) expression.getJavaType(), NAME , expression);
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
		public MAX(CriteriaBuilderImpl criteriaBuilder, Expression<N> expression) {
			super( criteriaBuilder, ( Class<N> ) expression.getJavaType(), NAME , expression);
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
		public LEAST(CriteriaBuilderImpl criteriaBuilder, Expression<X> expression) {
			super( criteriaBuilder, ( Class<X> ) expression.getJavaType(), NAME , expression);
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
		public GREATEST(CriteriaBuilderImpl criteriaBuilder, Expression<X> expression) {
			super( criteriaBuilder, ( Class<X> ) expression.getJavaType(), NAME , expression);
		}
	}
}
