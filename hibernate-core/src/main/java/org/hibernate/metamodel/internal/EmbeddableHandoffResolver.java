/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;


import java.util.Objects;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;

import jakarta.annotation.Nullable;

/// Resolves applied embeddable metadata by stable mapping role during
/// runtime/JPA metamodel creation.
///
/// @since 9.0
/// @author Steve Ebersole
public class EmbeddableHandoffResolver {
	private final RuntimeMappingHandoff runtimeMappingHandoff;

	public EmbeddableHandoffResolver(RuntimeMappingHandoff runtimeMappingHandoff) {
		this.runtimeMappingHandoff = Objects.requireNonNull( runtimeMappingHandoff );
	}

	/// Resolve the applied component-member usage that produced the materialized
	/// property, when the runtime handoff has an applied product for the
	/// component's intrinsic role.
	public @Nullable AttributeUsageHandoff findMemberBinding(Component component, Property property) {
		final var role = property.getMappingRole() != null
				? property.getMappingRole()
				: component.getMappingRole() == null
						? null
						: component.getMappingRole().appendAttribute( property.getName() );
		return runtimeMappingHandoff.findAttribute( role );
	}

	/// Determines whether an embeddable attribute should be registered as a
	/// concrete generic attribute while building the runtime/JPA metamodel.
	///
	/// Applied component-member metadata is authoritative when available: a usage
	/// is concrete-generic when its resolved usage type differs from its
	/// declaration type.  The copied [Property#isGenericSpecialization()]
	/// and mapped-superclass generic marker remain compatibility fallbacks for
	/// paths where no applied component-member handoff is available.
	public boolean isConcreteGenericAttribute(
			Component component,
			Property property,
			@Nullable Property superclassProperty) {
		final var memberBinding = findMemberBinding( component, property );
		if ( memberBinding != null ) {
			return isConcreteGenericUsage( memberBinding );
		}
		return isGenericSpecializationFallback( property )
				|| superclassProperty != null && superclassProperty.isGeneric();
	}

	private static boolean isConcreteGenericUsage(@Nullable AttributeUsageHandoff memberBinding) {
		if ( memberBinding == null ) {
			return false;
		}
		return memberBinding.isConcreteGenericUsage();
	}

	/// Transitional fallback to the legacy mapping flag.
	///
	/// Keep calls to [Property#isGenericSpecialization()] localized here so
	/// runtime/JPA metamodel code can ask semantic questions through this resolver
	/// instead of spreading copied-property bridge checks.
	private static boolean isGenericSpecializationFallback(Property property) {
		return property.isGenericSpecialization();
	}
}
