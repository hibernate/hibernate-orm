/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.predicate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;

/**
 * Models an <tt>[NOT] IN</tt> restriction
 *
 * @author Steve Ebersole
 */
public class InPredicate<T>
		extends AbstractSimplePredicate
		implements CriteriaBuilderImpl.In<T>, Serializable {
	private final Expression<? extends T> expression;
	private final List<Expression<? extends T>> values;

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with an empty list of values.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression) {
		this( criteriaBuilder, expression, new ArrayList<Expression<? extends T>>() );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given list of expression values.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			Expression<? extends T>... values) {
		this( criteriaBuilder, expression, Arrays.asList( values ) );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given list of expression values.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			List<Expression<? extends T>> values) {
		super( criteriaBuilder );
		this.expression = expression;
		this.values = values;
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given given literal value list.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			T... values) {
		this( criteriaBuilder, expression, Arrays.asList( values ) );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given literal value list.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			Collection<T> values) {
		super( criteriaBuilder );
		this.expression = expression;
		this.values = new ArrayList<Expression<? extends T>>( values.size() );
		final Class<? extends T> javaType = expression.getJavaType();
		ValueHandlerFactory.ValueHandler<? extends T> valueHandler = javaType != null && ValueHandlerFactory.isNumeric(javaType)
				? ValueHandlerFactory.determineAppropriateHandler((Class<? extends T>) javaType)
				: new ValueHandlerFactory.NoOpValueHandler<T>();
		for ( T value : values ) {
			this.values.add(
					new LiteralExpression<T>( criteriaBuilder, valueHandler.convert( value ) )
			);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Expression<T> getExpression() {
		return ( Expression<T> ) expression;
	}

	public Expression<? extends T> getExpressionInternal() {
		return expression;
	}

	public List<Expression<? extends T>> getValues() {
		return values;
	}

	@Override
	public InPredicate<T> value(T value) {
		return value( new LiteralExpression<T>( criteriaBuilder(), value ) );
	}

	@Override
	public InPredicate<T> value(Expression<? extends T> value) {
		values.add( value );
		return this;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getExpressionInternal(), registry );
		for ( Expression value : getValues() ) {
			Helper.possibleParameter(value, registry);
		}
	}
}
