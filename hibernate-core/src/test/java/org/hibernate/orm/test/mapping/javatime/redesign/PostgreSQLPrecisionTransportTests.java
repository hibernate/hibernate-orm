package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.dialect.temporaltype.internal.StandardTemporalPrecision;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetDateTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLUtcTimeJdbcType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.jdbc.GradleParallelTestingResolver;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.hibernate.TemporalValueException;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Independent PostgreSQL carrier evidence, runnable before temporal mapping admission.
/// Prepared fixed points have an independent oracle; unprepared driver/server values
/// characterize reduction separately. Protocol configurations use separate connections.
/// No production contribution or temporal entity is installed by this fixture.
///
/// @author Steve Ebersole
@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = JdbcSettings.JDBC_TIME_ZONE, value = "Asia/Kathmandu")
})
@DomainModel
@SessionFactory
class PostgreSQLPrecisionTransportTests {
	enum Protocol { DEFAULT, TEXT, BINARY, SIMPLE }

	@ParameterizedTest
	@EnumSource(Protocol.class)
	void storageFixedPointsAndReduction(Protocol protocol, SessionFactoryScope scope) {
		scope.inSession( session -> {
			try ( var connection = connect( protocol ) ) {
				var options = (SharedSessionContractImplementor) session;
				System.out.println( "PGPC_ENV protocol=" + protocol + " server=" + connection.getMetaData().getDatabaseProductVersion()
						+ " driver=" + connection.getMetaData().getDriverVersion() + " JVM=" + ZoneId.systemDefault()
						+ " JDBC=" + options.getJdbcTimeZone().getID() );
				for ( var route : routes() ) {
					storage( connection, route, options, protocol );
				}
			}
			catch (SQLException failure) { throw new IllegalStateException( failure ); }
		} );
	}

