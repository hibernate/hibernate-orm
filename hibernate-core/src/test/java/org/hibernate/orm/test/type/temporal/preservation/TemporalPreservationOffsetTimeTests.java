package org.hibernate.orm.test.type.temporal.preservation;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.MappingException;
import org.hibernate.Session;
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
import org.hibernate.mapping.Component;
import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies against a live database what temporal preservation stores for an
/// `OffsetTime`: the whole value by default, in a single column where the
/// database keeps the offset to the second and in two columns where it does
/// not, and the time of day at UTC with the `NORMALIZED` policy.
///
/// Each value is written under one combination of the default time zone of
/// the JVM, the JDBC time zone of the session and the time zone of the
/// database session, and read back under every combination, none of which
/// may make a difference.
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
class TemporalPreservationOffsetTimeTests {
	private static final String[] DATABASE_SESSION_ZONES = { null, "Asia/Kathmandu", "America/New_York" };
	private static final String[] SESSION_JDBC_ZONES = { null, "Pacific/Chatham" };

	/// Whole seconds, since the precision of the column is a separate matter.
	private static final List<OffsetTime> SAMPLES = List.of(
			OffsetTime.parse( "10:15:30+02:00" ),
			OffsetTime.parse( "00:00:00Z" ),
			// the time at UTC is on the previous day, and on the next
			OffsetTime.parse( "01:30:00+05:00" ),
			OffsetTime.parse( "22:30:00-05:00" ),
			OffsetTime.parse( "10:15:30+05:45" ),
			// an offset with seconds
			OffsetTime.parse( "10:15:30+01:02:03" ),
			OffsetTime.parse( "00:00:00+18:00" ),
			OffsetTime.parse( "23:59:59-18:00" )
	);

	private final AtomicInteger ids = new AtomicInteger();

	@Test
	void wholeValueIsPreservedByDefault() {
		// one column where it keeps the offset to the second, and two where it does not
		assertThat( valueOf( WithOffsetTime.class ) ).isInstanceOf(
				DialectContext.getDialect() instanceof H2Dialect || DialectContext.getDialect() instanceof HSQLDialect
						? BasicValue.class
						: Component.class
		);
		checkRoundTrip( WithOffsetTime.class, false );
	}

	@Test
	void secondColumnPreservesTheWholeValue() {
		assertThat( valueOf( WithOffsetTimeColumn.class ) ).isInstanceOf( Component.class );
		checkRoundTrip( WithOffsetTimeColumn.class, false );
	}

	@Test
	void normalizedPolicyPreservesTheTimeAtUtc() {
		assertThat( valueOf( WithNormalizedOffsetTime.class ) ).isInstanceOf( BasicValue.class );
		checkRoundTrip( WithNormalizedOffsetTime.class, true );
	}

	/// Stored in a single column, the value is found by a parameter and by a
	/// literal which denote the same time at UTC, whatever their offset.
	@Test
	void singleColumnIsComparedByItsTimeAtUtc() {
		inEveryEnvironment( List.of( WithNormalizedOffsetTime.class ), (factory, environment) -> {
			final var entity = new WithNormalizedOffsetTime();
			entity.id = ids.incrementAndGet();
			entity.theValue = OffsetTime.parse( "10:15:30+02:00" );
			factory.inTransaction( session -> session.persist( entity ) );
			final OffsetTime sameTime = OffsetTime.parse( "13:15:30+05:00" );
			environment.accept( session -> {
				assertThat( session.createQuery(
						"select count(*) from WithNormalizedOffsetTime e where e.id = :id and e.theValue = :value",
						Long.class
				).setParameter( "id", entity.id ).setParameter( "value", sameTime ).getSingleResult() ).isEqualTo( 1L );
				final var builder = session.getCriteriaBuilder();
				final var query = builder.createQuery( Long.class );
				final var root = query.from( WithNormalizedOffsetTime.class );
				query.select( builder.count( root ) ).where(
						builder.equal( root.get( "id" ), builder.literal( entity.id ) ),
						builder.equal( root.get( "theValue" ), builder.literal( sameTime ) )
				);
				final var dialect = factory.getJdbcServices().getDialect();
				// On H2 the column keeps the offset, and a literal with one is comparable with it.
				// Where the time at UTC is stored in a plain time column, so is the literal rendered.
				if ( dialect instanceof H2Dialect
						|| ( dialect instanceof PostgreSQLDialect || dialect instanceof CockroachDialect )
						|| dialect instanceof MySQLDialect ) {
					assertThat( session.createQuery( query ).getSingleResult() ).isEqualTo( 1L );
				}
				else {
					// the literal would be rendered without its offset
					assertThatThrownBy( () -> session.createQuery( query ).getSingleResult() )
							.isInstanceOf( TemporalPreservationException.class )
							.hasMessageContaining( "bind the value as a parameter" );
				}
			} );
		} );
	}

