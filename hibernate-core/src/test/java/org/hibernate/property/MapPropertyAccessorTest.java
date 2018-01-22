package org.hibernate.property;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;

import org.hibernate.mapping.Map;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

public class MapPropertyAccessorTest extends BaseUnitTestCase {
	
	/**
	 * Tests the basic functionality of the property access.
	 */
	@Test
	public void testBasicMapClass() {
		testBasic(Map.class);
	}
	
	/**
	 * Ensures {@link PropertyAccessStrategyMapImpl#buildPropertyAccess(Class, String)} does not require a Map argument.
	 */
	@Test
	public void testBasicNullClass() {
		testBasic(null);
	}

	private void testBasic(final Class<?> clazz) {
		
		final String key = "testKey";
		final String value = "testValue";

		final PropertyAccessStrategyMapImpl accessStrategy = PropertyAccessStrategyMapImpl.INSTANCE;

		final PropertyAccess access =
				// Intentially pass null for the Class as required by the method contract.
				accessStrategy.buildPropertyAccess(clazz, key);
		
		final HashMap<String, String> map = new HashMap<>();

		access.getSetter().set(map, value, null);
		assertEquals(value, map.get(key));
		assertEquals(value, access.getGetter().get(map));
	}
	
	@Test(expected = Exception.class)
	public void testNonMap() {
		final PropertyAccessStrategyMapImpl accessStrategy = PropertyAccessStrategyMapImpl.INSTANCE;
		
		// Just trying to build the accessor should throw an exception.
		accessStrategy.buildPropertyAccess(Date.class, "time");
	}

}
