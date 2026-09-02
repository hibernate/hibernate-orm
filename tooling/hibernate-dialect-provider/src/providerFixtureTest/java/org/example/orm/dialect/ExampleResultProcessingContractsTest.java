/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.sql.results.spi.ResultsConsumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies that the standalone provider fixture can implement the supported
/// result-building and result-consumption contracts without internal imports.
///
/// @author Steve Ebersole
public class ExampleResultProcessingContractsTest {
	@Test
	void implementsResultBuilderAndConsumerContracts() {
		final var builder = new ExampleResultBuilder();
		assertInstanceOf( ResultBuilder.class, builder );
		assertSame( builder, builder.cacheKeyInstance() );

		final var consumer = new ExampleResultsConsumer();
		assertInstanceOf( ResultsConsumer.class, consumer );
		assertFalse( consumer.canResultsBeCached() );
	}
}
