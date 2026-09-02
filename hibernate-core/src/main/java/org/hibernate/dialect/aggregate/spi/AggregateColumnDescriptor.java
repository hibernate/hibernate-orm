/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import java.util.List;
import jakarta.annotation.Nullable;
import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.SqlTypedMapping;

import static org.hibernate.SPI.Role.USE;

/// Describes one aggregate or scalar column without exposing mutable boot
/// mapping state. Treat nested components as ordered and immutable.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public interface AggregateColumnDescriptor {
	String columnName();
	int sqlTypeCode();
	String sqlTypeName();
	SqlTypedMapping typeMapping();
	boolean nullable();
	List<AggregateColumnDescriptor> components();
	@Nullable AggregateArrayElementDescriptor arrayElement();
}
