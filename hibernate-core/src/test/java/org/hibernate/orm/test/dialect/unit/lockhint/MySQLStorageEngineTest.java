/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.lockhint;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RequiresDialect(MySQLDialect.class)
@BaseUnitTest
public class MySQLStorageEngineTest {

	@Test
	public void testDefaultStorage() {
		assertThat( new MySQLDialect().getTableTypeString() ).isEqualTo( " engine=InnoDB" );
	}

	@Test
	public void testOverrideStorage() throws NoSuchFieldException, IllegalAccessException {
		final Field globalPropertiesField = Environment.class.getDeclaredField( "GLOBAL_PROPERTIES" );
		globalPropertiesField.setAccessible( true );
		final Properties systemProperties = (Properties) globalPropertiesField.get( null );
		assertThat( systemProperties ).isNotNull();
		final Object previousValue = systemProperties.setProperty( AvailableSettings.STORAGE_ENGINE, "myisam" );
		try {
			assertThat( new MySQLDialect().getTableTypeString() ).isEqualTo( " engine=MyISAM" );
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

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {@Setting(name = AvailableSettings.STORAGE_ENGINE, value = "myisam")})
	@DomainModel(annotatedClasses = {TestEntity.class})
	public void testOverrideStorageWithConfigurationProperties(SessionFactoryScope scope) {
		Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		assertThat( dialect.getTableTypeString() ).isEqualTo( " engine=MyISAM" );
	}

	@Test
	public void testOverrideStorageEngineConfigurationPropertyHasPrecedenceOverSystemProperty()
			throws Exception {
		final Field globalPropertiesField = Environment.class.getDeclaredField( "GLOBAL_PROPERTIES" );
		globalPropertiesField.setAccessible( true );
		final Properties systemProperties = (Properties) globalPropertiesField.get( null );
		assertThat( systemProperties ).isNotNull();
		final Object previousValue = systemProperties.setProperty( AvailableSettings.STORAGE_ENGINE, "myisam" );
		try {
			final Map<String, Object> configurationValues = new HashMap<>();
			configurationValues.put(  AvailableSettings.STORAGE_ENGINE, "innodb" );
			Dialect dialect = new MySQLDialect(
					TestingDialectResolutionInfo.forDatabaseInfo(
							"MySQL",
							null,
							DatabaseVersion.NO_VERSION,
							DatabaseVersion.NO_VERSION,
							configurationValues ) );
			assertThat( dialect.getTableTypeString() ).isEqualTo( " engine=InnoDB" );
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

	@Entity(name = "TestEntity")
	public static class TestEntity{
		@Id
		private Long id;

		private String name;
	}

}
