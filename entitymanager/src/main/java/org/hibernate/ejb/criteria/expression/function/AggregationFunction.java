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
package org.hibernate.ejb.criteria.expression.function;

import javax.persistence.criteria.Expression;
import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.ejb.criteria.expression.LiteralExpression;

/**
 * Models SQL aggregation functions (<tt>MIN</tt>, <tt>MAX</tt>, <tt>COUNT</tt>, etc).
 *
 * @author Steve Ebersole
 */
public class AggregationFunction<T> extends ParameterizedFunctionExpression<T> {
	/**
	 * Constructs an aggregation function with a single literal argument.
	 *
	 * @param queryBuilder The query builder instance.
	 * @param returnType The function return type.
	 * @param functionName The name of the function.
	 * @param argument The literal argument
	 */
	public AggregationFunction(
			QueryBuilderImpl queryBuilder,
			Class<T> returnType,
			String functionName,
			Object argument) {
		this( queryBuilder, returnType, functionName, new LiteralExpression( queryBuilder, argument ) );
	}

	/**
	 * Constructs an aggregation function with a single literal argument.
	 *
	 * @param queryBuilder The query builder instance.
	 * @param returnType The function return type.
	 * @param functionName The name of the function.
	 * @param argument The argument
	 */
	public AggregationFunction(
			QueryBuilderImpl queryBuilder,
			Class<T> returnType,
			String functionName,
			Expression<?> argument) {
		super( queryBuilder, returnType, functionName, argument );
	}

	@Override
	public boolean isAggregation() {
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

		public COUNT(QueryBuilderImpl queryBuilder, Expression<?> expression, boolean distinct) {
			super( queryBuilder, Long.class, NAME , expression );
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

		public AVG(QueryBuilderImpl queryBuilder, Expression<? extends Number> expression) {
			super( queryBuilder, Double.class, NAME, expression );
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

		public SUM(QueryBuilderImpl queryBuilder, Expression<N> expression) {
			super( queryBuilder, expression.getJavaType(), NAME , expression);
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

		public MIN(QueryBuilderImpl queryBuilder, Expression<N> expression) {
			super( queryBuilder, expression.getJavaType(), NAME , expression);
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

		public MAX(QueryBuilderImpl queryBuilder, Expression<N> expression) {
			super( queryBuilder, expression.getJavaType(), NAME , expression);
		}
	}

	/**
	 * Models  the <tt>MIN</tt> function in terms of non-numeric expressions.
	 *
	 * @see MIN
	 */
	public static class LEAST<X extends Comparable<X>> extends AggregationFunction<X> {
		public static final String NAME = "min";

		public LEAST(QueryBuilderImpl queryBuilder, Expression<X> expression) {
			super( queryBuilder, expression.getJavaType(), NAME , expression);
		}
	}

	/**
	 * Models  the <tt>MAX</tt> function in terms of non-numeric expressions.
	 *
	 * @see MAX
	 */
	public static class GREATEST<X extends Comparable<X>> extends AggregationFunction<X> {
		public static final String NAME = "max";

		public GREATEST(QueryBuilderImpl queryBuilder, Expression<X> expression) {
			super( queryBuilder, expression.getJavaType(), NAME , expression);
		}
	}
}
