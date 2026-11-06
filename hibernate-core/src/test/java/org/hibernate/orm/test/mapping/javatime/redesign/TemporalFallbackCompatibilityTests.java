package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Calendar;

import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.type.internal.PostgreSQLTimestampUtcAsJdbcTimestampJdbcType;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.jdbc.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/// Direct-enabled priming may choose new handlers; legacy descriptor identity and
/// UTC-Calendar binding stay unchanged. Check named/null paths independently too.
///
/// @author Steve Ebersole
class TemporalFallbackCompatibilityTests {
	@Test
	void literalsUseTheirSqlRepresentationInsteadOfAmbientJdbcZone() {
		final var value = Instant.parse( "2024-02-29T23:59:58.123456Z" );
		final var options = mock( org.hibernate.type.descriptor.WrapperOptions.class );
		when( options.getJdbcTimeZone() ).thenReturn( java.util.TimeZone.getTimeZone( "America/Los_Angeles" ) );
		for ( org.hibernate.type.descriptor.jdbc.JdbcType type : java.util.List.of(
				TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE,
				new org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE ),
				TimestampUtcAsZonedJdbcTimestampJdbcType.INSTANCE ) ) {
			final var sql = new org.hibernate.sql.spi.StringBuilderSqlAppender();
			type.getJdbcLiteralFormatter( InstantJavaType.INSTANCE ).appendJdbcLiteral( sql, value, new H2Dialect(), options );
			assertThat( sql.toString() ).contains( "2024-02-29 23:59:58.123456" );
			assertThat( sql.toString().contains( "with time zone" ) )
					.isEqualTo( type.getDdlTypeCode() == Types.TIMESTAMP_WITH_TIMEZONE );
		}
	}

	@Test
	void unsafeLegacyOverrideRejectsOnlyOnDirectEnabledPath() {
		for ( boolean direct : new boolean[] { false, true } ) {
			try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
					.applySetting( "hibernate.dialect", H2Dialect.class )
					.applySetting( "hibernate.type.java_time_use_direct_jdbc", direct ).build() ) {
				final var sources = new MetadataSources( registry ).addAnnotatedClass( LegacyInstant.class );
				if ( direct ) {
					org.assertj.core.api.Assertions.assertThatThrownBy( sources::buildMetadata )
							.isInstanceOf( org.hibernate.MappingException.class ).hasMessageContaining( "INSTANT" );
				}
				else {
					final var value = (org.hibernate.mapping.BasicValue) sources.buildMetadata()
							.getEntityBinding( LegacyInstant.class.getName() ).getProperty( "value" ).getValue();
					assertThat( value.resolve().getJdbcType().getClass() ).isEqualTo( TimestampUtcAsJdbcTimestampJdbcType.class );
				}
			}
	}

	}

	@jakarta.persistence.Entity
	static class LegacyInstant {
		@jakarta.persistence.Id Long id;
		@org.hibernate.annotations.JdbcType(TimestampUtcAsJdbcTimestampJdbcType.class)
		Instant value;
	}

	@Test
	void primingKeepsLegacyDescriptors() {
		for ( boolean direct : new boolean[] { false, true } ) {
			for ( Class<?> dialect : new Class<?>[] { H2Dialect.class, SQLServerDialect.class, OracleDialect.class } ) {
				try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
						.applySetting( "hibernate.dialect", dialect )
						.applySetting( "hibernate.type.java_time_use_direct_jdbc", direct ).build() ) {
					final var metadata = (org.hibernate.boot.spi.MetadataImplementor) new MetadataSources( registry ).buildMetadata();
					final var jdbc = metadata.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.TIMESTAMP_UTC );
					assertThat( jdbc.getClass() ).isEqualTo( dialect == H2Dialect.class
							? direct ? TimestampUtcAsOffsetDateTimeJdbcType.class : TimestampUtcAsInstantJdbcType.class
							: dialect == OracleDialect.class ? TimestampUtcAsOffsetDateTimeJdbcType.class
									: direct ? TimestampUtcAsPlainJdbcTimestampJdbcType.class : TimestampUtcAsJdbcTimestampJdbcType.class );
					if ( dialect == OracleDialect.class ) {
						assertThat( metadata.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.LOCAL_TIME ).getClass() )
								.isEqualTo( direct ? org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType.class : LocalTimeJdbcType.class );
					}
				}
			}
		}
	}

	@Test
	void legacyCalendarBindingIsUnchanged() throws Exception {
		final var statement = mock( PreparedStatement.class );
		final var value = Instant.parse( "2024-02-29T23:59:58.123456Z" );
		TimestampUtcAsJdbcTimestampJdbcType.INSTANCE.getBinder( InstantJavaType.INSTANCE ).bind( statement, value, 1, null );
		final var calendar = ArgumentCaptor.forClass( Calendar.class );
		verify( statement ).setTimestamp( eq( 1 ), eq( Timestamp.from( value ) ), calendar.capture() );
		assertThat( calendar.getValue().getTimeZone().getID() ).isEqualTo( "UTC" );
		assertThat( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE.getDdlTypeCode() ).isEqualTo( Types.TIMESTAMP );
	}

	@Test
	void zonedBindingHandlesNamedAndNullValues() throws Exception {
		final var value = Instant.parse( "2024-02-29T23:59:58.123456Z" );
		final var statement = mock( CallableStatement.class );
		final var type = TimestampUtcAsZonedJdbcTimestampJdbcType.INSTANCE;
		type.getBinder( InstantJavaType.INSTANCE ).bind( statement, value, 1, null );
		type.getBinder( InstantJavaType.INSTANCE ).bind( statement, value, "value", null );
		type.getBinder( InstantJavaType.INSTANCE ).bind( statement, null, "missing", null );
		verify( statement ).setObject( 1, Timestamp.from( value ), Types.TIMESTAMP_WITH_TIMEZONE );
		verify( statement ).setObject( "value", Timestamp.from( value ), Types.TIMESTAMP_WITH_TIMEZONE );
		verify( statement ).setNull( "missing", Types.TIMESTAMP_WITH_TIMEZONE );
		final var pg = mock( CallableStatement.class );
		PostgreSQLTimestampUtcAsJdbcTimestampJdbcType.INSTANCE.getBinder( InstantJavaType.INSTANCE ).bind( pg, value, "value", null );
		verify( pg ).setTimestamp( eq( "value" ), eq( Timestamp.from( value ) ), any( Calendar.class ) );
	}
}