	/// The local time of day and the offset are available to queries.
	@Test
	void localFieldsCanBeExtracted() {
		inEveryEnvironment( List.of( WithOffsetTime.class ), (factory, environment) -> {
			final OffsetTime value = OffsetTime.parse( "10:15:30+05:45" );
			final var entity = new WithOffsetTime();
			entity.id = ids.incrementAndGet();
			entity.theValue = value;
			factory.inTransaction( session -> session.persist( entity ) );
			environment.accept( session -> {
				assertThat( session.createQuery(
						"select extract(hour from e.theValue) from WithOffsetTime e where e.id = :id", Integer.class
				).setParameter( "id", entity.id ).getSingleResult() ).isEqualTo( 10 );
				assertThat( session.createQuery(
						"select extract(minute from e.theValue) from WithOffsetTime e where e.id = :id", Integer.class
				).setParameter( "id", entity.id ).getSingleResult() ).isEqualTo( 15 );
				// compared with a parameter, the whole value is compared
				assertThat( session.createQuery(
						"select count(*) from WithOffsetTime e where e.id = :id and e.theValue = :value", Long.class
				).setParameter( "id", entity.id ).setParameter( "value", value ).getSingleResult() ).isEqualTo( 1L );
			} );
		} );
	}

	/// Stored in two columns, the first holds a time of day without an
	/// offset, which the database cannot compare with a value that has one.
	@Test
	void twoColumnsCannotBeComparedWithASingleValue() {
		inEveryEnvironment( List.of( WithOffsetTimeColumn.class ), (factory, environment) ->
				assertThatThrownBy( () -> factory.inTransaction( session -> session.createQuery(
						"select count(*) from WithOffsetTimeColumn e where e.theValue = current_time", Long.class
				).getSingleResult() ) )
						.hasRootCauseInstanceOf( SemanticException.class )
						.hasStackTraceContaining( MappingSettings.TEMPORAL_PRESERVATION ) );
	}

	/// With a declared offset, a single column which keeps the time at UTC suffices.
	@Test
	void declaredOffsetIsApplied() {
		assertThat( valueOf( WithOffsetTimeAtPlusTwo.class ) ).isInstanceOf( BasicValue.class );
		final ZoneOffset plusTwo = ZoneOffset.ofHours( 2 );
		inEveryEnvironment( List.of( WithOffsetTimeAtPlusTwo.class, WithNormalizedOffsetTimeAtPlusTwo.class ), (factory, environment) -> {
			final var entity = new WithOffsetTimeAtPlusTwo();
			entity.id = ids.incrementAndGet();
			entity.theValue = OffsetTime.parse( "10:15:30+02:00" );
			factory.inTransaction( session -> session.persist( entity ) );
			environment.accept( session -> assertThat( session.find( WithOffsetTimeAtPlusTwo.class, entity.id ).theValue )
					.isEqualTo( entity.theValue ) );

			final var other = new WithOffsetTimeAtPlusTwo();
			other.id = ids.incrementAndGet();
			other.theValue = OffsetTime.parse( "10:15:30+05:00" );
			assertThatThrownBy( () -> factory.inTransaction( session -> session.persist( other ) ) )
					.isInstanceOf( TemporalPreservationException.class )
					.hasMessageContaining( "@TimeZoneAdjustment" );

			final var normalized = new WithNormalizedOffsetTimeAtPlusTwo();
			normalized.id = ids.incrementAndGet();
			normalized.theValue = OffsetTime.parse( "10:15:30+05:00" );
			factory.inTransaction( session -> session.persist( normalized ) );
			environment.accept( session -> assertThat(
					session.find( WithNormalizedOffsetTimeAtPlusTwo.class, normalized.id ).theValue
			).isEqualTo( normalized.theValue.withOffsetSameInstant( plusTwo ) ) );
		} );
	}

