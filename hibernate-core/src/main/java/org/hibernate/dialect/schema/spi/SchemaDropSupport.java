/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import java.util.List;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines table-drop composition and ordered pre-drop commands.
///
/// The cascade clause is appended verbatim, including provider-owned leading
/// whitespace.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getSchemaDropSupport()
@SPI({ USE, SUPPLY })
public record SchemaDropSupport(
		List<String> beforeDropCommands,
		ConstraintDropMode constraintDropMode,
		String cascadeConstraintsClause) {
	public static final SchemaDropSupport STANDARD = new SchemaDropSupport(
			List.of(),
			ConstraintDropMode.EXPLICIT,
			""
	);

	public SchemaDropSupport {
		beforeDropCommands = List.copyOf( requireNonNull( beforeDropCommands ) );
		requireNonNull( constraintDropMode );
		requireNonNull( cascadeConstraintsClause );
	}
}
