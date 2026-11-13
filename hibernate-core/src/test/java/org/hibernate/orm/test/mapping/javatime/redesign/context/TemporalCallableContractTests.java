package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.lang.reflect.Proxy;
import org.hibernate.type.OutputableType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.UnaryOperator;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.procedure.ProcedureCall;

import org.hibernate.MappingException;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import java.util.Map;
import java.time.Instant;
import org.hibernate.TemporalValueException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Access;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Boundary;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TemporalCallablePrecisionJdbcType;
import org.hibernate.type.descriptor.jdbc.TemporalJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.TemporalCallableType;
import org.hibernate.testing.orm.junit.RequiresDialect;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.orm.test.mapping.javatime.redesign.context.TemporalCallablePrecisionTests.INPUT;
import static org.hibernate.orm.test.mapping.javatime.redesign.context.TemporalCallablePrecisionTests.received;

/// Contributions certify separate input/output routes; they never round arguments
/// or repair results. These controls also exercise actual OutputableType extraction.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalCallableContractTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void contributionsAndInputFailures(boolean metadata) {
		try ( var scope = TemporalCallablePrecisionTests.open( metadata ) ) {
			TemporalCallablePrecisionTests.create( scope );
			try {
				for ( String kind : new String[] { "decline", "refuse", "null", "throw", "recursive" } ) {
					scope.provider.callableMethods.clear();
					scope.tx( session -> {
						try ( var call = session.createStoredProcedureCall( "precision_callable_echo" ) ) {
							var parameter = call.registerParameter( 1, type( kind ), ParameterMode.IN );
							call.setParameter( parameter, INPUT );
							assertThatThrownBy( call::getOutputs ).isInstanceOf( MappingException.class );
						}
					} );
					assertThat( scope.provider.callableMethods ).isEmpty();
				}
				scope.tx( session -> {
					for ( LocalDateTime value : new LocalDateTime[] { INPUT, INPUT.withNano( 123499999 ), INPUT.withNano( 123500000 ), INPUT.withNano( 999999999 ) } ) {
						received.clear();
						scope.provider.callableMethods.clear();
						try ( var call = session.createStoredProcedureCall( "precision_callable_echo" ) ) {
							var parameter = call.registerParameter( 1, type( "input3" ), ParameterMode.IN );
							call.setParameter( parameter, value );
							assertThatThrownBy( call::getOutputs ).isInstanceOf( TemporalValueException.class )
									.hasMessageContaining( "INPUT" ).hasMessageContaining( "input3" );
							assertThat( resources( (SharedSessionContractImplementor) session ) ).isFalse();
						}
						assertThat( received ).isEmpty();
						assertThat( scope.provider.callableMethods ).noneMatch( method -> method.startsWith( "set" ) || method.startsWith( "execute" ) );
					}
					for ( LocalDateTime value : new LocalDateTime[] { INPUT.withNano( 123000000 ), null, INPUT.withNano( 0 ) } ) {
						try ( var call = session.createStoredProcedureCall( "precision_callable_echo" ) ) {
							call.markAsFunctionCall( type( "exact" ) );
							var parameter = call.registerParameter( "value", type( "input3" ), ParameterMode.IN );
							call.setParameter( parameter, value );
							assertThat( call.getSingleResult() ).isEqualTo( value );
						}
					}
				} );
			}
			finally { TemporalCallablePrecisionTests.drop( scope ); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void outputValidationIsNotRepair(boolean metadata) {
		try ( var scope = TemporalCallablePrecisionTests.open( metadata ) ) {
			TemporalCallablePrecisionTests.create( scope );
			try {
				scope.tx( session -> {
					try ( var call = session.createStoredProcedureCall( "precision_callable_echo" ) ) {
						call.markAsFunctionCall( type( "output3" ) );
						var parameter = call.registerParameter( 1, type( "exact" ), ParameterMode.IN );
						call.setParameter( parameter, INPUT );
						received.clear();
						assertThatThrownBy( call::getSingleResult ).isInstanceOf( TemporalValueException.class ).hasMessageContaining( "OUTPUT" );
						assertThat( received ).containsExactly( INPUT );
						assertThat( resources( (SharedSessionContractImplementor) session ) ).isFalse();
					}
					assertThat( resources( (SharedSessionContractImplementor) session ) ).isFalse();
					session.doWork( connection -> {
						var sessionImpl = (SharedSessionContractImplementor) session;
						var resolved = TemporalCallableType.resolve( type( "output3" ), sessionImpl,
								"raw return", ParameterMode.OUT, true, Access.INDEXED );
						try ( var call = connection.prepareCall( "{? = call precision_callable_echo(?)}" ) ) {
							call.registerOutParameter( 1, Types.TIMESTAMP );
							call.setObject( 2, INPUT );
							call.execute();
							assertThatThrownBy( () -> resolved.extract( call, 1, sessionImpl ) )
									.isInstanceOf( TemporalValueException.class ).hasMessageContaining( "INDEXED" );
						}
					} );
					assertThatThrownBy( () -> TemporalCallableType.resolve( new CustomExtractionType(),
							(SharedSessionContractImplementor) session, "custom return", ParameterMode.OUT, true, Access.INDEXED ) )
							.isInstanceOf( MappingException.class ).hasMessageContaining( "Custom callable extraction" );
				} );
			}
			finally { TemporalCallablePrecisionTests.drop( scope ); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void convertedValuesAreExecutionLocal(boolean metadata) {
		try ( var scope = TemporalCallablePrecisionTests.open( metadata ) ) {
			TemporalCallablePrecisionTests.create( scope );
			try {
				var converter = new CountingConverter();
				var converted = new ConvertedBasicTypeImpl<>( "callable-counter", new ContractJdbcType( "exact" ), converter );
				for ( String argument : new String[] { "first", null, "null-value", "last" } ) {
					converter.value = INPUT.plusSeconds( converter.writes );
					LocalDateTime expected = "null-value".equals( argument ) ? null : converter.value;
					int writes = converter.writes;
					int reads = converter.reads;
					scope.tx( session -> {
						try ( var call = session.createStoredProcedureCall( "precision_callable_echo" ) ) {
							call.markAsFunctionCall( converted );
							var parameter = call.registerParameter( 1, converted, ParameterMode.IN );
							call.setParameter( parameter, argument );
							assertThat( call.getSingleResult() ).isEqualTo( expected == null ? "null-result" : expected.toString() );
						}
					} );
					assertThat( converter.writes ).isEqualTo( writes + 1 );
					assertThat( converter.reads ).isEqualTo( reads + 1 );
				}
				converter.failRead = true;
				scope.tx( session -> {
					try ( var call = session.createStoredProcedureCall( "precision_callable_echo" ) ) {
						call.markAsFunctionCall( converted );
						var parameter = call.registerParameter( 1, converted, ParameterMode.IN );
						call.setParameter( parameter, "read-failure" );
						assertThatThrownBy( call::getSingleResult ).isInstanceOf( IllegalStateException.class );
						assertThat( resources( (SharedSessionContractImplementor) session ) ).isFalse();
					}
				} );
				converter.failRead = false;

				scope.tx( session -> {
					try ( var call = session.createStoredProcedureCall( "precision_callable_echo" ) ) {
						var parameter = call.registerParameter( 1, converted, ParameterMode.IN );
						call.setParameter( parameter, "throw" );
						received.clear();
						assertThatThrownBy( call::getOutputs ).isInstanceOf( IllegalArgumentException.class );
						assertThat( resources( (SharedSessionContractImplementor) session ) ).isFalse();
						assertThat( received ).isEmpty();
					}
				} );
			}
			finally { TemporalCallablePrecisionTests.drop( scope ); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void namedCallsAndGeneratedMementoLimitation(boolean metadata) {
		try ( var scope = TemporalCallablePrecisionTests.open( metadata ) ) {
			TemporalCallablePrecisionTests.create( scope );
			try {
				var memento = new NamedCallableQueryMemento[1];
				scope.tx( session -> {
					try ( var call = session.createStoredProcedureCall( "precision_callable_echo" ) ) {
						var parameter = call.registerParameter( "value", type( "exact" ), ParameterMode.IN );
						call.setParameter( parameter, INPUT );
						call.execute();
						memento[0] = ((ProcedureCallImplementor<?>) call).toMemento( "precision-memento" );
					}
				} );
				received.clear();
				scope.tx( session -> {
					// Existing generated mementos replay an irrelevant locking hint. Retain
					// that failure separately; declared named calls provide the supported control.
					assertThatThrownBy( () -> memento[0].makeProcedureCall( (SharedSessionContractImplementor) session ) )
							.isInstanceOf( IllegalArgumentException.class ).hasMessageContaining( "Locking is not relevant" );
					try ( var call = session.createNamedStoredProcedureQuery( "precision_named" ).unwrap( ProcedureCall.class ) ) {
						call.setParameter( "value", INPUT.plusSeconds( 1 ) );
						call.execute();
					}
				} );
				assertThat( received ).containsExactly( INPUT.plusSeconds( 1 ) );
			}
			finally { TemporalCallablePrecisionTests.drop( scope ); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void legacyModeDoesNotInvokeCallableAssessment(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Map.of( "hibernate.type.temporal_preservation", false ),
				LegacyRow.class ) ) {
			TemporalCallablePrecisionTests.create( scope );
			try {
				scope.tx( session -> {
					try ( var call = session.createStoredProcedureCall( "precision_callable_echo" ) ) {
						var parameter = call.registerParameter( 1, type( "throw" ), ParameterMode.IN );
						call.setParameter( parameter, INPUT );
						received.clear();
						call.execute();
						assertThat( received ).containsExactly( INPUT );
					}
				} );
			}
			finally { TemporalCallablePrecisionTests.drop( scope ); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void finiteTimestampRangeRejectsBeforeBinding(boolean metadata) {
		try ( var scope = TemporalCallablePrecisionTests.open( metadata ) ) {
			scope.tx( session -> {
				var mapped = (BasicType<?>) scope.factory.getMappingMetamodel()
						.getEntityDescriptor( TemporalCallablePrecisionTests.Row.class ).getPropertyType( "instant" );
				var input = (BasicType<?>) TemporalCallableType.resolve( mapped,
						(SharedSessionContractImplementor) session, "finite input", ParameterMode.IN, false, Access.INDEXED );
				var seconds = (BasicType<?>) TemporalCallableType.resolve( type( "input0" ),
						(SharedSessionContractImplementor) session, "whole seconds", ParameterMode.IN, false, Access.INDEXED );
				assertThatThrownBy( () -> seconds.convertToRelationalValue( INPUT ) ).isInstanceOf( TemporalValueException.class );
				assertThat( seconds.convertToRelationalValue( INPUT.withNano( 0 ) ) ).isEqualTo( INPUT.withNano( 0 ) );
				assertThatThrownBy( () -> input.convertToRelationalValue( Instant.MAX ) )
						.isInstanceOf( TemporalValueException.class ).hasMessageContaining( "validation failed" );
			} );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void allInputsValidateBeforeFirstSetter(boolean metadata) {
		try ( var scope = TemporalCallablePrecisionTests.open( metadata ) ) {
			scope.tx( session -> session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "create alias callable_pair for '" + TemporalCallablePrecisionTests.class.getName() + ".pair'" );
				}
			} ) );
			try {
				scope.tx( session -> {
					scope.provider.callableMethods.clear();
					received.clear();
					try ( var call = session.createStoredProcedureCall( "callable_pair" ) ) {
						call.registerParameter( 1, type( "exact" ), ParameterMode.IN );
						call.registerParameter( 2, type( "input3" ), ParameterMode.IN );
						call.setParameter( 1, INPUT );
						call.setParameter( 2, INPUT );
						assertThatThrownBy( call::getOutputs ).isInstanceOf( TemporalValueException.class );
						assertThat( resources( (SharedSessionContractImplementor) session ) ).isFalse();
					}
					assertThat( scope.provider.callableMethods ).noneMatch( method -> method.startsWith( "set" ) || method.startsWith( "execute" ) );
					assertThat( received ).isEmpty();
					var opaque = (OutputableType<?>) Proxy.newProxyInstance( getClass().getClassLoader(),
							new Class<?>[] { OutputableType.class }, (proxy, method, args) -> {
								if ( method.getName().equals( "getJavaType" ) ) { return LocalDateTime.class; }
								throw new AssertionError( "Opaque extraction must not be invoked" );
							} );
					assertThatThrownBy( () -> TemporalCallableType.resolve( opaque, (SharedSessionContractImplementor) session,
							"opaque return", ParameterMode.OUT, true, Access.INDEXED ) )
							.isInstanceOf( MappingException.class ).hasMessageContaining( "opaque OutputableType" );
				} );
			}
			finally {
				scope.tx( session -> session.doWork( connection -> {
					try ( var statement = connection.createStatement() ) { statement.execute( "drop alias callable_pair" ); }
				} ) );
			}
		}
	}

	@Entity(name = "CallableLegacyRow")
	static class LegacyRow {
		@Id int id;
		LocalDateTime stamp;
	}

	static class CountingConverter implements BasicValueConverter<String, LocalDateTime> {
		int writes;
		int reads;
		boolean failRead;
		LocalDateTime value;

		@Override
		public LocalDateTime toRelationalValue(String domain) {
			writes++;
			if ( "throw".equals( domain ) ) { throw new IllegalArgumentException( "conversion failed" ); }
			return "null-value".equals( domain ) ? null : value;
		}

		@Override
		public String toDomainValue(LocalDateTime relational) {
			reads++;
			if ( failRead ) { throw new IllegalStateException( "read conversion failed" ); }
			return relational == null ? "null-result" : relational.toString();
		}

		@Override
		public JavaType<String> getDomainJavaType() { return StringJavaType.INSTANCE; }

		@Override
		public JavaType<LocalDateTime> getRelationalJavaType() { return LocalDateTimeJavaType.INSTANCE; }
	}

	private static boolean resources(SharedSessionContractImplementor session) {
		return session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources();
	}

	static BasicType<LocalDateTime> type(String kind) {
		return new BasicTypeImpl<>( LocalDateTimeJavaType.INSTANCE, new ContractJdbcType( kind ) );
	}

	static class ContractJdbcType extends LocalDateTimeJdbcType implements TemporalJdbcType, TemporalCallablePrecisionJdbcType {
		private final String kind;
		ContractJdbcType(String kind) { this.kind = kind; }

		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect) {
			return Optional.of( dialect.getTemporalPreservation( javaType, LocalDateTimeJdbcType.INSTANCE ) );
		}

		@Override
		public Optional<TemporalPrecisionSupport> getTemporalCallablePrecisionSupport(TemporalCallablePrecisionContext context) {
			return switch ( kind ) {
				case "decline" -> Optional.empty();
				case "refuse" -> Optional.of( TemporalPrecisionSupport.unsupported( "deliberate refusal" ) );
				case "null" -> null;
				case "throw" -> throw new IllegalStateException( "callback failure" );
				case "recursive" -> Optional.of( context.dialect().getTemporalCallablePrecisionSupport( context ) );
				default -> Optional.of( TemporalPrecisionSupport.supported( kind + ':' + context.boundary(), value -> {
					if ( value == null ) { return null; }
					var dateTime = (LocalDateTime) value;
					if ( kind.equals( "input0" ) && context.boundary() == Boundary.INPUT ) { return dateTime.withNano( 0 ); }
					return kind.equals( "input3" ) && context.boundary() == Boundary.INPUT
							|| kind.equals( "output3" ) && context.boundary() == Boundary.OUTPUT
							? dateTime.withNano( dateTime.getNano() / 1_000_000 * 1_000_000 ) : dateTime;
				}, UnaryOperator.identity() ) );
			};
		}
	}

	static class CustomExtractionType extends BasicTypeImpl<LocalDateTime> {
		CustomExtractionType() { super( LocalDateTimeJavaType.INSTANCE, new ContractJdbcType( "exact" ) ); }
		@Override
		public LocalDateTime extract(CallableStatement statement, int index, SharedSessionContractImplementor session) throws SQLException {
			throw new AssertionError( "Custom extraction must not be silently bypassed" );
		}
	}
}
