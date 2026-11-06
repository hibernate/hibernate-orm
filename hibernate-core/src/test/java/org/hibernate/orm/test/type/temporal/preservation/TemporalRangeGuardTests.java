package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.*;
import java.time.*;
import java.util.stream.Stream;
import org.hibernate.TemporalValueException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.*;
import org.hibernate.dialect.type.internal.OracleOffsetDateTimeJdbcType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.property.access.spi.PropertyValueAccessor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.*;
import org.hibernate.type.descriptor.jdbc.*;
import org.hibernate.usertype.CompositeUserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/// Finite boundaries reject before ordinal/named/getBindValue operations, without
/// changing explicit conversion or losing dedicated composite value diagnostics.
///
/// @author Steve Ebersole
class TemporalRangeGuardTests {
	record Binding(Dialect dialect, JdbcType jdbc, JavaType java, Object value) {}
	static Stream<Binding> invalid() {
		return Stream.of(
				new Binding( new OracleDialect(), LocalDateJdbcType.INSTANCE, LocalDateJavaType.INSTANCE, LocalDate.of( 10000, 1, 1 ) ),
				new Binding( new OracleDialect(), LocalDateJdbcType.INSTANCE, LocalDateJavaType.INSTANCE, LocalDate.of( -4712, 1, 1 ) ),
				new Binding( new PostgreSQLDialect(), LocalDateJdbcType.INSTANCE, LocalDateJavaType.INSTANCE, LocalDate.MAX ),
				new Binding( new PostgreSQLDialect(), LocalDateJdbcType.INSTANCE, LocalDateJavaType.INSTANCE, LocalDate.MIN ),
				new Binding( new PostgreSQLDialect(), LocalDateTimeJdbcType.INSTANCE, LocalDateTimeJavaType.INSTANCE, LocalDateTime.of( 294277, 1, 1, 0, 0 ) ),
				new Binding( new OracleDialect(), OracleOffsetDateTimeJdbcType.INSTANCE, OffsetDateTimeJavaType.INSTANCE, OffsetDateTime.parse( "+10000-01-01T00:00Z" ) ),
				new Binding( new H2Dialect(), TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE, InstantJavaType.INSTANCE, Instant.MAX ),
				new Binding( new H2Dialect(), TimestampUtcAsZonedJdbcTimestampJdbcType.INSTANCE, InstantJavaType.INSTANCE, Instant.MIN ),
				new Binding( new OracleDialect(), TimestampUtcAsOffsetDateTimeJdbcType.INSTANCE, InstantJavaType.INSTANCE, Instant.MAX ) );
	}
	@SuppressWarnings("unchecked")
	@ParameterizedTest
	@MethodSource("invalid")
	void rejectsBeforeBinding(Binding binding) throws Exception {
		final var statement = mock( PreparedStatement.class );
		final var callable = mock( CallableStatement.class );
		final var options = options( binding.dialect, true );
		final var binder = binding.jdbc.getBinder( binding.java );
		assertThatThrownBy( () -> binder.bind( statement, binding.value, 1, options ) ).isInstanceOf( TemporalValueException.class );
		assertThatThrownBy( () -> binder.bind( callable, binding.value, "value", options ) ).isInstanceOf( TemporalValueException.class );
		assertThatThrownBy( () -> binder.getBindValue( binding.value, options ) ).isInstanceOf( TemporalValueException.class );
		verifyNoInteractions( statement, callable );
	}
	@Test
	void explicitConversionRetainsItsBinding() throws Exception {
		final var statement = mock( PreparedStatement.class );
		final var date = LocalDate.of( 10000, 1, 1 );
		LocalDateJdbcType.INSTANCE.getBinder( LocalDateJavaType.INSTANCE ).bind( statement, date, 1, options( new OracleDialect(), false ) );
		verify( statement ).setObject( 1, date, Types.DATE );
	}
	@Test
	void extractionRejectsInfinitySentinels() throws Exception {
		final var result = mock( ResultSet.class );
		final var callable = mock( CallableStatement.class );
		when( result.getObject( 1, LocalDate.class ) ).thenReturn( LocalDate.MAX );
		when( callable.getObject( 1, LocalDate.class ) ).thenReturn( LocalDate.MIN );
		when( callable.getObject( "value", LocalDate.class ) ).thenReturn( LocalDate.MAX );
		final var extractor = LocalDateJdbcType.INSTANCE.getExtractor( LocalDateJavaType.INSTANCE );
		final var options = options( new PostgreSQLDialect(), true );
		assertThatThrownBy( () -> extractor.extract( result, 1, options ) ).isInstanceOf( TemporalValueException.class );
		assertThatThrownBy( () -> extractor.extract( callable, 1, options ) ).isInstanceOf( TemporalValueException.class );
		assertThatThrownBy( () -> extractor.extract( callable, "value", options ) ).isInstanceOf( TemporalValueException.class );
	}
	@Test
	@SuppressWarnings("unchecked")
	void compositeRejectionRetainsItsDedicatedException() {
		final CompositeUserType<Object> type = mock( CompositeUserType.class );
		final Object owner = new Object();
		final var failure = new TemporalValueException( "Unsupported temporal value" );
		when( type.getPropertyValue( owner, 0 ) ).thenThrow( failure );
		assertThatThrownBy( () -> PropertyValueAccessor.compositeUserType( type, 0 ).get( owner ) ).isSameAs( failure );
	}
	@Test
	void utcBindValueMatchesBindingOnlyOnTheNewPath() throws Exception {
		final var value = OffsetDateTime.parse( "2024-02-29T12:34:56+05:30" );
		final var binder = TimestampUtcAsOffsetDateTimeJdbcType.INSTANCE.getBinder( OffsetDateTimeJavaType.INSTANCE );
		assertThat( binder.getBindValue( value, options( new H2Dialect(), false ) ) ).isEqualTo( value );
		assertThat( binder.getBindValue( value, options( new H2Dialect(), true ) ) ).isEqualTo( value.withOffsetSameInstant( ZoneOffset.UTC ) );
	}

	private static WrapperOptions options(Dialect dialect, boolean direct) {
		final var options = mock( WrapperOptions.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var factoryOptions = mock( SessionFactoryOptions.class );
		when( options.getSessionFactory() ).thenReturn( factory );
		when( options.getDialect() ).thenReturn( dialect );
		when( factory.getSessionFactoryOptions() ).thenReturn( factoryOptions );
		when( factoryOptions.isPreferJavaTimeJdbcTypesEnabled() ).thenReturn( direct );
		return options;
	}
}
