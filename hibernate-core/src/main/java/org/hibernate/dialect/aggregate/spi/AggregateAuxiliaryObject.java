/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

/// Identifies a declarative aggregate schema object materialized by Hibernate.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public sealed interface AggregateAuxiliaryObject
		permits AggregateSqlAuxiliaryObject, AggregateUserDefinedArrayType {
}
