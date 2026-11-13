package org.hibernate.usertype.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.HibernateException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.SqlTypes;

/**
 * @author Christian Beikov
 */
public class ZonedDateTimeCompositeUserType extends AbstractTimeZoneStorageCompositeUserType<ZonedDateTime> {

	@Override
	@Nullable
	public Object getPropertyValue(@Nonnull ZonedDateTime component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component.toInstant();
			case 1 -> component.getOffset();
			default -> null;
		};
	}

	@Override
	@Nullable
	public ZonedDateTime instantiate(@Nonnull ValueAccess values) {
		final Instant instant = values.getValue( 0, Instant.class );
		final ZoneOffset zoneOffset = values.getValue( 1, ZoneOffset.class );
		return instant == null || zoneOffset == null ? null : ZonedDateTime.ofInstant( instant, zoneOffset );
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return ZonedDateTimeEmbeddable.class;
	}

	@Override
	@Nonnull
	public Class<ZonedDateTime> returnedClass() {
		return ZonedDateTime.class;
	}

	public static class ZonedDateTimeEmbeddable {
		private Instant instant;
		@JdbcTypeCode( SqlTypes.INTEGER )
		private ZoneOffset zoneOffset;
	}
}
