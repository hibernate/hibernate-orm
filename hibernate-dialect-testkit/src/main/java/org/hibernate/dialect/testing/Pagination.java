/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing;

import org.hibernate.Incubating;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Pagination requested for a database-free SQL translation.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(USE)
public record Pagination(int firstResult, int maxResults) {
	public Pagination {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException( "firstResult must not be negative" );
		}
		if ( maxResults < 1 ) {
			throw new IllegalArgumentException( "maxResults must be positive" );
		}
	}
}
