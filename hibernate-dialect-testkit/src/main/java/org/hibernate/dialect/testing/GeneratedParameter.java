/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing;

import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// One JDBC parameter occurrence in generated SQL.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(USE)
public record GeneratedParameter(int jdbcPosition, @Nullable String queryParameterName) {
	public GeneratedParameter {
		if ( jdbcPosition < 1 ) {
			throw new IllegalArgumentException( "jdbcPosition is one-based" );
		}
	}
}
