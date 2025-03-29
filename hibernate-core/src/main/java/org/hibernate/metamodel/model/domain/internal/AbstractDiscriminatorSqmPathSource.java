/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.SqmPathSource;

import static jakarta.persistence.metamodel.Bindable.BindableType.SINGULAR_ATTRIBUTE;
import static org.hibernate.metamodel.mapping.EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME;

/**
 * Abstract SqmPathSource implementation for discriminators
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDiscriminatorSqmPathSource<D> extends AbstractSqmPathSource<D>
		implements ReturnableType<D> {
	public AbstractDiscriminatorSqmPathSource(DomainType<D> domainType) {
		super( DISCRIMINATOR_ROLE_NAME, null, domainType, SINGULAR_ATTRIBUTE );
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		throw new IllegalStateException( "Entity discriminator cannot be de-referenced" );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class<D> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public DomainType<D> getSqmType() {
		return this;
	}
}
