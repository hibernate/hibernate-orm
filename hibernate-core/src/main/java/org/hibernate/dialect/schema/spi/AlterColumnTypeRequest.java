/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Describes one requested column-type alteration.
///
/// Implementations choose between [#columnType] and the complete
/// [#columnDefinition] according to database grammar and must not retain this
/// request.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record AlterColumnTypeRequest(
		String columnName,
		String columnType,
		String columnDefinition) {
	public AlterColumnTypeRequest {
		requireNonNull( columnName );
		requireNonNull( columnType );
		requireNonNull( columnDefinition );
	}
}
