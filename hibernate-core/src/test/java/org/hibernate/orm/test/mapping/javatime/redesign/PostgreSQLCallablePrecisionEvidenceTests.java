package org.hibernate.orm.test.mapping.javatime.redesign;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetTimeJdbcType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.orm.test.mapping.javatime.redesign.PostgreSQLPrecisionTransportTests.Protocol;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Independent callable evidence. The timestamp identity function uses the full
/// PostgreSQL timestamp carrier; a separate explicit cast owns intentional p3
/// narrowing. Timetz IN capture does not authorize OUT/INOUT extraction.
///
/// @author Steve Ebersole
@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = JdbcSettings.JDBC_TIME_ZONE, value = "Asia/Kathmandu")
})
@DomainModel
@SessionFactory
class PostgreSQLCallablePrecisionEvidenceTests {
	@ParameterizedTest
	@EnumSource(Protocol.class)
	void timestampFunctionAndTimetzInput(Protocol protocol, SessionFactoryScope scope) {
		scope.inSession( session -> {
			try ( var connection = PostgreSQLPrecisionTransportTests.connect( protocol ) ) {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "create temporary table pgpc_time_capture(v time with time zone)" );
					statement.execute( "create function pg_temp.pgpc_echo(v timestamp) returns timestamp language sql as 'select v'" );
					statement.execute( "create function pg_temp.pgpc_narrow(v timestamp) returns timestamp language sql as 'select cast(v as timestamp(3))'" );
					statement.execute( "create procedure pg_temp.pgpc_time_in(v time with time zone) language sql as 'insert into pg_temp.pgpc_time_capture values(v)'" );
					statement.execute( "set time zone 'Asia/Kathmandu'" );
				}
				var options = (SharedSessionContractImplementor) session;
				timestamp( connection, options, protocol );
				timetz( connection, options, protocol );
			}
			catch (SQLException failure) { throw new IllegalStateException( failure ); }
		} );
	}

	private static void timestamp(Connection connection, SharedSessionContractImplementor options, Protocol protocol) throws SQLException {
		var jdbc = new UtcTimestampJdbcType( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE );
		var javaType = OffsetDateTimeJavaType.INSTANCE;
		var methods = new ArrayList<String>();
		var values = new ArrayList<OffsetDateTime>();
		for ( String date : List.of( "1969-12-31T23:59:59", "1999-12-31T23:59:59", "2000-01-01T00:00:00", "2026-12-31T23:59:59" ) ) {
			for ( int nanos : new int[] { 0, 123000000, 123456000, 123499999, 123500000, 999999499, 999999500, 999999999 } ) {
				values.add( LocalDateTime.parse( date ).withNano( nanos ).atOffset( ZoneOffset.ofHoursMinutes( 5, 30 ) ) );
			}
		}
		values.add( null );
		try ( var call = observe( connection.prepareCall( "{? = call pg_temp.pgpc_echo(?)}" ), methods );
				var resultCall = observe( connection.prepareCall( "select pg_temp.pgpc_echo(?)" ), methods ) ) {
			jdbc.registerOutParameter( call, 1 );
			for ( var value : values ) {
				jdbc.getBinder( javaType ).bind( call, value, 2, options );
				if ( protocol == Protocol.SIMPLE ) {
					// This raw OUT form is not Hibernate's PostgreSQL scalar-function path.
					assertThatThrownBy( call::execute ).isInstanceOf( SQLException.class )
							.satisfies( failure -> assertThat( ((SQLException) failure).getSQLState() ).isEqualTo( "42883" ) );
				}
				else {
					call.execute();
					var actual = jdbc.getExtractor( javaType ).extract( call, 1, options );
					if ( value == null || value.getNano() % 1000 == 0 ) {
						assertThat( actual ).isEqualTo( value == null ? null : value.withOffsetSameInstant( ZoneOffset.UTC ) );
					}
					System.out.println( "PGPC_CALL route=R2 protocol=" + protocol + " access=OUT input=" + value + " read=" + actual );
				}
				jdbc.getBinder( javaType ).bind( resultCall, value, 1, options );
				resultCall.execute();
				try ( var rows = resultCall.getResultSet() ) {
					assertThat( rows.next() ).isTrue();
					var result = jdbc.getExtractor( javaType ).extract( rows, 1, options );
					if ( value == null || value.getNano() % 1000 == 0 ) {
						assertThat( result ).isEqualTo( value == null ? null : value.withOffsetSameInstant( ZoneOffset.UTC ) );
					}
					System.out.println( "PGPC_CALL route=R2 protocol=" + protocol + " access=RESULT_SET input=" + value + " read=" + result );
				}
			}
		}
		try ( var call = connection.prepareCall( "select pg_temp.pgpc_narrow(?)" ) ) {
			var value = OffsetDateTime.parse( "2026-07-01T12:00:00.123456Z" );
			jdbc.getBinder( javaType ).bind( call, value, 1, options );
			call.execute();
			try ( var rows = call.getResultSet() ) {
				rows.next();
				assertThat( jdbc.getExtractor( javaType ).extract( rows, 1, options ) ).isEqualTo( value.withNano( 123000000 ) );
			}
		}
		assertThat( methods ).contains( "registerOutParameter:1", "setTimestamp:2", "execute", "getResultSet" );
		if ( protocol != Protocol.SIMPLE ) { assertThat( methods ).contains( "getTimestamp:1" ); }
		System.out.println( "PGPC_METHODS R2 " + protocol + ' ' + methods.stream().distinct().toList() );
	}

	private static void timetz(Connection connection, SharedSessionContractImplementor options, Protocol protocol) throws SQLException {
		var jdbc = PostgreSQLOffsetTimeJdbcType.INSTANCE;
		var javaType = OffsetTimeJavaType.INSTANCE;
		var methods = new ArrayList<String>();
		try ( var call = observe( connection.prepareCall( "{call pg_temp.pgpc_time_in(?)}" ), methods );
				var read = connection.prepareStatement( "select v, v::text from pg_temp.pgpc_time_capture where ?=1" );
				var statement = connection.createStatement() ) {
			read.setInt( 1, 1 );
			for ( OffsetTime value : new OffsetTime[] {
					OffsetTime.parse( "01:00:00.123456+05:30:45" ), OffsetTime.parse( "23:59:59.999999+15:59:59" ),
					OffsetTime.parse( "00:00:00-15:59:59" ), OffsetTime.parse( "23:59:59.999999499+05:30" ),
					OffsetTime.parse( "23:59:59.999999500+05:30" ), OffsetTime.parse( "01:00:00.123499999Z" ), null } ) {
				statement.execute( "set time zone 'Asia/Kathmandu'" );
				jdbc.getBinder( javaType ).bind( call, value, 1, options );
				call.execute();
				statement.execute( "set time zone 'America/Los_Angeles'" );
				try ( var rows = read.executeQuery() ) {
					assertThat( rows.next() ).isTrue();
					var actual = jdbc.getExtractor( javaType ).extract( rows, 1, options );
					if ( value == null || value.getNano() % 1000 == 0 ) { assertThat( actual ).isEqualTo( value ); }
					System.out.println( "PGPC_CALL route=R3 protocol=" + protocol + " input=" + value + " read=" + actual + " server=" + rows.getString( 2 ) );
				}
				statement.executeUpdate( "delete from pg_temp.pgpc_time_capture" );
			}
		}
		assertThat( methods ).contains( "setObject:1", "setNull:1", "execute" );
		System.out.println( "PGPC_METHODS R3 " + protocol + ' ' + methods.stream().distinct().toList() );
	}

	private static CallableStatement observe(CallableStatement statement, List<String> methods) {
		return (CallableStatement) Proxy.newProxyInstance( CallableStatement.class.getClassLoader(),
				new Class<?>[] { CallableStatement.class }, (proxy, method, arguments) -> {
					if ( method.getName().startsWith( "set" ) || method.getName().startsWith( "get" )
							|| method.getName().startsWith( "register" ) || method.getName().startsWith( "execute" ) ) {
						methods.add( method.getName() + (arguments == null || arguments.length == 0 ? "" : ":" + arguments[0]) );
					}
					try { return method.invoke( statement, arguments ); }
					catch (InvocationTargetException failure) { throw failure.getCause(); }
				} );
	}
}
