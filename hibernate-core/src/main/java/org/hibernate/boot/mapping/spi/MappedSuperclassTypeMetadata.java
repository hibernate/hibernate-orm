/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

/// Read-only categorized description of a mapped superclass visible in an
/// entity hierarchy.
///
/// A mapped superclass contributes declarations to concrete entity
/// applications but does not itself define an entity mapping.
///
/// @since 9.0
/// @author Steve Ebersole
public interface MappedSuperclassTypeMetadata extends IdentifiableTypeMetadata {
	@Override
	default Kind getManagedTypeKind() {
		return Kind.MAPPED_SUPER;
	}
}
