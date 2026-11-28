package org.hibernate.usertype.internal;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

import org.hibernate.HibernateException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.SqlTypes;

/// Stores a [ZonedDateTime] as its instant in one column and the identifier
/// of its zone, such as `Europe/Paris` or `+05:45`, in a second. The two
/// together determine the value exactly, including which occurrence it is of
/// a time repeated when clocks are set back.
///
/// Used for `@TimeZoneRepresentation(COLUMN)` with temporal preservation
/// enabled. Unlike [ZonedDateTimeCompositeUserType], which keeps the offset
/// and not the zone, this keeps the zone.
///
/// @since 8.1
/// @author Steve Ebersole
public class ZonedDateTimeZoneIdCompositeUserType extends AbstractTimeZoneStorageCompositeUserType<ZonedDateTime> {
	public static final String ZONE_ID_NAME = "zoneId";

	@Override
	@Nonnull
	public Object getPropertyValue(@Nonnull ZonedDateTime component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component.toInstant();
			case 1 -> component.getZone();
			default -> throw new HibernateException( "Illegal property index: " + property );
		};
	}

	@Override
	@Nullable
	public ZonedDateTime instantiate(@Nonnull ValueAccess values) {
		final Instant instant = values.getValue( 0, Instant.class );
		final ZoneId zoneId = values.getValue( 1, ZoneId.class );
		return instant == null || zoneId == null ? null : ZonedDateTime.ofInstant( instant, zoneId );
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return ZonedDateTimeZoneIdEmbeddable.class;
	}

	@Override
	@Nonnull
	public Class<ZonedDateTime> returnedClass() {
		return ZonedDateTime.class;
	}

	/// Mapped by its fields whatever the access type of the entity is, since
	/// it has no accessor methods to carry the mapping.
	@Access(AccessType.FIELD)
	public static class ZonedDateTimeZoneIdEmbeddable {
		private Instant instant;
		@JdbcTypeCode( SqlTypes.VARCHAR )
		private ZoneId zoneId;
	}
}
