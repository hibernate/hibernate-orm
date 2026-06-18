/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.domain.SqmDomainType;

import static jakarta.persistence.metamodel.Bindable.BindableType.SINGULAR_ATTRIBUTE;
import static jakarta.persistence.metamodel.Type.PersistenceType.BASIC;
import static org.hibernate.metamodel.mapping.EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME;

/**
 * Abstract SqmPathSource implementation for discriminators
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDiscriminatorSqmPathSource<D> extends AbstractSqmPathSource<D>
		implements ReturnableType<D>, SqmDomainType<D> {
	public AbstractDiscriminatorSqmPathSource(DomainType<D> domainType) {
		super( DISCRIMINATOR_ROLE_NAME, null, domainType, SINGULAR_ATTRIBUTE );
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		throw new IllegalStateException( "Entity discriminator cannot be de-referenced" );
	}

	@Override
	@Nonnull
	public PersistenceType getPersistenceType() {
		return BASIC;
	}

	@Override
	@Nonnull
	public Class<D> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public @Nullable SqmDomainType<D> getSqmType() {
		return this;
	}
}
