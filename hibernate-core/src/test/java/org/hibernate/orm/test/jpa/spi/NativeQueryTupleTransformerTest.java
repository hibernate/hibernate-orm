/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import jakarta.persistence.Tuple;

import org.hibernate.jpa.spi.NativeQueryTupleTransformer;

import org.junit.Test;

/**
 * @author Maksym Symonov
 */
public class NativeQueryTupleTransformerTest {

	private final NativeQueryTupleTransformer nativeQueryTupleTransformer = new NativeQueryTupleTransformer();

	@Test
	public void nullValueIsExtractedFromTuple() {
		final Tuple tuple = (Tuple) nativeQueryTupleTransformer.transformTuple(
			new Object[] { 1L, null },
			new String[] { "id", "value" }
		);
		assertEquals(1L, tuple.get("id"));
		assertNull(tuple.get("value"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingAliasCausesExceptionWhenIsExtractedFromTuple() {
		final Tuple tuple = (Tuple) nativeQueryTupleTransformer.transformTuple(
			new Object[] { 1L, null },
			new String[] { "id", "value" }
		);
		tuple.get("unknownAlias");
	}
}
