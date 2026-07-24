/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.util.IdentityHashMap;
import java.util.Map;

import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionDetails;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.MappingRole;

/// Opt-in binding-phase capture which freezes resolution details only when an archive is stored.
///
/// @see MappingSettings#METADATA_SERIALIZATION_ENABLED
///
/// @since 9.0
/// @author Steve Ebersole
public final class MappingResolutionDetailsCollector {
	private final Map<BasicValue, BasicValueResolutionDetails> resolutionDetails = new IdentityHashMap<>();

	public void capture(BasicValueResolutionDetails details) {
		resolutionDetails.putIfAbsent( details.value(), details );
	}

	public MappingResolutionSnapshot freeze(MetadataImplementor metadata) {
		final MappingModelGraphIndex graphIndex = MappingModelGraphIndex.from( metadata );
		final Map<MappingRole, BasicValueRestorationRecipe> frozen = new java.util.LinkedHashMap<>();
		graphIndex.basicValuesByRole().forEach( (role, values) -> {
			for ( BasicValue value : values ) {
				final BasicValueResolutionDetails details = resolutionDetails.get( value );
				if ( details == null ) {
					continue;
				}
				final BasicValueRestorationRecipe recipe;
				try {
					recipe = BasicValueRestorationRecipe.from( details );
				}
				catch (IllegalArgumentException e) {
					throw new IllegalArgumentException(
							"BasicValue resolution for mapping role '" + role + "' cannot be archived",
							e
					);
				}
				final BasicValueRestorationRecipe previous = frozen.putIfAbsent( role, recipe );
				if ( previous != null && !previous.equals( recipe ) ) {
					throw new IllegalStateException( "Conflicting BasicValue resolution recipes for mapping role '"
							+ role + "'" );
				}
			}
			if ( !frozen.containsKey( role ) ) {
				throw new IllegalStateException( "Missing BasicValue resolution recipe for mapping role '"
						+ role + "'" );
			}
		} );
		return new MappingResolutionSnapshot(
				frozen,
				MappingResolutionEnvironmentFingerprint.from( metadata )
		);
	}
}
