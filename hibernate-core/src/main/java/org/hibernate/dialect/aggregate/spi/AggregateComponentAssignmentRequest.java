/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Supplies one call's aggregate-component assignment-expression facts.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record AggregateComponentAssignmentRequest(
		String aggregateParentAssignmentExpression,
		String columnExpression,
		int aggregateColumnTypeCode,
		SqlTypedMapping column,
		TypeConfiguration typeConfiguration) {
	public AggregateComponentAssignmentRequest {
		requireNonNull( aggregateParentAssignmentExpression );
		requireNonNull( columnExpression );
		requireNonNull( column );
		requireNonNull( typeConfiguration );
	}
}
