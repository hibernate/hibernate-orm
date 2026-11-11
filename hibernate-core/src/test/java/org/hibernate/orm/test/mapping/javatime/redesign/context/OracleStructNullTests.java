package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.annotations.Struct;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.type.internal.OracleReflectionStructJdbcType;
import org.hibernate.dialect.type.internal.OracleStructJdbcType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Whole-object SQL nulls require the Oracle object type name, independently of
/// member conversions, preservation settings, or bootstrap metadata access.
///
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
class OracleStructNullTests {
	static Stream<Arguments> modes() {
		return Stream.of( true, false ).flatMap( preservation -> Stream.of( true, false )
				.map( metadata -> Arguments.of( preservation, metadata ) ) );
	}

	@ParameterizedTest
	@MethodSource("modes")
	void nullLifecycleAndCallableInputs(boolean preservation, boolean metadata) {
		try ( var scope = ContextScope.open( metadata, false,
				Map.of( "hibernate.type.temporal_preservation", preservation ), List.of(), Row.class ) ) {
			// A non-temporal member proves that the defect is independent of Java Time.
			scope.tx( session -> {
				var row = new Row();
				row.id = 1;
				session.persist( row );
			} );
			scope.tx( session -> {
				var row = session.find( Row.class, 1 );
				assertThat( row.payload ).isNull();
				row.payload = new Payload( "first" );
			} );
			scope.tx( session -> {
				var row = session.find( Row.class, 1 );
				assertThat( row.payload.text ).isEqualTo( "first" );
				row.payload = null;
			} );
			scope.tx( session -> {
				var row = session.find( Row.class, 1 );
				assertThat( row.payload ).isNull();
				row.payload = new Payload( "restored" );
			} );
			scope.tx( session -> assertThat( session.find( Row.class, 1 ).payload.text ).isEqualTo( "restored" ) );

			// Both Oracle implementations share this binder. Resolve them against the
			// real mapped SQL type, then exercise actual indexed and named driver calls.
			var creationContext = mock( RuntimeModelCreationContext.class );
			when( creationContext.getBootModel() ).thenReturn( (MetadataImplementor) scope.metadata );
			List<AggregateJdbcType> descriptors = List.of(
					new OracleStructJdbcType(), OracleReflectionStructJdbcType.INSTANCE );
			scope.tx( session -> session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "create or replace procedure struct_null_probe(value_in struct_null_payload, result_out out number) as begin if value_in is null then result_out := 1; else result_out := 0; end if; end;" );
				}
				try {
					for ( var descriptor : descriptors ) {
						var resolved = descriptor.resolveAggregateJdbcType( null, "struct_null_payload", creationContext );
						var binder = resolved.getBinder( StringJavaType.INSTANCE );
						try ( var statement = connection.prepareStatement( "select case when ? is null then 1 else 0 end from dual" ) ) {
							binder.bind( statement, null, 1, null );
							try ( var results = statement.executeQuery() ) {
								assertThat( results.next() ).isTrue();
								assertThat( results.getInt( 1 ) ).isEqualTo( 1 );
							}
						}
						try ( var statement = connection.prepareCall( "{call struct_null_probe(?,?)}" ) ) {
							binder.bind( statement, null, 1, null );
							statement.registerOutParameter( 2, Types.INTEGER );
							statement.execute();
							assertThat( statement.getInt( 2 ) ).isEqualTo( 1 );
						}
						try ( var statement = connection.prepareCall( "{call struct_null_probe(?,?)}" ) ) {
							binder.bind( statement, null, "value_in", null );
							statement.registerOutParameter( "result_out", Types.INTEGER );
							statement.execute();
							assertThat( statement.getInt( "result_out" ) ).isEqualTo( 1 );
						}
					}
				}
				finally {
					try ( var statement = connection.createStatement() ) {
						statement.execute( "drop procedure struct_null_probe" );
					}
				}
			} ) );
		}
	}

	@Entity(name = "OracleStructNullRow")
	@Table(name = "oracle_struct_null_row")
	static class Row {
		@Id int id;
		@Struct(name = "struct_null_payload") Payload payload;
	}

	@Embeddable
	static class Payload {
		String text;

		Payload() {
		}

		Payload(String text) {
			this.text = text;
		}
	}
}
