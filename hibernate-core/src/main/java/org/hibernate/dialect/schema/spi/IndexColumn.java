/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// A rendered index-column expression and its mapping nullability.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record IndexColumn(String expression, boolean nullable) {
	public IndexColumn {
		requireNonNull( expression );
	}
}
