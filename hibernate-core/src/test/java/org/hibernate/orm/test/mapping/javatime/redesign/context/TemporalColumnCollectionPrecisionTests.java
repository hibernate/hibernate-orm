package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/// A COLUMN tuple belongs to each nested attribute/collection occurrence, including
/// duplicates. Cache publication must retain the values actually assigned.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalColumnCollectionPrecisionTests {
	private static final ZonedDateTime INPUT = Instant.parse( "2026-03-08T06:59:59.999999900Z" ).atZone( ZoneOffset.UTC );
	private static final ZonedDateTime EXPECTED = Instant.parse( "2026-03-08T07:00:00Z" ).atZone( ZoneId.of( "America/New_York" ) );
	@ParameterizedTest
	@CsvSource({ "graph,0", "graph,5", "legacy,0", "legacy,5" })
	void occurrencesAndMutations(String queue, int batch) {
		try ( var scope = ContextScope.open( false, false, Map.of(
				"hibernate.type.temporal_preservation", true, "hibernate.flush.queue.type", queue,
				"hibernate.jdbc.batch_size", batch ), List.of(), Owner.class ) ) {
			final var owner = new Owner();
			final var shared = new Stamp(); shared.value = INPUT;
			owner.nested = new Holder(); owner.nested.stamp = shared;
			owner.repeated = new Stamp(); owner.repeated.value = INPUT.plusSeconds( 2 );
			owner.nested.readOnly = new ReadOnlyStamp(); owner.nested.readOnly.value = INPUT;
			owner.list.add( shared ); owner.list.add( shared );
			owner.bag.add( INPUT ); owner.bag.add( INPUT ); owner.set.add( INPUT );
			scope.tx( session -> session.persist( owner ) );
			assertThat( (Object) shared.value ).isEqualTo( INPUT );
			for ( int phase = 0; phase < 3; phase++ ) {
				if ( phase == 1 ) { scope.factory.getCache().evictAllRegions(); }
				scope.tx( session -> {
					final var loaded = session.find( Owner.class, 1 );
					assertThat( (Object) loaded.repeated.value ).isEqualTo( EXPECTED.plusSeconds( 2 ) );
					assertThat( (Object) loaded.nested.stamp.value ).isEqualTo( EXPECTED );
					assertThat( (Object) loaded.nested.readOnly.value ).isEqualTo( EXPECTED );
					assertThat( loaded.list ).extracting( stamp -> stamp.value ).containsExactly( EXPECTED, EXPECTED );
					assertThat( loaded.bag ).containsExactly( EXPECTED, EXPECTED );
					assertThat( loaded.set ).containsExactly( EXPECTED );
				} );
			}
			scope.tx( session -> {
				final var loaded = session.find( Owner.class, 1 );
					assertThat( (Object) loaded.repeated.value ).isEqualTo( EXPECTED.plusSeconds( 2 ) );
				final var replacement = new Stamp(); replacement.value = INPUT.plusSeconds( 10 );
				loaded.list.set( 0, replacement ); loaded.list.remove( 1 );
				loaded.nested.stamp = replacement;
				loaded.nested.readOnly.value = INPUT.plusSeconds( 10 );
				loaded.bag.remove( 0 ); loaded.set.clear();
			} );
			for ( int phase = 0; phase < 2; phase++ ) {
				if ( phase == 1 ) { scope.factory.getCache().evictAllRegions(); }
				scope.tx( session -> {
					final var loaded = session.find( Owner.class, 1 );
					assertThat( (Object) loaded.repeated.value ).isEqualTo( EXPECTED.plusSeconds( 2 ) );
					assertThat( loaded.list ).extracting( stamp -> stamp.value ).containsExactly( EXPECTED.plusSeconds( 10 ) );
					assertThat( loaded.bag ).containsExactly( EXPECTED );
					assertThat( (Object) loaded.nested.readOnly.value ).isEqualTo( EXPECTED );
					assertThat( loaded.set ).isEmpty();
				} );
			}
		}
	}
	@Entity(name = "ColumnCollectionOwner") @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Owner {
		@Id int id = 1;
		@Embedded Holder nested;
		@Embedded @AttributeOverrides({
				@AttributeOverride(name = "value.instant", column = @Column(name = "repeated_value", secondPrecision = 3)),
				@AttributeOverride(name = "value.zoneOffset", column = @Column(name = "repeated_offset"))
		}) Stamp repeated;
		@ElementCollection @OrderColumn @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		List<Stamp> list = new ArrayList<>();
		@ElementCollection @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("America/New_York")
		@Column(secondPrecision = 0) List<ZonedDateTime> bag = new ArrayList<>();
		@ElementCollection @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("America/New_York")
		@Column(secondPrecision = 0) Set<ZonedDateTime> set = new HashSet<>();
	}
	@Embeddable static class Holder { @Embedded Stamp stamp; @Embedded ReadOnlyStamp readOnly; }
	@Embeddable static class Stamp {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("America/New_York")
		@Column(secondPrecision = 0) ZonedDateTime value;
	}
	@Embeddable static class ReadOnlyStamp {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneAdjustment("America/New_York")
		@Column(name = "readonly_value", secondPrecision = 0, updatable = false)
		@TimeZoneColumn(name = "readonly_offset", updatable = false) ZonedDateTime value;
	}

}
