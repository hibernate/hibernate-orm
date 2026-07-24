/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.List;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.mapping.MappingRole;

import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.EMBEDDED;

/// Materializes the compatibility property used to refer to multiple physical
/// attributes as one association target or table key.
///
/// The synthetic root has its own mapping role, while its properties alias the
/// original applications so that later resolution of a source value remains
/// visible through the projection.
///
/// @since 9.0
/// @author Steve Ebersole
final class SyntheticPropertyMappingProjection {
	private SyntheticPropertyMappingProjection() {
	}

	static SyntheticProperty create(
			PersistentClass owner,
			List<Property> sourceProperties,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		final MappingRole mappingRole = MappingRole.entity( owner.getEntityName() )
				.appendAttribute( propertyName );
		final Component component = new Component( buildingContext, owner );
		component.setComponentClassDetails( owner.getClassName(), false, buildingContext );
		component.setFlattened( true );
		component.setPreservePropertyOrder( true );
		component.setMappingRole( mappingRole );
		for ( Property sourceProperty : sourceProperties ) {
			final Property alias = sourceProperty.copyForSameApplication();
			alias.setInsertable( false );
			alias.setUpdatable( false );
			alias.setNaturalIdentifier( false );
			alias.setPersistentClass( owner );
			component.addProperty( alias );
		}

		final SyntheticProperty syntheticProperty = new SyntheticProperty();
		syntheticProperty.setName( propertyName );
		syntheticProperty.setPersistentClass( owner );
		syntheticProperty.setUpdatable( false );
		syntheticProperty.setInsertable( false );
		syntheticProperty.setValue( component );
		syntheticProperty.setMappingRole( mappingRole );
		syntheticProperty.setPropertyAccessorName( EMBEDDED.getExternalName() );
		owner.addProperty( syntheticProperty );
		return syntheticProperty;
	}
}
