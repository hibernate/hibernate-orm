package org.hibernate.dialect.temporaltype.spi;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Describes preservation facts for individual parts of a resolved Java/JDBC descriptor pair.
/// These facts validate a mapping; they do not configure JDBC execution or promise requested adjustment output.
///
/// @see org.hibernate.dialect.Dialect#getTemporalPreservation(JavaType, JdbcType)
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
@Incubating( since = "8.0" )
public record TemporalPreservation(
		Level date,
		Level time,
		Level instant,
		Level offset,
		Level zoneId,
		Level normalizedTime) {

	public static final TemporalPreservation UNKNOWN = new TemporalPreservation(
			Level.UNKNOWN, Level.UNKNOWN, Level.UNKNOWN, Level.UNKNOWN, Level.UNKNOWN, Level.UNKNOWN );

	/// Compatibility constructor for contributors that do not assess normalized time.
	public TemporalPreservation(Level date, Level time, Level instant, Level offset, Level zoneId) {
		this( date, time, instant, offset, zoneId, Level.UNKNOWN );
	}

	public TemporalPreservation {
		if ( date == null || time == null || instant == null || offset == null || zoneId == null || normalizedTime == null ) {
			throw new IllegalArgumentException( "Temporal preservation parts must not be null" );
		}
	}

	public Level preserves(TemporalPart part) {
		return switch ( part ) {
			case DATE -> date;
			case TIME -> time;
			case INSTANT -> instant;
			case OFFSET -> offset;
			case ZONE_ID -> zoneId;
			case NORMALIZED_TIME -> normalizedTime != Level.UNKNOWN ? normalizedTime
					: time == Level.SUPPORTED && offset == Level.SUPPORTED ? Level.SUPPORTED : Level.UNKNOWN;
		};
	}

	public enum Level {
		UNKNOWN, UNSUPPORTED, IRRELEVANT, SUPPORTED
	}

	public enum TemporalPart {
		DATE, TIME, INSTANT, OFFSET, ZONE_ID, NORMALIZED_TIME
	}
}
