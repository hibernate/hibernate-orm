package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.DriverManager;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.hibernate.HibernateException;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLUtcTimeJdbcType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.type.descriptor.DateTimeUtils.roundToSecondPrecision;

/// Actual descriptor operations across pgJDBC text/binary transfer and JVM zones.
///
/// @author Steve Ebersole
@RequiresDialect(PostgreSQLDialect.class)
@DomainModel
@SessionFactory(exportSchema = false)
class PostgreSQLTemporalDescriptorTests {
	@Test
	void unsupportedCallableOutputsFailBeforeExecution() {
		final var statement = org.mockito.Mockito.mock( java.sql.CallableStatement.class );
		assertThatThrownBy( () -> PostgreSQLOffsetTimeJdbcType.INSTANCE.registerOutParameter( statement, 1 ) )
				.isInstanceOf( org.hibernate.MappingException.class ).hasMessageContaining( "OUT/INOUT" );
		assertThatThrownBy( () -> PostgreSQLUtcTimeJdbcType.INSTANCE.registerOutParameter( statement, "result" ) )
				.isInstanceOf( org.hibernate.MappingException.class ).hasMessageContaining( "OUT/INOUT" );
		org.mockito.Mockito.verifyNoInteractions( statement );
	}

	@Test
	void offsetSecondsEndOfDayAndNulls(SessionFactoryScope scope) throws Exception {
		final var settings = new java.util.HashMap<>( scope.getSessionFactory().getServiceRegistry()
				.requireService( org.hibernate.engine.config.spi.ConfigurationService.class ).getSettings() );
		org.hibernate.testing.jdbc.GradleParallelTestingResolver.resolveFromSettings( settings );
		final String baseUrl = settings.get( "hibernate.connection.url" ).toString();
		final var previous = TimeZone.getDefault();
		try {
			for ( String mode : new String[] { "preferQueryMode=simple", "prepareThreshold=1&binaryTransferEnable=1266,1083" } ) {
				final String url = baseUrl + (baseUrl.contains( "?" ) ? "&" : "?") + mode;
				try ( var connection = DriverManager.getConnection( url,
						settings.get( "hibernate.connection.username" ).toString(),
						settings.get( "hibernate.connection.password" ).toString() );
						var statement = connection.prepareStatement( "select cast(? as time(6) with time zone), cast(? as time(6))" ) ) {
					final var direct = PostgreSQLOffsetTimeJdbcType.INSTANCE;
					final var utc = PostgreSQLUtcTimeJdbcType.INSTANCE;
					for ( String text : new String[] { "00:15+05:45", "12:34:56.123456+05:30:45", "23:59:59.999999999-03:00", null } ) {
						final var input = text == null ? null : OffsetTime.parse( text );
						TimeZone.setDefault( TimeZone.getTimeZone( "America/Los_Angeles" ) );
						direct.getBinder( OffsetTimeJavaType.INSTANCE ).bind( statement, input, 1, null );
						utc.getBinder( OffsetTimeJavaType.INSTANCE ).bind( statement,
								input == null ? null : input.withOffsetSameInstant( ZoneOffset.UTC ), 2, null );
						TimeZone.setDefault( TimeZone.getTimeZone( "Asia/Tokyo" ) );
						try ( var result = statement.executeQuery() ) {
							assertThat( result.next() ).isTrue();
							final var expected = input == null ? null : roundToSecondPrecision( input, 6 );
							assertThat( direct.getExtractor( OffsetTimeJavaType.INSTANCE ).extract( result, 1, null ) ).isEqualTo( expected );
							assertThat( utc.getExtractor( OffsetTimeJavaType.INSTANCE ).extract( result, 2, null ) )
									.isEqualTo( expected == null ? null : expected.withOffsetSameInstant( ZoneOffset.UTC ) );
						}
					}
					assertThatThrownBy( () -> direct.getBinder( OffsetTimeJavaType.INSTANCE )
							.bind( statement, OffsetTime.parse( "12:00+18:00" ), 1, null ) )
							.isInstanceOf( HibernateException.class ).hasMessageContaining( "-15:59:59" );
				}
			}
		}
		finally {
			TimeZone.setDefault( previous );
		}
	}
}
