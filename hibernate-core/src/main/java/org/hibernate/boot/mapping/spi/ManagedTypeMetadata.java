/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import java.util.Collection;

import jakarta.persistence.AccessType;

import org.hibernate.models.spi.ClassDetails;

/// Read-only categorized description of a persistent managed type.
///
/// Managed types own categorized attribute declarations. Entity and mapped
/// superclass metadata additionally participate in an [EntityHierarchy];
/// embeddable declarations are exposed separately as
/// [EmbeddableTypeMetadata].
///
/// @since 9.0
/// @author Steve Ebersole
public interface ManagedTypeMetadata {
	/// The persistent managed-type category.
	enum Kind {
		/// An entity type.
		ENTITY,
		/// A mapped-superclass type.
		MAPPED_SUPER,
		/// An embeddable type.
		EMBEDDABLE
	}

	/// The persistent managed-type category.
	Kind getManagedTypeKind();

	/// The source Java class.
	ClassDetails getClassDetails();

	/// The effective access strategy for persistent attributes.
	AccessType getAccessType();

	/// The number of categorized attributes declared by this type.
	int getNumberOfAttributes();

	/// The categorized attributes declared by this type.
	Collection<? extends AttributeMetadata> getAttributes();

	/// Finds a declared attribute by name, returning `null` when none exists.
	AttributeMetadata findAttribute(String name);
}
