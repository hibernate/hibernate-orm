/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDomainType<J> implements DomainType<J> {
	private final SessionFactoryImplementor sessionFactory;

	private final JavaTypeDescriptor<J> javaTypeDescriptor;

	@SuppressWarnings("WeakerAccess")
	public AbstractDomainType(
			JavaTypeDescriptor<J> javaTypeDescriptor,
			SessionFactoryImplementor sessionFactory) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.sessionFactory = sessionFactory;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	@Override
	public JavaTypeDescriptor<J> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}
}
