package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;

import org.hibernate.TemporalValueException;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.type.internal.OracleOffsetDateTimeJdbcType;
import org.hibernate.dialect.type.internal.OracleOffsetTimeJdbcType;
import org.hibernate.dialect.type.internal.OracleZonedDateTimeJdbcType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/// Native Oracle support-domain checks reject loss before invoking JDBC binding.
///
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
@ServiceRegistry(settings = @Setting(name = "hibernate.type.java_time_use_direct_jdbc", value = "true"))
@DomainModel(annotatedClasses = OracleTemporalDescriptorTests.CallableEvent.class)
@SessionFactory(exportSchema = false)
class OracleTemporalDescriptorTests {
	@Test
	void localTimeUsesZoneLessFieldsIncludingNanoseconds(SessionFactoryScope scope) {
		scope.inSession( session -> session.doWork( connection -> {
			final var type = org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType.INSTANCE;
			final var javaType = org.hibernate.type.descriptor.java.LocalTimeJavaType.INSTANCE;
			for ( java.time.LocalTime value : new java.time.LocalTime[] { java.time.LocalTime.parse( "23:59:58.123456789" ), null } ) {
				try ( var statement = connection.prepareStatement( "select cast(? as timestamp(9)) from dual" ) ) {
					type.getBinder( javaType ).bind( statement, value, 1, null );
					try ( var result = statement.executeQuery() ) {
						assertThat( result.next() ).isTrue();
						assertThat( type.getExtractor( javaType ).extract( result, 1, null ) ).isEqualTo( value );
					}
				}
				try ( var statement = connection.prepareCall( "begin :result_value := :input_value; end;" ) ) {
					type.registerOutParameter( statement, "result_value" );
					type.getBinder( javaType ).bind( statement, value, "input_value", null );
					statement.execute();
					assertThat( type.getExtractor( javaType ).extract( statement, "result_value", null ) ).isEqualTo( value );
				}
			}
		} ) );
	}

