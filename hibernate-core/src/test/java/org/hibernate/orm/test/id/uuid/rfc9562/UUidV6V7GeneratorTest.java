/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.rfc9562;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.uuid.UuidValueGenerator;
import org.hibernate.id.uuid.UuidVersion6Strategy;
import org.hibernate.id.uuid.UuidVersion7Strategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class UUidV6V7GeneratorTest {

	private static final UUID NIL_UUID = new UUID( 0L, 0L );
	private static final int ITERATIONS = 1_000_000;

	@Test
	void testMonotonicityUuid6() {
		testMonotonicity( UuidVersion6Strategy.INSTANCE );
	}

	@Test
	void testMonotonicityUuid7() {
		testMonotonicity( UuidVersion7Strategy.INSTANCE );
	}

	private static void testMonotonicity(UuidValueGenerator generator) {
		final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class );
		final UUID[] uuids = new UUID[ITERATIONS + 1];
		uuids[0] = NIL_UUID;
		for ( int n = 1; n <= ITERATIONS; ++n ) {
			uuids[n] = generator.generateUuid( session );
		}

		for ( var n = 0; n < ITERATIONS; ++n ) {
			assertThat( uuids[n + 1].toString() ).isGreaterThan( uuids[n].toString() );
			assertThat( uuids[n + 1] ).isGreaterThan( uuids[n] );
		}
	}
}
