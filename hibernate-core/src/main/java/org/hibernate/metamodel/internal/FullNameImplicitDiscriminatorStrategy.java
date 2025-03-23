/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * ImplicitDiscriminatorStrategy implementation using entity {@linkplain EntityMappingType#getEntityName() full-names}.
 *
 * @author Steve Ebersole
 */
public class FullNameImplicitDiscriminatorStrategy implements ImplicitDiscriminatorStrategy {
	public static final FullNameImplicitDiscriminatorStrategy FULL_NAME_STRATEGY = new FullNameImplicitDiscriminatorStrategy();

	@Override
	public Object toDiscriminatorValue(EntityMappingType entityMapping, NavigableRole discriminatorRole, MappingMetamodelImplementor mappingModel) {
		return entityMapping.getEntityName();
	}

	@Override
	public EntityMappingType toEntityMapping(Object discriminatorValue, NavigableRole discriminatorRole, MappingMetamodelImplementor mappingModel) {
		if ( discriminatorValue instanceof String assumedEntityName ) {
			final EntityPersister persister = mappingModel.findEntityDescriptor( assumedEntityName );
			if ( persister != null ) {
				return persister;
			}
		}

		throw new HibernateException( "Cannot interpret discriminator value (" + discriminatorRole + ") : " + discriminatorValue );
	}
}
