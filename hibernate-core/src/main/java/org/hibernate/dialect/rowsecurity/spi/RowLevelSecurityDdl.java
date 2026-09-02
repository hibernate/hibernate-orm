/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Declares one ordered row-level-security schema object or command group.
///
/// The export identifier must be stable across renderings. Hibernate uses it
/// to deduplicate database-level declarations. Command and scope collections
/// are immutable snapshots, and at least one create or drop command is
/// required.
///
/// @param exportIdentifier the stable schema-export identifier
/// @param phase whether creation occurs before or after table creation
/// @param createCommands the ordered creation commands
/// @param dropCommands the ordered drop commands
/// @param dialectScopes optional fully qualified Dialect class names
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record RowLevelSecurityDdl(
		String exportIdentifier,
		Phase phase,
		List<String> createCommands,
		List<String> dropCommands,
		Set<String> dialectScopes) {
	public RowLevelSecurityDdl {
		if ( exportIdentifier == null || exportIdentifier.isBlank() ) {
			throw new IllegalArgumentException( "Row-level-security export identifier must not be blank" );
		}
		if ( phase == null ) {
			throw new IllegalArgumentException( "Row-level-security DDL phase must not be null" );
		}
		createCommands = List.copyOf( createCommands );
		dropCommands = List.copyOf( dropCommands );
		dialectScopes = Set.copyOf( dialectScopes );
		if ( createCommands.isEmpty() && dropCommands.isEmpty() ) {
			throw new IllegalArgumentException( "Row-level-security DDL must declare a create or drop command" );
		}
	}

	/// Defines schema-creation ordering for a row-level-security declaration.
	public enum Phase {
		BEFORE_TABLES,
		AFTER_TABLES
	}
}
