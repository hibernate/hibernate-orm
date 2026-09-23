/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Facts for naming an entity primary table.
///
/// All reference components are non-null.
///
/// @param entity The mapped entity whose primary table needs an implicit name
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record PrimaryTableNamingInput(
		EntityNamingInput entity) {
	public PrimaryTableNamingInput {
		requireNonNull( entity, "entity" );
	}
}
