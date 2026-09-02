/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import org.hibernate.SPI;
import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Describes a structured array or table element and capacity.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record AggregateArrayElementDescriptor(
		String typeName, int sqlTypeCode, int ddlTypeCode, int arrayLength) {
	public AggregateArrayElementDescriptor {
		requireNonNull( typeName );
	}
}
