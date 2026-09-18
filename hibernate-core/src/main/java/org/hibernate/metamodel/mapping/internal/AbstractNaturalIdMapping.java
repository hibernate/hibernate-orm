/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;

import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNaturalIdMapping implements NaturalIdMapping {
	private final EntityMappingType declaringType;
	private final boolean mutable;
	@Nullable
	private final NaturalIdDataAccess cachesAccess;

	private final NavigableRole role;

	public AbstractNaturalIdMapping(
			EntityMappingType declaringType,
			boolean mutable) {
		this.declaringType = declaringType;
		this.mutable = mutable;
		this.cachesAccess = declaringType.getEntityPersister().getNaturalIdCacheAccessStrategy();
		this.role = declaringType.getNavigableRole().append( PART_NAME );
	}

	public EntityMappingType getDeclaringType() {
		return declaringType;
	}

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return role;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Nullable
	@Override
	public NaturalIdDataAccess getCacheAccess() {
		return cachesAccess;
	}

	@Nonnull
	@Override
	public EntityMappingType findContainingEntityMapping() {
		return declaringType;
	}
}
