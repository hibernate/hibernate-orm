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

/**
 * ImplicitDiscriminatorStrategy implementation using entity {@linkplain EntityMappingType#getEntityName() full-names}.
 *
 * @author Steve Ebersole
 */
public class ShortNameImplicitDiscriminatorStrategy implements ImplicitDiscriminatorStrategy {
	public static final ShortNameImplicitDiscriminatorStrategy SHORT_NAME_STRATEGY = new ShortNameImplicitDiscriminatorStrategy();

	@Override
	public Object toDiscriminatorValue(EntityMappingType entityMapping, NavigableRole discriminatorRole, MappingMetamodelImplementor mappingModel) {
		return entityMapping.getImportedName();
	}

	@Override
	public EntityMappingType toEntityMapping(Object discriminatorValue, NavigableRole discriminatorRole, MappingMetamodelImplementor mappingModel) {
		if ( discriminatorValue instanceof String assumedEntityName ) {
			final String importedName = mappingModel.getImportedName( assumedEntityName );
			final var entityMapping = mappingModel.findEntityDescriptor( importedName );
			if ( entityMapping != null ) {
				return entityMapping;
			}
		}

		throw new HibernateException( "Cannot interpret discriminator value (" + discriminatorRole + ") : " + discriminatorValue );
	}
}
