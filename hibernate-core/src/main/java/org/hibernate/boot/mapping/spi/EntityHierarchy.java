/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import jakarta.persistence.AccessType;
import jakarta.persistence.InheritanceType;

/// Read-only categorized entity inheritance hierarchy.
///
/// The entity root may be preceded by mapped-superclass nodes, represented by
/// [#getAbsoluteRoot()].
///
/// @since 9.0
/// @author Steve Ebersole
public interface EntityHierarchy {
	/// The root entity of the hierarchy.
	EntityTypeMetadata getRoot();

	/// The highest persistent type in the hierarchy, which may be a mapped
	/// superclass.
	IdentifiableTypeMetadata getAbsoluteRoot();

	/// The entity inheritance strategy.
	InheritanceType getInheritanceType();

	/// The default access strategy inherited within the hierarchy.
	AccessType getDefaultAccessType();
}
