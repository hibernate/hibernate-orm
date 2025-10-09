/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stat.internal;

import org.hibernate.stat.internal.StatsNamedContainer;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

@JiraKey(value = "HHH-13645")
public class StatsNamedContainerNullComputedValueTest {

	private final static AtomicInteger invocationCounterNullProducer = new AtomicInteger();
	private final static AtomicInteger invocationCounterValueProducer = new AtomicInteger();

	@Test
	public void testNullComputedValue() {
		final var statsNamedContainer = new StatsNamedContainer<Integer>();
		Assertions.assertNull( statsNamedContainer.getOrCompute(
				"key",
				v -> null
		) );
	}

	@Test
	public void ableToStoreNullValues() {
		final var statsNamedContainer = new StatsNamedContainer<Integer>();
		Assertions.assertEquals( 0, invocationCounterNullProducer.get() );
		Assertions.assertNull( getCacheWithNullValue( statsNamedContainer ) );
		Assertions.assertEquals( 1, invocationCounterNullProducer.get() );
		Assertions.assertNull( getCacheWithNullValue( statsNamedContainer ) );
		Assertions.assertEquals( 1, invocationCounterNullProducer.get() );
	}

	@Test
	public void ableToStoreActualValues() {
		final var statsNamedContainer = new StatsNamedContainer<Integer>();
		Assertions.assertEquals( 0, invocationCounterValueProducer.get() );
		Assertions.assertEquals( 5, getCacheWithActualValue( statsNamedContainer ) );
		Assertions.assertEquals( 1, invocationCounterValueProducer.get() );
		Assertions.assertEquals( 5, getCacheWithActualValue( statsNamedContainer ) );
		Assertions.assertEquals( 1, invocationCounterValueProducer.get() );
	}

	private Integer getCacheWithActualValue(StatsNamedContainer<Integer> statsNamedContainer) {
		return statsNamedContainer.getOrCompute(
				"key",
				StatsNamedContainerNullComputedValueTest::produceValue
		);
	}

	private Integer getCacheWithNullValue(StatsNamedContainer<Integer> statsNamedContainer) {
		return statsNamedContainer.getOrCompute(
				"key",
				StatsNamedContainerNullComputedValueTest::produceNull
		);
	}

	private static Integer produceValue(Object o) {
		invocationCounterValueProducer.getAndIncrement();
		return 5;
	}

	private static Integer produceNull(Object v) {
		invocationCounterNullProducer.getAndIncrement();
		return null;
	}

}
