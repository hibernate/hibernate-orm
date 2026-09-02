/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import java.util.List;
import java.util.Set;
import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

/// Declares ordered SQL commands for one aggregate schema object.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record AggregateSqlAuxiliaryObject(
		String exportIdentifier, List<String> createCommands, List<String> dropCommands,
		Set<String> dialectScopes, boolean beforeTables) implements AggregateAuxiliaryObject {
	public AggregateSqlAuxiliaryObject {
		if ( exportIdentifier == null || exportIdentifier.isBlank() ) {
			throw new IllegalArgumentException( "Aggregate auxiliary export identifier must not be blank" );
		}
		createCommands = List.copyOf( createCommands );
		dropCommands = List.copyOf( dropCommands );
		dialectScopes = Set.copyOf( dialectScopes );
		if ( createCommands.isEmpty() && dropCommands.isEmpty() ) {
			throw new IllegalArgumentException( "Aggregate auxiliary object must declare a create or drop command" );
		}
	}
}
