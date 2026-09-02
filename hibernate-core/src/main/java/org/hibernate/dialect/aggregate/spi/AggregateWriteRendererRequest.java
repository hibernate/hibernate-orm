/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import java.util.List;
import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Supplies one call's aggregate write-renderer mappings.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record AggregateWriteRendererRequest(
		SelectableMapping aggregateColumn,
		List<SelectableMapping> columnsToUpdate,
		TypeConfiguration typeConfiguration) {
	public AggregateWriteRendererRequest {
		requireNonNull( aggregateColumn );
		columnsToUpdate = List.copyOf( columnsToUpdate );
		requireNonNull( typeConfiguration );
	}
}
