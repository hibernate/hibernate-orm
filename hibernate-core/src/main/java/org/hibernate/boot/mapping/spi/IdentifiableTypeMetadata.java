/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

/// Read-only categorized description of an entity or mapped superclass in an
/// entity hierarchy.
///
/// The supertype and subtype links include both entity and mapped-superclass
/// nodes. Use [EntityTypeMetadata] when entity-specific settings are required.
///
/// @since 9.0
/// @author Steve Ebersole
public interface IdentifiableTypeMetadata extends ManagedTypeMetadata {
	/// The entity hierarchy containing this type.
	EntityHierarchy getHierarchy();

	/// The immediate persistent supertype, or `null` for the absolute root.
	IdentifiableTypeMetadata getSuperType();

	/// Whether the source Java type is abstract.
	default boolean isAbstract() {
		return getClassDetails().isAbstract();
	}

	/// Whether this type has persistent subtypes.
	boolean hasSubTypes();

	/// The number of immediate persistent subtypes.
	int getNumberOfSubTypes();

	/// The immediate persistent subtypes.
	Iterable<? extends IdentifiableTypeMetadata> getSubTypes();
}
