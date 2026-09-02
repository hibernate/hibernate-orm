/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import java.util.List;
import org.hibernate.SPI;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Supplies one call's aggregate custom-write facts.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record AggregateCustomWriteRequest(
		AggregateColumnDescriptor aggregateColumn,
		List<AggregateColumnDescriptor> components,
		TypeConfiguration typeConfiguration) {
	public AggregateCustomWriteRequest {
		requireNonNull( aggregateColumn );
		components = List.copyOf( components );
		requireNonNull( typeConfiguration );
	}
}
