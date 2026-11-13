package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Map;

import jakarta.persistence.ParameterMode;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.BasicType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Callable transport retains microseconds independently of a p3 attribute grid.
/// A later invalid argument must prevent every setter and routine execution.
///
/// @author Steve Ebersole
@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
class PostgreSQLCallablePrecisionTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	@SuppressWarnings("unchecked")
	void scalarResultAndRoutineNarrowing(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, TemporalCallableQueryTests.OBSERVE, PostgreSQLPrecisionLifecycleTests.Event.class ) ) {
			final var type = (BasicType<OffsetDateTime>) scope.factory.getMappingMetamodel()
					.getEntityDescriptor( PostgreSQLPrecisionLifecycleTests.Event.class ).getPropertyType( "adjustedStamp" );
			ddl( scope, "create function pgpc_scalar(v timestamp, narrow boolean) returns timestamp language sql as 'select case when narrow then cast(v as timestamp(3)) else v end'" );
			try {
				for ( boolean narrow : new boolean[] { false, true } ) {
					for ( OffsetDateTime value : new OffsetDateTime[] { OffsetDateTime.parse( "2026-07-01T12:00:00.123456+02:00" ), null } ) {
						scope.tx( session -> {
							try ( var call = session.createStoredProcedureCall( "pgpc_scalar" ) ) {
								call.markAsFunctionCall( type );
								call.setParameter( call.registerParameter( 1, type, ParameterMode.IN ), value );
								call.registerParameter( 2, Boolean.class, ParameterMode.IN ); call.setParameter( 2, narrow );
								assertThat( call.getSingleResult() ).isEqualTo( value == null ? null : OffsetDateTime.parse(
										narrow ? "2026-07-01T15:30:00.123+05:30" : "2026-07-01T15:30:00.123456+05:30" ) );
							}
						} );
					}
				}
				assertThat( scope.provider.callableMethods ).contains( "getResultSet" );
				assertThat( scope.provider.callableMethods ).noneMatch( method -> method.startsWith( "registerOutParameter" ) );
			}
			finally { ddl( scope, "drop function pgpc_scalar(timestamp,boolean)" ); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	@SuppressWarnings("unchecked")
	void allArgumentsAreValidatedBeforeAnySetter(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Map.of( "hibernate.test.temporal.observe_callable", true ),
				PostgreSQLPrecisionLifecycleTests.Event.class ) ) {
			final var entity = scope.factory.getMappingMetamodel().getEntityDescriptor( PostgreSQLPrecisionLifecycleTests.Event.class );
			final var stamp = (BasicType<OffsetDateTime>) entity.getPropertyType( "adjustedStamp" );
			final var clock = (BasicType<OffsetTime>) entity.getPropertyType( "nativeClock" );
			ddl( scope, "create table pgpc_calls(v integer)" );
			ddl( scope, "create procedure pgpc_two(a timestamp,b timetz) language sql as 'insert into pgpc_calls values(1)'" );
			try {
				for ( boolean invalidFirst : new boolean[] { false, true } ) {
					scope.provider.callableMethods.clear();
					scope.tx( session -> {
						try ( var call = session.createStoredProcedureCall( "pgpc_two" ) ) {
							call.setParameter( call.registerParameter( 1, stamp, ParameterMode.IN ),
									OffsetDateTime.parse( invalidFirst ? "2026-07-01T12:00:00.123456789Z" : "2026-07-01T12:00:00.123456Z" ) );
							call.setParameter( call.registerParameter( 2, clock, ParameterMode.IN ),
									OffsetTime.parse( invalidFirst ? "12:00:00.123456+05:45" : "12:00:00.123456789+05:45" ) );
							assertThatThrownBy( call::getOutputs ).hasMessageContaining( "precision" );
						}
					} );
					assertThat( scope.provider.callableMethods ).noneMatch( method -> method.startsWith( "set" ) || method.startsWith( "execute" ) );
				}
				scope.tx( session -> session.doWork( connection -> {
					try ( var statement = connection.createStatement(); var result = statement.executeQuery( "select count(*) from pgpc_calls" ) ) {
						result.next(); assertThat( result.getInt( 1 ) ).isZero();
					}
				} ) );
			}
			finally { ddl( scope, "drop procedure pgpc_two(timestamp,timetz)" ); ddl( scope, "drop table pgpc_calls" ); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	@SuppressWarnings("unchecked")
	void authoritativeInfinityOutputIsRejectedAfterExecution(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, TemporalCallableQueryTests.OBSERVE, PostgreSQLPrecisionLifecycleTests.Event.class ) ) {
			final var type = (BasicType<OffsetDateTime>) scope.factory.getMappingMetamodel()
					.getEntityDescriptor( PostgreSQLPrecisionLifecycleTests.Event.class ).getPropertyType( "adjustedStamp" );
			ddl( scope, "create function pgpc_infinite(v timestamp) returns timestamp language sql as 'select cast(''infinity'' as timestamp)'" );
			try {
				scope.provider.callableMethods.clear();
				try ( var session = scope.factory.openSession() ) {
					final var transaction = session.beginTransaction();
					try ( var call = session.createStoredProcedureCall( "pgpc_infinite" ) ) {
						call.markAsFunctionCall( type );
						call.setParameter( call.registerParameter( 1, type, ParameterMode.IN ), OffsetDateTime.parse( "2026-01-01T00:00Z" ) );
						assertThatThrownBy( call::getSingleResult ).hasMessageContaining( "finite" );
					}
					finally { transaction.rollback(); }
				}
				assertThat( scope.provider.callableMethods ).anyMatch( method -> method.startsWith( "execute" ) );
			}
			finally { ddl( scope, "drop function pgpc_infinite(timestamp)" ); }
		}
	}

	private static void ddl(ContextScope scope, String sql) {
		scope.tx( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) { statement.execute( sql ); }
		} ) );
	}
}
