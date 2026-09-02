/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import java.util.List;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Describes the rendered inputs needed for index DDL policy.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record IndexDdlRequest(boolean unique, List<IndexColumn> columns) {
	public IndexDdlRequest {
		columns = List.copyOf( requireNonNull( columns ) );
		if ( columns.isEmpty() ) {
			throw new IllegalArgumentException( "Index columns must not be empty" );
		}
	}
}
