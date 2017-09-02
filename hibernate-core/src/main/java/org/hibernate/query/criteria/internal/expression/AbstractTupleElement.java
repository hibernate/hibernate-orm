/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;

import org.hibernate.query.criteria.internal.AbstractNode;
import org.hibernate.query.criteria.spi.JpaCriteriaBuilderImplementor;
import org.hibernate.query.criteria.spi.JpaTupleElementImplementor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Abstract implementation of the JPA criteria TupleElement
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTupleElement<X>
		extends AbstractNode
		implements JpaTupleElementImplementor<X>, Serializable {
	private final JavaTypeDescriptor<X> originalJavaType;
	private JavaTypeDescriptor<X> javaTypeDescriptor;
	private String alias;

	protected AbstractTupleElement(
			JpaCriteriaBuilderImplementor criteriaBuilder,
			JavaTypeDescriptor<X> javaTypeDescriptor) {
		super( criteriaBuilder );
		this.originalJavaType = javaTypeDescriptor;
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public JavaTypeDescriptor<X> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	public JavaTypeDescriptor<X> getOriginalJavaTypeDescriptor() {
		return originalJavaType;
	}

	@SuppressWarnings({ "unchecked" })
	protected void resetJavaTypeDescriptor(JavaTypeDescriptor newDescriptor) {
		this.javaTypeDescriptor = newDescriptor;
	}

	@SuppressWarnings({ "unchecked" })
	protected void resetJavaTypeDescriptorIfNotSet(JavaTypeDescriptor newDescriptor) {
		if ( javaTypeDescriptor != null ) {
			return;
		}

		this.javaTypeDescriptor = newDescriptor;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	/**
	 * Protected access to define the alias.
	 *
	 * @param alias The alias to use.
	 */
	protected void setAlias(String alias) {
		this.alias = alias;
	}
}
