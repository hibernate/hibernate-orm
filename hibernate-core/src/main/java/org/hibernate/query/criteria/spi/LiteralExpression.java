/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.io.Serializable;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Represents a literal expression.
 *
 * @author Steve Ebersole
 */
public class LiteralExpression<T> extends AbstractExpression<T> implements Serializable {
	private Object value;

	@SuppressWarnings({ "unchecked" })
	public LiteralExpression(T value, CriteriaNodeBuilder builder) {
		this( (Class<T>) determineClass( value ), value, builder );
	}

	private static Class determineClass(Object literal) {
		return literal == null ? null : literal.getClass();
	}

	public LiteralExpression(Class<T> type, T value, CriteriaNodeBuilder builder) {
		super( type, builder );
		this.value = value;
	}

	@SuppressWarnings({ "unchecked" })
	public T getValue() {
		return (T) value;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected void setJavaType(Class targetType) {
		final JavaTypeDescriptor jtd = getJavaTypeDescriptor();

		super.setJavaType( targetType );

		value = jtd.unwrap( value, targetType, null );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitLiteral( this );
	}
}
