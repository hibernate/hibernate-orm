/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.type.SqlTypes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies provider implementation and supply of the public aggregate SPI.
///
/// @author Steve Ebersole
/// @since 8.0
public class ExampleAggregateSupportTest {
	@Test
	void suppliesTheProviderOwnedStrategy() {
		assertSame( ExampleAggregateSupport.INSTANCE, new ExampleDialect().getAggregateSupport() );
	}

	@Test
	void extendsTheStandardSelectionProfile() {
		assertFalse( ExampleAggregateSupport.INSTANCE.preferSelectAggregateMapping( SqlTypes.JSON ) );
		assertTrue( ExampleAggregateSupport.INSTANCE.preferSelectAggregateMapping( SqlTypes.STRUCT ) );
		assertTrue( ExampleAggregateSupport.INSTANCE.preferBindAggregateMapping( SqlTypes.JSON ) );
	}
}
