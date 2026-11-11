package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.hibernate.TemporalValueException;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// EDB's common finite DATE domain is enforced without bootstrap connections.
/// Statement mocks verify guards, not live callable support; the provider matrix
/// independently verifies ORM endpoint round trips on the configured database.
///
/// @author Steve Ebersole
class EdbFiniteDateRangeTests {
	@Test
	void rejectsBeforeBindingAndRejectsExtractedValues() throws Exception {
		try ( var scope = boot( true, true ); var session = scope.factory.openSession() ) {
			var binder = LocalDateJdbcType.INSTANCE.getBinder( LocalDateJavaType.INSTANCE );
			var extractor = LocalDateJdbcType.INSTANCE.getExtractor( LocalDateJavaType.INSTANCE );
			for ( var value : List.of( LocalDate.of( -4713, 12, 31 ), LocalDate.of( 294277, 1, 1 ),
					LocalDate.of( 5874897, 12, 31 ), LocalDate.MIN, LocalDate.MAX ) ) {
				var statement = mock( PreparedStatement.class );
				var callable = mock( CallableStatement.class );
				assertThatThrownBy( () -> binder.bind( statement, value, 1, session ) )
						.isInstanceOf( TemporalValueException.class )
						.hasMessageContaining( "PostgresPlusDialect" ).hasMessageContaining( value.toString() )
						.hasMessageContaining( "finite DATE range -4712-01-01..+294276-12-31" );
				assertThatThrownBy( () -> binder.bind( callable, value, 1, session ) ).isInstanceOf( TemporalValueException.class );
				assertThatThrownBy( () -> binder.bind( callable, value, "value", session ) ).isInstanceOf( TemporalValueException.class );
				assertThatThrownBy( () -> binder.getBindValue( value, session ) ).isInstanceOf( TemporalValueException.class );
				verifyNoInteractions( statement, callable );

				var result = mock( ResultSet.class );
				when( result.getObject( 1, LocalDate.class ) ).thenReturn( value );
				when( callable.getObject( 1, LocalDate.class ) ).thenReturn( value );
				when( callable.getObject( "value", LocalDate.class ) ).thenReturn( value );
				assertThatThrownBy( () -> extractor.extract( result, 1, session ) ).isInstanceOf( TemporalValueException.class );
				assertThatThrownBy( () -> extractor.extract( callable, 1, session ) ).isInstanceOf( TemporalValueException.class );
				assertThatThrownBy( () -> extractor.extract( callable, "value", session ) ).isInstanceOf( TemporalValueException.class );
			}
			assertThat( scope.provider.attempts ).isZero();
		}
	}

	@Test
	void endpointsAndNullRemainAccepted() throws Exception {
		try ( var scope = boot( true, true ); var session = scope.factory.openSession() ) {
			var binder = LocalDateJdbcType.INSTANCE.getBinder( LocalDateJavaType.INSTANCE );
			var extractor = LocalDateJdbcType.INSTANCE.getExtractor( LocalDateJavaType.INSTANCE );
			for ( var value : List.of( LocalDate.of( -4712, 1, 1 ), LocalDate.of( 294276, 12, 31 ) ) ) {
				var statement = mock( PreparedStatement.class );
				var callable = mock( CallableStatement.class );
				binder.bind( statement, value, 1, session );
				binder.bind( callable, value, "value", session );
				verify( statement ).setObject( 1, value, Types.DATE );
				verify( callable ).setObject( "value", value, Types.DATE );
				assertThat( binder.getBindValue( value, session ) ).isEqualTo( value );
				var result = mock( ResultSet.class );
				when( result.getObject( 1, LocalDate.class ) ).thenReturn( value );
				when( callable.getObject( 1, LocalDate.class ) ).thenReturn( value );
				when( callable.getObject( "value", LocalDate.class ) ).thenReturn( value );
				assertThat( extractor.extract( result, 1, session ) ).isEqualTo( value );
				assertThat( extractor.extract( callable, 1, session ) ).isEqualTo( value );
				assertThat( extractor.extract( callable, "value", session ) ).isEqualTo( value );
			}
			var statement = mock( PreparedStatement.class );
			binder.bind( statement, null, 1, session );
			verify( statement ).setNull( 1, Types.DATE );
			assertThat( extractor.extract( mock( ResultSet.class ), 1, session ) ).isNull();
			assertThat( scope.provider.attempts ).isZero();
		}
	}

	@Test
	void postgresqlRetainsItsWiderDateRange() throws Exception {
		verifyWiderBinding( false, true );
	}

	@Test
	void legacyExplicitConversionIsNotGuarded() throws Exception {
		verifyWiderBinding( true, false );
	}

	private static void verifyWiderBinding(boolean edb, boolean preservation) throws Exception {
		try ( var scope = boot( edb, preservation ); var session = scope.factory.openSession() ) {
			var value = LocalDate.of( 5874897, 12, 31 );
			var statement = mock( PreparedStatement.class );
			LocalDateJdbcType.INSTANCE.getBinder( LocalDateJavaType.INSTANCE ).bind( statement, value, 1, session );
			verify( statement ).setObject( 1, value, Types.DATE );
			var result = mock( ResultSet.class );
			when( result.getObject( 1, LocalDate.class ) ).thenReturn( value );
			assertThat( LocalDateJdbcType.INSTANCE.getExtractor( LocalDateJavaType.INSTANCE )
					.extract( result, 1, session ) ).isEqualTo( value );
			assertThat( scope.provider.attempts ).isZero();
		}
	}

	private static ContextScope boot(boolean edb, boolean preservation) {
		return ContextScope.boot( false, preservation, Map.of(
				"hibernate.dialect", edb ? PostgresPlusDialect.class : PostgreSQLDialect.class,
				"jakarta.persistence.database-product-version", "17.4",
				"hibernate.type.temporal_preservation", preservation ), Row.class );
	}

	@Entity(name = "EdbFiniteDateRow")
	static class Row {
		@Id int id;
		LocalDate value;
	}
}
