/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;

/// Constructor input for [MetadataBuildingContextRootImpl].
///
/// @since 9.0
/// @author Steve Ebersole
public record MetadataBuildingContextRootInput(
		String contributor,
		BootstrapContext bootstrapContext,
		MappingResolutionServices serviceComponents,
		MappingResolutionOptions buildingPlan,
		InFlightMetadataCollector metadataCollector,
		EffectiveMappingDefaults mappingDefaults) {

	public static MetadataBuildingContextRootInput create(
			String contributor,
			BootstrapContext bootstrapContext,
			MappingResolutionOptions buildingPlan,
			InFlightMetadataCollector metadataCollector,
			EffectiveMappingDefaults mappingDefaults) {
		return new MetadataBuildingContextRootInput(
				contributor,
				bootstrapContext,
				new MappingResolutionServicesImpl( bootstrapContext ),
				buildingPlan,
				metadataCollector,
				mappingDefaults
		);
	}

	public static MetadataBuildingContextRootInput contributor(
			String contributor,
			MetadataBuildingContext parent) {
		return contributor(
				contributor,
				parent,
				new RootMappingDefaults(
						parent.getBuildingPlan().getMappingDefaults(),
						parent.getMetadataCollector().getPersistenceUnitMetadata()
				)
		);
	}

	public static MetadataBuildingContextRootInput contributor(
			String contributor,
			MetadataBuildingContext parent,
			EffectiveMappingDefaults mappingDefaults) {
		return new MetadataBuildingContextRootInput(
				contributor,
				parent.getBootstrapContext(),
				parent.getServiceComponents(),
				parent.getBuildingPlan(),
				parent.getMetadataCollector(),
				mappingDefaults
		);
	}
}
