/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Type.PersistenceType.BASIC;

/**
 * {@link SqmPathSource} implementation for {@link AnyDiscriminator}
 *
 */
public class AnyDiscriminatorSqmPathSource<D> extends AbstractSqmPathSource<D>
		implements ReturnableType<D>, SqmBindableType<D> {

	private final BasicType<D> domainType;

	public AnyDiscriminatorSqmPathSource(
			String localPathName,
			SqmPathSource<D> pathModel,
			SimpleDomainType<D> domainType,
			BindableType jpaBindableType) {
		super( localPathName, pathModel, domainType, jpaBindableType );
		this.domainType = (BasicType<D>) domainType; // TODO: don't like this cast!
	}

	@Override
	public SqmPath<D> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		final NavigablePath navigablePath =
				intermediatePathSource == null
						? lhs.getNavigablePath()
						: lhs.getNavigablePath().append( intermediatePathSource.getPathName() );
		return new AnyDiscriminatorSqmPath<>( navigablePath, pathModel, lhs, lhs.nodeBuilder() );
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		throw new IllegalStateException( "Entity discriminator cannot be de-referenced" );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return BASIC;
	}

	@Override
	public Class<D> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public BasicType<D> getSqmType() {
		return getPathType();
	}

	@Override
	public BasicType<D> getPathType() {
		return domainType;
	}

	@Override
	public String getTypeName() {
		return super.getTypeName();
	}

	@Override
	public JavaType<D> getExpressibleJavaType() {
		return getPathType().getExpressibleJavaType();
	}
}
