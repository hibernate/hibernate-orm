/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.property;

import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@BaseUnitTest
public class PropertyAccessStrategyMapTest {

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
		IllegalArgumentException exception = assertThrows( IllegalArgumentException.class, () ->
				accessStrategy.buildPropertyAccess( Date.class, "time", true )
		);
		assertThat( exception.getMessage() ).isEqualTo(
				"Expecting class: [java.util.Map], but containerJavaType is of type: [java.util.Date] for propertyName: [time]"
		);
	}

	private void testBasic(final Class<?> clazz) {

		final String key = "testKey";
		final String value = "testValue";

		final var accessStrategy = PropertyAccessStrategyMapImpl.INSTANCE;
		final PropertyAccess access = accessStrategy.buildPropertyAccess( clazz, key, true );

		final HashMap<String, String> map = new HashMap<>();

		Setter setter = access.getSetter();
		assertThat( setter ).isNotNull();
		setter.set( map, value );
		assertThat( map.get( key ) ).isEqualTo( value );
		assertThat( access.getGetter().get( map ) ).isEqualTo( value );
	}
}