	/// Without a date, the offset of a region cannot be determined.
	@Test
	void declaredRegionIsRejected() {
		assertThatThrownBy( () -> valueOf( WithOffsetTimeInParis.class, true ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "must be a fixed offset" );
	}

	private void checkRoundTrip(Class<? extends Holder> entityClass, boolean normalized) {
		inEveryEnvironment( List.of( entityClass ), (factory, environment) -> {
			final List<Holder> written = new ArrayList<>();
			environment.accept( session -> {
				for ( var value : SAMPLES ) {
					final Holder entity = newInstance( entityClass );
					entity.setId( ids.incrementAndGet() );
					entity.setValue( value );
					if ( exceedsColumn( factory, entityClass, value ) ) {
						// the value would be written with its own offset, which the column cannot hold
						assertThatThrownBy( () -> factory.inTransaction( other -> other.persist( entity ) ) )
								.isInstanceOf( TemporalPreservationException.class );
					}
					else {
						session.persist( entity );
						written.add( entity );
					}
				}
				session.getTransaction().commit();
				session.beginTransaction();
			} );
			// a value must not depend on the default time zone of the JVM which wrote it
			final TimeZone writerZone = TimeZone.getDefault();
			TimeZone.setDefault( TimeZone.getTimeZone( "Asia/Tokyo" ) );
			environment.accept( session -> {
				for ( var entity : written ) {
					final OffsetTime read = session.find( entityClass, entity.getId() ).getValue();
					if ( normalized ) {
						assertThat( read.withOffsetSameInstant( ZoneOffset.UTC ) )
								.as( "time at UTC read for %s", entity.getValue() )
								.isEqualTo( entity.getValue().withOffsetSameInstant( ZoneOffset.UTC ) );
					}
					else {
						assertThat( read ).as( "value read for %s", entity.getValue() ).isEqualTo( entity.getValue() );
					}
				}
				session.clear();
			} );
			TimeZone.setDefault( writerZone );
		} );
	}

	/// A single column of SQL Server holds no offset beyond fourteen hours.
	/// In two columns the first holds the time at UTC, whatever the offset.
	private static boolean exceedsColumn(SessionFactoryImplementor factory, Class<?> entityClass, OffsetTime value) {
		return factory.getJdbcServices().getDialect() instanceof SQLServerDialect
			&& Math.abs( value.getOffset().getTotalSeconds() ) > 14 * 3600
			&& factory.getMappingMetamodel().getEntityDescriptor( entityClass )
					.findAttributeMapping( "theValue" ).getJdbcTypeCount() == 1;
	}

	private static Holder newInstance(Class<? extends Holder> entityClass) {
		try {
			return entityClass.getDeclaredConstructor().newInstance();
		}
		catch (ReflectiveOperationException e) {
			throw new IllegalStateException( e );
		}
	}

	private static Object valueOf(Class<?> entityClass) {
		return valueOf( entityClass, false );
	}

	private static Object valueOf(Class<?> entityClass, boolean build) {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true );
		try ( var registry = registryBuilder.build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( entityClass ).buildMetadata();
			if ( build ) {
				metadata.buildSessionFactory().close();
			}
			return metadata.getEntityBinding( entityClass.getName() ).getProperty( "theValue" ).getValue();
		}
	}

