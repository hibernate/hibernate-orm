/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception.spi;

import java.sql.SQLException;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import jakarta.annotation.Nullable;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Extracts a violated database-constraint name from a vendor
/// [SQLException].
///
/// Return `null` when the exception does not expose a constraint name. An
/// absent name must not be represented by an empty string or a sentinel.
/// Implementations may inspect nested vendor exceptions when the driver
/// reports constraint details there.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getViolatedConstraintNameExtractor()
@SPI({ USE, IMPLEMENT, SUPPLY })
@FunctionalInterface
public interface ViolatedConstraintNameExtractor {
	/// Extract the violated constraint name from the given exception.
	///
	/// @param sqle the exception caused by the constraint violation
	/// @return the extracted name, or `null` when no name can be recovered
	@Nullable String extractConstraintName(SQLException sqle);
}
