/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Renders ordered table-truncation commands.
///
/// Return an empty list for an empty request. Implementations must not retain
/// the request or reorder its table names.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getTruncateSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface TruncateSupport {
	TruncateSupport STANDARD = new TruncateSupport() {};

	default TruncateMode truncateMode() {
		return TruncateMode.PER_TABLE;
	}

	default List<String> renderCommands(TruncateRequest request) {
		final var tableNames = requireNonNull( request ).tableNames();
		final var commands = new ArrayList<String>( tableNames.size() );
		for ( var tableName : tableNames ) {
			commands.add( "truncate table " + tableName );
		}
		return List.copyOf( commands );
	}
}
