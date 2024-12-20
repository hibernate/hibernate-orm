/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * SqmPathSource implementation for entity discriminator
 *
 * @author Steve Ebersole
 */
public class EntityDiscriminatorSqmPathSource<D> extends AbstractDiscriminatorSqmPathSource<D> {
	private final EntityDomainType<?> entityDomainType;
	private final EntityMappingType entityMapping;

	public EntityDiscriminatorSqmPathSource(
			DomainType<D> discriminatorValueType,
			EntityDomainType<?> entityDomainType,
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
	public SqmPath<D> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
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
