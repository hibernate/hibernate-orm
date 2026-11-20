package org.hibernate.usertype.internal;

import java.time.OffsetTime;
import java.time.ZoneOffset;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.type.SqlTypes;

/// Stores an [java.time.OffsetTime] as its time of day at UTC in one column
/// and its offset from UTC, as a number of seconds, in a second.
///
/// Used for `@TimeZoneRepresentation(COLUMN)` with temporal preservation
/// enabled. It stores exactly what [OffsetTimeCompositeUserType] does, and
/// differs in mapping its two columns by field whatever the access type of
/// the entity is, and in selecting the column for the time at UTC the way
/// temporal preservation does for an `OffsetTime` stored in a single column.
///
/// @since 8.1
/// @author Steve Ebersole
public class OffsetTimeOffsetCompositeUserType extends OffsetTimeCompositeUserType {
	@Override
	@Nonnull
	public Class<?> embeddable() {
		return OffsetTimeOffsetEmbeddable.class;
	}

	/// Mapped by its fields whatever the access type of the entity is, since
	/// it has no accessor methods to carry the mapping.
	@Access(AccessType.FIELD)
	public static class OffsetTimeOffsetEmbeddable {
		// always at UTC, so its own offset need not be kept
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		private OffsetTime utcTime;
		@JdbcTypeCode( SqlTypes.INTEGER )
		private ZoneOffset zoneOffset;
	}
}
