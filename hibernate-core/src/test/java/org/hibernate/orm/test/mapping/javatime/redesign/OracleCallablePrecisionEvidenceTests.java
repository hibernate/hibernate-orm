package org.hibernate.orm.test.mapping.javatime.redesign;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Raw callable transport evidence, separate from column precision or ORM mapping
/// admission. The actual setters, registrations and extractors remain observable.
///
/// @author Steve Ebersole
@RequiresDialect(value = OracleDialect.class, matchSubTypes = false)
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = JdbcSettings.JDBC_TIME_ZONE, value = "Asia/Kathmandu")
})
@DomainModel
@SessionFactory
class OracleCallablePrecisionEvidenceTests {
	@Test
	void timestampFunction(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var options = (SharedSessionContractImplementor) session;
			final var jdbc = new UtcTimestampJdbcType( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE );
			try ( var connection = OraclePrecisionEvidenceTests.connect(); var statement = connection.createStatement() ) {
				statement.execute( "create table opc_stamp_capture(v timestamp(9))" );
				statement.execute( "create function opc_echo(v in timestamp) return timestamp as begin insert into opc_stamp_capture values(v); return v; end;" );
				try {
					final var values = new ArrayList<OffsetDateTime>();
					for ( String text : List.of( "1969-12-31T23:59:59.123456789Z", "2026-12-31T23:59:59.999999999+05:45:30", "2026-01-01T00:00Z" ) ) {
						values.add( OffsetDateTime.parse( text ) );
					}
					values.add( null );
					for ( var value : values ) {
						statement.execute( "alter session set time_zone='Asia/Kathmandu'" );
						final var methods = new ArrayList<String>();
						try ( var call = observed( connection.prepareCall( "{? = call opc_echo(?)}" ), methods ) ) {
							jdbc.registerOutParameter( call, 1 );
							jdbc.getBinder( OffsetDateTimeJavaType.INSTANCE ).bind( call, value, 2, options );
							call.execute();
							statement.execute( "alter session set time_zone='America/Los_Angeles'" );
							final var actual = jdbc.getExtractor( OffsetDateTimeJavaType.INSTANCE ).extract( call, 1, options );
							final var expected = value == null ? null : value.getNano() == 0 ? value
									: OffsetDateTime.parse( value.getNano() == 123_456_789
											? "1969-12-31T23:59:59.123457Z" : "2026-12-31T18:14:30Z" );
							assertThat( actual ).isEqualTo( expected );
							try ( var rows = statement.executeQuery( "select to_char(v,'YYYY-MM-DD HH24:MI:SS.FF9') from opc_stamp_capture" ) ) {
								rows.next(); System.out.println( "OPC_CAPTURE O1 input=" + value + " server=" + rows.getString( 1 ) );
							}
							statement.executeUpdate( "delete from opc_stamp_capture" );
							System.out.println( "OPC_CALL O1 input=" + value + " output=" + actual + " methods=" + methods );
						}
					}
				}
				finally { statement.execute( "drop function opc_echo" ); statement.execute( "drop table opc_stamp_capture purge" ); }
			}
			catch (SQLException failure) { throw new IllegalStateException( failure ); }
		} );
	}

	@Test
	void timeProcedureDirections(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var options = (SharedSessionContractImplementor) session;
			final var jdbc = new UtcTimeJdbcType( TimeUtcAsOffsetTimeJdbcType.INSTANCE );
			try ( var connection = OraclePrecisionEvidenceTests.connect(); var statement = connection.createStatement() ) {
				statement.execute( "create table opc_time_capture(a timestamp(9) with time zone,c timestamp(9) with time zone)" );
				statement.execute( "create procedure opc_time(a in timestamp with time zone,b out timestamp with time zone,c in out timestamp with time zone) as begin insert into opc_time_capture values(a,c); b := a; end;" );
				try {
					final var values = new ArrayList<OffsetTime>();
					for ( String text : List.of( "00:00Z", "12:34:56.123456789Z", "23:59:59.999999999Z" ) ) { values.add( OffsetTime.parse( text ) ); }
					values.add( null );
					for ( var value : values ) {
						statement.execute( "alter session set time_zone='Asia/Kathmandu'" );
						final var methods = new ArrayList<String>();
						try ( var call = observed( connection.prepareCall( "{call opc_time(?,?,?)}" ), methods ) ) {
							jdbc.registerOutParameter( call, 2 ); jdbc.registerOutParameter( call, 3 );
							jdbc.getBinder( OffsetTimeJavaType.INSTANCE ).bind( call, value, 1, options );
							jdbc.getBinder( OffsetTimeJavaType.INSTANCE ).bind( call, value, 3, options );
							call.execute();
							statement.execute( "alter session set time_zone='America/Los_Angeles'" );
							final var output = jdbc.getExtractor( OffsetTimeJavaType.INSTANCE ).extract( call, 2, options );
							final var both = jdbc.getExtractor( OffsetTimeJavaType.INSTANCE ).extract( call, 3, options );
							final var expected = value == null ? null : value.getNano() == 0 ? value
									: OffsetTime.parse( value.getNano() == 123_456_789 ? "12:34:56.123457Z" : "00:00Z" );
							assertThat( output ).isEqualTo( expected ); assertThat( both ).isEqualTo( expected );
							try ( var rows = statement.executeQuery( "select to_char(a,'YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM'),to_char(c,'YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM') from opc_time_capture" ) ) {
								rows.next(); System.out.println( "OPC_CAPTURE O2 input=" + value + " serverIN=" + rows.getString( 1 ) + " serverINOUT=" + rows.getString( 2 ) );
							}
							statement.executeUpdate( "delete from opc_time_capture" );
							System.out.println( "OPC_CALL O2 input=" + value + " output=" + output + " inout=" + both + " methods=" + methods );
						}
					}
				}
				finally { statement.execute( "drop procedure opc_time" ); statement.execute( "drop table opc_time_capture purge" ); }
			}
			catch (SQLException failure) { throw new IllegalStateException( failure ); }
		} );
	}

	@Test
	void anonymousBlocksSeparateTransportFromRoutineDeclarations(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var options = (SharedSessionContractImplementor) session;
			final var stamp = new UtcTimestampJdbcType( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE );
			final var clock = new UtcTimeJdbcType( TimeUtcAsOffsetTimeJdbcType.INSTANCE );
			try ( var connection = OraclePrecisionEvidenceTests.connect(); var statement = connection.createStatement() ) {
				statement.execute( "create table opc_direct(s timestamp(9),t timestamp(9) with time zone)" );
				try {
					final var methods = new ArrayList<String>();
					try ( var call = observed( connection.prepareCall( "begin insert into opc_direct values(?,?); end;" ), methods ) ) {
						stamp.getBinder( OffsetDateTimeJavaType.INSTANCE ).bind( call, OffsetDateTime.parse( "1969-12-31T23:59:59.123456789Z" ), 1, options );
						clock.getBinder( OffsetTimeJavaType.INSTANCE ).bind( call, OffsetTime.parse( "12:34:56.123456789Z" ), 2, options );
						call.execute();
					}
					try ( var rows = statement.executeQuery( "select to_char(s,'YYYY-MM-DD HH24:MI:SS.FF9'),to_char(t,'YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM') from opc_direct" ) ) {
						rows.next();
						assertThat( rows.getString( 1 ) ).isEqualTo( "1969-12-31 23:59:59.123456789" );
						assertThat( rows.getString( 2 ) ).isEqualTo( "1970-01-01 12:34:56.123456789 +00:00" );
						System.out.println( "OPC_DIRECT input timestamp=" + rows.getString( 1 ) + " time=" + rows.getString( 2 ) + " methods=" + methods );
					}
					methods.clear();
					try ( var call = observed( connection.prepareCall( "begin ? := to_timestamp('1969-12-31 23:59:59.123456789','YYYY-MM-DD HH24:MI:SS.FF9'); ? := to_timestamp_tz('1970-01-01 12:34:56.123456789 +00:00','YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM'); end;" ), methods ) ) {
						stamp.registerOutParameter( call, 1 ); clock.registerOutParameter( call, 2 ); call.execute();
						assertThat( stamp.getExtractor( OffsetDateTimeJavaType.INSTANCE ).extract( call, 1, options ) )
								.isEqualTo( OffsetDateTime.parse( "1969-12-31T23:59:59.123456789Z" ) );
						assertThat( clock.getExtractor( OffsetTimeJavaType.INSTANCE ).extract( call, 2, options ) )
								.isEqualTo( OffsetTime.parse( "12:34:56.123456789Z" ) );
						System.out.println( "OPC_DIRECT output timestamp=" + stamp.getExtractor( OffsetDateTimeJavaType.INSTANCE ).extract( call, 1, options )
								+ " time=" + clock.getExtractor( OffsetTimeJavaType.INSTANCE ).extract( call, 2, options ) + " methods=" + methods );
					}
				}
				finally { statement.execute( "drop table opc_direct purge" ); }
			}
			catch (SQLException failure) { throw new IllegalStateException( failure ); }
		} );
	}

	@Test
	void directTimeInputsAndOutputsRetainNanosecondsWithReusedStatement(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var options = (SharedSessionContractImplementor) session;
			final var jdbc = new UtcTimeJdbcType( TimeUtcAsOffsetTimeJdbcType.INSTANCE );
			try ( var connection = OraclePrecisionEvidenceTests.connect();
					var call = connection.prepareCall( "begin ? := ?; end;" ) ) {
				jdbc.registerOutParameter( call, 1 );
				for ( var value : new OffsetTime[] { OffsetTime.parse( "23:59:59.999999999Z" ),
						OffsetTime.parse( "00:00:00.123456789Z" ), null } ) {
					jdbc.getBinder( OffsetTimeJavaType.INSTANCE ).bind( call, value, 2, options );
					call.execute();
					assertThat( jdbc.getExtractor( OffsetTimeJavaType.INSTANCE ).extract( call, 1, options ) ).isEqualTo( value );
				}
			}
			catch (SQLException failure) { throw new IllegalStateException( failure ); }
		} );
	}

	@Test
	void functionReturnPrecisionIsMeasuredIndependently(SessionFactoryScope scope) {
		scope.inSession( session -> {
			try ( var connection = OraclePrecisionEvidenceTests.connect(); var statement = connection.createStatement() ) {
				statement.execute( "create function opc_constant return timestamp as begin return to_timestamp('2026-07-01 10:00:00.123456789','YYYY-MM-DD HH24:MI:SS.FF9'); end;" );
				try {
					try ( var rows = statement.executeQuery( "select to_char(opc_constant(),'YYYY-MM-DD HH24:MI:SS.FF9') from dual" ) ) {
						rows.next(); assertThat( rows.getString( 1 ) ).isEqualTo( "2026-07-01 10:00:00.123457000" );
						System.out.println( "OPC_RETURN SQL=" + rows.getString( 1 ) );
					}
					statement.execute( "create table opc_return(v timestamp(9))" );
					try {
						statement.execute( "begin insert into opc_return values(opc_constant()); end;" );
						try ( var rows = statement.executeQuery( "select to_char(v,'YYYY-MM-DD HH24:MI:SS.FF9') from opc_return" ) ) {
							rows.next(); assertThat( rows.getString( 1 ) ).isEqualTo( "2026-07-01 10:00:00.123457000" );
							System.out.println( "OPC_RETURN BLOCK=" + rows.getString( 1 ) );
						}
					}
					finally { statement.execute( "drop table opc_return purge" ); }
				}
				finally { statement.execute( "drop function opc_constant" ); }
			}
			catch (SQLException failure) { throw new IllegalStateException( failure ); }
		} );
	}

	private static CallableStatement observed(CallableStatement delegate, List<String> methods) {
		return (CallableStatement) Proxy.newProxyInstance( CallableStatement.class.getClassLoader(), new Class<?>[] { CallableStatement.class },
				(proxy, method, args) -> {
					if ( method.getName().startsWith( "set" ) || method.getName().startsWith( "get" )
							|| method.getName().equals( "registerOutParameter" ) || method.getName().equals( "execute" ) ) {
						methods.add( method.getName() + (args == null || args.length == 0 ? "" : ":" + args[0])
								+ (method.getName().equals( "registerOutParameter" ) || method.getName().equals( "setNull" ) ? ":code=" + args[1]
								: method.getName().equals( "setObject" ) && args.length > 2 ? ":code=" + args[2] : "") );
					}
					try { return method.invoke( delegate, args ); }
					catch (InvocationTargetException failure) { throw failure.getCause(); }
				} );
	}
}
