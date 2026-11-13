package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.List;
import java.util.Properties;
import java.util.function.UnaryOperator;

import org.hibernate.TemporalValueException;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.jdbc.GradleParallelTestingResolver;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Independent ordinary Oracle carrier evidence before stock precision admission.
/// Aligned inputs are an independent fixed-point oracle; raw reduction is recorded
/// separately. No temporal entity or synthetic precision contribution is installed.
///
/// @author Steve Ebersole
@RequiresDialect(value = OracleDialect.class, matchSubTypes = false)
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = JdbcSettings.JDBC_TIME_ZONE, value = "Asia/Kathmandu")
})
@DomainModel
@SessionFactory
class OraclePrecisionEvidenceTests {
	@Test
	void ordinaryStorageAndReconstruction(SessionFactoryScope scope) {
		scope.inSession( session -> {
			for ( int physicalConnection = 0; physicalConnection < 2; physicalConnection++ ) {
				try ( var connection = connect() ) {
					final var options = (SharedSessionContractImplementor) session;
					System.out.println( "OPC_ENV connection=" + physicalConnection + " server=" + connection.getMetaData().getDatabaseProductVersion()
							+ " driver=" + connection.getMetaData().getDriverVersion() + " JVM=" + ZoneId.systemDefault()
							+ " JDBC=" + options.getJdbcTimeZone().getID() );
					for ( var route : routes() ) { storage( connection, route, options ); }
				}
				catch (SQLException failure) { throw new IllegalStateException( failure ); }
			}
		} );
	}

	@Test
	void calendarFiniteBoundaries(SessionFactoryScope scope) {
		scope.inSession( session -> {
			try ( var connection = connect() ) {
				for ( var route : routes() ) {
					if ( !route.zoned ) { boundaries( connection, route, (SharedSessionContractImplementor) session ); }
				}
			}
			catch (SQLException failure) { throw new IllegalStateException( failure ); }
		} );
	}

