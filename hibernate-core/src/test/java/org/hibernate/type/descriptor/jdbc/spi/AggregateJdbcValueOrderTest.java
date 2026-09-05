/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// @author Steve Ebersole
class AggregateJdbcValueOrderTest {
	@Test
	void buildsBothDirectionsAndDefensivelyCopiesThePermutation() {
		final int[] physicalOrder = { 2, 0, 1 };
		final AggregateJdbcValueOrder order = AggregateJdbcValueOrder.physicalOrder( physicalOrder );
		physicalOrder[0] = 0;

		assertArrayEquals( new int[] { 2, 0, 1 }, order.physicalOrder( 3 ) );
		assertArrayEquals( new int[] { 1, 2, 0 }, order.logicalOrder( 3 ) );
	}

	@Test
	void canonicalizesIdentityPermutations() {
		assertSame(
				AggregateJdbcValueOrder.identity(),
				AggregateJdbcValueOrder.physicalOrder( 0, 1, 2 )
		);
	}

	@Test
	void rejectsInvalidPermutations() {
		assertThrows( IllegalArgumentException.class, () -> AggregateJdbcValueOrder.physicalOrder( 0, 0 ) );
		assertThrows( IllegalArgumentException.class, () -> AggregateJdbcValueOrder.physicalOrder( -1, 0 ) );
		assertThrows( IllegalArgumentException.class, () -> AggregateJdbcValueOrder.physicalOrder( 0, 2 ) );
	}

	@Test
	void rejectsMappingSizeMismatches() {
		final AggregateJdbcValueOrder order = AggregateJdbcValueOrder.physicalOrder( 1, 0 );

		assertThrows( IllegalArgumentException.class, () -> order.physicalOrder( 3 ) );
		assertThrows( IllegalArgumentException.class, () -> order.logicalOrder( 1 ) );
	}
}
