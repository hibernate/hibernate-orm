package org.hibernate.orm.test.mapping.embeddable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLDataException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.SQLXML;

/// Executes older-profile expressions on the actual configured Oracle server.
/// This is expression evidence, not a claim of live Oracle19/21 ORM coverage.
///
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
@DomainModel
@SessionFactory
class OracleLocalTimeAggregateExpressionTests {
	@ParameterizedTest
	@ValueSource(ints = {19, 21})
	void textExpressionsIgnoreSessionFormatting(int profile, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.doWork( connection -> {
			final String originalZone = scalar( connection, "select sessiontimezone from dual" );
			final String originalDate = scalar( connection,
					"select value from nls_session_parameters where parameter='NLS_DATE_FORMAT'" );
			final String originalTimestamp = scalar( connection,
					"select value from nls_session_parameters where parameter='NLS_TIMESTAMP_FORMAT'" );
			try {
				System.out.println( "ORACLE_TIME_TEXT profile=" + profile + " actualServer="
						+ connection.getMetaData().getDatabaseProductVersion() + " driver="
						+ connection.getMetaData().getDriverVersion() );
				alter( connection, "NLS_DATE_FORMAT", "DD/MM/YYYY" );
				alter( connection, "NLS_TIMESTAMP_FORMAT", "DD/MM/YYYY HH24:MI:SS" );
				for ( String zone : new String[] { "+05:30", "-04:00" } ) {
					alter( connection, "TIME_ZONE", zone );
					for ( int format : new int[] { JSON, SQLXML } ) {
						for ( String time : Arrays.asList( "00:00", "01:02:03", "12:34:56.123",
								"23:59:59.123456789", null ) ) {
							assertRead( connection, profile, format, time, false );
							assertRead( connection, profile, format, time, true );
						}
						assertMissing( connection, profile, format );
					}
					assertWriteRead( connection, profile );
					assertXmlWriteRead( connection, profile );
				}
			}
			finally {
				alter( connection, "TIME_ZONE", originalZone );
				alter( connection, "NLS_DATE_FORMAT", originalDate );
				alter( connection, "NLS_TIMESTAMP_FORMAT", originalTimestamp );
			}
		} ) );
	}

	private static String projection(int profile, int format, boolean nested) {
		return OracleLocalTimeAggregateSqlTests.read( profile, format,
				format == JSON ? "doc" : "case when doc is null then null else xmltype(doc) end",
				nested ? (format == JSON ? "nested.value" : "nested/value") : "value",
				OracleLocalTimeAggregateSqlTests.TIME );
	}

	private static void assertRead(Connection connection, int profile, int format, String time, boolean nested)
			throws SQLException {
		String doc = format == JSON ? "{\"value\":" + (time == null ? "null" : "\"" + time + "\"") + "}"
				: "<value>" + (time == null ? "" : time) + "</value>";
		if ( nested ) {
			doc = format == JSON ? "{\"nested\":" + doc + "}" : "<nested>" + doc + "</nested>";
		}
		if ( format == SQLXML ) {
			doc = "<e>" + doc + "</e>";
		}
		final LocalDateTime expected = time == null ? null : LocalTime.parse( time ).atDate( LocalDate.of( 1970, 1, 1 ) );
		final String read = projection( profile, format, nested );
		try ( var statement = connection.prepareStatement( "select " + read + ",case when " + read
				+ "=expected then 1 else 0 end from (select cast(? as varchar2(4000)) doc,cast(? as timestamp(9)) expected from dual)" ) ) {
			statement.setString( 1, doc );
			statement.setObject( 2, expected );
			try ( var result = statement.executeQuery() ) {
				assertThat( result.next() ).isTrue();
				assertThat( result.getObject( 1, LocalDateTime.class ) ).isEqualTo( expected );
				assertThat( result.getInt( 2 ) ).isEqualTo( expected == null ? 0 : 1 );
			}
		}
	}

	private static void assertMissing(Connection connection, int profile, int format) throws SQLException {
		try ( var statement = connection.prepareStatement( "select " + projection( profile, format, false )
				+ " from (select cast(? as varchar2(4000)) doc from dual)" ) ) {
			for ( String doc : Arrays.asList( format == JSON ? "{}" : "<e/>", null ) ) {
				statement.setString( 1, doc );
				try ( var result = statement.executeQuery() ) {
					assertThat( result.next() ).isTrue();
					assertThat( result.getObject( 1 ) ).isNull();
				}
			}
			for ( String invalid : new String[] { "1970-01-01T01:00:00", "01:02:03.1234567891", "25:00:00" } ) {
				statement.setString( 1, format == JSON ? "{\"value\":\"" + invalid + "\"}"
						: "<e><value>" + invalid + "</value></e>" );
				assertThatThrownBy( statement::executeQuery ).isInstanceOf( SQLDataException.class )
						.hasMessageContaining( "ORA-018" );
			}
		}
	}

	private static void assertWriteRead(Connection connection, int profile) throws SQLException {
		final String write = OracleLocalTimeAggregateSqlTests.write( profile, "cast(? as timestamp(9))",
				OracleLocalTimeAggregateSqlTests.TIME );
		try ( var statement = connection.prepareStatement( "select " + write + " from dual" ) ) {
			for ( LocalTime time : Arrays.asList( LocalTime.MIDNIGHT, LocalTime.of( 12, 34, 56, 123456789 ), null ) ) {
				statement.setObject( 1, time == null ? null : time.atDate( LocalDate.of( 1970, 1, 1 ) ) );
				try ( var result = statement.executeQuery() ) {
					assertThat( result.next() ).isTrue();
					final String text = result.getString( 1 );
					if ( time == null ) {
						assertThat( text ).isNull();
					}
					else {
						assertThat( LocalTime.parse( text ) ).isEqualTo( time );
						final String writerZone = scalar( connection, "select sessiontimezone from dual" );
						alter( connection, "TIME_ZONE", writerZone.equals( "+05:30" ) ? "-04:00" : "+05:30" );
						try {
							assertRead( connection, profile, JSON, text, false );
						}
						finally {
							alter( connection, "TIME_ZONE", writerZone );
						}
					}
				}
			}
		}
	}

	private static void assertXmlWriteRead(Connection connection, int profile) throws SQLException {
		final String write = OracleLocalTimeAggregateSqlTests.xmlWrite( profile, "cast(? as timestamp(9))" );
		final String read = OracleLocalTimeAggregateSqlTests.read( profile, SQLXML, "doc", "value",
				OracleLocalTimeAggregateSqlTests.TIME );
		try ( var statement = connection.prepareStatement( "select " + read
				+ ",xmlserialize(content doc as varchar2(4000)) from (select " + write + " doc from dual)" ) ) {
			for ( LocalTime time : Arrays.asList( LocalTime.MIDNIGHT, LocalTime.of( 23, 59, 59, 123456789 ), null ) ) {
				final var expected = time == null ? null : time.atDate( LocalDate.of( 1970, 1, 1 ) );
				statement.setObject( 1, expected );
				try ( var result = statement.executeQuery() ) {
					assertThat( result.next() ).isTrue();
					assertThat( result.getObject( 1, LocalDateTime.class ) ).isEqualTo( expected );
					assertThat( result.getString( 2 ) ).doesNotContain( "1970", "T00:", "T23:" );
				}
			}
		}
	}

	private static String scalar(Connection connection, String sql) throws SQLException {
		try ( var statement = connection.createStatement(); var result = statement.executeQuery( sql ) ) {
			result.next();
			return result.getString( 1 );
		}
	}

	private static void alter(Connection connection, String setting, String value) throws SQLException {
		try ( var statement = connection.createStatement() ) {
			statement.execute( "alter session set " + setting + "='" + value.replace( "'", "''" ) + "'" );
		}
	}
}
