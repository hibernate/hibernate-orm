package org.hibernate.orm.test.type.temporal.preservation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.MappingException;
import org.hibernate.TemporalPreservationException;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationMappingTests.OffsetDateTimeToString;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests `@TimeZoneAdjustment`, which declares the zone of every value of an
/// attribute: the value is stored in a single column on every database, is
/// read back in the declared zone, and a value written in another zone is
/// rejected with the `VALUE` policy and converted with the `NORMALIZED` policy.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(HSQLDialect.class)
@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
@RequiresDialect(PostgresPlusDialect.class)
@RequiresDialect(CockroachDialect.class)
@RequiresDialect(MySQLDialect.class)
@RequiresDialect(SQLServerDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalPreservationAdjustmentTests {
	private static final Map<String, Object> ENABLED = Map.of( MappingSettings.TEMPORAL_PRESERVATION, true );
	private static final Map<String, Object> DISABLED = Map.of( MappingSettings.TEMPORAL_PRESERVATION, false );

	private static final ZoneId NEW_YORK = ZoneId.of( "America/New_York" );
	private static final ZoneId PARIS = ZoneId.of( "Europe/Paris" );
	private static final Instant SUMMER = Instant.parse( "2024-07-01T12:34:56Z" );
	private static final Instant WINTER = Instant.parse( "2024-01-15T03:04:05Z" );

	/// The zone is part of the mapping, so a single column suffices everywhere,
	/// including where a single column keeps neither the zone nor the offset.
	@Test
	void declaredZoneNeedsOnlyOneColumn() {
		for ( var entityClass : List.of( ZonedInNewYork.class, OffsetInNewYork.class, NormalizedInNewYork.class ) ) {
			inFactory( ENABLED, entityClass, factory -> {
			} );
			assertThat( valueOf( ENABLED, entityClass ) ).isInstanceOf( BasicValue.class );
		}
	}

	@Test
	void valueInTheDeclaredZoneIsPreserved() {
		inFactory( ENABLED, ZonedInNewYork.class, factory -> {
			int id = 0;
			for ( var instant : List.of( SUMMER, WINTER ) ) {
				final var entity = new ZonedInNewYork();
				entity.id = ++id;
				entity.theValue = instant.atZone( NEW_YORK );
				factory.inTransaction( session -> session.persist( entity ) );
				factory.inTransaction( session -> assertThat( session.find( ZonedInNewYork.class, entity.id ).theValue )
						.isEqualTo( instant.atZone( NEW_YORK ) ) );
				// the instant itself was stored
				factory.inTransaction( session -> assertThat( session.createQuery(
						"select count(*) from ZonedInNewYork where theValue = :instant", Long.class
				).setParameter( "instant", instant.atZone( NEW_YORK ) ).getSingleResult() ).isEqualTo( 1L ) );
			}
		} );
		inFactory( ENABLED, OffsetInNewYork.class, factory -> {
			int id = 0;
			for ( var instant : List.of( SUMMER, WINTER ) ) {
				// the offset of New York differs between summer and winter
				final var value = instant.atZone( NEW_YORK ).toOffsetDateTime();
				final var entity = new OffsetInNewYork();
				entity.id = ++id;
				entity.theValue = value;
				factory.inTransaction( session -> session.persist( entity ) );
				factory.inTransaction( session -> assertThat( session.find( OffsetInNewYork.class, entity.id ).theValue )
						.isEqualTo( value ) );
			}
		} );
	}

	@Test
	void valueInAnotherZoneIsRejectedWithTheValuePolicy() {
		inFactory( ENABLED, ZonedInNewYork.class, factory -> {
			final var entity = new ZonedInNewYork();
			entity.id = 1;
			entity.theValue = SUMMER.atZone( PARIS );
			assertRejected( () -> factory.inTransaction( session -> session.persist( entity ) ) );
			factory.inTransaction( session -> assertThat( session.find( ZonedInNewYork.class, 1 ) ).isNull() );
			// a query parameter compared with the attribute is held to the same
			assertRejected( () -> factory.inTransaction( session -> session.createQuery(
					"select count(*) from ZonedInNewYork where theValue = :instant", Long.class
			).setParameter( "instant", SUMMER.atZone( PARIS ) ).getSingleResult() ) );
		} );
		inFactory( ENABLED, OffsetInNewYork.class, factory -> {
			final var entity = new OffsetInNewYork();
			entity.id = 1;
			// the offset which New York has in winter, at an instant in summer
			entity.theValue = SUMMER.atOffset( ZoneOffset.ofHours( -5 ) );
			assertRejected( () -> factory.inTransaction( session -> session.persist( entity ) ) );
		} );
	}

	@Test
	void valueInAnotherZoneIsConvertedWithTheNormalizedPolicy() {
		inFactory( ENABLED, NormalizedInNewYork.class, factory -> {
			final var entity = new NormalizedInNewYork();
			entity.id = 1;
			entity.theValue = SUMMER.atZone( PARIS );
			entity.theOffsetValue = SUMMER.atOffset( ZoneOffset.ofHoursMinutes( 5, 45 ) );
			factory.inTransaction( session -> session.persist( entity ) );
			factory.inTransaction( session -> {
				final var read = session.find( NormalizedInNewYork.class, 1 );
				assertThat( read.theValue ).isEqualTo( SUMMER.atZone( NEW_YORK ) );
				assertThat( read.theOffsetValue ).isEqualTo( SUMMER.atZone( NEW_YORK ).toOffsetDateTime() );
			} );
			factory.inTransaction( session -> assertThat( session.createQuery(
					"select count(*) from NormalizedInNewYork where theValue = :instant", Long.class
			).setParameter( "instant", SUMMER.atZone( PARIS ) ).getSingleResult() ).isEqualTo( 1L ) );
		} );
	}

	/// `UTC` and `Z` identify the same zone.
	@Test
	void equivalentIdentifiersOfTheZoneAreAccepted() {
		inFactory( ENABLED, ZonedInUtc.class, factory -> {
			final var entity = new ZonedInUtc();
			entity.id = 1;
			entity.theValue = SUMMER.atZone( ZoneOffset.UTC );
			factory.inTransaction( session -> session.persist( entity ) );
			factory.inTransaction( session -> assertThat( session.find( ZonedInUtc.class, 1 ).theValue.toInstant() )
					.isEqualTo( SUMMER ) );
		} );
	}

	@Test
	void elementsOfACollectionAreAdjusted() {
		inFactory( ENABLED, WithElementsInNewYork.class, factory -> {
			final var entity = new WithElementsInNewYork();
			entity.id = 1;
			entity.theValues = List.of( SUMMER.atZone( NEW_YORK ), WINTER.atZone( NEW_YORK ) );
			factory.inTransaction( session -> session.persist( entity ) );
			factory.inTransaction( session -> assertThat( session.find( WithElementsInNewYork.class, 1 ).theValues )
					.containsExactlyInAnyOrder( SUMMER.atZone( NEW_YORK ), WINTER.atZone( NEW_YORK ) ) );
			final var other = new WithElementsInNewYork();
			other.id = 2;
			other.theValues = List.of( SUMMER.atZone( PARIS ) );
			assertRejected( () -> factory.inTransaction( session -> session.persist( other ) ) );
		} );
	}

	@Test
	void declaredZoneIsInheritedAndCancelled() {
		inFactory( ENABLED, WithClassZone.class, factory -> {
			final var entity = new WithClassZone();
			entity.id = 1;
			entity.theValue = SUMMER.atZone( NEW_YORK );
			entity.unadjusted = SUMMER.atZone( PARIS );
			factory.inTransaction( session -> session.persist( entity ) );
			factory.inTransaction( session -> {
				final var read = session.find( WithClassZone.class, 1 );
				assertThat( read.theValue ).isEqualTo( SUMMER.atZone( NEW_YORK ) );
				// NONE cancels the zone of the class, and the zone is stored in a second column
				assertThat( read.unadjusted ).isEqualTo( SUMMER.atZone( PARIS ) );
			} );
		} );
		// a zone declared on the class is not applied to an identifier, which is held to its policy
		assertThat( valueOf( ENABLED, WithClassZone.class ) ).isInstanceOf( BasicValue.class );
	}

	@Test
	void declarationsWhichCannotBeHonoredAreRejected() {
		assertInvalid( WithZoneAndColumn.class, "cannot be combined with a second column" );
		assertInvalid( WithInvalidZone.class, "is not a time zone" );
		assertInvalid( WithZoneOnLocalDate.class, "@TimeZoneAdjustment applies only to" );
		assertInvalid( WithZoneOnId.class, "@TimeZoneAdjustment applies only to" );
		assertInvalid( WithZoneOnConverted.class, "@TimeZoneAdjustment applies only to" );
	}

	@Test
	void declaredZoneIsInertWhenDisabled() {
		for ( var entityClass : List.of( WithInvalidZone.class, WithZoneOnLocalDate.class, WithZoneAndColumn.class ) ) {
			inFactory( DISABLED, entityClass, factory -> {
			} );
		}
		inFactory( DISABLED, ZonedInNewYork.class, factory -> {
			final var entity = new ZonedInNewYork();
			entity.id = 1;
			entity.theValue = SUMMER.atZone( PARIS );
			factory.inTransaction( session -> session.persist( entity ) );
			factory.inTransaction( session -> assertThat( session.find( ZonedInNewYork.class, 1 ).theValue.getZone() )
					.isNotEqualTo( NEW_YORK ) );
		} );
	}

	private static void assertRejected(Runnable action) {
		assertThatThrownBy( action::run )
				.isInstanceOf( TemporalPreservationException.class )
				.hasMessageContaining( "@TimeZoneAdjustment" )
				.hasMessageContaining( MappingSettings.TEMPORAL_PRESERVATION );
	}

	private static void assertInvalid(Class<?> entityClass, String problem) {
		assertThatThrownBy( () -> inFactory( ENABLED, entityClass, factory -> {
		} ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( problem );
	}

	private static Object valueOf(Map<String, Object> settings, Class<?> entityClass) {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		settings.forEach( registryBuilder::applySetting );
		try ( var registry = registryBuilder.build() ) {
			return new MetadataSources( registry ).addAnnotatedClass( entityClass )
					.buildMetadata()
					.getEntityBinding( entityClass.getName() )
					.getProperty( "theValue" )
					.getValue();
		}
	}

	private static void inFactory(
			Map<String, Object> settings,
			Class<?> entityClass,
			Consumer<SessionFactoryImplementor> action) {
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

	@Entity(name = "ZonedInNewYork")
	@Table(name = "tp_adj_zoned")
	static class ZonedInNewYork {
		@Id
		Integer id;
		@TimeZoneAdjustment("America/New_York")
		ZonedDateTime theValue;
	}

	@Entity(name = "OffsetInNewYork")
	@Table(name = "tp_adj_offset")
	static class OffsetInNewYork {
		@Id
		Integer id;
		@TimeZoneAdjustment("America/New_York")
		OffsetDateTime theValue;
	}

	@Entity(name = "NormalizedInNewYork")
	@Table(name = "tp_adj_normalized")
	@TemporalPreservationPolicy(TemporalPreservationPolicy.Type.NORMALIZED)
	@TimeZoneAdjustment("America/New_York")
	static class NormalizedInNewYork {
		@Id
		Integer id;
		ZonedDateTime theValue;
		OffsetDateTime theOffsetValue;
	}

	@Entity(name = "ZonedInUtc")
	@Table(name = "tp_adj_utc")
	static class ZonedInUtc {
		@Id
		Integer id;
		@TimeZoneAdjustment("UTC")
		ZonedDateTime theValue;
	}

	@Entity(name = "WithElementsInNewYork")
	@Table(name = "tp_adj_elements")
	static class WithElementsInNewYork {
		@Id
		Integer id;
		@ElementCollection(fetch = FetchType.EAGER)
		@TimeZoneAdjustment("America/New_York")
		// a bag, since Oracle does not accept a timestamp with time zone in a primary key
		List<ZonedDateTime> theValues;
	}

	@Entity(name = "WithClassZone")
	@Table(name = "tp_adj_class")
	@TimeZoneAdjustment("America/New_York")
	static class WithClassZone {
		@Id
		Integer id;
		ZonedDateTime theValue;
		@TimeZoneAdjustment(TimeZoneAdjustment.NONE)
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		ZonedDateTime unadjusted;
		LocalDate unrelated;
	}

	@Entity(name = "WithZoneAndColumn")
	static class WithZoneAndColumn {
		@Id
		Integer id;
		@TimeZoneAdjustment("America/New_York")
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		ZonedDateTime theValue;
	}

	@Entity(name = "WithInvalidZone")
	static class WithInvalidZone {
		@Id
		Integer id;
		@TimeZoneAdjustment("Mars/Olympus")
		ZonedDateTime theValue;
	}

	@Entity(name = "WithZoneOnLocalDate")
	static class WithZoneOnLocalDate {
		@Id
		Integer id;
		@TimeZoneAdjustment("America/New_York")
		LocalDate theValue;
	}

	@Entity(name = "WithZoneOnId")
	static class WithZoneOnId {
		@Id
		@TimeZoneAdjustment("America/New_York")
		OffsetDateTime id;
	}

	@Entity(name = "WithZoneOnConverted")
	static class WithZoneOnConverted {
		@Id
		Integer id;
		@Convert(converter = OffsetDateTimeToString.class)
		@TimeZoneAdjustment("America/New_York")
		OffsetDateTime theValue;
	}
}
