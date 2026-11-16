package org.hibernate.orm.test.type.temporal.preservation;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.MappingException;
import org.hibernate.TemporalPreservationException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.type.spi.TemporalPreservation;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;

import org.hibernate.type.descriptor.jdbc.TimestampUtcAsInstantJdbcType;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests which mappings and operations are accepted and which are rejected
/// with temporal preservation enabled, and that nothing is rejected with it
/// disabled.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalPreservationMappingTests {
	private static final Map<String, Object> ENABLED = Map.of( MappingSettings.TEMPORAL_PRESERVATION, true );
	private static final Map<String, Object> DISABLED = Map.of( MappingSettings.TEMPORAL_PRESERVATION, false );
	private static final Map<String, Object> UNSET = Map.of();

	@Test
	void supportedRolesAreAccepted() {
		assertAccepted( ENABLED, SupportedRoles.class );
	}

	@Test
	void typesWithOffsetOrZoneAreRejected() {
		assertRejected( ENABLED, WithOffsetDateTime.class, "WithOffsetDateTime.theValue", "INSTANT" );
		assertRejected( ENABLED, WithOffsetTime.class, "WithOffsetTime.theValue", "TIME" );
		assertRejected( ENABLED, WithZonedDateTime.class, "WithZonedDateTime.theValue", "INSTANT" );
	}

	@Test
	void arraysAreRejected() {
		assertRejected( ENABLED, WithArray.class, "WithArray.theValues", "arrays and collections" );
	}

	@Test
	void conversionThroughJavaSqlTypesIsRejected() {
		assertRejected( ENABLED, WithJavaSqlDate.class, "WithJavaSqlDate.theValue", "DateJdbcType" );
	}

	@Test
	void collectionElementsAreChecked() {
		assertRejected( ENABLED, WithOffsetElements.class, "WithOffsetElements.theValues.<element>", "INSTANT" );
	}

	@Test
	void embeddableMembersAreChecked() {
		assertRejected( ENABLED, WithOffsetMember.class, "WithOffsetMember.embedded.theValue", "INSTANT" );
	}

	@Test
	void membersOfAggregatedEmbeddablesAreRejected() {
		assertRejected(
				ENABLED,
				WithAggregate.class,
				"WithAggregate.aggregated.theMember",
				"aggregated into a single column"
		);
		assertAccepted( DISABLED, WithAggregate.class );
	}

	@Test
	void identifiersAndVersionsAreChecked() {
		assertRejected( ENABLED, WithOffsetId.class, "WithOffsetId.<id>", "INSTANT" );
		assertRejected( ENABLED, WithOffsetVersion.class, "WithOffsetVersion.version", "INSTANT" );
	}

	/// The value an attribute converter produces for the database is what is
	/// stored. A converter producing something other than a Java Time value
	/// takes the attribute out of temporal preservation altogether.
	@Test
	void convertedValuesAreCheckedByWhatIsStored() {
		assertAccepted( ENABLED, WithConversionToString.class );
		assertRejected(
				ENABLED,
				WithConversionToOffsetDateTime.class,
				"WithConversionToOffsetDateTime.theValue",
				"INSTANT"
		);
	}

	@Test
	void unassessedDialectIsRejected() {
		final Map<String, Object> settings = Map.of(
				MappingSettings.TEMPORAL_PRESERVATION, true,
				JdbcSettings.DIALECT, UnassessedDialect.class.getName()
		);
		assertRejected( settings, WithLocalDate.class, "WithLocalDate.theValue", "has not been established" );
	}

	@Test
	void knownLossIsRejected() {
		final Map<String, Object> settings = Map.of(
				MappingSettings.TEMPORAL_PRESERVATION, true,
				JdbcSettings.DIALECT, LosingDialect.class.getName()
		);
		assertRejected( settings, WithLocalDate.class, "WithLocalDate.theValue", "is known to lose the DATE part" );
	}

	/// Asking for direct JDBC access is not asking for temporal preservation:
	/// the same JDBC types are selected, and nothing is rejected.
	@Test
	void directJdbcSettingAloneRejectsNothing() {
		final Map<String, Object> directOnly = Map.of(
				MappingSettings.TEMPORAL_PRESERVATION, false,
				MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, true
		);
		for ( var entityClass : new Class<?>[] {
				WithOffsetDateTime.class,
				WithOffsetTime.class,
				WithZonedDateTime.class,
				WithArray.class,
				WithAggregate.class
		} ) {
			assertAccepted( directOnly, entityClass );
		}
		assertAccepted(
				Map.of(
						MappingSettings.TEMPORAL_PRESERVATION, false,
						MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, true,
						JdbcSettings.DIALECT, UnassessedDialect.class.getName()
				),
				WithLocalDate.class
		);
	}

	/// The H2 descriptor for an `Instant` retries through a `java.sql.Timestamp`
	/// when the driver fails on the `Instant` itself. That path has not been
	/// assessed, so with temporal preservation enabled it is not taken.
	@Test
	void unassessedDriverFallbackIsNotTaken() {
		final var binder = TimestampUtcAsInstantJdbcType.INSTANCE.getBinder( InstantJavaType.INSTANCE );
		final var extractor = TimestampUtcAsInstantJdbcType.INSTANCE.getExtractor( InstantJavaType.INSTANCE );
		final Instant value = Instant.parse( "2021-03-14T07:30:00Z" );
		final List<String> calls = new ArrayList<>();
		final var statement = (PreparedStatement) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { PreparedStatement.class },
				(proxy, method, arguments) -> {
					calls.add( method.getName() );
					if ( method.getName().equals( "setObject" ) ) {
						throw new SQLException( "not supported" );
					}
					return null;
				}
		);
		final var resultSet = (ResultSet) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { ResultSet.class },
				(proxy, method, arguments) -> switch ( method.getName() ) {
					case "getObject" -> throw new SQLException( "not supported" );
					case "getTimestamp" -> Timestamp.from( value );
					case "wasNull" -> false;
					default -> throw new UnsupportedOperationException( method.getName() );
				}
		);
		inFactory( ENABLED, SupportedRoles.class, factory -> factory.inSession( session -> {
			assertThatThrownBy( () -> binder.bind( statement, value, 1, options( session ) ) )
					.isInstanceOf( TemporalPreservationException.class )
					.hasCauseInstanceOf( SQLException.class );
			assertThat( calls ).containsExactly( "setObject" );
			assertThatThrownBy( () -> extractor.extract( resultSet, 1, options( session ) ) )
					.isInstanceOf( TemporalPreservationException.class )
					.hasCauseInstanceOf( SQLException.class );
		} ) );
		calls.clear();
		inFactory( DISABLED, SupportedRoles.class, factory -> factory.inSession( session -> {
			try {
				binder.bind( statement, value, 1, options( session ) );
				assertThat( calls ).containsExactly( "setObject", "setTimestamp" );
				assertThat( extractor.extract( resultSet, 1, options( session ) ) ).isEqualTo( value );
			}
			catch (SQLException e) {
				throw new AssertionError( e );
			}
		} ) );
	}

	@Test
	@DisabledIfSystemProperty(
		named = "hibernate.type.temporal_preservation",
		matches = "true",
		disabledReason = "Verifies the behavior with the setting left unset"
	)
	void nothingIsRejectedWhenDisabled() {
		for ( var settings : new Map[] { UNSET, DISABLED } ) {
			for ( var entityClass : new Class<?>[] {
					SupportedRoles.class,
					WithOffsetDateTime.class,
					WithOffsetTime.class,
					WithZonedDateTime.class,
					WithArray.class,
					WithJavaSqlDate.class,
					WithOffsetElements.class,
					WithOffsetMember.class,
					WithOffsetVersion.class,
					WithConversionToOffsetDateTime.class
			} ) {
				assertAccepted( settings, entityClass );
			}
		}
		assertAccepted(
				Map.of( JdbcSettings.DIALECT, UnassessedDialect.class.getName() ),
				WithLocalDate.class
		);
	}

	@Test
	void unsupportedParameterIsRejectedWhenBound() {
		inFactory( ENABLED, SupportedRoles.class, factory -> {
			for ( var value : new Object[] {
					OffsetDateTime.now(),
					ZonedDateTime.now(),
					OffsetTime.now(),
					new LocalDate[] { LocalDate.now() }
			} ) {
				assertThatThrownBy( () -> factory.inTransaction( session -> session.createNativeQuery(
						"select count(*) from SupportedRoles where ? is not null",
						Object.class
				).setParameter( 1, value ).getSingleResult() ) )
						.as( "parameter of %s", value.getClass() )
						.isInstanceOf( TemporalPreservationException.class )
						.hasMessageContaining( MappingSettings.TEMPORAL_PRESERVATION );
			}
		} );
		inFactory( DISABLED, SupportedRoles.class, factory -> factory.inTransaction( session -> assertThat(
				session.createNativeQuery( "select count(*) from SupportedRoles where ? is not null", Object.class )
						.setParameter( 1, OffsetDateTime.now() )
						.getSingleResult()
		).isNotNull() ) );
	}

	@Test
	void outputParametersAreRejected() {
		final var extractor = LocalDateJdbcType.INSTANCE.getExtractor( LocalDateJavaType.INSTANCE );
		final var statement = (CallableStatement) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { CallableStatement.class },
				(proxy, method, arguments) -> switch ( method.getName() ) {
					case "getObject" -> LocalDate.of( 2021, 3, 14 );
					case "wasNull" -> false;
					default -> throw new UnsupportedOperationException( method.getName() );
				}
		);
		inFactory( ENABLED, SupportedRoles.class, factory -> factory.inSession( session -> {
			assertThatThrownBy( () -> extractor.extract( statement, 1, options( session ) ) )
					.isInstanceOf( TemporalPreservationException.class )
					.hasMessageContaining( "output parameter" );
			assertThatThrownBy( () -> extractor.extract( statement, "theValue", options( session ) ) )
					.isInstanceOf( TemporalPreservationException.class )
					.hasMessageContaining( "output parameter" );
		} ) );
		inFactory( DISABLED, SupportedRoles.class, factory -> factory.inSession( session -> {
			try {
				assertThat( extractor.extract( statement, 1, options( session ) ) ).isEqualTo( LocalDate.of( 2021, 3, 14 ) );
			}
			catch (java.sql.SQLException e) {
				throw new AssertionError( e );
			}
		} ) );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private static WrapperOptions options(Object session) {
		return (SharedSessionContractImplementor) session;
	}

	private static void assertAccepted(Map<String, Object> settings, Class<?> entityClass) {
		inFactory( settings, entityClass, factory -> {
		} );
	}

	private static void assertRejected(
			Map<String, Object> settings,
			Class<?> entityClass,
			String role,
			String reason) {
		assertThatThrownBy( () -> assertAccepted( settings, entityClass ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( role )
				.hasMessageContaining( reason )
				.hasMessageContaining( MappingSettings.TEMPORAL_PRESERVATION );
	}

	private static void inFactory(
			Map<String, Object> settings,
			Class<?> entityClass,
			java.util.function.Consumer<SessionFactoryImplementor> action) {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "create-drop" );
		settings.forEach( registryBuilder::applySetting );
		try ( var registry = registryBuilder.build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( entityClass ).buildMetadata();
			try ( var factory = (SessionFactoryImplementor) metadata.buildSessionFactory() ) {
				action.accept( factory );
			}
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Entity(name = "SupportedRoles")
	static class SupportedRoles {
		@Id
		LocalDate id;
		@Version
		Instant version;
		LocalDateTime basic;
		@Embedded
		LocalMember embedded;
		@ElementCollection
		Set<LocalDate> elements;
		@ElementCollection
		Map<LocalDate, Instant> entries;
	}

	@Embeddable
	static class LocalMember {
		LocalDate theMember;
	}

	@Entity(name = "WithLocalDate")
	static class WithLocalDate {
		@Id
		Integer id;
		LocalDate theValue;
	}

	@Entity(name = "WithOffsetDateTime")
	static class WithOffsetDateTime {
		@Id
		Integer id;
		OffsetDateTime theValue;
	}

	@Entity(name = "WithOffsetTime")
	static class WithOffsetTime {
		@Id
		Integer id;
		OffsetTime theValue;
	}

	@Entity(name = "WithZonedDateTime")
	static class WithZonedDateTime {
		@Id
		Integer id;
		ZonedDateTime theValue;
	}

	@Entity(name = "WithArray")
	static class WithArray {
		@Id
		Integer id;
		LocalDate[] theValues;
	}

	@Entity(name = "WithJavaSqlDate")
	static class WithJavaSqlDate {
		@Id
		Integer id;
		@JdbcTypeCode(Types.DATE)
		LocalDate theValue;
	}

	@Entity(name = "WithOffsetElements")
	static class WithOffsetElements {
		@Id
		Integer id;
		@ElementCollection
		Set<OffsetDateTime> theValues;
	}

	@Entity(name = "WithOffsetMember")
	static class WithOffsetMember {
		@Id
		Integer id;
		@Embedded
		OffsetMember embedded;
	}

	@Embeddable
	static class OffsetMember {
		OffsetDateTime theValue;
	}

	@Entity(name = "WithAggregate")
	static class WithAggregate {
		@Id
		Integer id;
		@JdbcTypeCode(SqlTypes.JSON)
		LocalMember aggregated;
	}

	@Entity(name = "WithOffsetId")
	static class WithOffsetId {
		@Id
		OffsetDateTime id;
	}

	@Entity(name = "WithOffsetVersion")
	static class WithOffsetVersion {
		@Id
		Integer id;
		@Version
		OffsetDateTime version;
	}

	@Entity(name = "WithConversionToString")
	static class WithConversionToString {
		@Id
		Integer id;
		@Convert(converter = OffsetDateTimeToString.class)
		OffsetDateTime theValue;
	}

	@Entity(name = "WithConversionToOffsetDateTime")
	static class WithConversionToOffsetDateTime {
		@Id
		Integer id;
		@Convert(converter = StringToOffsetDateTime.class)
		String theValue;
	}

	static class OffsetDateTimeToString implements AttributeConverter<OffsetDateTime, String> {
		@Override
		public String convertToDatabaseColumn(OffsetDateTime attribute) {
			return attribute == null ? null : attribute.toString();
		}

		@Override
		public OffsetDateTime convertToEntityAttribute(String dbData) {
			return dbData == null ? null : OffsetDateTime.parse( dbData );
		}
	}

	static class StringToOffsetDateTime implements AttributeConverter<String, OffsetDateTime> {
		@Override
		public OffsetDateTime convertToDatabaseColumn(String attribute) {
			return attribute == null ? null : OffsetDateTime.parse( attribute );
		}

		@Override
		public String convertToEntityAttribute(OffsetDateTime dbData) {
			return dbData == null ? null : dbData.toString();
		}
	}

	public static class UnassessedDialect extends H2Dialect {
		@Override
		public TemporalPreservation getTemporalPreservation(JavaType<?> javaType, JdbcType jdbcType) {
			return TemporalPreservation.unknown();
		}
	}

	public static class LosingDialect extends H2Dialect {
		@Override
		public TemporalPreservation getTemporalPreservation(JavaType<?> javaType, JdbcType jdbcType) {
			return new TemporalPreservation(
					TemporalPreservation.Level.UNSUPPORTED,
					TemporalPreservation.Level.UNSUPPORTED,
					TemporalPreservation.Level.UNSUPPORTED,
					TemporalPreservation.Level.UNSUPPORTED,
					TemporalPreservation.Level.UNSUPPORTED,
					null,
					null,
					null
			);
		}
	}
}