	private interface Body {
		void run(SessionFactoryImplementor factory, Consumer<Consumer<Session>> environment);
	}

	/// Run the body with a factory under each default time zone of the JVM.
	/// What the body hands to the environment is run once under each
	/// combination of session JDBC time zone and database session time zone.
	private static void inEveryEnvironment(List<Class<?>> entityClasses, Body body) {
		final TimeZone original = TimeZone.getDefault();
		try {
			for ( var jvmZone : new String[] { "UTC", "America/New_York" } ) {
				TimeZone.setDefault( TimeZone.getTimeZone( jvmZone ) );
				final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
				registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "create-drop" );
				registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true );
				try ( var registry = registryBuilder.build() ) {
					final var sources = new MetadataSources( registry );
					entityClasses.forEach( sources::addAnnotatedClass );
					try ( var factory = (SessionFactoryImplementor) sources.buildMetadata().buildSessionFactory() ) {
						body.run( factory, action -> {
							for ( var sessionJdbcZone : SESSION_JDBC_ZONES ) {
								for ( var databaseSessionZone : DATABASE_SESSION_ZONES ) {
									TemporalPreservationOffsetTests.inSession(
											factory,
											sessionJdbcZone,
											databaseSessionZone,
											action
									);
								}
							}
						} );
					}
				}
			}
		}
		finally {
			TimeZone.setDefault( original );
		}
	}

	interface Holder {
		Integer getId();

		void setId(Integer id);

		OffsetTime getValue();

		void setValue(OffsetTime value);
	}

	@Entity(name = "WithOffsetTime")
	@Table(name = "tp_ot_default")
	static class WithOffsetTime implements Holder {
		@Id
		Integer id;
		OffsetTime theValue;

		@Override
		public Integer getId() {
			return id;
		}

		@Override
		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public OffsetTime getValue() {
			return theValue;
		}

		@Override
		public void setValue(OffsetTime value) {
			theValue = value;
		}
	}

	@Entity(name = "WithOffsetTimeColumn")
	@Table(name = "tp_ot_column")
	static class WithOffsetTimeColumn implements Holder {
		@Id
		Integer id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		OffsetTime theValue;

		@Override
		public Integer getId() {
			return id;
		}

		@Override
		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public OffsetTime getValue() {
			return theValue;
		}

		@Override
		public void setValue(OffsetTime value) {
			theValue = value;
		}
	}

	@Entity(name = "WithNormalizedOffsetTime")
	@Table(name = "tp_ot_normalized")
	static class WithNormalizedOffsetTime implements Holder {
		@Id
		Integer id;
		@TemporalPreservationPolicy(TemporalPreservationPolicy.Type.NORMALIZED)
		OffsetTime theValue;

		@Override
		public Integer getId() {
			return id;
		}

		@Override
		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public OffsetTime getValue() {
			return theValue;
		}

		@Override
		public void setValue(OffsetTime value) {
			theValue = value;
		}
	}

	@Entity(name = "WithOffsetTimeAtPlusTwo")
	@Table(name = "tp_ot_adjusted")
	static class WithOffsetTimeAtPlusTwo {
		@Id
		Integer id;
		@TimeZoneAdjustment("+02:00")
		OffsetTime theValue;
	}

	@Entity(name = "WithNormalizedOffsetTimeAtPlusTwo")
	@Table(name = "tp_ot_adjusted_normalized")
	static class WithNormalizedOffsetTimeAtPlusTwo {
		@Id
		Integer id;
		@TimeZoneAdjustment("+02:00")
		@TemporalPreservationPolicy(TemporalPreservationPolicy.Type.NORMALIZED)
		OffsetTime theValue;
	}

	@Entity(name = "WithOffsetTimeInParis")
	static class WithOffsetTimeInParis {
		@Id
		Integer id;
		@TimeZoneAdjustment("Europe/Paris")
		OffsetTime theValue;
	}
}
