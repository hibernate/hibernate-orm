/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines existence-check placement for schema DDL targets.
///
/// Supply one immutable profile for the Dialect lifetime. Each placement is
/// interpreted relative to the corresponding target name.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getIfExistsSupport()
@SPI({ USE, SUPPLY })
public record IfExistsSupport(
		ExistenceCheckPlacement alterTablePlacement,
		ExistenceCheckPlacement dropTablePlacement,
		ExistenceCheckPlacement dropConstraintPlacement,
		ExistenceCheckPlacement dropIndexPlacement) {
	public static final IfExistsSupport NONE = new IfExistsSupport(
			ExistenceCheckPlacement.NONE,
			ExistenceCheckPlacement.NONE,
			ExistenceCheckPlacement.NONE,
			ExistenceCheckPlacement.NONE
	);

	public IfExistsSupport {
		requireNonNull( alterTablePlacement );
		requireNonNull( dropTablePlacement );
		requireNonNull( dropConstraintPlacement );
		requireNonNull( dropIndexPlacement );
	}
}
