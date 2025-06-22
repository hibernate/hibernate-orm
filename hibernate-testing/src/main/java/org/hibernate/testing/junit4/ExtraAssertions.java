/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.junit4;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public final class ExtraAssertions {
	private ExtraAssertions() {
	}

	public static void assertClassAssignability(Class<?> expected, Class<?> actual) {
		if ( !expected.isAssignableFrom( actual ) ) {
			Assert.fail(
					"Expected class [" + expected.getName() + "] was not assignable from actual [" +
							actual.getName() + "]"
			);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T assertTyping(Class<T> expectedType, Object value) {
		if ( !expectedType.isInstance( value ) ) {
			Assert.fail(
					String.format(
							"Expecting value of type [%s], but found [%s]",
							expectedType.getName(),
							value == null ? "<null>" : value
					)
			);
		}
		return (T) value;
	}

	public static void assertJdbcTypeCode(int expected, int actual) {
		if ( expected != actual ) {
			final String message = String.format(
					"JDBC type codes did not match...\n" +
							"Expected: %s (%s)\n" +
							"Actual  : %s (%s)",
					jdbcTypeCodeMap().get( expected ),
					expected,
					jdbcTypeCodeMap().get( actual ),
					actual
			);
			fail( message );
		}
	}

	public static void assertJdbcTypeCode(int[] expected, int actual) {
		for ( int code : expected ) {
			if ( code == actual ) {
				return;
			}
		}
		final String message = String.format(
				"JDBC type codes did not match...\n" +
						"Actual  : %s (%s)",
				jdbcTypeCodeMap().get( actual ),
				actual
		);
		fail( message );
	}

	private static Map<Integer, String> jdbcTypeCodeMap;

	private static synchronized Map<Integer, String> jdbcTypeCodeMap() {
		if (jdbcTypeCodeMap == null) {
			jdbcTypeCodeMap = generateJdbcTypeCache();
		}
		return jdbcTypeCodeMap;
	}

	private static Map generateJdbcTypeCache() {
		final Field[] fields = Types.class.getFields();
		Map cache = new HashMap( (int)( fields.length * .75 ) + 1 );
		for ( Field field : fields ) {
			if ( Modifier.isStatic( field.getModifiers() ) ) {
				try {
					cache.put( field.get( null ), field.getName() );
				}
				catch ( Throwable ignore ) {
				}
			}
		}
		return cache;
	}
}