	@Test
	@SuppressWarnings("unchecked")
	void mappedAdjustmentSurvivesProcedureInOut(SessionFactoryScope scope) {
		final var type = (org.hibernate.type.BasicType<OffsetTime>) scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( CallableEvent.class ).getPropertyType( "adjustedTime" );
		scope.inSession( session -> {
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "create or replace procedure temporal_adjustment_probe(p_value in out timestamp with time zone) as begin null; end;" );
				}
			} );
			try {
				for ( OffsetTime input : new OffsetTime[] { OffsetTime.parse( "01:00+05:30:45" ), null } ) {
					try ( var call = session.createStoredProcedureCall( "temporal_adjustment_probe" ) ) {
						final var parameter = call.registerParameter( 1, type, jakarta.persistence.ParameterMode.INOUT );
						call.setParameter( parameter, input );
						assertThat( call.getOutputs().getOutputParameterValue( parameter ) )
								.isEqualTo( input == null ? null : OffsetTime.parse( "13:29:15+18:00" ) );
					}
				}
			}
			finally {
				session.doWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						statement.execute( "drop procedure temporal_adjustment_probe" );
					}
				} );
			}
		} );
	}

	@jakarta.persistence.Entity(name = "TemporalCallableEvent")
	static class CallableEvent {
		@jakarta.persistence.Id Long id;
		@org.hibernate.annotations.TimeZoneAdjustment("+18:00")
		OffsetTime adjustedTime;
	}

	@Test
	void offsetSecondsAndUnknownZoneIdentityFailBeforeBinding(SessionFactoryScope scope) {
		final var statement = mock( PreparedStatement.class );
		assertThatThrownBy( () -> OracleOffsetDateTimeJdbcType.INSTANCE.getBinder( OffsetDateTimeJavaType.INSTANCE )
				.bind( statement, OffsetDateTime.parse( "2026-07-01T12:34:56+05:30:45" ), 1, null ) )
				.isInstanceOf( TemporalValueException.class );
		assertThatThrownBy( () -> OracleOffsetTimeJdbcType.INSTANCE.getBinder( OffsetTimeJavaType.INSTANCE )
				.bind( statement, OffsetTime.parse( "12:34:56+05:30:45" ), 1, null ) )
				.isInstanceOf( TemporalValueException.class );
		final var zoned = (OracleZonedDateTimeJdbcType) scope.getSessionFactory().getTypeConfiguration()
				.getJdbcTypeRegistry().getDescriptor( SqlTypes.ZONED_DATE_TIME );
		assertThatThrownBy( () -> zoned.getBinder( ZonedDateTimeJavaType.INSTANCE ).bind( statement,
				LocalDateTime.parse( "2026-07-01T12:34:56" ).atZone( ZoneId.of( "GMT+05:30" ) ), 1, null ) )
				.isInstanceOf( TemporalValueException.class );
		verifyNoInteractions( statement );
	}

	@Test
	void positionalNamedAndNullCallableOutputs(SessionFactoryScope scope) {
		scope.inSession( session -> session.doWork( connection -> {
			final var type = (OracleZonedDateTimeJdbcType) scope.getSessionFactory().getTypeConfiguration()
					.getJdbcTypeRegistry().getDescriptor( SqlTypes.ZONED_DATE_TIME );
			for ( java.time.ZonedDateTime value : new java.time.ZonedDateTime[] {
					java.time.ZonedDateTime.parse( "2026-11-01T01:30:00-04:00[America/New_York]" ), null } ) {
				try ( var statement = connection.prepareCall( "begin ? := ?; end;" ) ) {
					type.registerOutParameter( statement, 1 );
					type.getBinder( ZonedDateTimeJavaType.INSTANCE ).bind( statement, value, 2, null );
					statement.execute();
					assertThat( type.getExtractor( ZonedDateTimeJavaType.INSTANCE ).extract( statement, 1, null ) ).isEqualTo( value );
				}
				try ( var statement = connection.prepareCall( "begin :result_value := :input_value; end;" ) ) {
					type.registerOutParameter( statement, "result_value" );
					type.getBinder( ZonedDateTimeJavaType.INSTANCE ).bind( statement, value, "input_value", null );
					statement.execute();
					assertThat( type.getExtractor( ZonedDateTimeJavaType.INSTANCE ).extract( statement, "result_value", null ) ).isEqualTo( value );
				}
			}
		} ) );
	}

	@Test
	void knownRegionAliasesAndFractionalTimesRoundTrip(SessionFactoryScope scope) {
		scope.inSession( session -> session.doWork( connection -> {
			final var zoned = (OracleZonedDateTimeJdbcType) scope.getSessionFactory().getTypeConfiguration()
					.getJdbcTypeRegistry().getDescriptor( SqlTypes.ZONED_DATE_TIME );
			try ( var statement = connection.prepareStatement( "select cast(? as timestamp(9) with time zone) from dual" ) ) {
				for ( String zone : new String[] { "UTC", "Etc/GMT-5", "US/Eastern", "America/New_York" } ) {
					final var input = LocalDateTime.parse( "2026-07-01T12:34:56.123456789" ).atZone( ZoneId.of( zone ) );
					zoned.getBinder( ZonedDateTimeJavaType.INSTANCE ).bind( statement, input, 1, null );
					try ( var result = statement.executeQuery() ) {
						assertThat( result.next() ).isTrue();
						assertThat( zoned.getExtractor( ZonedDateTimeJavaType.INSTANCE ).extract( result, 1, null ) ).isEqualTo( input );
					}
				}
				final var time = OffsetTime.parse( "12:34:56.123456789+05:30" );
				OracleOffsetTimeJdbcType.INSTANCE.getBinder( OffsetTimeJavaType.INSTANCE ).bind( statement, time, 1, null );
				try ( var result = statement.executeQuery() ) {
					assertThat( result.next() ).isTrue();
					assertThat( OracleOffsetTimeJdbcType.INSTANCE.getExtractor( OffsetTimeJavaType.INSTANCE ).extract( result, 1, null ) ).isEqualTo( time );
				}
			}
		} ) );
	}
}
