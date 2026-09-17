/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

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

	@Test
	public void toArrayDoesNotExposeTupleStorage() {
		final Tuple tuple = nativeQueryTupleTransformer.transformTuple(
				new Object[] { 1L }, new String[] { "id" }
		);
		tuple.toArray()[0] = 2L;
		assertEquals( 1L, tuple.get( 0 ) );
		assertEquals( 1L, tuple.get( "id" ) );
	}

	@Test
	public void nullValueIsExtractedByTupleElement() {
		final Tuple tuple = nativeQueryTupleTransformer.transformTuple(
				new Object[] { null }, new String[] { "value" }
		);
		final var element = tuple.getElements().get( 0 );
		assertEquals( Object.class, element.getJavaType() );
		assertNull( tuple.get( element ) );
	}

	@Test
	public void tupleElementWithoutAliasIsRejected() {
		final Tuple tuple = nativeQueryTupleTransformer.transformTuple(
				new Object[] { 1L }, new String[] { "id" }
		);
		final var element = new TupleElement<Long>() {
			@Override
			public @Nonnull Class<Long> getJavaType() {
				return Long.class;
			}

			@Override
			public @Nullable String getAlias() {
				return null;
			}
		};
		assertEquals( "TupleElement has no alias",
				assertThrows( IllegalArgumentException.class, () -> tuple.get( element ) ).getMessage() );
	}
}
