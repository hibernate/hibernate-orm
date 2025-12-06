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
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;

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
	private final String json;

	protected JsonJavaTimeMappingTests() {
		final var localDate = LocalDate.of( 2025, 12, 1 );
		final var localTime = LocalTime.of( 9, 9, 42 );
		final var localDateTime = LocalDateTime.of( localDate, localTime );
		final var instant = localDateTime.atZone( ZoneId.of( "Europe/Paris" ) ).toInstant();
		final var duration = Duration.ofHours( 3 ).plusMinutes( 14 );
		javaTime = new JavaTime( instant, localDateTime, localDate, localTime, duration );
		this.json = "{\"instant\":1764576582.000000000,\"localDateTime\":[2025,12,1,9,9,42],\"localDate\":[2025,12,1],\"localTime\":[9,9,42],\"duration\":11640.000000000}";
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

	@Test
	@SkipForDialect(dialectClass = DerbyDialect.class,
			reason = "Derby doesn't support comparing CLOBs with the = operator")
	@SkipForDialect(dialectClass = HANADialect.class,
			reason = "HANA doesn't support comparing LOBs with the = operator")
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true,
			reason = "Sybase doesn't support comparing LOBs with the = operator")
	@SkipForDialect(dialectClass = OracleDialect.class,
			reason = "Oracle doesn't support comparing JSON with the = operator")
	@SkipForDialect(dialectClass = AltibaseDialect.class,
			reason = "Altibase doesn't support comparing CLOBs with the = operator")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Blobs are not allowed in this expression")
	public void verifyComparisonWorks(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// PostgreSQL returns the JSON slightly formatted
					String alternativePostgreSQLJson =
							"{\"instant\": 1764576582.000000000, \"duration\": 11640.000000000, \"localDate\": [2025, 12, 1], \"localTime\": [9, 9, 42], \"localDateTime\": [2025, 12, 1, 9, 9, 42]}";
					String alternativeMariaDBJson =
							"{\"instant\": 1764576582.000000000, \"localDateTime\": [2025, 12, 1, 9, 9, 42], \"localDate\": [2025, 12, 1], \"localTime\": [9, 9, 42], \"duration\": 11640.000000000}";
					String alternativeMySQLJson =
							"{\"instant\": 1764576582.0, \"duration\": 11640.0, \"localDate\": [2025, 12, 1], \"localTime\": [9, 9, 42], \"localDateTime\": [2025, 12, 1, 9, 9, 42]}";
					EntityWithJson entityWithJson = session.createQuery(
									"from EntityWithJson e where e.javaTime = :param",
									EntityWithJson.class
							)
							.setParameter( "param", javaTime )
							.getSingleResult();
					assertThat( entityWithJson, notNullValue() );
					assertThat( entityWithJson.javaTime, is( javaTime ) );
					Object nativeJson = session.createNativeQuery(
									"select javaTime from EntityWithJson",
									Object.class
							)
							.getResultList()
							.get( 0 );
					final String jsonText;
					try {
						if ( nativeJson instanceof Blob blob ) {
							jsonText = new String(
									blob.getBytes( 1L, (int) blob.length() ),
									StandardCharsets.UTF_8
							);
						}
						else if ( nativeJson instanceof Clob jsonClob ) {
							jsonText = jsonClob.getSubString( 1L, (int) jsonClob.length() );
						}
						else {
							jsonText = (String) nativeJson;
						}
					}
					catch (Exception e) {
						throw new RuntimeException( e );
					}
					assertThat( jsonText, is( oneOf( json, alternativePostgreSQLJson, alternativeMariaDBJson, alternativeMySQLJson ) ) );
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
