/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing;

import java.util.List;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Immutable create and drop commands for the test kit's fixed mapping model.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(USE)
public record SchemaGenerationResult(List<String> createCommands, List<String> dropCommands) {
	public SchemaGenerationResult {
		createCommands = List.copyOf( Objects.requireNonNull( createCommands, "createCommands" ) );
		dropCommands = List.copyOf( Objects.requireNonNull( dropCommands, "dropCommands" ) );
	}
}
