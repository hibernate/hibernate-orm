/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Supplies table and view creation grammar.
///
/// Command and option fragments are consumed verbatim. Resolve any configured
/// storage engine before this strategy is supplied and return stable behavior
/// for the Dialect lifetime.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getTableCreationSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface TableCreationSupport {
	default String createTableCommand(TableCreationKind kind) {
		requireNonNull( kind );
		return "create table";
	}

	default String tableCreationOptions() {
		return "";
	}

	default boolean requiresViewColumnList() {
		return false;
	}
}
