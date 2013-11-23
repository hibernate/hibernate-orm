/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

	public static void assertClassAssignability(Class expected, Class actual) {
		if ( ! expected.isAssignableFrom( actual ) ) {
			Assert.fail(
					"Expected class [" + expected.getName() + "] was not assignable from actual [" +
							actual.getName() + "]"
			);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T assertTyping(Class<T> expectedType, Object value) {
		if ( ! expectedType.isInstance( value ) ) {
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

	private static Map<Integer,String> jdbcTypeCodeMap;

	private static synchronized Map<Integer,String> jdbcTypeCodeMap() {
		if ( jdbcTypeCodeMap == null ) {
			jdbcTypeCodeMap = generateJdbcTypeCache();
		}
		return jdbcTypeCodeMap;
	}

	private static Map generateJdbcTypeCache() {
		final Field[] fields = Types.class.getFields();
		Map cache = new HashMap( (int)( fields.length * .75 ) + 1 );
		for ( int i = 0; i < fields.length; i++ ) {
			final Field field = fields[i];
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
