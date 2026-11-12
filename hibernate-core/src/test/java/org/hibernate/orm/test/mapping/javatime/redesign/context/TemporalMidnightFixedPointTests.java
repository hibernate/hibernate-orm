package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Independent typed JDBC fixed points qualify H2's prebinding midnight carry.
/// Raw unrounded H2 input still clamps; no claim is made about other providers.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalMidnightFixedPointTests {
	@Test
	void preparedMidnightIsAProviderFixedPoint() {
		try ( var scope = ContextScope.open( false, false, Map.of( "hibernate.type.temporal_preservation", true ), List.of(), Row.class ) ) {
			scope.tx( session -> session.doWork( connection -> {
				final var below = LocalTime.parse( "23:59:59.999999" );
				for ( var input : List.of( below, LocalTime.MIDNIGHT ) ) {
					try ( var statement = connection.prepareStatement( "select cast(? as time(6)), cast(? as time(6) with time zone)" ) ) {
						statement.setObject( 1, input );
						final var offset = input.atOffset( ZoneOffset.ofHoursMinutes( 5, 45 ) );
						statement.setObject( 2, offset );
						try ( var rows = statement.executeQuery() ) {
							assertThat( rows.next() ).isTrue();
							assertThat( rows.getObject( 1, LocalTime.class ) ).isEqualTo( input );
							assertThat( (Object) rows.getObject( 2, OffsetTime.class ) ).isEqualTo( offset );
						}
					}
				}
				for ( String input : List.of( "23:59:59.999999499", "23:59:59.999999500", "23:59:59.999999900" ) ) {
					try ( var statement = connection.prepareStatement( "select cast(? as time(6))" ) ) {
						statement.setObject( 1, LocalTime.parse( input ) );
						try ( var rows = statement.executeQuery() ) {
							assertThat( rows.next() ).isTrue();
							assertThat( rows.getObject( 1, LocalTime.class ) ).isEqualTo( below );
						}
					}
				}
			} ) );
		}
	}
	@Entity(name = "MidnightFixedPointRow") static class Row { @Id int id; }
}
