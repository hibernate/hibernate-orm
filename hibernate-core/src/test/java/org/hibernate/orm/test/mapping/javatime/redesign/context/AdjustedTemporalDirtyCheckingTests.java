package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.BasicType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Domain representation and actual prepared coordinates have distinct roles in
/// adjusted dirty checking, including target-region transitions and enhanced state.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@BytecodeEnhanced(runNotEnhancedAsWell = true, testEnhancedClasses = AdjustedTemporalDirtyCheckingTests.Row.class)
class AdjustedTemporalDirtyCheckingTests {
	@Test
	void targetTransitionDoesNotReplaceSourceRepresentation() {
		try ( var scope = ContextScope.open( false, Map.of( "hibernate.type.temporal_preservation", true ), Row.class ) ) {
			final var persister = scope.factory.getMappingMetamodel().getEntityDescriptor( Row.class );
			final var zone = (BasicType<?>) persister.getPropertyType( "zone" );
			final var stamp = (BasicType<?>) persister.getPropertyType( "stamp" );
			final var micros = (BasicType<?>) persister.getPropertyType( "micros" );
			final var clock = (BasicType<?>) persister.getPropertyType( "clock" );
			final var before = ZonedDateTime.parse( "2026-03-08T06:59:59.999600Z" );
			final var after = ZonedDateTime.parse( "2026-03-08T07:00:00.000100Z" );
			assertThat( zone.isDirty( before, after, null ) ).isFalse();
			assertThat( zone.isDirty( before, after.withZoneSameInstant( ZoneId.of( "UTC" ) ), null ) ).isTrue();
			final var overlap = ZonedDateTime.parse( "2026-11-01T01:30:00-04:00[America/New_York]" );
			assertThat( zone.isDirty( overlap, overlap.withLaterOffsetAtOverlap(), null ) ).isTrue();
			final var first = OffsetDateTime.parse( "2026-03-08T06:59:59.999600Z" );
			final var second = first.withNano( 999_700_000 );
			assertThat( stamp.isDirty( first, second, null ) ).isFalse();
			assertThat( micros.isDirty( first, second, null ) ).isTrue();
			assertThat( stamp.isDirty( first, first.withOffsetSameInstant( ZoneOffset.ofHours( 2 ) ), null ) ).isTrue();
			final var midnight = OffsetTime.parse( "23:59:59.999600+18:00" );
			final var next = OffsetTime.parse( "00:00:00.000100+18:00" );
			assertThat( clock.isDirty( midnight, next, null ) ).isFalse();
			assertThat( clock.isDirty( midnight, midnight.withOffsetSameInstant( ZoneOffset.UTC ), null ) ).isTrue();
			assertThat( clock.isDirty( null, null, null ) ).isFalse();
			assertThat( clock.isDirty( null, next, null ) ).isTrue();
			assertThat( clock.isDirty( midnight, next, new boolean[] { true }, null ) ).isFalse();
			assertThat( clock.isModified( midnight, next, new boolean[] { true }, null ) ).isFalse();
			assertThat( clock.isModified( null, next, new boolean[] { false }, null ) ).isFalse();
		}
	}

	@Test
	void repeatedFlushMergeRefreshAndRollbackUseTheSameComparison() {
		try ( var scope = ContextScope.open( false, Map.of( "hibernate.type.temporal_preservation", true ), Row.class ) ) {
			final var row = new Row(); row.id = 1;
			row.clock = OffsetTime.parse( "01:00:00.123100+18:00" );
			row.stamp = OffsetDateTime.parse( "2026-07-01T00:00:00.123100+05:45" );
			scope.tx( session -> {
				session.persist( row ); session.flush();
				row.clock = row.clock.withNano( 123_200_000 );
				row.stamp = row.stamp.withNano( 123_200_000 );
				session.flush();
			} );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isZero();
			scope.tx( session -> {
				final var merged = session.merge( row );
				session.flush(); session.refresh( merged );
				assertThat( merged.clock ).isEqualTo( OffsetTime.parse( "01:00:00.123+18:00" ) );
				assertThat( merged.stamp ).isEqualTo( OffsetDateTime.parse( "2026-07-01T00:00:00.123+05:45" ) );
			} );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isZero();
			try ( var session = scope.factory.openSession() ) {
				var tx = session.beginTransaction();
				session.find( Row.class, 1 ).clock = OffsetTime.parse( "02:00+18:00" );
				session.flush(); tx.rollback();
			}
			scope.measure();
			scope.tx( session -> {
				final var loaded = session.find( Row.class, 1 );
				assertThat( loaded.clock ).isEqualTo( OffsetTime.parse( "01:00:00.123+18:00" ) );
				loaded.clock = loaded.clock.withNano( 123_300_000 );
			} );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isZero();
			scope.tx( session -> session.find( Row.class, 1 ).clock = null );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
		}
	}

	@Entity(name = "AdjustedDirtyRow")
	static class Row {
		@Id int id;
		@Column(secondPrecision = 3) @TimeZoneAdjustment("+18:00") OffsetTime clock;
		@Column(secondPrecision = 3) @TimeZoneAdjustment("+05:45") OffsetDateTime stamp;
		@Column(secondPrecision = 6) @TimeZoneAdjustment("+05:45") OffsetDateTime micros;
		@Column(secondPrecision = 3) @TimeZoneAdjustment("America/New_York") ZonedDateTime zone;
	}
}
