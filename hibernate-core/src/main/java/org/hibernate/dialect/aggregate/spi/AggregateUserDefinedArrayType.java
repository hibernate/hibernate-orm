/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import org.hibernate.SPI;
import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Declares facts materialized into Hibernate's structured-array model.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record AggregateUserDefinedArrayType(
		String typeName, int arraySqlTypeCode, int arrayLength,
		String elementTypeName, int elementSqlTypeCode,
		int elementDdlTypeCode) implements AggregateAuxiliaryObject {
	public AggregateUserDefinedArrayType {
		requireNonNull( typeName );
		requireNonNull( elementTypeName );
	}
}
