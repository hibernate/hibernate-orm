package org.hibernate.type.internal;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.dialect.type.internal.OracleOffsetDateTimeJdbcType;
import org.hibernate.dialect.type.internal.OracleOffsetTimeJdbcType;
import org.hibernate.dialect.type.internal.OracleZonedDateTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetDateTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLUtcTimeJdbcType;
import org.hibernate.type.descriptor.converter.internal.TemporalAdjustmentConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PreservationAwareTemporalJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.DATE;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.INSTANT;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.OFFSET;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.TIME;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.ZONE_ID;

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
		final var javaType = type.getValueConverter() instanceof JpaAttributeConverter<?,?> application
				? application.getRelationalJavaType()
				: type.getJavaTypeDescriptor();
		final var adjustment = type.getValueConverter() instanceof TemporalAdjustmentConverter<?> converter
				? converter
				: null;
		validate(
				dialect,
				javaType,
				type.getJdbcType(),
				adjustment == null ? null : adjustment.getTarget(),
				role,
				adjustment != null && adjustment.usesUtcTimeCarrier()
		);
	}

	public static void validate(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType, ZoneId adjustment, String role) {
		validate( dialect, javaType, jdbcType, adjustment, role, false );
	}

	public static void validate(
			Dialect dialect,
			JavaType<?> javaType,
			JdbcType jdbcType,
			ZoneId adjustment,
			String role,
			boolean utcTimeCarrier) {
		final Class<?> type = javaType.getJavaTypeClass();
		final Set<TemporalPreservation.TemporalPart> declared;
		try {
			declared = requirements( javaType );
		}
		catch (RuntimeException failure) {
			throw assessmentFailure( javaType, jdbcType, role, "Java-side SPI requirements", failure );
		}
		if ( declared == null ) {
			return;
		}
		final var required = EnumSet.copyOf( declared );
		if ( adjustment != null && supportsRepresentation( type ) ) {
			required.remove( OFFSET );
			required.remove( ZONE_ID );
		}

		if ( (jdbcType.getClass() == PostgreSQLOffsetTimeJdbcType.class
				|| jdbcType.getClass() == PostgreSQLOffsetDateTimeJdbcType.class)
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
				&& (jdbcType.getClass() == OracleOffsetTimeJdbcType.class
				|| jdbcType.getClass() == OracleOffsetDateTimeJdbcType.class) ) {
			throw new MappingException( "Temporal mapping '" + role + "' targets " + adjustment
					+ " but its Oracle native descriptor requires whole-minute offsets" );
		}
		if ( adjustment != null && jdbcType instanceof OracleZonedDateTimeJdbcType oracle
				&& !oracle.supportsZone( adjustment ) ) {
			throw new MappingException( "Temporal mapping '" + role + "' targets unsupported native Oracle zone '" + adjustment + "'" );
		}
		if ( jdbcType instanceof UtcTimeJdbcType && !utcTimeCarrier ) {
			throw new MappingException( "Temporal mapping '" + role + "' lacks UTC time encoding before JDBC binding" );
		}
		final var preservation = assess( dialect, javaType, jdbcType, role );

		for ( var part : required ) {
			if ( preservation.preserves( part ) != TemporalPreservation.Level.SUPPORTED ) {
				throw new MappingException( String.format( Locale.ROOT,
						"%s requires %s preservation but %s + %s reports %s; select established "
								+ "handling/representation, supply a TemporalJdbcType assessment for established "
								+ "custom execution, or declare custom pair guarantees in the Dialect",
						mappingLabel( role ),
						part,
						javaType.getClass().getName(),
						jdbcType.getClass().getName(),
						preservation.preserves( part )
				) );
			}
		}
	}

	/// Assess a resolved pair with mapping context, also for auxiliary COLUMN components.
	public static TemporalPreservation assess(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType, String role) {
		final TemporalPreservation preservation;
		try {
			preservation = dialect.getTemporalPreservation( javaType, jdbcType );
		}
		catch (RuntimeException failure) {
			throw assessmentFailure( javaType, jdbcType, role, "Dialect/SPI assessment", failure );
		}
		if ( preservation == null ) {
			throw new MappingException( "Dialect returned no preservation assessment for " + mappingLabel( role )
					+ " (" + javaType.getClass().getName() + " + " + jdbcType.getClass().getName() + ")" );
		}

		return preservation;
	}

	private static Set<TemporalPreservation.TemporalPart> requirements(JavaType<?> javaType) {
		final var type = javaType.getJavaTypeClass();
		final Set<TemporalPreservation.TemporalPart> canonical;
		if ( type == LocalDate.class ) {
			canonical = Set.of( DATE );
		}
		else if ( type == LocalTime.class ) {
			canonical = Set.of( TIME );
		}
		else if ( type == LocalDateTime.class ) {
			canonical = Set.of( DATE, TIME );
		}
		else if ( type == Instant.class ) {
			canonical = Set.of( INSTANT );
		}
		else if ( type == OffsetTime.class ) {
			canonical = Set.of( TIME, OFFSET );
		}
		else if ( type == OffsetDateTime.class ) {
			canonical = Set.of( INSTANT, OFFSET );
		}
		else if ( type == ZonedDateTime.class ) {
			canonical = Set.of( INSTANT, OFFSET, ZONE_ID );
		}
		else {
			canonical = null;
		}

		if ( javaType instanceof PreservationAwareTemporalJavaType<?> aware ) {
			final var parts = aware.getRequiredTemporalParts();
			if ( parts == null || parts.isEmpty() ) {
				throw new MappingException( "PreservationAwareTemporalJavaType must declare a non-null, nonempty requirement set" );
			}
			final Set<TemporalPreservation.TemporalPart> copy;
			try {
				copy = Set.copyOf( parts );
			}
			catch (NullPointerException failure) {
				throw new MappingException( "PreservationAwareTemporalJavaType requirements contain a null part", failure );
			}
			if ( canonical != null && !canonical.equals( copy ) ) {
				throw new MappingException( "PreservationAwareTemporalJavaType declaration " + copy
						+ " contradicts the required Java Time contract " + canonical + " for " + type.getName() );
			}
			return copy;
		}
		return canonical;
	}

	private static String mappingLabel(String role) {
		return role == null ? "Temporal mapping" : "Temporal mapping '" + role + "'";
	}

	private static MappingException assessmentFailure(
			JavaType<?> javaType, JdbcType jdbcType, String role, String source, RuntimeException failure) {
		return new MappingException( mappingLabel( role ) + " (" + javaType.getClass().getName()
				+ " + " + jdbcType.getClass().getName() + ") " + source + " failed: " + failure.getMessage(), failure );
	}

}
