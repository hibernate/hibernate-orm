/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Models an ANSI SQL <tt>COALESCE</tt> expression.  <tt>COALESCE</tt> is a specialized <tt>CASE</tt> statement.
 *
 * @author Steve Ebersole
 */
public class CoalesceExpression<T> extends AbstractExpression<T> implements JpaCoalesce<T> {
	private final List<ExpressionImplementor<? extends T>> expressions;

	public CoalesceExpression(CriteriaNodeBuilder builder) {
		super( ( JavaTypeDescriptor<T>) null, builder );
		this.expressions = new ArrayList<>();
	}

	public CoalesceExpression(List<ExpressionImplementor<T>> expressions, CriteriaNodeBuilder builder) {
		super( expressions.get( 0 ).getJavaTypeDescriptor(), builder );
		this.expressions = new ArrayList<>( expressions );
	}

	public List<ExpressionImplementor<? extends T>> getExpressions() {
		return expressions;
	}

	@Override
	public CoalesceExpression<T> value(T value) {
		return value( nodeBuilder().literal( value ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public CoalesceExpression<T> value(Expression<? extends T> value) {
		return value( (ExpressionImplementor) value );
	}

	public CoalesceExpression<T> value(ExpressionImplementor<? extends T> value) {
		expressions.add( value );

		if ( getJavaTypeDescriptor() == null ) {
			setJavaTypeDescriptor( value.getJavaTypeDescriptor() );
		}

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public CoalesceExpression<T> value(JpaExpression<? extends T> value) {
		return value( (ExpressionImplementor) value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public CoalesceExpression<T> values(T... values) {
		return values( (List) nodeBuilder().literals( values ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public CoalesceExpression<T> values(JpaExpression<? extends T>... values) {
		return values( (List) Arrays.asList( values ) );
	}

	public CoalesceExpression<T> values(List<ExpressionImplementor<? extends T>> values) {
		expressions.addAll( values );

		if ( getJavaTypeDescriptor() == null ) {
			setJavaTypeDescriptor( values.get( 0 ).getJavaTypeDescriptor() );
		}

		return this;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitCoalesceExpression( this );
	}
}
