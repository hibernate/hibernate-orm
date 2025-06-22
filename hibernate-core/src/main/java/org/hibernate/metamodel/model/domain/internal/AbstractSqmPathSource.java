/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPathSource<J> implements SqmPathSource<J> {
	private final String localPathName;
	protected final SqmPathSource<J> pathModel;
	private final SqmDomainType<J> domainType;
	private final BindableType jpaBindableType;

	public AbstractSqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			DomainType<J> domainType,
			BindableType jpaBindableType) {
		this.localPathName = localPathName;
		this.pathModel = pathModel == null ? this : pathModel;
		this.domainType = (SqmDomainType<J>) domainType;
		this.jpaBindableType = jpaBindableType;
	}

	@Override
	public Class<J> getBindableJavaType() {
		return domainType.getJavaType();
	}

	@Override
	public String getPathName() {
		return localPathName;
	}

	@Override
	public SqmDomainType<J> getPathType() {
		return domainType;
	}

	@Override
	public BindableType getBindableType() {
		return jpaBindableType;
	}

	@Override
	public JavaType<J> getExpressibleJavaType() {
		return domainType.getExpressibleJavaType();
	}
}
