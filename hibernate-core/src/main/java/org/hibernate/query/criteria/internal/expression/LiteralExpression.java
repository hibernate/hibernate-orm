/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;

import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.spi.JpaCriteriaBuilderImplementor;
import org.hibernate.query.criteria.spi.JpaExpressionImplementor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Represents a literal expression.
 *
 * @author Steve Ebersole
 */
public class LiteralExpression<T> extends AbstractExpression<T> implements JpaExpressionImplementor<T>, Serializable {
	private Object literal;

	@SuppressWarnings({ "unchecked" })
	public LiteralExpression(JpaCriteriaBuilderImplementor criteriaBuilder, T literal) {
		this(
				criteriaBuilder,
				determineJavaTypeDescriptor( literal, criteriaBuilder ),
				literal
		);
	}

	@SuppressWarnings("unchecked")
	private static <T> JavaTypeDescriptor<T> determineJavaTypeDescriptor(
			T literal,
			JpaCriteriaBuilderImplementor criteriaBuilder) {
		return criteriaBuilder.getSessionFactory().getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				// Java generics fun - why in the fudge do we need this cast?
				.getDescriptor( (Class<T>) literal.getClass() );
	}

	public LiteralExpression(
			JpaCriteriaBuilderImplementor criteriaBuilder,
			JavaTypeDescriptor<T> type,
			T literal) {
		super( criteriaBuilder, type );
		this.literal = literal;
	}

	@SuppressWarnings({ "unchecked" })
	public T getLiteral() {
		return (T) literal;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}
}
