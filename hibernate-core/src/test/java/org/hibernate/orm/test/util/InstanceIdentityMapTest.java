/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util;

import org.hibernate.engine.spi.InstanceIdentity;
import org.hibernate.internal.util.collections.InstanceIdentityMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class InstanceIdentityMapTest {
	private final InstanceIdentityMap<TestInstance, String> testMap = new InstanceIdentityMap<>();

	private static final class TestInstance implements InstanceIdentity {
		private int instanceId;

		public TestInstance(int instanceId) {
			this.instanceId = instanceId;
		}

		@Override
		public void $$_hibernate_setInstanceId(int instanceId) {
			this.instanceId = instanceId;
		}

		@Override
		public int $$_hibernate_getInstanceId() {
			return instanceId;
		}
	}

	@AfterEach
	public void setUp() {
		testMap.clear();
	}

	@Test
	public void testSimpleMapOperations() {
		final TestInstance i1 = new TestInstance( 1 );
		final TestInstance i2 = new TestInstance( 2 );
		final TestInstance i3 = new TestInstance( 3 );
		testMap.put( i1, "instance_1" );
		testMap.putIfAbsent( i2, "instance_2" );
		//noinspection RedundantCollectionOperation
		testMap.putAll( Map.of( i3, "instance_3" ) );
		assertThat( testMap ).hasSize( 3 ).containsKeys( i1, i2, i3 )
				.containsValues( "instance_1", "instance_2", "instance_3" );

		testMap.remove( i1 );
		assertThat( testMap ).hasSize( 2 ).doesNotContainKeys( i1 );

		testMap.remove( i2 );
		assertThat( testMap ).hasSize( 1 ).doesNotContainKeys( i1, i2 );

		final TestInstance i3New = new TestInstance( 3 );
		testMap.put( i3New, "new_instance_3" );
		assertThat( testMap ).hasSize( 1 ).containsExactly( entry( i3New, "new_instance_3" ) );

		testMap.clear();
		assertThat( testMap ).isEmpty();
	}

	@Test
	public void testMapIteration() {
		for ( int i = 1; i <= 100; i++ ) {
			testMap.put( new TestInstance( i ), "instance_" + i );
		}

		assertThat( testMap ).hasSize( 100 );

		final AtomicInteger count = new AtomicInteger();
		testMap.forEach( (k, v) -> {
			assertThat( k.$$_hibernate_getInstanceId() ).isBetween( 1, 100 );
			assertThat( v ).isEqualTo( "instance_" + k.$$_hibernate_getInstanceId() );
			count.getAndIncrement();
		} );
		assertThat( count.get() ).isEqualTo( 100 );
	}

	@Test
	public void testSets() {
		final Map<TestInstance, String> map = new HashMap<TestInstance, String>();
		for ( int i = 1; i <= 100; i++ ) {
			map.put( new TestInstance( i ), "instance_" + i );
		}

		testMap.putAll( map );
		assertThat( testMap ).hasSize( 100 );

		assertThat( testMap.keySet() ).hasSize( 100 ).containsAll( map.keySet() );
		assertThat( testMap.values() ).hasSize( 100 ).containsAll( map.values() );
		assertThat( testMap.entrySet() ).hasSize( 100 ).containsAll( map.entrySet() );
	}
}
