package org.hibernate.usertype.internal;

import java.time.Instant;
import java.time.ZoneOffset;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/// Stores an [java.time.OffsetDateTime] as its instant in one column and its
/// offset from UTC, as a number of seconds, in a second.
///
/// Used for `@TimeZoneRepresentation(COLUMN)` with temporal preservation
/// enabled. It stores exactly what [OffsetDateTimeCompositeUserType] does,
/// and differs only in mapping its two columns by field whatever the access
/// type of the entity is.
///
/// @since 8.1
/// @author Steve Ebersole
public class OffsetDateTimeOffsetCompositeUserType extends OffsetDateTimeCompositeUserType {
	@Override
	@Nonnull
	public Class<?> embeddable() {
		return OffsetDateTimeOffsetEmbeddable.class;
	}

	/// Mapped by its fields whatever the access type of the entity is, since
	/// it has no accessor methods to carry the mapping.
	@Access(AccessType.FIELD)
	public static class OffsetDateTimeOffsetEmbeddable {
		private Instant instant;
		@JdbcTypeCode( SqlTypes.INTEGER )
		private ZoneOffset zoneOffset;
	}
}
