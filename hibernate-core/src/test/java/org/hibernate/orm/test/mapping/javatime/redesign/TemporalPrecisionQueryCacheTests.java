package org.hibernate.orm.test.mapping.javatime.redesign;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.type.BindableType;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Query keys retain comparison precision; bulk COLUMN assignments prepare the
/// primary and offset as a unit. Expectations are independent of ORM quantizers.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.cache.use_query_cache", value = "true"),
		@Setting(name = "hibernate.cache.query_cache_layout", value = "FULL")
})
@DomainModel(annotatedClasses = TemporalPrecisionQueryCacheTests.Row.class)
@SessionFactory(generateStatistics = true, useCollectingStatementInspector = true)
class TemporalPrecisionQueryCacheTests {
	private static final LocalDateTime STORED = LocalDateTime.parse( "1969-12-31T23:59:59.123" );
	private static final LocalDateTime BELOW = STORED.withNano( 122_900_000 );
	private static final LocalDateTime ABOVE = STORED.withNano( 123_100_000 );

	@BeforeEach
	void seed(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAllRegions();
		scope.inTransaction( session -> {
			final var row = new Row(); row.id = 1; row.stamp = STORED; row.converted = new Stamp( STORED );
			row.zone = Instant.parse( "2026-01-01T00:00:00Z" ).atZone( ZoneOffset.UTC );
			row.fineZone = Instant.parse( "2026-03-08T06:59:59.600Z" ).atZone( ZoneId.of( "America/New_York" ) );
			row.fineUtc = Instant.parse( "2026-03-08T06:59:59.600Z" ).atZone( ZoneId.of( "UTC" ) );
			row.offset = row.zone.toOffsetDateTime(); row.time = OffsetTime.parse( "12:00+05:45" );
			session.persist( row );
		} );
		scope.getSessionFactory().getCache().evictQueryRegions();
		scope.getSessionFactory().getStatistics().clear();
		StampConverter.writes.set( 0 );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) { StampConverter.sensitive = false; StampConverter.fail = false; scope.dropData(); }

	@Test
	void sameWriteQuantumDoesNotAliasPredicateKeys(SessionFactoryScope scope) {
		assertThat( ids( scope, "stamp < :value", BELOW ) ).isEmpty();
		assertThat( ids( scope, "stamp < :value", ABOVE ) ).containsExactly( 1 );
		assertThat( ids( scope, "stamp < :value", BELOW ) ).isEmpty();
		assertThat( ids( scope, "stamp < :value", ABOVE ) ).containsExactly( 1 );
		assertThat( scope.getSessionFactory().getStatistics().getQueryCacheMissCount() ).isEqualTo( 2 );
		assertThat( scope.getSessionFactory().getStatistics().getQueryCacheHitCount() ).isEqualTo( 2 );
	}

	@Test
	void convertedKeyAndBindingShareOneConversion(SessionFactoryScope scope) {
		assertThat( ids( scope, "converted < :value", new Stamp( ABOVE ) ) ).containsExactly( 1 );
		assertThat( StampConverter.writes ).hasValue( 1 );
		assertThat( ids( scope, "converted < :value", new Stamp( ABOVE ) ) ).containsExactly( 1 );
		assertThat( StampConverter.writes ).hasValue( 2 );
		assertThat( scope.getSessionFactory().getStatistics().getQueryCacheHitCount() ).isEqualTo( 1 );
	}

