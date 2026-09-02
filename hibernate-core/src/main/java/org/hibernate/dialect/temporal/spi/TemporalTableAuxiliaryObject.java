/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.spi;

import java.util.List;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Declares ordered schema commands required by a temporal table strategy.
///
/// Database-scoped descriptors are deduplicated by export identifier. Table-
/// scoped descriptors are isolated by table name plus export identifier.
/// Command lists are immutable snapshots and at least one command is required.
///
/// @param exportIdentifier the stable descriptor identifier within its scope
/// @param scope whether the descriptor is shared or belongs to one table
/// @param beforeTables whether creation precedes table creation
/// @param createCommands the ordered creation commands
/// @param dropCommands the ordered drop commands
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record TemporalTableAuxiliaryObject(
		String exportIdentifier,
		Scope scope,
		boolean beforeTables,
		List<String> createCommands,
		List<String> dropCommands) {
	public TemporalTableAuxiliaryObject {
		if ( exportIdentifier == null || exportIdentifier.isBlank() ) {
			throw new IllegalArgumentException( "Temporal auxiliary export identifier must not be blank" );
		}
		if ( scope == null ) {
			throw new IllegalArgumentException( "Temporal auxiliary scope must not be null" );
		}
		createCommands = List.copyOf( createCommands );
		dropCommands = List.copyOf( dropCommands );
		if ( createCommands.isEmpty() && dropCommands.isEmpty() ) {
			throw new IllegalArgumentException( "Temporal auxiliary object must declare a create or drop command" );
		}
	}

	/// Defines whether an auxiliary object is shared by the database or belongs
	/// to one temporal table.
	public enum Scope {
		DATABASE,
		TABLE
	}
}
