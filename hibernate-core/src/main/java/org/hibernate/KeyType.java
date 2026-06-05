/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.FindOption;

/// Specifies whether an identifier value passed to
/// {@link jakarta.persistence.EntityHandler#get get()} or
/// {@link jakarta.persistence.EntityHandler#find find()} should be
/// interpreted as a [regular id][jakarta.persistence.Id] (the default)
/// or as a [natural id][org.hibernate.annotations.NaturalId].
///
/// @see jakarta.persistence.EntityHandler#find
/// @see jakarta.persistence.EntityHandler#get
/// @see jakarta.persistence.EntityHandler#findMultiple
/// @see jakarta.persistence.EntityHandler#getMultiple
/// @see Session#getReference(Class, Object, KeyType)
///
/// @since 7.3
///
/// @author Steve Ebersole
/// @author Gavin King
public enum KeyType implements FindOption {
	/// Indicates to find by the entity's identifier. The default.
	///
	/// @see jakarta.persistence.Id
	/// @see jakarta.persistence.EmbeddedId
	/// @see jakarta.persistence.IdClass
	IDENTIFIER,

	/// Indicates to find based on the entity's natural id, if it has one.
	///
	/// @see org.hibernate.annotations.NaturalId
	/// @see org.hibernate.annotations.NaturalIdClass
	/// @see NaturalIdSynchronization
	///
	/// @implSpec Triggers an [IllegalArgumentException] if the entity
	///           does not define a natural id.
	NATURAL
}