	@Test
	void columnBulkAssignmentRecomputesOffsetAtCanonicalInstant(SessionFactoryScope scope) {
		final var input = Instant.parse( "2026-03-08T06:59:59.600Z" ).atZone( ZoneOffset.ofHoursMinutes( 5, 45 ) );
		scope.inTransaction( session -> assertThat( session.createMutationQuery(
				"update PrecisionCacheRow set zone=:value where id=1" ).setParameter( "value", input ).executeUpdate() ).isEqualTo( 1 ) );
		scope.inTransaction( session -> {
			final var actual = session.find( Row.class, 1 ).zone;
			assertThat( (Object) actual ).isEqualTo( Instant.parse( "2026-03-08T07:00:00Z" ).atZone( ZoneId.of( "America/New_York" ) ) );
			session.doWork( connection -> {
				try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
						"select zone_offset from PrecisionCacheRow where id=1" ) ) {
					assertThat( rows.next() ).isTrue();
					assertThat( rows.getInt( 1 ) ).isEqualTo( -14400 );
				}
			} );
		} );
	}


	@Test
	void predicateOperatorsAndListsRetainBoundaries(SessionFactoryScope scope) {
		for ( var entry : Map.of( "=", List.of( false, true, false ), "<>", List.of( true, false, true ),
				"<", List.of( false, false, true ), "<=", List.of( false, true, true ),
				">", List.of( true, false, false ), ">=", List.of( true, true, false ),
				"is not distinct from", List.of( false, true, false ) ).entrySet() ) {
			for ( int repeat = 0; repeat < 2; repeat++ ) {
				int i = 0;
				for ( var boundary : List.of( BELOW, STORED, ABOVE ) ) {
					assertThat( ids( scope, "stamp " + entry.getKey() + " :value", boundary ) )
							.containsExactlyElementsOf( entry.getValue().get( i++ ) ? List.of( 1 ) : List.of() );
				}
			}
		}
		scope.inTransaction( session -> {
			for ( boolean cache : new boolean[] { false, true } ) {
				assertThat( session.createSelectionQuery( "select id from PrecisionCacheRow where stamp between :low and :high", Integer.class )
						.setParameter( "low", BELOW ).setParameter( "high", ABOVE ).setCacheable( cache ).getResultList() ).containsExactly( 1 );
				assertThat( session.createSelectionQuery( "select id from PrecisionCacheRow where stamp in :values", Integer.class )
						.setParameterList( "values", List.of( BELOW, ABOVE ) ).setCacheable( cache ).getResultList() ).isEmpty();
				assertThat( session.createSelectionQuery( "select id from PrecisionCacheRow where stamp in :values", Integer.class )
						.setParameterList( "values", List.of( STORED, ABOVE ) ).setCacheable( cache ).getResultList() ).containsExactly( 1 );
				assertThat( session.createSelectionQuery( "select id from PrecisionCacheRow where stamp in :values", Integer.class )
						.setParameterList( "values", List.<LocalDateTime>of() ).setCacheable( cache ).getResultList() ).isEmpty();
				assertThat( session.createSelectionQuery( "select id from PrecisionCacheRow where stamp is null", Integer.class )
						.setCacheable( cache ).getResultList() ).isEmpty();
			}
		} );
		assertThat( ids( scope, "stamp is not distinct from :value", null ) ).isEmpty();
		assertThat( ids( scope, "stamp is not distinct from :value", null ) ).isEmpty();
	}

	@Test
	void convertedListsReuseAndFailureDoNotLeakState(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = session.createSelectionQuery( "select id from PrecisionCacheRow where converted in :values", Integer.class )
					.setCacheable( true );
			final var values = List.of( new Stamp( STORED ), new Stamp( ABOVE ) );
			assertThat( query.setParameterList( "values", values ).getResultList() ).containsExactly( 1 );
			assertThat( StampConverter.writes ).hasValue( 2 );
			assertThat( query.setParameterList( "values", values ).getResultList() ).containsExactly( 1 );
			assertThat( StampConverter.writes ).hasValue( 4 );
			assertThat( query.setParameterList( "values", List.of( new Stamp( ABOVE ) ) ).getResultList() ).isEmpty();
			StampConverter.fail = true;
			assertThatThrownBy( query::getResultList ).hasStackTraceContaining( "intentional converter failure" );
			StampConverter.fail = false;
			assertThat( query.setParameterList( "values", values ).getResultList() ).containsExactly( 1 );
		} );
	}

	@Test
	void invocationSensitiveConverterKeysTheValueActuallyBound(SessionFactoryScope scope) {
		StampConverter.sensitive = true;
		assertThat( ids( scope, "converted < :value", new Stamp( ABOVE ) ) ).containsExactly( 1 );
		assertThat( StampConverter.writes ).hasValue( 1 );
		assertThat( ids( scope, "converted < :value", new Stamp( ABOVE ) ) ).isEmpty();
		assertThat( StampConverter.writes ).hasValue( 2 );
		assertThat( ids( scope, "converted < :value", new Stamp( ABOVE ) ) ).containsExactly( 1 );
		assertThat( StampConverter.writes ).hasValue( 3 );
		assertThat( scope.getSessionFactory().getStatistics().getQueryCacheHitCount() ).isEqualTo( 1 );
	}

	@Test
	void projectionsAndEntityResultsAgreeOnCacheHits(SessionFactoryScope scope) {
		for ( int pass = 0; pass < 3; pass++ ) {
			scope.getSessionFactory().getCache().evictEntityData( Row.class );
			scope.inTransaction( session -> {
				final var row = session.createSelectionQuery( "from PrecisionCacheRow", Row.class ).setCacheable( true ).getSingleResult();
				assertThat( row.stamp ).isEqualTo( STORED );
				assertThat( row.converted ).isEqualTo( new Stamp( STORED ) );
				final var values = session.createSelectionQuery( "select stamp,converted,zone from PrecisionCacheRow", Object[].class )
						.setCacheable( true ).getSingleResult();
				assertThat( values[0] ).isEqualTo( STORED );
				assertThat( values[1] ).isEqualTo( new Stamp( STORED ) );
				assertThat( values[2] ).isEqualTo( row.zone );
			} );
		}
		assertThat( scope.getSessionFactory().getStatistics().getQueryCacheHitCount() ).isEqualTo( 4 );
		assertThat( StampConverter.writes ).hasValue( 0 );
	}

	@Test
	void bulkAndSynchronizedNativeMutationsInvalidateResults(SessionFactoryScope scope) {
		assertThat( ids( scope, "stamp < :value", ABOVE ) ).containsExactly( 1 );
		scope.inTransaction( session -> session.createMutationQuery( "update PrecisionCacheRow set stamp=:value where id=1" )
				.setParameter( "value", STORED.plusSeconds( 1 ) ).executeUpdate() );
		assertThat( ids( scope, "stamp < :value", ABOVE ) ).isEmpty();
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var tx = session.beginTransaction();
			session.createMutationQuery( "update PrecisionCacheRow set stamp=:value where id=1" ).setParameter( "value", STORED ).executeUpdate();
			tx.rollback();
		}
		assertThat( ids( scope, "stamp < :value", ABOVE ) ).isEmpty();
		scope.inTransaction( session -> session.createNativeQuery( "update PrecisionCacheRow set stamp=:value where id=1", Integer.class )
				.addSynchronizedEntityClass( Row.class ).setParameter( "value", STORED ).executeUpdate() );
		assertThat( ids( scope, "stamp < :value", ABOVE ) ).containsExactly( 1 );
	}

	@Test
	void criteriaNativeAndBasicInsertSelectKeepExpressionSemantics(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var criteria = cb.createQuery( Integer.class );
			final var root = criteria.from( Row.class );
			final var value = cb.parameter( LocalDateTime.class, "value" );
			criteria.select( root.get( "id" ) ).where( cb.lessThan( root.get( "stamp" ), value ) );
			final var query = session.createSelectionQuery( criteria ).setCacheable( true );
			assertThat( query.setParameter( "value", BELOW ).getResultList() ).isEmpty();
			assertThat( query.setParameter( "value", ABOVE ).getResultList() ).containsExactly( 1 );
			assertThat( query.setParameter( "value", BELOW ).getResultList() ).isEmpty();
			assertThat( session.createNativeQuery( "select stamp from PrecisionCacheRow where stamp < :value", LocalDateTime.class )
					.setParameter( "value", ABOVE ).getResultList() ).containsExactly( STORED );
			assertThat( session.createMutationQuery( "insert into PrecisionCacheRow (id,stamp) select id+10,stamp from PrecisionCacheRow where id=1" )
					.executeUpdate() ).isEqualTo( 1 );
		} );
		scope.inTransaction( session -> assertThat( session.find( Row.class, 11 ).stamp ).isEqualTo( STORED ) );
	}

	@Test
	@SuppressWarnings("unchecked")
	void explicitlyTypedNativeConverterUsesActualBinding(SessionFactoryScope scope) {
		final var mapping = (BasicValuedModelPart) scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( Row.class ).findAttributeMapping( "converted" );
		final var type = (BindableType<Stamp>) mapping.getJdbcMapping();
		for ( int pass = 0; pass < 2; pass++ ) {
			scope.inTransaction( session -> assertThat( session.createNativeQuery(
					"select id from PrecisionCacheRow where converted < :value", Integer.class )
					.addSynchronizedEntityClass( Row.class ).setParameter( "value", new Stamp( ABOVE ), type )
					.setCacheable( true ).getResultList() ).containsExactly( 1 ) );
			assertThat( StampConverter.writes ).hasValue( pass + 1 );
		}
		assertThat( scope.getSessionFactory().getStatistics().getQueryCacheHitCount() ).isEqualTo( 1 );
	}

	@Test
	void fixedOffsetColumnCopyMayChangePrecision(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createMutationQuery( "update PrecisionCacheRow set utc=fineUtc where id=1" ).executeUpdate() ).isEqualTo( 1 );
			assertThat( session.createMutationQuery( "insert into PrecisionCacheRow (id,utc) select 4,fineUtc from PrecisionCacheRow where id=1" )
					.executeUpdate() ).isEqualTo( 1 );
		} );
		scope.inTransaction( session -> {
			final var expected = Instant.parse( "2026-03-08T07:00:00Z" ).atZone( ZoneId.of( "UTC" ) );
			assertThat( (Object) session.find( Row.class, 1 ).utc ).isEqualTo( expected );
			assertThat( (Object) session.find( Row.class, 4 ).utc ).isEqualTo( expected );
		} );
	}

	@Test
	void incompatibleColumnExpressionIsRejectedBeforeMutation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThatThrownBy( () -> session.createMutationQuery( "update PrecisionCacheRow set zone=fineZone where id=1" )
					.executeUpdate() ).hasStackTraceContaining( "COLUMN assignment" )
					.hasStackTraceContaining( "precision=0" ).hasStackTraceContaining( "precision=3" );
		} );
		scope.inTransaction( session -> {
			assertThatThrownBy( () -> session.createMutationQuery(
					"insert into PrecisionCacheRow (id,zone) select 9,fineZone from PrecisionCacheRow where id=1" )
					.executeUpdate() ).hasStackTraceContaining( "COLUMN assignment" )
					.hasStackTraceContaining( "precision=0" ).hasStackTraceContaining( "precision=3" );
		} );
	}

	@Test
	void finerExpressionsAndConvertedNulls(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int pass = 0; pass < 2; pass++ ) {
				assertThat( session.createSelectionQuery(
						"select timestampadd(nanosecond, 456789, stamp) from PrecisionCacheRow", LocalDateTime.class )
						.setCacheable( true ).getSingleResult() ).isEqualTo( STORED.plusNanos( 456789 ) );
				assertThat( session.createSelectionQuery(
						"select cast(:value as LocalDateTime) from PrecisionCacheRow", LocalDateTime.class )
						.setParameter( "value", ABOVE ).setCacheable( true ).getSingleResult() ).isEqualTo( ABOVE );
			}
		} );
		StampConverter.writes.set( 0 );
		assertThat( ids( scope, "converted is not distinct from :value", null ) ).isEmpty();
		assertThat( ids( scope, "converted is not distinct from :value", null ) ).isEmpty();
		assertThat( StampConverter.writes ).hasValue( 2 );
		assertThat( ids( scope, "converted < :value or converted = :value", new Stamp( ABOVE ) ) ).containsExactly( 1 );
		// The plan reuses one converted binding for these equivalent occurrences.
		assertThat( StampConverter.writes ).hasValue( 3 );
	}

	@Test
	void columnCriteriaAndDatabaseOwnedExpressions(SessionFactoryScope scope) {
		final var input = Instant.parse( "2026-11-01T05:59:59.600Z" ).atZone( ZoneOffset.UTC );
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var update = cb.createCriteriaUpdate( Row.class );
			final var root = update.from( Row.class );
			update.set( root.<ZonedDateTime>get( "zone" ), cb.parameter( ZonedDateTime.class, "value" ) );
			assertThat( session.createMutationQuery( update ).setParameter( "value", input ).executeUpdate() ).isEqualTo( 1 );
			assertThat( session.createMutationQuery( "update PrecisionCacheRow set zone=zone where id=1" ).executeUpdate() ).isEqualTo( 1 );
			assertThat( session.createMutationQuery( "insert into PrecisionCacheRow (id,zone) select 3,zone from PrecisionCacheRow where id=1" )
					.executeUpdate() ).isEqualTo( 1 );
		} );
		scope.inTransaction( session -> {
			assertThat( (Object) session.find( Row.class, 3 ).zone ).isEqualTo(
					Instant.parse( "2026-11-01T06:00:00Z" ).atZone( ZoneId.of( "America/New_York" ) ) );
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Row.class );
			final var root = query.from( Row.class );
			assertThatThrownBy( () -> session.createSelectionQuery( query.where( cb.equal( root.get( "zone" ), cb.literal( input ) ) ) )
					.getResultList() ).hasStackTraceContaining( "multi-column" );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "2026-03-08T06:59:59.600Z", "2026-11-01T05:59:59.600Z" })
	void columnInsertValuesNullsAndAllDomainTypes(String text, SessionFactoryScope scope) {
		final var instant = Instant.parse( text );
		final var zone = instant.atZone( ZoneOffset.ofHoursMinutes( 5, 45 ) );
		final var offset = zone.toOffsetDateTime();
		final var time = OffsetTime.parse( "23:59:59.600+05:45" );
		scope.inTransaction( session -> assertThat( session.createMutationQuery(
				"insert into PrecisionCacheRow (id,zone,offset,time) values (2,:zone,:offset,:time)" )
				.setParameter( "zone", zone ).setParameter( "offset", offset ).setParameter( "time", time ).executeUpdate() ).isEqualTo( 1 ) );
		scope.inTransaction( session -> {
			final var row = session.find( Row.class, 2 );
			assertThat( (Object) row.zone ).isEqualTo( instant.plusSeconds( 1 ).with( ChronoField.NANO_OF_SECOND, 0 )
					.atZone( ZoneId.of( "America/New_York" ) ) );
			assertThat( (Object) row.offset ).isEqualTo( offset.plusSeconds( 1 ).withNano( 0 ) );
			assertThat( (Object) row.time ).isEqualTo( OffsetTime.parse( "00:00+05:45" ) );
			session.doWork( connection -> {
				try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
						"select zone_offset,offset_seconds,time_offset from PrecisionCacheRow where id=2" ) ) {
					assertThat( rows.next() ).isTrue();
					assertThat( rows.getInt( 1 ) ).isEqualTo( text.startsWith( "2026-03" ) ? -14400 : -18000 );
					assertThat( rows.getInt( 2 ) ).isEqualTo( 20700 );
					assertThat( rows.getInt( 3 ) ).isEqualTo( 20700 );
				}
			} );
		} );
		for ( String property : List.of( "zone", "offset", "time" ) ) {
			scope.inTransaction( session -> session.createMutationQuery( "update PrecisionCacheRow set " + property + "=:value where id=2" )
					.setParameter( "value", null ).executeUpdate() );
		}
		scope.inTransaction( session -> {
			final var row = session.find( Row.class, 2 );
			assertThat( row.zone ).isNull(); assertThat( row.offset ).isNull(); assertThat( row.time ).isNull();
		} );
	}

	private static List<Integer> ids(SessionFactoryScope scope, String predicate, Object value) {
		return scope.fromTransaction( session -> session.createSelectionQuery(
				"select id from PrecisionCacheRow where " + predicate, Integer.class )
				.setParameter( "value", value ).setCacheable( true ).getResultList() );
	}

	@Entity(name = "PrecisionCacheRow")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Row {
		@Id int id;
		@Column(secondPrecision = 3) LocalDateTime stamp;
		@Convert(converter = StampConverter.class) @Column(secondPrecision = 3) Stamp converted;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneAdjustment("America/New_York")
		@TimeZoneColumn(name = "zone_offset")
		@Column(name = "zone_value", secondPrecision = 0) ZonedDateTime zone;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneAdjustment("America/New_York")
		@TimeZoneColumn(name = "fine_offset")
		@Column(name = "fine_value", secondPrecision = 3) ZonedDateTime fineZone;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("UTC")
		@Column(secondPrecision = 0) ZonedDateTime utc;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("UTC")
		@Column(secondPrecision = 3) ZonedDateTime fineUtc;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "offset_seconds") @Column(name = "offset_value", secondPrecision = 0) OffsetDateTime offset;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "time_offset") @Column(name = "time_value", secondPrecision = 0) OffsetTime time;
	}

	record Stamp(LocalDateTime value) implements Serializable {
	}

	@Converter
	public static class StampConverter implements AttributeConverter<Stamp, LocalDateTime> {
		static final AtomicInteger writes = new AtomicInteger();
		static boolean sensitive;
		static boolean fail;
		@Override public LocalDateTime convertToDatabaseColumn(Stamp value) {
			final int call = writes.incrementAndGet();
			if ( fail ) { throw new IllegalArgumentException( "intentional converter failure" ); }
			return value == null ? null : sensitive && call % 2 == 0 ? BELOW : value.value();
		}
		@Override public Stamp convertToEntityAttribute(LocalDateTime value) { return value == null ? null : new Stamp( value ); }
	}
}
