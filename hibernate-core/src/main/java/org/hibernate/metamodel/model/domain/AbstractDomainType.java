/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDomainType<J> implements SimpleDomainType<J> {
	private final JavaType<J> javaType;

	public AbstractDomainType(JavaType<J> javaType) {
		this.javaType = javaType;
	}

	@Override
	public JavaType<J> getExpressibleJavaType() {
		return javaType;
	}

	@Override
	public Class<J> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getJavaType();
	}
}
