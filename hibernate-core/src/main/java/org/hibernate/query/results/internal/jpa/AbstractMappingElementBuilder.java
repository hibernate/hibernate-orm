/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.jpa;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMappingElementBuilder<T> implements MappingElementBuilder<T> {
	protected final String alias;
	protected final JavaType<T> javaType;

	protected final SessionFactoryImplementor sessionFactory;

	public AbstractMappingElementBuilder(String alias, Class<T> javaType, SessionFactoryImplementor sessionFactory) {
		this( alias, sessionFactory.getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( javaType ), sessionFactory );
	}

	public AbstractMappingElementBuilder(String alias, JavaType<T> javaType, SessionFactoryImplementor sessionFactory) {
		this.alias = alias;
		this.javaType = javaType;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public Class<T> getJavaType() {
		return javaType.getJavaTypeClass();
	}
}
