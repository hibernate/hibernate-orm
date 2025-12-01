/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.nullValue;

@DomainModel(annotatedClasses = JsonJavaTimeMappingTests.EntityWithJson.class)
@SessionFactory
public abstract class JsonJavaTimeMappingTests {

	@ServiceRegistry(settings = @Setting(name = AvailableSettings.JSON_FORMAT_MAPPER, value = "jackson"))
	public static class Jackson extends JsonJavaTimeMappingTests {

		public Jackson() {
			super();
		}
	}

	@ServiceRegistry(settings = @Setting(name = AvailableSettings.JSON_FORMAT_MAPPER, value = "jackson3"))
	public static class Jackson3 extends JsonJavaTimeMappingTests {

		public Jackson3() {
			super();
		}
	}

	private final JavaTime javaTime;

	protected JsonJavaTimeMappingTests() {
		final var localDate = LocalDate.of( 2025, 12, 1 );
		final var localTime = LocalTime.of( 9, 9, 42 );
		final var localDateTime = LocalDateTime.of( localDate, localTime );
		final var instant = localDateTime.atZone( ZoneId.of( "Europe/Paris" ) ).toInstant();
		final var duration = Duration.ofHours( 3 ).plusMinutes( 14 );
		javaTime = new JavaTime( instant, localDateTime, localDate, localTime, duration );
	}

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.persist( new EntityWithJson( 1, javaTime ) );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( EntityWithJson.class );
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		final BasicAttributeMapping stringMapAttribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping(
				"javaTime" );

		assertThat( stringMapAttribute.getJavaType().getJavaTypeClass(), equalTo( JavaTime.class ) );

		final JdbcType jsonType = jdbcTypeRegistry.getDescriptor( SqlTypes.JSON );
		assertThat( stringMapAttribute.getJdbcMapping().getJdbcType(), isA( jsonType.getClass() ) );
	}

	@Test
	public void verifyReadWorks(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					EntityWithJson entityWithJson = session.find( EntityWithJson.class, 1 );
					assertThat( entityWithJson.javaTime, is( javaTime ) );
				}
		);
	}

	@Test
	public void verifyMergeWorks(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.merge( new EntityWithJson( 2, null ) )
		);

		scope.inTransaction(
				(session) -> {
					EntityWithJson entityWithJson = session.find( EntityWithJson.class, 2 );
					assertThat( entityWithJson.javaTime, is( nullValue() ) );
				}
		);
	}

	public record JavaTime(Instant instant, LocalDateTime localDateTime, LocalDate localDate, LocalTime localTime,
						Duration duration) implements Serializable {
	}

	@Entity(name = "EntityWithJson")
	@Table(name = "EntityWithJson")
	public static class EntityWithJson {
		@Id
		private Integer id;

		@JdbcTypeCode(SqlTypes.JSON)
		private JavaTime javaTime;

		public EntityWithJson() {
		}

		public EntityWithJson(Integer id, JavaTime javaTime) {
			this.id = id;
			this.javaTime = javaTime;
		}
	}
}
