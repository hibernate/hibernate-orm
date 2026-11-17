package org.hibernate.usertype.internal;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

import org.hibernate.HibernateException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.SqlTypes;

/// Stores an [java.time.OffsetTime] as its time of day at UTC in one column
/// and its offset from UTC, as a number of seconds, in a second.
///
/// Used for `@TimeZoneRepresentation(COLUMN)` with temporal preservation
/// enabled, on a database where an `OffsetTime` itself is not exchanged
/// with the JDBC driver without loss. It stores what
/// [OffsetTimeOffsetCompositeUserType] does, and differs in exchanging the
/// time at UTC as a [LocalTime], which involves no time zone.
///
/// @since 8.1
/// @author Steve Ebersole
public class OffsetTimeLocalTimeCompositeUserType extends OffsetTimeCompositeUserType {
	@Override
	@Nonnull
	public Object getPropertyValue(@Nonnull OffsetTime component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component.withOffsetSameInstant( ZoneOffset.UTC ).toLocalTime();
			case 1 -> component.getOffset();
			default -> throw new HibernateException( "Illegal property index: " + property );
		};
	}

	@Override
	@Nullable
	public OffsetTime instantiate(@Nonnull ValueAccess values) {
		final LocalTime utcTime = values.getValue( 0, LocalTime.class );
		final ZoneOffset zoneOffset = values.getValue( 1, ZoneOffset.class );
		return utcTime == null || zoneOffset == null
				? null
				: utcTime.atOffset( ZoneOffset.UTC ).withOffsetSameInstant( zoneOffset );
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return OffsetTimeLocalTimeEmbeddable.class;
	}

	/// Mapped by its fields whatever the access type of the entity is, since
	/// it has no accessor methods to carry the mapping.
	@Access(AccessType.FIELD)
	public static class OffsetTimeLocalTimeEmbeddable {
		private LocalTime utcTime;
		@JdbcTypeCode( SqlTypes.INTEGER )
		private ZoneOffset zoneOffset;
	}
}
