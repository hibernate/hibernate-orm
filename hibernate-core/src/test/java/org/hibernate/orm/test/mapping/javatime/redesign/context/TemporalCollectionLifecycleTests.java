package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

/// Mutable cached sets, ordered lists and bags preserve element contexts and
/// retain real changes while unchanged assembly/flush performs no mutation DML.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalCollectionLifecycleTests {
	@ParameterizedTest @ValueSource(booleans={true,false})
	void cachedCollectionsRetainTargetsOrderMultiplicityAndRealChanges(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Event.class ) ) {
			final var input = OffsetDateTime.parse( "2026-07-01T12:00+02:00" );
			final var time = OffsetTime.parse( "01:00+02:00" );
			final var zone = input.atZoneSameInstant( ZoneId.of( "Europe/Paris" ) );
			final var source = new Event(); source.id=1;
			source.values.add( input ); source.times.addAll( List.of( time, time.plusMinutes(1) ) ); source.bag.addAll( List.of( zone, zone ) );
			scope.tx( session -> { session.persist( source ); session.flush(); scope.measure(); session.flush(); scope.noMutation(); } );
			assertThat( source.values ).containsExactly( input ); assertThat( source.times ).containsExactly( time, time.plusMinutes(1) ); assertThat( source.bag ).containsExactly( zone, zone );
			// Initial collection population may occur on the first load; measure its
			// own puts/hits independently from entity caching.
			scope.factory.getCache().evictAllRegions(); scope.measure();
			scope.tx( session -> check( session.find( Event.class, 1 ), input, time, zone, false ) );
			for ( String role : roles() ) assertThat( scope.factory.getStatistics().getDomainDataRegionStatistics( role ).getPutCount() ).as(role).isPositive();
			scope.measure();
			scope.tx( session -> { check( session.find( Event.class, 1 ), input, time, zone, false ); session.flush(); } );
			assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 4 ); scope.noSelect(); scope.noMutation();
			for ( String role : roles() ) assertThat( scope.factory.getStatistics().getDomainDataRegionStatistics( role ).getHitCount() ).as(role).isEqualTo(1);
			scope.measure();
			scope.tx( session -> {
				final var row = session.find( Event.class, 1 );
				row.values.clear(); row.values.add( input.plusMinutes(2) );
				row.times.remove(0); row.times.add( time.plusMinutes(2) );
				row.bag.remove(0); row.bag.add( zone.plusMinutes(2) );
				session.flush();
			} );
			assertThat( scope.factory.getStatistics().getCollectionUpdateCount() ).isEqualTo(3);
			// Test automatic update/invalidation before explicit eviction; otherwise a
			// stale collection entry could be hidden by the database-origin control.
			scope.tx(session -> { check(session.find(Event.class,1),input,time,zone,true); session.flush(); });
			assertThat(source.values).containsExactly(input);
			assertThat(source.times).containsExactly(time,time.plusMinutes(1));
			assertThat(source.bag).containsExactly(zone,zone);
			scope.factory.getCache().evictAllRegions();
			scope.tx( session -> check( session.find(Event.class,1),input,time,zone,true) );
			scope.measure();
			scope.tx( session -> {check(session.find(Event.class,1),input,time,zone,true);session.flush();} );
			scope.noSelect();scope.noMutation();assertThat(scope.factory.getStatistics().getSecondLevelCacheHitCount()).isEqualTo(4);
		}
	}
	static List<String> roles() { return List.of(Event.class.getName()+".values",Event.class.getName()+".times",Event.class.getName()+".bag"); }
	static void check(Event row,OffsetDateTime input,OffsetTime time,ZonedDateTime zone,boolean changed) {
		final var adjusted = input.atZoneSameInstant(ZoneId.of("America/New_York")).toOffsetDateTime();
		final var fixed = time.withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5,30));
		final var raw = zone.toInstant().atZone(zone.getOffset());
		assertThat(row.values).containsExactly(changed?adjusted.plusMinutes(2):adjusted);
		assertThat(row.times).containsExactly(changed?fixed.plusMinutes(1):fixed,changed?fixed.plusMinutes(2):fixed.plusMinutes(1));
		assertThat(row.bag).containsExactlyInAnyOrder(raw,changed?raw.plusMinutes(2):raw);
	}
	@Entity(name="ContextCollectionEvent") @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	static class Event {
		@Id int id;
		@ElementCollection @Cache(usage=CacheConcurrencyStrategy.READ_WRITE) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("America/New_York") Set<OffsetDateTime> values=new HashSet<>();
		@ElementCollection @OrderColumn(name="position_index") @Cache(usage=CacheConcurrencyStrategy.READ_WRITE) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("+05:30") List<OffsetTime> times=new ArrayList<>();
		@ElementCollection @Cache(usage=CacheConcurrencyStrategy.READ_WRITE) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) List<ZonedDateTime> bag=new ArrayList<>();
	}
}
