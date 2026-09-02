/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.constraint.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.internal.util.StringHelper.isBlank;

/// Describes one foreign-key constraint to be dropped from an existing table.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record ForeignKeyDropRequest(
		String constraintName,
		ExistenceCheckPlacement ifExistsPlacement) {
	public ForeignKeyDropRequest {
		if ( isBlank( constraintName ) ) {
			throw new IllegalArgumentException( "constraintName must not be blank" );
		}
		requireNonNull( ifExistsPlacement, "ifExistsPlacement" );
	}
}
