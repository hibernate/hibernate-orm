package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.TemporalValueException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// COLUMN assignments must agree in both columns, while dirty comparison retains
/// the original offset and zone distinctions independently of storage adjustment.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@BytecodeEnhanced(runNotEnhancedAsWell = true, testEnhancedClasses = TemporalColumnPrecisionTests.Row.class)
class TemporalColumnPrecisionTests {
	private static final Instant INPUT = Instant.parse( "2026-03-08T06:59:59.999999900Z" );
	private static final Instant STORED = Instant.parse( "2026-03-08T07:00:00Z" );
	private static final ZoneId NEW_YORK = ZoneId.of( "America/New_York" );

	@Test
	void tupleCarryAndCacheParity() {
		for ( String queue : List.of( "graph", "legacy" ) ) {
			for ( int batch : new int[] { 0, 5 } ) { tupleCarryAndCacheParity( queue, batch ); }
		}
	}

	private void tupleCarryAndCacheParity(String queue, int batch) {
		try ( var scope = open( queue, batch ) ) {
			final var row = new Row();
			row.zone = INPUT.atZone( ZoneOffset.UTC );
			row.offset = INPUT.atOffset( ZoneOffset.ofHoursMinutes( 5, 45 ) );
			row.fixedZone = INPUT.atZone( NEW_YORK );
			row.nanos = row.offset;
			row.automatic = row.offset;
			row.time = OffsetTime.parse( "05:44:59.999999900+05:45" );
			scope.tx( session -> session.persist( row ) );
			assertThat( (Object) row.zone ).isEqualTo( INPUT.atZone( ZoneOffset.UTC ) );
			scope.tx( session -> session.doWork( connection -> {
				try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
						"select zone_value,zone_offset from ColumnPrecisionRow where id=1" ) ) {
					assertThat( rows.next() ).isTrue();
					assertThat( rows.getObject( 1, OffsetDateTime.class ).toInstant() ).isEqualTo( STORED );
					assertThat( rows.getInt( 2 ) ).isEqualTo( -14400 );
				}
			} ) );
			for ( int phase = 0; phase < 3; phase++ ) {
				if ( phase == 1 ) { scope.factory.getCache().evictAllRegions(); }
				scope.measure();
				scope.tx( session -> {
					final var loaded = session.find( Row.class, 1 );
					assertThat( (Object) loaded.zone ).isEqualTo( STORED.atZone( NEW_YORK ) );
					assertThat( (Object) loaded.offset ).isEqualTo( STORED.atOffset( ZoneOffset.ofHoursMinutes( 5, 45 ) ) );
					assertThat( (Object) loaded.nanos ).isEqualTo( row.nanos );
					assertThat( (Object) loaded.automatic ).isEqualTo( STORED.atOffset( row.offset.getOffset() ) );
					assertThat( (Object) loaded.fixedZone ).isEqualTo( STORED.atZone( ZoneOffset.ofHours( -5 ) ) );
					assertThat( (Object) loaded.time ).isEqualTo( OffsetTime.parse( "05:45+05:45" ) );
				} );
				assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( phase == 1 ? 0 : 1 );
			}
		}
	}

	@Test
	void assignedOffsetAndZoneChangesRemainDirty() {
		for ( String queue : List.of( "graph", "legacy" ) ) {
			for ( int batch : new int[] { 0, 5 } ) { assignedOffsetAndZoneChangesRemainDirty( queue, batch ); }
		}
	}

	private void assignedOffsetAndZoneChangesRemainDirty(String queue, int batch) {
		try ( var scope = open( queue, batch ) ) {
			final var row = new Row();
			row.zone = Instant.parse( "2026-01-02T12:34:56.123100Z" ).atZone( ZoneOffset.UTC );
			row.normalized = row.zone.toOffsetDateTime();
			scope.tx( session -> {
				session.persist( row ); session.flush(); scope.measure();
				row.normalized = row.normalized.withNano( 123200000 );
				session.flush();
				assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isZero();
				row.normalized = row.normalized.withOffsetSameInstant( ZoneOffset.ofHours( 2 ) );
				session.flush();
				assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
				scope.measure();
				row.zone = row.zone.withZoneSameInstant( ZoneId.of( "Etc/UTC" ) );
				session.flush();
				assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
				scope.measure(); session.flush(); scope.noMutation();
			} );
		}
	}

	@Test
	void overlapNullMergeAndRollback() {
		for ( String queue : List.of( "graph", "legacy" ) ) {
			for ( int batch : new int[] { 0, 5 } ) { overlapNullMergeAndRollback( queue, batch ); }
		}
	}

	private void overlapNullMergeAndRollback(String queue, int batch) {
		try ( var scope = open( queue, batch ) ) {
			final var row = new Row();
			row.zone = Instant.parse( "2026-11-01T05:59:59.999999900Z" ).atZone( ZoneOffset.UTC );
			row.offset = INPUT.atOffset( ZoneOffset.ofHoursMinutes( 5, 45 ) );
			scope.tx( session -> session.persist( row ) );
			final var expected = Instant.parse( "2026-11-01T06:00:00Z" ).atZone( NEW_YORK );
			scope.tx( session -> {
				final var loaded = session.find( Row.class, 1 );
				assertThat( (Object) loaded.zone ).isEqualTo( expected );
				loaded.normalized = INPUT.atOffset( ZoneOffset.UTC );
			} );
			// A partial update must retain the tuple assembled from the cache.
			scope.tx( session -> assertThat( (Object) session.find( Row.class, 1 ).zone ).isEqualTo( expected ) );
			try ( var session = scope.factory.openSession() ) {
				final var tx = session.beginTransaction();
				session.find( Row.class, 1 ).zone = null;
				session.flush(); tx.rollback();
			}
			scope.tx( session -> assertThat( (Object) session.find( Row.class, 1 ).zone ).isEqualTo( expected ) );
			final Row[] detached = new Row[1];
			scope.tx( session -> detached[0] = session.find( Row.class, 1 ) );
			detached[0].offset = detached[0].offset.withNano( 100000 );
			scope.measure();
			scope.tx( session -> session.merge( detached[0] ) );
			scope.noMutation();
			scope.tx( session -> session.find( Row.class, 1 ).zone = null );
			scope.tx( session -> assertThat( (Object) session.find( Row.class, 1 ).zone ).isNull() );
			scope.factory.getCache().evictAllRegions();
			scope.tx( session -> session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.executeUpdate( "update ColumnPrecisionRow set zone_offset=0 where id=1" );
				}
			} ) );
			assertThatThrownBy( () -> scope.tx( session -> session.find( Row.class, 1 ) ) )
					.isInstanceOf( TemporalValueException.class ).hasMessageContaining( "Partially null" );
		}
	}

	private ContextScope open(String queue, int batch) {
		return ContextScope.open( false, false, Map.of( "hibernate.type.temporal_preservation", true,
				"hibernate.flush.queue.type", queue, "hibernate.jdbc.batch_size", batch ), List.of(), Row.class );
	}

	@Entity(name = "ColumnPrecisionRow")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DynamicUpdate
	static class Row {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@Column(secondPrecision = 9) OffsetDateTime nanos;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		OffsetDateTime automatic;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@Column(secondPrecision = 0) ZonedDateTime fixedZone;
		@Id int id = 1;
		@Version int version;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("America/New_York")
		@Column(name = "zone_value", secondPrecision = 0) @TimeZoneColumn(name = "zone_offset") ZonedDateTime zone;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@Column(name = "offset_value", secondPrecision = 3) @TimeZoneColumn(name = "offset_offset") OffsetDateTime offset;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@Column(name = "time_value", secondPrecision = 6) @TimeZoneColumn(name = "time_offset") OffsetTime time;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("Z")
		@Column(name = "normalized_value", secondPrecision = 3) @TimeZoneColumn(name = "normalized_offset") OffsetDateTime normalized;
	}
}
