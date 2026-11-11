package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Independent server equality distinguishes write-side truncation from extraction.
/// jTDS deliberately does not map BIGDATETIME and must not execute this fixture.
///
/// @author Steve Ebersole
@RequiresDialectFeature(feature = SybaseCachePrecisionSupport.JConnect.class)
@DomainModel
@SessionFactory
class SybaseTimestampPrecisionTests {
	@ParameterizedTest
	@ValueSource(strings = { "123456000", "123456100", "123456500", "123456789", "999999900" })
	void timestampWritesTruncateToMicroseconds(String fraction, SessionFactoryScope scope) {
		final var input = Instant.parse( "2026-03-08T06:59:59." + fraction + "Z" );
		final var expected = Instant.parse( "2026-03-08T06:59:59." + fraction.substring( 0, 6 ) + "Z" );
		final var literal = "2026-03-08 06:59:59." + fraction.substring( 0, 6 );
		scope.inTransaction( session -> session.doWork( connection -> {
			System.out.println( "SYBASE_PRECISION_DRIVER " + connection.getMetaData().getDriverVersion()
					+ " database=" + connection.getMetaData().getDatabaseProductVersion() );
			try ( var statement = connection.createStatement() ) {
				statement.execute( "create table #temporal_precision (id int, stamp bigdatetime null)" );
			}
			try {
				for ( int mode = 0; mode < 4; mode++ ) {
					try ( var statement = connection.prepareStatement( "insert into #temporal_precision values (?,?)" ) ) {
						statement.setInt( 1, mode );
						switch ( mode ) {
							case 0 -> statement.setTimestamp( 2, Timestamp.valueOf( LocalDateTime.ofInstant( input, ZoneOffset.UTC ) ) );
							case 1 -> statement.setTimestamp( 2, Timestamp.from( input ), utc() );
							case 2 -> statement.setString( 2, literal );
							case 3 -> statement.setNull( 2, Types.TIMESTAMP );
						}
						assertThat( statement.executeUpdate() ).isEqualTo( 1 );
					}
				}
				try ( var statement = connection.createStatement() ) {
					statement.executeUpdate( "insert into #temporal_precision values (4,convert(bigdatetime,'" + literal + "'))" );
				}
				try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
						"select id,stamp,case when stamp=convert(bigdatetime,'" + literal
								+ "') then 1 when stamp is null then 2 else 0 end from #temporal_precision order by id" ) ) {
					for ( int id = 0; id < 5; id++ ) {
						assertThat( rows.next() ).isTrue();
						assertThat( rows.getInt( 1 ) ).isEqualTo( id );
						assertThat( rows.getInt( 3 ) ).as( "server equality for binding mode " + id ).isEqualTo( id == 3 ? 2 : 1 );
						if ( id == 3 ) {
							assertThat( rows.getTimestamp( 2 ) ).isNull();
							assertThat( rows.getTimestamp( 2, utc() ) ).isNull();
						}
						else {
							assertThat( rows.getTimestamp( 2 ).toLocalDateTime() ).isEqualTo( LocalDateTime.ofInstant( expected, ZoneOffset.UTC ) );
							assertThat( rows.getTimestamp( 2, utc() ).toInstant() ).isEqualTo( expected );
						}
					}
					assertThat( rows.next() ).isFalse();
				}
			}
			finally {
				try ( var statement = connection.createStatement() ) { statement.execute( "drop table #temporal_precision" ); }
			}
		}));
	}

	private static Calendar utc() { return Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ); }
}
