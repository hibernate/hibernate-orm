package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.persistence.ParameterMode;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.BasicType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Actual indexed Oracle calls retain transport precision independently of the
/// attribute grid and routine precision. Rejected inputs execute no setters.
///
/// @author Steve Ebersole
@RequiresDialect(value = OracleDialect.class, matchSubTypes = false)
class OracleCallablePrecisionTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	@SuppressWarnings("unchecked")
	void scalarTransportAndRoutineNarrowing(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, TemporalCallableQueryTests.OBSERVE, OraclePrecisionLifecycleTests.Event.class ) ) {
			final var type = (BasicType<OffsetDateTime>) scope.factory.getMappingMetamodel()
					.getEntityDescriptor( OraclePrecisionLifecycleTests.Event.class ).getPropertyType( "stamp" );
			ddl( scope, "create table opc_orm_capture(v timestamp(9))" );
			ddl( scope, "create function opc_orm_scalar(v timestamp, narrow number) return timestamp as result timestamp(9); begin insert into opc_orm_capture values(v); if v is null then return null; elsif narrow=1 then select cast(v as timestamp(3)) into result from dual; return result; else return to_timestamp('2026-07-01 10:00:00.123456789','YYYY-MM-DD HH24:MI:SS.FF9'); end if; end;" );
			try {
				for ( boolean narrow : new boolean[] { false, true } ) {
					for ( var value : new OffsetDateTime[] { OffsetDateTime.parse( "2026-07-01T12:00:00.123456789+02:00" ), null } ) {
						scope.tx( session -> {
							try ( var call = session.createStoredProcedureCall( "opc_orm_scalar" ) ) {
								call.markAsFunctionCall( type );
								call.setParameter( call.registerParameter( 1, type, ParameterMode.IN ), value );
								call.setParameter( call.registerParameter( 2, Integer.class, ParameterMode.IN ), narrow ? 1 : 0 );
								// The function return declaration narrows to six digits even in a plain SQL SELECT.
								assertThat( call.getSingleResult() ).isEqualTo( value == null ? null : OffsetDateTime.parse(
										narrow ? "2026-07-01T15:30:00.123+05:30" : "2026-07-01T15:30:00.123457+05:30" ) );
							}
							session.doWork( connection -> {
								try ( var statement = connection.createStatement(); var rows = statement.executeQuery( "select to_char(v,'YYYY-MM-DD HH24:MI:SS.FF9') from opc_orm_capture" ) ) {
									rows.next(); assertThat( rows.getString( 1 ) ).isEqualTo( value == null ? null : "2026-07-01 10:00:00.123457000" );
								}
							} );
						} );
						ddl( scope, "delete from opc_orm_capture" );
					}
				}
				assertThat( scope.provider.callableMethods ).anyMatch( method -> method.startsWith( "registerOutParameter" ) );
				assertThat( scope.provider.callableMethods ).anyMatch( method -> method.startsWith( "getTimestamp" ) );
			}
			finally { ddl( scope, "drop function opc_orm_scalar" ); ddl( scope, "drop table opc_orm_capture purge" ); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	@SuppressWarnings("unchecked")
	void laterInvalidInputPreventsEverySetter(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Map.of( "hibernate.test.temporal.observe_callable", true ), OraclePrecisionLifecycleTests.Event.class ) ) {
			final var type = (BasicType<OffsetDateTime>) scope.factory.getMappingMetamodel()
					.getEntityDescriptor( OraclePrecisionLifecycleTests.Event.class ).getPropertyType( "stamp" );
			ddl( scope, "create table opc_orm_calls(v integer)" );
			ddl( scope, "create procedure opc_orm_two(a timestamp,b timestamp) as begin insert into opc_orm_calls values(1); end;" );
			try {
				scope.provider.callableMethods.clear();
				scope.tx( session -> {
					try ( var call = session.createStoredProcedureCall( "opc_orm_two" ) ) {
						call.setParameter( call.registerParameter( 1, type, ParameterMode.IN ), OffsetDateTime.parse( "2026-07-01T12:00:00.123456789Z" ) );
						call.setParameter( call.registerParameter( 2, type, ParameterMode.IN ), OffsetDateTime.parse( "+10000-01-01T12:00Z" ) );
						assertThatThrownBy( call::getOutputs ).hasMessageContaining( "finite" );
					}
				} );
				assertThat( scope.provider.callableMethods ).noneMatch( method -> method.startsWith( "set" ) || method.startsWith( "execute" ) );
				scope.tx( session -> session.doWork( connection -> {
					try ( var statement = connection.createStatement(); var rows = statement.executeQuery( "select count(*) from opc_orm_calls" ) ) {
						rows.next(); assertThat( rows.getInt( 1 ) ).isZero();
					}
				} ) );
			}
			finally { ddl( scope, "drop procedure opc_orm_two" ); ddl( scope, "drop table opc_orm_calls purge" ); }
		}
	}

	private static void ddl(ContextScope scope, String sql) {
		scope.tx( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.execute( sql );
				if ( sql.startsWith( "create function" ) || sql.startsWith( "create procedure" ) ) {
					try ( var errors = statement.executeQuery( "select text from user_errors where name in ('OPC_ORM_SCALAR','OPC_ORM_TWO') order by sequence" ) ) {
						final var messages = new StringBuilder();
						while ( errors.next() ) { messages.append( errors.getString( 1 ) ); }
						assertThat( messages.toString() ).isEmpty();
					}
				}
			}
		} ) );
	}
}
