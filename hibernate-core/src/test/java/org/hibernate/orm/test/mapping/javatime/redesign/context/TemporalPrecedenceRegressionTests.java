package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.MappingException;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.mapping.BasicValue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// A selected ineligible default stops broader adjustment lookup, while a local
/// legacy UTC contribution remains nearer. Global migration errors name the
/// setting.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalPrecedenceRegressionTests {
	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void skippedNearerRegionDoesNotRetryGlobalUtc( boolean metadata ) {
		for ( Map<String, Object> settings : List.<Map<String, Object>>of( Map.of(),
				Map.of( "hibernate.timezone.default_storage", "NORMALIZE_UTC" ) ) )
			try ( var scope = ContextScope.open( metadata, settings, Times.class ) ) {
				final var row = new Times();
				row.id = 1;
				row.skipped = OffsetTime.parse( "10:15+05:30" );
				row.fixed = row.skipped;
				row.none = row.skipped;
				row.legacy = row.skipped;
				final var value = ( BasicValue ) scope.metadata.getEntityBinding( Times.class.getName() )
						.getProperty( "skipped" ).getValue();
				assertThat( value.resolve().getValueConverter() ).isNull();
				scope.tx( session -> session.persist( row ) );
				scope.tx( session -> {
					var actual = session.find( Times.class, 1 );
					assertThat( actual.skipped ).isEqualTo( row.skipped );
					assertThat( actual.none ).isEqualTo( row.none );
					assertThat( actual.fixed ).isEqualTo( row.fixed.withOffsetSameInstant( ZoneOffset.ofHours( 3 ) ) );
					assertThat( actual.legacy ).isEqualTo( row.legacy.withOffsetSameInstant( ZoneOffset.UTC ) );
				} );
			}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void uncoveredGlobalNamesItsSettingAndRole( boolean metadata ) {
		for ( String policy : List.of( "NORMALIZE", "AUTO", "DEFAULT" ) ) {
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( metadata, true,
						Map.of( "hibernate.timezone.default_storage", policy ), Uncovered.class ) ) {
				}
			} ).isInstanceOf( MappingException.class ).hasMessageContaining( "hibernate.timezone.default_storage" )
					.hasMessageContaining( policy ).hasMessageContaining( "stamp" ).hasMessageContaining( "storage" );
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void ineligibleLocalAndInstantDefaultsStopBeforeGlobal( boolean metadata ) {
		try ( var scope = ContextScope.boot( metadata, true,
				Map.of( "hibernate.timezone.default_storage", "NORMALIZE_UTC" ), LocalControls.class ) ) {
			for ( String member : List.of( "date", "time", "stamp", "instant", "none" ) ) {
				final var value = ( BasicValue ) scope.metadata.getEntityBinding( LocalControls.class.getName() )
						.getProperty( member ).getValue();
				assertThat( value.resolve().getValueConverter() ).as( member ).isNull();
			}
		}
	}

	@Entity( name = "DeclarationLocalControls" )
	@TimeZoneAdjustment( "America/New_York" )
	static class LocalControls {
		@Id
		int id;
		LocalDate date;
		LocalTime time;
		LocalDateTime stamp;
		Instant instant;
		@TimeZoneAdjustment( TimeZoneAdjustment.NONE )
		LocalDateTime none;
	}

	@MappedSuperclass
	@TimeZoneAdjustment( "+04:00" )
	static class Farther {
		@Id
		int id;
	}

	@Entity( name = "DeclarationTimes" )
	@TimeZoneAdjustment( "America/New_York" )
	static class Times extends Farther {
		OffsetTime skipped;
		@TimeZoneAdjustment( "+03:00" )
		OffsetTime fixed;
		@TimeZoneAdjustment( TimeZoneAdjustment.NONE )
		OffsetTime none;
		@TimeZoneStorage( TimeZoneStorageType.NORMALIZE_UTC )
		OffsetTime legacy;
	}

	@Entity( name = "DeclarationUncovered" )
	static class Uncovered {
		@Id
		int id;
		OffsetDateTime stamp;
	}
}
