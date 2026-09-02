/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Renders alter-table and add-column grammar.
///
/// Return `null` from [#alterColumnType] when in-place type alteration is not
/// supported. Returned fragments are consumed verbatim and must not depend on
/// retained request state.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getAlterTableSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface AlterTableSupport {
	default String alterTableCommand(String tableName, ExistenceCheckPlacement ifExistsPlacement) {
		requireNonNull( tableName );
		return switch ( requireNonNull( ifExistsPlacement ) ) {
			case NONE -> "alter table " + tableName;
			case BEFORE_NAME -> "alter table if exists " + tableName;
			case AFTER_NAME -> "alter table " + tableName + " if exists";
		};
	}

	default String addColumnPrefix() {
		return "add column";
	}

	default String addColumnSuffix() {
		return "";
	}

	default @Nullable String alterColumnType(AlterColumnTypeRequest request) {
		requireNonNull( request );
		return null;
	}
}
