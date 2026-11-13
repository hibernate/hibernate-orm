package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.Types;
import java.sql.CallableStatement;
import java.sql.SQLException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.hibernate.annotations.TimeZoneAdjustment;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.StoredProcedureParameter;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Actual callable transport is independent of the mapped attribute's write grid.
/// Raw OUT and ResultSet extraction are deliberately separate observations.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
public class TemporalCallablePrecisionTests {
	static final List<LocalDateTime> received = new ArrayList<>();
	static final LocalDateTime INPUT = LocalDateTime.parse( "1969-12-31T23:59:59.123456789" );

	public static LocalDateTime echo(LocalDateTime value) { received.add( value ); return value; }

	public static LocalDateTime pair(LocalDateTime first, LocalDateTime second) {
		received.add( first );
		received.add( second );
		return first;
	}

	static ContextScope open(boolean metadata) {
		return ContextScope.open( metadata, Map.of(
				"hibernate.type.temporal_preservation", true,
				"hibernate.test.temporal.observe_callable", true ), Row.class );
	}

	static void create(ContextScope scope) {
		scope.tx( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.execute( "create alias precision_callable_echo for '" + TemporalCallablePrecisionTests.class.getName() + ".echo'" );
			}
		} ) );
	}

	static void drop(ContextScope scope) {
		scope.tx( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) { statement.execute( "drop alias precision_callable_echo" ); }
		} ) );
	}

	@ParameterizedTest @ValueSource(booleans = { true, false })
	void rawIndexedOutAndResultSet(boolean metadata) {
		try ( var scope = open( metadata ) ) {
			create( scope );
			try {
				scope.tx( session -> session.doWork( connection -> {
					for ( String property : List.of( "stamp", "instant", "adjusted" ) ) {
						final var type = (BasicType<?>) scope.factory.getMappingMetamodel().getEntityDescriptor( Row.class ).getPropertyType( property );
						final var jdbc = type.getJdbcType();
						final var binder = jdbc.getBinder( type.getJdbcJavaType() );
						final var extractor = jdbc.getExtractor( type.getJdbcJavaType() );
						for ( LocalDateTime local : new LocalDateTime[] { INPUT, INPUT.withNano( 0 ), INPUT.withNano( 123499999 ),
								INPUT.withNano( 123500000 ), LocalDateTime.parse( "2026-12-31T23:59:59.999999999" ), null } ) {
							final Object value = local == null ? null : property.equals( "instant" ) ? local.toInstant( ZoneOffset.UTC )
									: property.equals( "adjusted" ) ? local.atOffset( ZoneOffset.UTC ) : local;
							received.clear();
							try ( var call = connection.prepareCall( "{? = call precision_callable_echo(?)}" ) ) {
								jdbc.registerOutParameter( call, 1 );
								bind( binder, call, value, 2, (SharedSessionContractImplementor) session );
								call.execute();
								assertThat( extractor.extract( call, 1, (SharedSessionContractImplementor) session ) ).isEqualTo( value );
							}
							assertThat( received ).containsExactly( local );
							try ( var call = connection.prepareCall( "{call precision_callable_echo(?)}" ) ) {
								bind( binder, call, value, 1, (SharedSessionContractImplementor) session ); call.execute();
								try ( var rows = call.getResultSet() ) {
									assertThat( rows.next() ).isTrue();
									assertThat( extractor.extract( rows, 1, (SharedSessionContractImplementor) session ) ).isEqualTo( value );
								}
							}
						}
					}
					try ( var call = connection.prepareCall( "{call precision_callable_echo(cast(? as timestamp(3)))}" ) ) {
						call.setObject( 1, INPUT, Types.TIMESTAMP ); call.execute();
						try ( var rows = call.getResultSet() ) { rows.next(); assertThat( rows.getObject( 1, LocalDateTime.class ) ).isEqualTo( INPUT.withNano( 123000000 ) ); }
					}
				} ) );
				assertThat( scope.provider.callableMethods ).contains( "registerOutParameter:1", "execute", "getResultSet" );
			}
			finally { drop( scope ); }
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void bind(ValueBinder binder, CallableStatement call, Object value,
			int index, WrapperOptions options) throws SQLException {
		binder.bind( call, value, index, options );
	}

	@ParameterizedTest @ValueSource(booleans = { true, false })
	@SuppressWarnings("unchecked")
	void mappedAttributeDoesNotSizeFunctionResult(boolean metadata) {
		try ( var scope = open( metadata ) ) {
			create( scope );
			try {
				final var type = (BasicType<LocalDateTime>) scope.factory.getMappingMetamodel().getEntityDescriptor( Row.class ).getPropertyType( "stamp" );
				scope.tx( session -> {
					final var row = new Row(); row.id = 1; row.stamp = INPUT; session.persist( row );
				} );
				scope.tx( session -> {
					try ( var call = session.createStoredProcedureCall( "precision_callable_echo" ) ) {
						call.markAsFunctionCall( type );
						var parameter = call.registerParameter( 1, type, ParameterMode.IN ); call.setParameter( parameter, INPUT );
						assertThat( call.getSingleResult() ).isEqualTo( INPUT );
					}
					assertThat( session.find( Row.class, 1 ).stamp ).isEqualTo( INPUT.withNano( 123000000 ) );
				} );
			}
			finally { drop( scope ); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void classAndCodeRegistrations(boolean metadata) {
		try ( var scope = open( metadata ) ) {
			create( scope );
			try {
				for ( boolean code : new boolean[] { true, false } ) {
					scope.tx( session -> {
						try ( var call = session.createStoredProcedureCall( "precision_callable_echo" ) ) {
							if ( code ) { call.markAsFunctionCall( SqlTypes.LOCAL_DATE_TIME ); }
							else { call.markAsFunctionCall( LocalDateTime.class ); }
							call.registerParameter( 1, LocalDateTime.class, ParameterMode.IN );
							call.setParameter( 1, INPUT );
							assertThat( call.getSingleResult() ).isEqualTo( INPUT );
						}
					} );
				}
			}
			finally { drop( scope ); }
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void customDayRawTransport(boolean metadata) {
		try ( var scope = open( metadata ) ) {
			scope.tx( session -> session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "create alias callable_day_probe for '" + TemporalDescriptorMappingTests.class.getName() + ".echoDay'" );
				}
				try {
					var javaType = new TemporalDescriptorMappingTests.DayJavaType();
					var jdbc = new TemporalDescriptorMappingTests.DayJdbc();
					for ( LocalDate date : new LocalDate[] { LocalDate.of( 1969, 12, 31 ), LocalDate.of( 2024, 2, 29 ), null } ) {
						var value = date == null ? null : new TemporalDescriptorMappingTests.Day( date );
						try ( var call = connection.prepareCall( "{call callable_day_probe(?)}" ) ) {
							jdbc.getBinder( javaType ).bind( call, value, 1, (SharedSessionContractImplementor) session );
							call.execute();
							try ( var rows = call.getResultSet() ) {
								assertThat( rows.next() ).isTrue();
								assertThat( rows.getObject( 1, LocalDate.class ) ).isEqualTo( date );
								assertThat( jdbc.getExtractor( javaType ).extract( rows, 1, (SharedSessionContractImplementor) session ) ).isEqualTo( value );
							}
						}
					}
				}
				finally {
					try ( var statement = connection.createStatement() ) { statement.execute( "drop alias callable_day_probe" ); }
				}
			} ) );
		}
	}

	@NamedStoredProcedureQuery(name = "precision_named", procedureName = "precision_callable_echo",
			parameters = @StoredProcedureParameter(name = "value", type = LocalDateTime.class, mode = ParameterMode.IN))
	@Entity(name = "CallablePrecisionRow")
	static class Row {
		@Id int id;
		@Column(secondPrecision = 3) LocalDateTime stamp;
		@Column(secondPrecision = 3) Instant instant;
		@TimeZoneAdjustment("+05:30") @Column(secondPrecision = 3) OffsetDateTime adjusted;
	}
}