	@SuppressWarnings("unchecked")
	private static <T extends Temporal> void boundaries(Connection connection, Route<T> route, SharedSessionContractImplementor options) throws SQLException {
		final var minimum = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) );
		minimum.clear(); minimum.set( Calendar.ERA, GregorianCalendar.BC ); minimum.set( 4712, Calendar.JANUARY, 1, 0, 0, 0 );
		try ( var statement = connection.createStatement() ) { statement.execute( "create table opc_boundary(v timestamp(9))" ); }
		try ( var insert = connection.prepareStatement( "insert into opc_boundary values(?)" ); var statement = connection.createStatement() ) {
			for ( var instant : List.of( minimum.toInstant(), Instant.parse( "9999-12-31T23:59:59.999999999Z" ) ) ) {
				final T value = (T) (route.java.getJavaTypeClass() == Instant.class ? instant : instant.atOffset( ZoneOffset.UTC ));
				route.jdbc.getBinder( route.java ).bind( insert, value, 1, options ); insert.executeUpdate();
				try ( var rows = statement.executeQuery( "select v,to_char(v,'YYYY-MM-DD AD HH24:MI:SS.FF9') from opc_boundary" ) ) {
					rows.next(); assertThat( route.jdbc.getExtractor( route.java ).extract( rows, 1, options ) ).isEqualTo( value );
					System.out.println( "OPC_BOUNDARY route=" + route.name + " input=" + value + " server=" + rows.getString( 2 ) );
				}
				statement.executeUpdate( "delete from opc_boundary" );
			}
			for ( var instant : List.of( minimum.toInstant().minusSeconds( 86400 ), Instant.parse( "+10000-01-01T00:00:00Z" ) ) ) {
				final T value = (T) (route.java.getJavaTypeClass() == Instant.class ? instant : instant.atOffset( ZoneOffset.UTC ));
				assertThatThrownBy( () -> route.jdbc.getBinder( route.java ).bind( insert, value, 1, options ) ).isInstanceOf( TemporalValueException.class );
			}
		}
		finally { try ( var statement = connection.createStatement() ) { statement.execute( "drop table opc_boundary purge" ); } }
	}

	static Connection connect() throws SQLException {
		final var settings = Environment.getProperties();
		GradleParallelTestingResolver.resolveFromSettings( settings );
		final var properties = new Properties();
		properties.setProperty( "user", settings.getProperty( JdbcSettings.USER ) );
		properties.setProperty( "password", settings.getProperty( JdbcSettings.PASS ) );
		return DriverManager.getConnection( settings.getProperty( JdbcSettings.URL ), properties );
	}

	private static List<Route<?>> routes() {
		final var stamps = new ArrayList<OffsetDateTime>();
		for ( String date : List.of( "1969-12-31T23:59:59", "1999-12-31T23:59:59", "2026-12-31T23:59:59" ) ) {
			for ( int nanos : new int[] { 0, 123_499_499, 123_499_500, 123_499_999, 123_500_000, 123_500_001,
					499_999_500, 500_000_000, 999_999_499, 999_999_500, 999_999_999 } ) {
				stamps.add( OffsetDateTime.parse( date + "Z" ).withNano( nanos ) );
			}
		}
		stamps.add( OffsetDateTime.parse( "0001-01-01T00:00Z" ) );
		stamps.add( OffsetDateTime.parse( "1582-10-15T00:00Z" ) );
		stamps.add( OffsetDateTime.parse( "2026-03-08T01:59:59.999999900-05:00" ) );
		stamps.add( OffsetDateTime.parse( "2026-07-01T12:00:00.123456789+05:45:30" ) );
		final var times = new ArrayList<OffsetTime>();
		for ( String base : List.of( "00:00:00Z", "01:00:00Z", "23:59:59Z" ) ) {
			for ( int nanos : new int[] { 0, 123_499_499, 123_499_999, 123_500_000, 123_500_001, 999_999_499, 999_999_500, 999_999_999 } ) {
				times.add( OffsetTime.parse( base ).withNano( nanos ) );
			}
		}
		return List.of(
				new Route<>( "O1", OffsetDateTimeJavaType.INSTANCE, new UtcTimestampJdbcType( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE ),
						false, stamps, value -> value.withOffsetSameInstant( ZoneOffset.UTC ) ),
				new Route<>( "O2", OffsetTimeJavaType.INSTANCE, new UtcTimeJdbcType( TimeUtcAsOffsetTimeJdbcType.INSTANCE ),
						true, times, UnaryOperator.identity() ),
				new Route<>( "O3", InstantJavaType.INSTANCE, TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE,
						false, stamps.stream().map( OffsetDateTime::toInstant ).toList(), UnaryOperator.identity() ) );
	}

	private static <T extends Temporal> void storage(Connection connection, Route<T> route, SharedSessionContractImplementor options) throws SQLException {
		for ( int precision : new int[] { 0, 3, 6, 9 } ) {
			final var sqlType = "timestamp(" + precision + ')' + (route.zoned ? " with time zone" : "");
			try ( var statement = connection.createStatement() ) { statement.execute( "create table opc_values(v " + sqlType + ")" ); }
			try ( var insert = connection.prepareStatement( "insert into opc_values values(?)" );
					var read = connection.prepareStatement( "select v,to_char(v,'YYYY-MM-DD HH24:MI:SS.FF9'),sessiontimezone from opc_values" );
					var statement = connection.createStatement() ) {
				final var values = new ArrayList<>( route.values ); values.add( null );
				for ( var source : values ) {
					for ( boolean prepared : new boolean[] { true, false } ) {
						final T value = prepared ? aligned( source, precision ) : source;
						statement.execute( "alter session set time_zone='Asia/Kathmandu'" );
						route.jdbc.getBinder( route.java ).bind( insert, value, 1, options ); insert.executeUpdate();
						statement.execute( "alter session set time_zone='America/Los_Angeles'" );
						try ( var rows = read.executeQuery() ) {
							assertThat( rows.next() ).isTrue();
							final T actual = route.jdbc.getExtractor( route.java ).extract( rows, 1, options );
							final T expected = prepared ? value : expectedReduction( value, precision );
							assertThat( actual ).as( "%s/p%s prepared=%s source=%s", route.name, precision, prepared, source )
									.isEqualTo( expected == null ? null : route.reconstruction.apply( expected ) );
							assertThat( rows.getString( 3 ) ).isEqualTo( "America/Los_Angeles" );
							System.out.println( "OPC_STORAGE route=" + route.name + " p=" + precision + " prepared=" + prepared
									+ " input=" + value + " read=" + actual + " server=" + rows.getString( 2 ) );
						}
						statement.executeUpdate( "delete from opc_values" );
					}
				}
			}
			finally { try ( var statement = connection.createStatement() ) { statement.execute( "drop table opc_values purge" ); } }
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Temporal> T aligned(T value, int precision) {
		if ( value == null ) { return null; }
		int unit = 1; for ( int digit = precision; digit < 9; digit++ ) { unit *= 10; }
		return (T) value.with( ChronoField.NANO_OF_SECOND, value.get( ChronoField.NANO_OF_SECOND ) / unit * unit );
	}

	/// Independent integer arithmetic oracle for the observed direct nanosecond
	/// reduction; does not call the stock precision implementation.
	@SuppressWarnings("unchecked")
	private static <T extends Temporal> T expectedReduction(T value, int precision) {
		if ( value == null ) { return null; }
		int unit = 1;
		for ( int digit = precision; digit < 9; digit++ ) { unit *= 10; }
		final int remainder = value.get( ChronoField.NANO_OF_SECOND ) % unit;
		final long change = 2L * remainder >= unit ? unit - remainder : -remainder;
		return (T) value.plus( change, ChronoUnit.NANOS );
	}

	private record Route<T extends Temporal>(String name, JavaType<T> java, JdbcType jdbc, boolean zoned,
			List<T> values, UnaryOperator<T> reconstruction) {}
}
