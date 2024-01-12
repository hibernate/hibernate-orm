/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.javatime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.DateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.type.descriptor.DateTimeUtils.roundToDefaultPrecision;

/**
 * Tests for "direct" JDBC handling of {@linkplain java.time Java Time} types.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(
		settings = @Setting(name = MappingSettings.PREFER_JAVA_TYPE_JDBC_TYPES, value = "false")
)
@DomainModel( annotatedClasses = JavaTimeJdbcTypeBaselineTests.EntityWithJavaTimeValues.class )
@SessionFactory
public class JavaTimeJdbcTypeBaselineTests {
	@Test
	void testMappings(DomainModelScope scope) {
		final PersistentClass entityBinding = scope.getEntityBinding( EntityWithJavaTimeValues.class );

		checkAttribute( entityBinding, "theLocalDate", DateJdbcType.class );
		checkAttribute( entityBinding, "theLocalDateTime", TimestampJdbcType.class );
		checkAttribute( entityBinding, "theLocalTime", TimeJdbcType.class );
	}

	private void checkAttribute(
			PersistentClass entityBinding,
			String attributeName,
			Class<?> expectedJdbcTypeDescriptorType) {
		final Property property = entityBinding.getProperty( attributeName );
		final BasicValue value = (BasicValue) property.getValue();
		final BasicValue.Resolution<?> resolution = value.resolve();
		final JdbcType jdbcType = resolution.getJdbcType();
		assertThat( jdbcType ).isInstanceOf( expectedJdbcTypeDescriptorType );
	}

	@Entity(name="EntityWithJavaTimeValues")
	@Table(name="EntityWithJavaTimeValues")
	public static class EntityWithJavaTimeValues {
		@Id
		private Integer id;
		private String name;

		private Instant theInstant;

		private LocalDateTime theLocalDateTime;

		private LocalDate theLocalDate;

		private LocalTime theLocalTime;
	}
}