	@Test
	void timestampFiniteBoundaries(SessionFactoryScope scope) {
		scope.inSession( session -> {
			var options = (SharedSessionContractImplementor) session;
			try ( var connection = connect( Protocol.DEFAULT ) ) {
				for ( JdbcType jdbc : List.of( PostgreSQLOffsetDateTimeJdbcType.INSTANCE,
						new UtcTimestampJdbcType( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE ) ) ) {
					boolean calendar = jdbc instanceof UtcTimestampJdbcType;
					try ( var statement = connection.createStatement() ) {
						statement.execute( "create temporary table pgpc_boundary(v timestamp(6)" + (calendar ? "" : " with time zone") + ")" );
					}
					try ( var insert = connection.prepareStatement( "insert into pgpc_boundary values(?)" ); var statement = connection.createStatement() ) {
						final var encodedMinimum = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) );
						encodedMinimum.clear();
						encodedMinimum.set( Calendar.ERA, GregorianCalendar.BC );
						encodedMinimum.set( 4713, Calendar.JANUARY, 1, 0, 0, 0 );
						final var minimum = calendar ? encodedMinimum.toInstant().atOffset( ZoneOffset.UTC )
								: OffsetDateTime.parse( "-4712-01-01T00:00:00Z" );
						assertThatThrownBy( () -> jdbc.getBinder( OffsetDateTimeJavaType.INSTANCE ).bind( insert, minimum.minusDays( 1 ), 1, options ) )
								.isInstanceOf( TemporalValueException.class );
						for ( String text : List.of( minimum.toString(), "+294276-12-31T23:59:59.999999Z" ) ) {
							var value = OffsetDateTime.parse( text );
							jdbc.getBinder( OffsetDateTimeJavaType.INSTANCE ).bind( insert, value, 1, options );
							insert.executeUpdate();
							try ( var rows = statement.executeQuery( "select v, v::text from pgpc_boundary" ) ) {
								rows.next();
								assertThat( jdbc.getExtractor( OffsetDateTimeJavaType.INSTANCE ).extract( rows, 1, options ) ).isEqualTo( value );
								System.out.println( "PGPC_BOUNDARY calendar=" + calendar + " value=" + value + " server=" + rows.getString( 2 ) );
							}
							statement.executeUpdate( "delete from pgpc_boundary" );
						}
						assertThatThrownBy( () -> jdbc.getBinder( OffsetDateTimeJavaType.INSTANCE ).bind( insert, OffsetDateTime.MAX, 1, options ) )
								.isInstanceOf( TemporalValueException.class );
					}
					finally { try ( var statement = connection.createStatement() ) { statement.execute( "drop table pgpc_boundary" ); } }
				}
			}
			catch (SQLException failure) { throw new IllegalStateException( failure ); }
		} );
	}

	static Connection connect(Protocol protocol) throws SQLException {
		var environment = Environment.getProperties();
		GradleParallelTestingResolver.resolveFromSettings( environment );
		var properties = new Properties();
		properties.setProperty( "user", environment.getProperty( JdbcSettings.USER ) );
		properties.setProperty( "password", environment.getProperty( JdbcSettings.PASS ) );
		switch ( protocol ) {
			case TEXT -> {
				properties.setProperty( "binaryTransfer", "false" );
				properties.setProperty( "prepareThreshold", "0" );
			}
			case BINARY -> {
				properties.setProperty( "binaryTransfer", "true" );
				properties.setProperty( "prepareThreshold", "-1" );
			}
			case SIMPLE -> properties.setProperty( "preferQueryMode", "simple" );
			default -> {}
		}
		return DriverManager.getConnection( environment.getProperty( JdbcSettings.URL ), properties );
	}

	private static List<Route<?>> routes() {
		var timestamps = new ArrayList<OffsetDateTime>();
		for ( String base : List.of( "1969-12-31T23:59:59", "1999-12-31T23:59:59", "2000-01-01T00:00:00", "2026-12-31T23:59:59" ) ) {
			for ( int nanos : new int[] { 0, 123499499, 123499500, 123499999, 123500000, 123500001,
					499999500, 500000000, 500000500, 999999499, 999999500, 999999999 } ) {
				timestamps.add( LocalDateTime.parse( base ).withNano( nanos ).atOffset( ZoneOffset.UTC ) );
			}
		}
		timestamps.add( OffsetDateTime.parse( "2026-03-08T01:59:59.999999-05:00" ) );
		timestamps.add( OffsetDateTime.parse( "2026-11-01T01:59:59.999999-04:00" ) );
		timestamps.add( OffsetDateTime.parse( "2026-07-01T12:00:00.123456+05:45:30" ) );
		timestamps.add( OffsetDateTime.parse( "0001-01-01T00:00:00Z" ) );
		timestamps.add( OffsetDateTime.parse( "9999-12-31T23:59:59.999999Z" ) );
		var times = new ArrayList<OffsetTime>();
		for ( int nanos : new int[] { 0, 123499499, 123499500, 123499999, 123500000, 123500001, 999999499, 999999500, 999999999 } ) {
			times.add( OffsetTime.parse( "23:59:59+05:45:30" ).withNano( nanos ) );
			times.add( OffsetTime.parse( "01:00:00-15:59:59" ).withNano( nanos ) );
		}
		times.add( OffsetTime.parse( "00:00:00+15:59:59" ) );
		return List.of(
				new Route<>( "R1", OffsetDateTimeJavaType.INSTANCE, PostgreSQLOffsetDateTimeJdbcType.INSTANCE,
						"timestamp", " with time zone", timestamps, value -> value.withOffsetSameInstant( ZoneOffset.UTC ), Object::toString ),
				new Route<>( "R2", OffsetDateTimeJavaType.INSTANCE, new UtcTimestampJdbcType( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE ),
						"timestamp", " without time zone", timestamps, value -> value.withOffsetSameInstant( ZoneOffset.UTC ),
						value -> value.withOffsetSameInstant( ZoneOffset.UTC ).toLocalDateTime().toString() ),
				new Route<>( "R5", InstantJavaType.INSTANCE, TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE,
						"timestamp", " without time zone", timestamps.stream().map( OffsetDateTime::toInstant ).toList(),
						UnaryOperator.identity(), value -> value.atOffset( ZoneOffset.UTC ).toLocalDateTime().toString() ),
				new Route<>( "R3", OffsetTimeJavaType.INSTANCE, PostgreSQLOffsetTimeJdbcType.INSTANCE,
						"time", " with time zone", times, UnaryOperator.identity(), Object::toString ),
				new Route<>( "R4", OffsetTimeJavaType.INSTANCE, new UtcTimeJdbcType( PostgreSQLUtcTimeJdbcType.INSTANCE ),
						"time", " without time zone", times.stream().map( value -> value.withOffsetSameInstant( ZoneOffset.UTC ) ).toList(),
						UnaryOperator.identity(), value -> value.toLocalTime().toString() ) );
	}

	private static <T extends Temporal> void storage(Connection connection, Route<T> route,
			SharedSessionContractImplementor options, Protocol protocol) throws SQLException {
		for ( int precision : new int[] { 0, 3, 6 } ) {
			var sqlType = route.base + '(' + precision + ')' + route.suffix;
			try ( var statement = connection.createStatement() ) {
				statement.execute( "create temporary table pgpc_values(v " + sqlType + ")" );
			}
			try ( var insert = connection.prepareStatement( "insert into pgpc_values values (?)" );
					var read = connection.prepareStatement( "select v, v::text, current_setting('TimeZone') from pgpc_values where ?=1" );
					var statement = connection.createStatement() ) {
				read.setInt( 1, 1 );
				var values = new ArrayList<>( route.values );
				values.add( null );
				int iteration = 0;
				for ( var source : values ) {
					for ( boolean prepared : new boolean[] { true, false } ) {
						// Aligned values prove fixed points, without choosing the production rounding rule.
						final T value = prepared ? aligned( source, precision ) : source;
						statement.execute( "set time zone 'Asia/Kathmandu'" );
						route.jdbc.getBinder( route.javaType ).bind( insert, value, 1, options );
						insert.executeUpdate();
						statement.execute( "set time zone 'America/Los_Angeles'" );
						try ( var rows = read.executeQuery() ) {
							assertThat( rows.next() ).isTrue();
							var actual = route.jdbc.getExtractor( route.javaType ).extract( rows, 1, options );
							final var contract = StandardTemporalPrecision.resolve( new TemporalPrecisionContext(
									route.javaType, route.jdbc, options.getDialect(), route.jdbc.getDdlTypeCode(), sqlType,
									precision, precision, null, null, false, route.name, TemporalPrecisionContext.Operation.ATTRIBUTE,
									TemporalPreservationPolicy.Type.NORMALIZED, TimeZoneRepresentation.Type.INLINE,
									route.name.equals( "R2" ) || route.name.equals( "R4" ) ? ZoneOffset.UTC : null ) );
							{
							assertThat( contract.isSupported() ).as( contract.getUnsupportedReason() ).isTrue();
							assertThat( contract.reconstruct( contract.quantize( value ) ) ).as( "%s/%s/p%s raw reduction %s", route.name, protocol, precision, value ).isEqualTo( actual );
							}
							if ( prepared ) { assertThat( actual ).as( "%s/%s/p%s fixed point %s", route.name, protocol, precision, source )
									.isEqualTo( value == null ? null : route.reconstruction.apply( value ) ); }
							assertThat( rows.getString( 3 ) ).isEqualTo( "America/Los_Angeles" );
							var bytes = rows.getBytes( 1 );
							System.out.println( "PGPC_VALUE route=" + route.name + " protocol=" + protocol + " p=" + precision
									+ " prepared=" + prepared + " input=" + value + " read=" + actual + " server=" + rows.getString( 2 )
									+ " bytes=" + (bytes == null ? 0 : bytes.length) + " iteration=" + iteration++ );
						}
						statement.executeUpdate( "delete from pgpc_values" );
						if ( !prepared && source != null ) {
							try ( var rows = statement.executeQuery( "select cast('" + route.literal.apply( source ) + "' as " + sqlType + ")::text" ) ) {
								rows.next();
								System.out.println( "PGPC_RAW route=" + route.name + " p=" + precision + " input=" + source + " server=" + rows.getString( 1 ) );
							}
						}
					}
				}
			}
			finally {
				try ( var statement = connection.createStatement() ) { statement.execute( "drop table pgpc_values" ); }
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Temporal> T aligned(T value, int precision) {
		if ( value == null ) { return null; }
		int quantum = 1;
		for ( int digit = precision; digit < 9; digit++ ) { quantum *= 10; }
		return (T) value.with( ChronoField.NANO_OF_SECOND, value.get( ChronoField.NANO_OF_SECOND ) / quantum * quantum );
	}

	private record Route<T extends Temporal>(String name, JavaType<T> javaType, JdbcType jdbc,
			String base, String suffix, List<T> values, UnaryOperator<T> reconstruction, Function<T, String> literal) {}
}
