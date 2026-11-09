package org.hibernate.type.internal;

import java.time.*;
import java.util.EnumSet;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLUtcTimeJdbcType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.*;

/// Compares domain requirements with facts about resolved execution descriptors.
/// Adjustment replaces offset/zone requirements only when the required primary
/// information survives. No precision canonicalization occurs here.
///
/// @since 8.0
/// @author Steve Ebersole
public final class TemporalMappingValidator {
	private TemporalMappingValidator() {
	}

	public static boolean supportsRepresentation(Class<?> type) {
		return type == OffsetTime.class || type == OffsetDateTime.class || type == ZonedDateTime.class;
	}

	public static void validate(Dialect dialect, org.hibernate.type.BasicType<?> type, String role) {
		final var javaType = type.getValueConverter() instanceof org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter<?,?> application
				? application.getRelationalJavaType() : type.getJavaTypeDescriptor();
		final var adjustment = type.getValueConverter() instanceof org.hibernate.type.descriptor.converter.internal.TemporalAdjustmentConverter<?> converter
				? converter : null;
		validate( dialect, javaType, type.getJdbcType(), adjustment == null ? null : adjustment.getTarget(),
				role, adjustment != null && adjustment.usesUtcTimeCarrier() );
	}

	public static void validate(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType, ZoneId adjustment, String role) {
		validate( dialect, javaType, jdbcType, adjustment, role, false );
	}

	public static void validate(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType, ZoneId adjustment, String role, boolean utcTimeCarrier) {
		final Class<?> type = javaType.getJavaTypeClass();
		final EnumSet<TemporalPreservation.TemporalPart> required;

		if ( type == LocalDate.class ) {
			required = EnumSet.of( DATE );
		}
		else if ( type == LocalTime.class ) {
			required = EnumSet.of( TIME );
		}
		else if ( type == LocalDateTime.class ) {
			required = EnumSet.of( DATE, TIME );
		}
		else if ( type == Instant.class ) {
			required = EnumSet.of( INSTANT );
		}
		else if ( type == OffsetTime.class ) {
			required = adjustment == null ? EnumSet.of( TIME, OFFSET ) : EnumSet.of( TIME );
		}
		else if ( type == OffsetDateTime.class ) {
			required = adjustment == null ? EnumSet.of( INSTANT, OFFSET ) : EnumSet.of( INSTANT );
		}
		else if ( type == ZonedDateTime.class ) {
			required = adjustment == null ? EnumSet.of( INSTANT, OFFSET, ZONE_ID ) : EnumSet.of( INSTANT );
		}
		else {
			return;
		}

		if ( (jdbcType.getClass() == PostgreSQLOffsetTimeJdbcType.class
				|| jdbcType.getClass() == org.hibernate.dialect.type.internal.PostgreSQLOffsetDateTimeJdbcType.class)
				&& adjustment instanceof ZoneOffset offset
				&& Math.abs( offset.getTotalSeconds() ) >= 16 * 3600 ) {
			throw new MappingException( "Temporal mapping '" + role + "' targets " + offset
					+ " outside PostgreSQL's supported offset range -15:59:59..+15:59:59" );
		}
		if ( jdbcType.getClass() == PostgreSQLUtcTimeJdbcType.class
				&& !ZoneOffset.UTC.equals( adjustment ) ) {
			throw new MappingException( "Temporal mapping '" + role + "' requires UTC adjustment for its UTC-only time descriptor" );
		}
		if ( adjustment instanceof ZoneOffset offset && offset.getTotalSeconds() % 60 != 0
				&& (jdbcType.getClass() == org.hibernate.dialect.type.internal.OracleOffsetTimeJdbcType.class
				|| jdbcType.getClass() == org.hibernate.dialect.type.internal.OracleOffsetDateTimeJdbcType.class) ) {
			throw new MappingException( "Temporal mapping '" + role + "' targets " + adjustment
					+ " but its Oracle native descriptor requires whole-minute offsets" );
		}
		if ( adjustment != null && jdbcType instanceof org.hibernate.dialect.type.internal.OracleZonedDateTimeJdbcType oracle
				&& !oracle.supportsZone( adjustment ) ) {
			throw new MappingException( "Temporal mapping '" + role + "' targets unsupported native Oracle zone '" + adjustment + "'" );
		}
		if ( jdbcType instanceof org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType && !utcTimeCarrier ) {
			throw new MappingException( "Temporal mapping '" + role + "' lacks UTC time encoding before JDBC binding" );
		}
		final var preservation = dialect.getTemporalPreservation( javaType, jdbcType );
		if ( preservation == null ) {
			throw new MappingException( "Dialect returned no preservation assessment for temporal mapping '" + role + "'" );
		}

		for ( var part : required ) {
			if ( preservation.preserves( part ) != TemporalPreservation.Level.SUPPORTED ) {
				throw new MappingException( "Temporal mapping '" + role + "' requires " + part
						+ " preservation but " + javaType.getClass().getName() + " + " + jdbcType.getClass().getName()
						+ " reports " + preservation.preserves( part )
						+ "; select established handling/representation or declare custom pair guarantees in the Dialect" );
			}
		}
	}
}
