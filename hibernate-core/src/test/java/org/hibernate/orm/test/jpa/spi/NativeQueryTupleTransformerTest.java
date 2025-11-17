/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.spi;

import jakarta.persistence.Tuple;

import org.hibernate.jpa.spi.NativeQueryTupleTransformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Maksym Symonov
 */
public class NativeQueryTupleTransformerTest {

	private final NativeQueryTupleTransformer nativeQueryTupleTransformer = new NativeQueryTupleTransformer();

	@Test
	public void nullValueIsExtractedFromTuple() {
		final Tuple tuple = nativeQueryTupleTransformer.transformTuple(
			new Object[] { 1L, null },
			new String[] { "id", "value" }
		);
		assertEquals(1L, tuple.get("id"));
		assertNull(tuple.get("value"));
	}

	@Test
	public void missingAliasCausesExceptionWhenIsExtractedFromTuple() {
		assertThrows(
				IllegalArgumentException.class,
				() -> {
					final Tuple tuple = nativeQueryTupleTransformer.transformTuple(
							new Object[] {1L, null},
							new String[] {"id", "value"}
					);
					tuple.get( "unknownAlias" );
				}
		);
	}
}
