/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.FindOption;

/// FindOption allowing to load based on either id (default) or natural-id.
///
/// @see jakarta.persistence.EntityManager#find
/// @see Session#findMultiple
/// @see Session#getReference(Class, Object, KeyType)
///
/// @since 7.3
///
/// @author Steve Ebersole
/// @author Gavin King
public enum KeyType implements FindOption {
	/// Indicates to find by the entity's identifier.  The default.
	///
	/// @see jakarta.persistence.Id
	/// @see jakarta.persistence.EmbeddedId
	/// @see jakarta.persistence.IdClass
	IDENTIFIER,

	/// Indicates to find based on the entity's natural-id, if one.
	///
	/// @see org.hibernate.annotations.NaturalId
	/// @see org.hibernate.annotations.NaturalIdClass
	///
	/// @implSpec Will trigger an [IllegalArgumentException] if the entity does
	/// not define a natural-id.
	NATURAL
}
