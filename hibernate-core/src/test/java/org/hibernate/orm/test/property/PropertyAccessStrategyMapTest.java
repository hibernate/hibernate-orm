/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.property;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PropertyAccessStrategyMapTest extends BaseUnitTestCase {

	@Test
	public void testBasicMapClass() {
		testBasic( Map.class );
	}

	@Test
	public void testBasicNullClass() {
		testBasic( null );
	}

	@Test
	public void testNonMap() {
		final var accessStrategy = PropertyAccessStrategyMapImpl.INSTANCE;

		try {
			accessStrategy.buildPropertyAccess( Date.class, "time", true );

			fail("Should throw IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			assertEquals(
				"Expecting class: [java.util.Map], but containerJavaType is of type: [java.util.Date] for propertyName: [time]",
				e.getMessage()
			);
		}
	}

	private void testBasic(final Class<?> clazz) {

		final String key = "testKey";
		final String value = "testValue";

		final var accessStrategy = PropertyAccessStrategyMapImpl.INSTANCE;
		final PropertyAccess access = accessStrategy.buildPropertyAccess( clazz, key, true );

		final HashMap<String, String> map = new HashMap<>();

		access.getSetter().set( map, value );
		assertEquals( value, map.get( key ) );
		assertEquals( value, access.getGetter().get( map ) );
	}
}
