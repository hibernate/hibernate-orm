/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable root-entity facts for naming an entity discriminator column.
///
/// All reference components are non-null.
///
/// @param entity The root entity of the hierarchy receiving the discriminator column
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record DiscriminatorColumnNamingInput(EntityNamingInput entity) {
	public DiscriminatorColumnNamingInput {
		requireNonNull( entity, "entity" );
	}
}
