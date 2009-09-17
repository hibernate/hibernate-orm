/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
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
package org.hibernate.ejb.criteria.predicate;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.persistence.criteria.Expression;

import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.ejb.criteria.expression.LiteralExpression;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class InPredicate<T> extends AbstractSimplePredicate implements QueryBuilderImpl.In<T> {
	private final Expression<? extends T> expression;
	private final List<Expression<? extends T>> values;

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with an empty list of values.
	 *
	 * @param queryBuilder The query builder from which this originates.
	 * @param expression The expression.
	 */
	public InPredicate(
			QueryBuilderImpl queryBuilder,
			Expression<? extends T> expression) {
		this( queryBuilder, expression, new ArrayList<Expression<? extends T>>() );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given list of expression values.
	 *
	 * @param queryBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			QueryBuilderImpl queryBuilder,
			Expression<? extends T> expression,
			Expression<? extends T>... values) {
		this( queryBuilder, expression, Arrays.asList( values ) );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given list of expression values.
	 *
	 * @param queryBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			QueryBuilderImpl queryBuilder,
			Expression<? extends T> expression,
			List<Expression<? extends T>> values) {
		super( queryBuilder );
		this.expression = expression;
		this.values = values;
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given given literal value list.
	 *
	 * @param queryBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			QueryBuilderImpl queryBuilder,
			Expression<? extends T> expression,
			T... values) {
		this( queryBuilder, expression, Arrays.asList( values ) );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given literal value list.
	 *
	 * @param queryBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			QueryBuilderImpl queryBuilder,
			Expression<? extends T> expression,
			Collection<T> values) {
		super( queryBuilder );
		this.expression = expression;
		// TODO : size this?
		this.values = new ArrayList<Expression<? extends T>>();
		for ( T value : values ) {
			this.values.add( new LiteralExpression<T>( queryBuilder, value ) );
		}
	}

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

	public InPredicate<T> value(T value) {
		return value( new LiteralExpression<T>( queryBuilder(), value ) );
	}

	public InPredicate<T> value(Expression<? extends T> value) {
		values.add( value );
		return this;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getExpressionInternal(), registry );
		for ( Expression value : getValues() ) {
			Helper.possibleParameter(value, registry);
		}
	}
}
