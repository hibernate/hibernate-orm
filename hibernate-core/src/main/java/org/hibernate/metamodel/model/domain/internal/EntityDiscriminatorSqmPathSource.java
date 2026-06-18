/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import jakarta.annotation.Nullable;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.spi.domain.SqmEntityDomainType;

/**
 * SqmPathSource implementation for entity discriminator
 *
 * @author Steve Ebersole
 */
public class EntityDiscriminatorSqmPathSource<D> extends AbstractDiscriminatorSqmPathSource<D> {
	private final SqmEntityDomainType<?> entityDomainType;
	private final EntityMappingType entityMapping;

	public EntityDiscriminatorSqmPathSource(
			SqmDomainType<D> discriminatorValueType,
			SqmEntityDomainType<?> entityDomainType,
			EntityMappingType entityMapping) {
		super( discriminatorValueType );
		this.entityDomainType = entityDomainType;
		this.entityMapping = entityMapping;
	}

	public EntityDomainType<?> getEntityDomainType() {
		return entityDomainType;
	}

	public EntityMappingType getEntityMapping() {
		return entityMapping;
	}

	@Override
	public SqmPath<D> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		return new EntityDiscriminatorSqmPath<>(
				PathHelper.append( lhs, this, intermediatePathSource ),
				pathModel,
				lhs,
				entityDomainType,
				entityMapping,
				lhs.nodeBuilder()
		);
	}
}
