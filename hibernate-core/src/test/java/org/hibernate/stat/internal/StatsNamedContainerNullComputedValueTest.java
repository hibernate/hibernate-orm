/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

@TestForIssue(jiraKey = "HHH-13645")
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
