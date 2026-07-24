/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.boot.mapping.internal.context.MappingResolutionServices;
import org.hibernate.boot.mapping.internal.context.MappingResolutionState;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionBuilder;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionDetails;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.MappingRole;

/// Stable recipes sufficient to reconstruct derived mapping type resolutions.
///
/// Applied values have unique intrinsic roles. Multiple equivalent roleless
/// declaration projections may share one fallback role and are all rebuilt
/// from its single declarative recipe.
///
/// @since 9.0
/// @author Steve Ebersole
public final class MappingResolutionSnapshot implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private final Map<MappingRole, BasicValueRestorationRecipe> basicValueRecipes;
	private final MappingResolutionEnvironmentFingerprint environmentFingerprint;

	public MappingResolutionSnapshot(
			Map<MappingRole, BasicValueRestorationRecipe> basicValueRecipes,
			MappingResolutionEnvironmentFingerprint environmentFingerprint) {
		this.basicValueRecipes = Map.copyOf( new LinkedHashMap<>( basicValueRecipes ) );
		this.environmentFingerprint = environmentFingerprint;
	}

	public Map<MappingRole, BasicValueRestorationRecipe> basicValueRecipes() {
		return basicValueRecipes;
	}

	public MappingResolutionEnvironmentFingerprint environmentFingerprint() {
		return environmentFingerprint;
	}

	public void restore(
			MetadataImplementor metadata,
			MappingResolutionServices services,
			MappingResolutionState state,
			MetadataBuildingContext buildingContext) {
		environmentFingerprint.validate( metadata );
		final var values = MappingModelGraphIndex.from( metadata ).basicValuesByRole();
		for ( MappingRole role : values.keySet() ) {
			if ( !basicValueRecipes.containsKey( role ) ) {
				throw new IllegalStateException( "Archived mapping-resolution snapshot has no recipe for mapping role '"
						+ role + "'" );
			}
		}
		for ( var entry : basicValueRecipes.entrySet() ) {
			final var roleValues = values.get( entry.getKey() );
			if ( roleValues == null ) {
				throw new IllegalStateException(
						"Archived BasicValue resolution recipe references missing mapping role '" + entry.getKey() + "'"
				);
			}
			for ( var value : roleValues ) {
				try {
					final BasicValueResolutionDetails details = BasicValueResolutionDetails.fromRecipe(
							value,
							entry.getValue(),
							services
					);
					BasicValueResolutionBuilder.applyResolution( details, services, state, buildingContext );
				}
				catch (RuntimeException e) {
					throw new IllegalStateException(
							"Could not restore BasicValue resolution for mapping role '" + entry.getKey()
									+ "': " + e.getMessage(),
							e
					);
				}
			}
		}
	}
}
