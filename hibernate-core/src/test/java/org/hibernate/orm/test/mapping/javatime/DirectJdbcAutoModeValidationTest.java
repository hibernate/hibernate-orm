package org.hibernate.orm.test.mapping.javatime;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MappingSettings;

import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies that {@code hibernate.type.java_time_use_direct_jdbc=auto} (the new default)
/// raises a {@link MappingException} when a mapping uses direct Java Time JDBC types
/// in combination with settings that direct access cannot honor.
@Jira("https://hibernate.atlassian.net/browse/HHH-0000")
class DirectJdbcAutoModeValidationTest {

	@Test
	void jdbcTimeZoneWithLocalDateTime() {
		var ssr = new StandardServiceRegistryBuilder()
				.applySetting( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, "auto" )
				.applySetting( AvailableSettings.JDBC_TIME_ZONE, "GMT+5" )
				.build();
		try {
			var e = assertThrows( MappingException.class, () ->
					new MetadataSources( ssr )
							.addAnnotatedClass( LocalDateTimeEntity.class )
							.buildMetadata()
			);
			assertThat( e.getMessage() ).contains( "Direct Java Time JDBC access" );
			assertThat( e.getMessage() ).contains( AvailableSettings.JDBC_TIME_ZONE );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	void jdbcTimeZoneWithLocalTime() {
		var ssr = new StandardServiceRegistryBuilder()
				.applySetting( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, "auto" )
				.applySetting( AvailableSettings.JDBC_TIME_ZONE, "GMT+5" )
				.build();
		try {
			var e = assertThrows( MappingException.class, () ->
					new MetadataSources( ssr )
							.addAnnotatedClass( LocalTimeEntity.class )
							.buildMetadata()
			);
			assertThat( e.getMessage() ).contains( "Direct Java Time JDBC access" );
			assertThat( e.getMessage() ).contains( AvailableSettings.JDBC_TIME_ZONE );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	void normalizeWithOffsetDateTime() {
		var ssr = new StandardServiceRegistryBuilder()
				.applySetting( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, "auto" )
				.applySetting( AvailableSettings.TIMEZONE_DEFAULT_STORAGE, "NORMALIZE" )
				.build();
		try {
			var e = assertThrows( MappingException.class, () ->
					new MetadataSources( ssr )
							.addAnnotatedClass( OffsetDateTimeEntity.class )
							.buildMetadata()
			);
			assertThat( e.getMessage() ).contains( "Direct Java Time JDBC access" );
			assertThat( e.getMessage() ).contains( "NORMALIZE" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	void explicitTrueBypassesValidation() {
		var ssr = new StandardServiceRegistryBuilder()
				.applySetting( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, "true" )
				.applySetting( AvailableSettings.JDBC_TIME_ZONE, "GMT+5" )
				.build();
		try {
			var metadata = new MetadataSources( ssr )
					.addAnnotatedClass( LocalDateTimeEntity.class )
					.buildMetadata();
			assertThat( metadata ).isNotNull();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	void explicitFalseBypassesValidation() {
		var ssr = new StandardServiceRegistryBuilder()
				.applySetting( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, "false" )
				.applySetting( AvailableSettings.JDBC_TIME_ZONE, "GMT+5" )
				.build();
		try {
			var metadata = new MetadataSources( ssr )
					.addAnnotatedClass( LocalDateTimeEntity.class )
					.buildMetadata();
			assertThat( metadata ).isNotNull();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "LocalDateTimeEntity")
	static class LocalDateTimeEntity {
		@Id
		@GeneratedValue
		Long id;
		LocalDateTime dateTime;
	}

	@Entity(name = "LocalTimeEntity")
	static class LocalTimeEntity {
		@Id
		@GeneratedValue
		Long id;
		LocalTime time;
	}

	@Entity(name = "OffsetDateTimeEntity")
	static class OffsetDateTimeEntity {
		@Id
		@GeneratedValue
		Long id;
		OffsetDateTime dateTime;
	}
}
