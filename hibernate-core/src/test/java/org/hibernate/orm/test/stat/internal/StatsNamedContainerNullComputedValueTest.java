/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stat.internal;

import org.hibernate.stat.internal.StatsNamedContainer;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNull;

@JiraKey(value = "HHH-13645")
public class StatsNamedContainerNullComputedValueTest {

	private final static AtomicInteger invocationCounterNullProducer = new AtomicInteger();
	private final static AtomicInteger invocationCounterValueProducer = new AtomicInteger();

	@Test
	public void testNullComputedValue() {
		final StatsNamedContainer statsNamedContainer = new StatsNamedContainer<Integer>();
		assertNull(
				statsNamedContainer.getOrCompute(
						"key",
						v -> {
							return null;
						}
				)
		);
	}

	@Test
	public void abletoStoreNullValues() {
		final StatsNamedContainer statsNamedContainer = new StatsNamedContainer<Integer>();
		Assert.assertEquals( 0, invocationCounterNullProducer.get() );
		assertNull(	getCacheWithNullValue( statsNamedContainer ) );
		Assert.assertEquals( 1, invocationCounterNullProducer.get() );
		assertNull(	getCacheWithNullValue( statsNamedContainer ) );
		Assert.assertEquals( 1, invocationCounterNullProducer.get() );
	}

	@Test
	public void abletoStoreActualValues() {
		final StatsNamedContainer statsNamedContainer = new StatsNamedContainer<Integer>();
		Assert.assertEquals( 0, invocationCounterValueProducer.get() );
		Assert.assertEquals( 5,	getCacheWithActualValue( statsNamedContainer ) );
		Assert.assertEquals( 1, invocationCounterValueProducer.get() );
		Assert.assertEquals( 5,	getCacheWithActualValue( statsNamedContainer ) );
		Assert.assertEquals( 1, invocationCounterValueProducer.get() );
	}

	private Object getCacheWithActualValue(StatsNamedContainer statsNamedContainer) {
		return statsNamedContainer.getOrCompute(
				"key",
				StatsNamedContainerNullComputedValueTest::produceValue
		);
	}

	private Object getCacheWithNullValue(StatsNamedContainer statsNamedContainer) {
		return statsNamedContainer.getOrCompute(
				"key",
				StatsNamedContainerNullComputedValueTest::produceNull
		);
	}

	private static Integer produceValue(Object o) {
		invocationCounterValueProducer.getAndIncrement();
		return Integer.valueOf( 5 );
	}

	private static Integer produceNull(Object v) {
		invocationCounterNullProducer.getAndIncrement();
		return null;
	}

}
