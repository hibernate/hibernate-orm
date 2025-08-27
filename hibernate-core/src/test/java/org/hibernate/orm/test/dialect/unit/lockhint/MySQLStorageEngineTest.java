/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.lockhint;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Properties;

@RequiresDialect(MySQLDialect.class)
public class MySQLStorageEngineTest extends BaseUnitTestCase {

	@Test
	public void testDefaultStorage() {
		assertEquals( " engine=InnoDB", new MySQLDialect().getTableTypeString() );
	}

	@Test
	public void testOverrideStorage() throws NoSuchFieldException, IllegalAccessException {
		final Field globalPropertiesField = Environment.class.getDeclaredField( "GLOBAL_PROPERTIES" );
		globalPropertiesField.setAccessible( true );
		final Properties systemProperties = (Properties) globalPropertiesField.get( null );
		assertNotNull( systemProperties );
		final Object previousValue = systemProperties.setProperty( AvailableSettings.STORAGE_ENGINE, "myisam" );
		try {
			assertEquals( " engine=MyISAM", new MySQLDialect().getTableTypeString() );
		}
		finally {
			if ( previousValue != null ) {
				systemProperties.setProperty( AvailableSettings.STORAGE_ENGINE, previousValue.toString() );
			}
			else {
				systemProperties.remove( AvailableSettings.STORAGE_ENGINE );
			}
		}
	}

}
