package org.hibernate.usertype.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.hibernate.HibernateException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.SqlTypes;

/**
 * @author Christian Beikov
 */
public class OffsetTimeCompositeUserType extends AbstractTimeZoneStorageCompositeUserType<OffsetTime> {

	public static final String LOCAL_TIME_NAME = "utcTime";

	@Override
	@Nullable
	public Object getPropertyValue(@Nonnull OffsetTime component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component.withOffsetSameInstant( ZoneOffset.UTC );
			case 1 -> component.getOffset();
			default -> null;
		};
	}

	@Override
	@Nullable
	public OffsetTime instantiate(@Nonnull ValueAccess values) {
		final OffsetTime utcTime = values.getValue( 0, OffsetTime.class );
		final ZoneOffset zoneOffset = values.getValue( 1, ZoneOffset.class );
		return utcTime == null || zoneOffset == null ? null : utcTime.withOffsetSameInstant( zoneOffset );
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return OffsetTimeEmbeddable.class;
	}

	@Override
	@Nonnull
	public Class<OffsetTime> returnedClass() {
		return OffsetTime.class;
	}

	public static class OffsetTimeEmbeddable {
		@TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
		private OffsetTime utcTime;
		@JdbcTypeCode( SqlTypes.INTEGER )
		private ZoneOffset zoneOffset;
	}
}
