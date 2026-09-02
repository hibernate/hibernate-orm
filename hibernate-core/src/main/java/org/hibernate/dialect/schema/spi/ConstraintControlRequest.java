/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Identifies one rendered table and foreign-key constraint.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record ConstraintControlRequest(String tableName, String constraintName) {
	public ConstraintControlRequest {
		requireNonNull( tableName );
		requireNonNull( constraintName );
	}
}
