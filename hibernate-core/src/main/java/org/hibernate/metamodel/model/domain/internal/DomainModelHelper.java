/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

import static org.hibernate.metamodel.mapping.internal.MappingModelHelper.isCompatibleModelPart;

/**
 * Helper containing utilities useful for domain model handling

 * @author Steve Ebersole
 */
public class DomainModelHelper {

	static boolean isCompatible(
			PersistentAttribute<?, ?> attribute1,
			PersistentAttribute<?, ?> attribute2,
			MappingMetamodel mappingMetamodel) {
		if ( attribute1 == attribute2 ) {
			return true;
		}
		else {
			final ModelPart modelPart1 =
					getEntityAttributeModelPart( attribute1, attribute1.getDeclaringType(), mappingMetamodel );
			final ModelPart modelPart2 =
					getEntityAttributeModelPart( attribute2, attribute2.getDeclaringType(), mappingMetamodel );
			return modelPart1 != null
				&& modelPart2 != null
				&& isCompatibleModelPart( modelPart1, modelPart2 );
		}
	}

	static ModelPart getEntityAttributeModelPart(
			PersistentAttribute<?, ?> attribute,
			ManagedDomainType<?> domainType,
			MappingMetamodel mappingMetamodel) {
		if ( domainType instanceof EntityDomainType<?> ) {
			final EntityMappingType entity = mappingMetamodel.getEntityDescriptor( domainType.getTypeName() );
			return entity.findSubPart( attribute.getName() );
		}
		else {
			ModelPart candidate = null;
			for ( ManagedDomainType<?> subType : domainType.getSubTypes() ) {
				final ModelPart modelPart = getEntityAttributeModelPart( attribute, subType, mappingMetamodel );
				if ( modelPart != null ) {
					if ( candidate != null && !isCompatibleModelPart( candidate, modelPart ) ) {
						return null;
					}
					candidate = modelPart;
				}
			}
			return candidate;
		}
	}
}
