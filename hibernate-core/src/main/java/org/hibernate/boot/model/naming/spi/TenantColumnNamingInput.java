/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;

/// Immutable facts for naming a tenant column. The attribute path is relative
/// to the owning entity, including any embeddable nesting.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record TenantColumnNamingInput(EntityNamingInput entity, String attributePath) {
	public TenantColumnNamingInput {
		requireNonNull( entity, "entity" );
		requireNonNull( attributePath, "attributePath" );
	}
}
