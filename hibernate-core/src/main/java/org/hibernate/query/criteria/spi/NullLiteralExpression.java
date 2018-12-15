/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Represents a <tt>NULL</tt>literal expression.
 *
 * @author Steve Ebersole
 */
public class NullLiteralExpression<T> extends AbstractExpression<T> {
	public NullLiteralExpression(CriteriaNodeBuilder builder) {
		super( (JavaTypeDescriptor<T>) null, builder );
	}

	public NullLiteralExpression(
			JavaTypeDescriptor<T> javaTypeDescriptor,
			CriteriaNodeBuilder criteriaBuilder) {
		super( javaTypeDescriptor, criteriaBuilder );
	}

	public NullLiteralExpression(Class<T> javaType, CriteriaNodeBuilder criteriaBuilder) {
		super( javaType, criteriaBuilder );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitNullLiteral( this );
	}
}
