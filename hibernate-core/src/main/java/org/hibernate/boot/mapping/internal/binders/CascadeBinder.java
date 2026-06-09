/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.EnumSet;

import org.hibernate.boot.mapping.internal.context.BindingState;

import jakarta.persistence.CascadeType;

/// Normalizes JPA cascade annotations for mapping properties.
///
/// Cascade metadata is declared on the source member, but the boot mapping model
/// stores the effective cascade style on [org.hibernate.mapping.Property].  This
/// helper mirrors the upstream aggregation rules closely enough for the phased
/// binder prototype: start with the JPA cascade array, apply orphan-removal when
/// the caller has such a source, and then include mapping defaults.
///
/// Keeping this as a small binder helper avoids making individual attribute
/// binders know how to merge explicit cascades with mapping defaults.
///
/// @since 9.0
/// @author Steve Ebersole
public class CascadeBinder {
	public static EnumSet<CascadeType> aggregateCascadeTypes(
			CascadeType[] cascadeTypes,
			boolean orphanRemoval,
			BindingState bindingState) {
		final EnumSet<CascadeType> cascades = convertToCascadeTypes( cascadeTypes );

		if ( orphanRemoval ) {
			cascades.add( CascadeType.REMOVE );
		}

		cascades.addAll( bindingState.getMetadataBuildingContext().getEffectiveDefaults().getDefaultCascadeTypes() );
		return cascades;
	}

	private static EnumSet<CascadeType> convertToCascadeTypes(CascadeType[] cascadeTypes) {
		final EnumSet<CascadeType> cascades = EnumSet.noneOf( CascadeType.class );
		if ( cascadeTypes == null ) {
			return cascades;
		}

		for ( CascadeType cascadeType : cascadeTypes ) {
			cascades.add( cascadeType );
		}
		return cascades;
	}
}
