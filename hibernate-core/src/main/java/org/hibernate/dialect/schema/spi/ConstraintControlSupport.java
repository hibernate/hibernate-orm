/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import java.util.List;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Supplies global or per-constraint control commands for table cleaning.
///
/// Implement only the command family selected by [#constraintControlMode]. Return immutable,
/// ordered lists; an empty list means that no command is required.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getConstraintControlSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface ConstraintControlSupport {
	ConstraintControlSupport NONE = () -> ConstraintControlMode.NONE;

	ConstraintControlMode constraintControlMode();

	default List<String> disableCommands() {
		return List.of();
	}

	default List<String> enableCommands() {
		return List.of();
	}

	default List<String> disableConstraintCommands(ConstraintControlRequest request) {
		requireNonNull( request );
		return List.of();
	}

	default List<String> enableConstraintCommands(ConstraintControlRequest request) {
		requireNonNull( request );
		return List.of();
	}
}
