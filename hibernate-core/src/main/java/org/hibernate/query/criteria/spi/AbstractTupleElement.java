/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Base support for {@link org.hibernate.query.criteria.JpaTupleElement} impls
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTupleElement<T>
		extends AbstractNode
		implements TupleElementImplementor<T> {

	private String alias;
	private JavaTypeDescriptor jtd;

	@SuppressWarnings("WeakerAccess")
	protected AbstractTupleElement(Class<T> javaType, CriteriaNodeBuilder criteriaBuilder) {
		super( criteriaBuilder );

		if ( javaType != null ) {
			this.jtd = criteriaBuilder.getSessionFactory()
					.getTypeConfiguration()
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( javaType );
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected AbstractTupleElement(JavaTypeDescriptor<T> javaTypeDescriptor, CriteriaNodeBuilder criteriaBuilder) {
		super( criteriaBuilder );

		this.jtd = javaTypeDescriptor;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	/**
	 * Protected access to set the alias.
	 */
	protected void setAlias(String alias) {
		this.alias = alias;
	}

	@Override
	@SuppressWarnings("unchecked")
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return jtd;
	}

	/**
	 * Protected access to set the JavaTypeDescriptor
	 */
	protected void setJavaTypeDescriptor(JavaTypeDescriptor jtd) {
		this.jtd = jtd;
	}

	/**
	 * Protected access to set the JavaTypeDescriptor via Java Class
	 */
	@SuppressWarnings({ "unchecked" })
	protected void setJavaType(Class targetType) {
		setJavaTypeDescriptor(
				nodeBuilder().getSessionFactory()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( targetType )
		);
	}

}
