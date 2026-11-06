package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.*;
import java.time.Instant;
import java.util.Calendar;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.dialect.type.internal.OracleTimestampUtcAsJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/// Oracle fallback creates UTC driver values only during binding, without
/// requiring a connection or metadata while constructing the descriptor.
///
/// @author Steve Ebersole
public class OracleTimestampFallbackTests {
	public static class Datum {
		final Connection connection;
		final Timestamp value;
		final Calendar calendar;
		public Datum(Connection connection, Timestamp value, Calendar calendar) {
			this.connection = connection;
			this.value = value;
			this.calendar = calendar;
		}
	}

	@Test
	void bindsExplicitUtcDatumThroughOrdinalAndNamedPaths() throws Exception {
		final var loaders = mock( ClassLoaderService.class );
		doReturn( Datum.class ).when( loaders ).classForName( "oracle.sql.TIMESTAMPTZ" );
		final var jdbc = new OracleTimestampUtcAsJdbcTimestampJdbcType( loaders );
		final var connection = mock( Connection.class );
		final var statement = mock( PreparedStatement.class );
		final var callable = mock( CallableStatement.class );
		verifyNoInteractions( connection, statement, callable );
		when( statement.getConnection() ).thenReturn( connection );
		when( callable.getConnection() ).thenReturn( connection );
		final var binder = jdbc.getBinder( InstantJavaType.INSTANCE );
		final var value = Instant.parse( "1500-01-01T00:00:00Z" );
		binder.bind( statement, value, 1, null );
		binder.bind( callable, value, "value", null );
		final var ordinal = org.mockito.ArgumentCaptor.forClass( Object.class );
		final var named = org.mockito.ArgumentCaptor.forClass( Object.class );
		verify( statement ).setObject( eq( 1 ), ordinal.capture(), eq( Types.TIMESTAMP_WITH_TIMEZONE ) );
		verify( callable ).setObject( eq( "value" ), named.capture(), eq( Types.TIMESTAMP_WITH_TIMEZONE ) );
		for ( Object encoded : new Object[] { ordinal.getValue(), named.getValue() } ) {
			final var datum = (Datum) encoded;
			assertThat( datum.connection ).isSameAs( connection );
			assertThat( datum.value.toInstant() ).isEqualTo( value );
			assertThat( datum.calendar.getTimeZone().getID() ).isEqualTo( "UTC" );
		}
		clearInvocations( statement, callable );
		binder.bind( statement, null, 1, null );
		binder.bind( callable, null, "value", null );
		verify( statement ).setNull( 1, Types.TIMESTAMP_WITH_TIMEZONE );
		verify( callable ).setNull( "value", Types.TIMESTAMP_WITH_TIMEZONE );
		verifyNoMoreInteractions( statement, callable );
	}
}
