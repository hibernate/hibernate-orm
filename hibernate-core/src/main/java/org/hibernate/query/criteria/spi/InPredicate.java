/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaInPredicate;
import org.hibernate.type.Type;

/**
 * Models a <tt>[NOT] IN</tt> restriction
 *
 * @author Steve Ebersole
 */
public class InPredicate<T> extends AbstractSimplePredicate implements JpaInPredicate<T> {
	private final ExpressionImplementor<? extends T> expression;
	private final List<ExpressionImplementor<? extends T>> values;

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with an
	 * initially empty list of values.
	 */
	public InPredicate(ExpressionImplementor<? extends T> expression, CriteriaNodeBuilder nodeBuilder) {
		this( expression, null, nodeBuilder );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the
	 * given list of expression values.
	 *
	 */
	public InPredicate(
			ExpressionImplementor<? extends T> expression,
			List<ExpressionImplementor<? extends T>> values,
			CriteriaNodeBuilder nodeBuilder) {
		super( nodeBuilder );

		if ( values == null ) {
			values = new ArrayList<>();
		}

		this.expression = expression;
		this.values = values;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ExpressionImplementor<T> getExpression() {
		return (ExpressionImplementor) expression;
	}

	public List<ExpressionImplementor<? extends T>> getValues() {
		return values;
	}

	@Override
	public InPredicate<T> value(T value) {
		return value( nodeBuilder().literal( value ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public InPredicate<T> value(JpaExpression<? extends T> value) {
		return value( (ExpressionImplementor) value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public InPredicate<T> value(Expression<? extends T> value) {
		return value( (ExpressionImplementor) value );
	}

	public InPredicate<T> value(ExpressionImplementor<? extends T> value) {
		values.add( value );
		return this;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitInPredicate( this );
	}
}
