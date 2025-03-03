/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype.internal;

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
	public Object getPropertyValue(ZonedDateTime component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component.toInstant();
			case 1 -> component.getOffset();
			default -> null;
		};
	}

	@Override
	public ZonedDateTime instantiate(ValueAccess values) {
		final Instant instant = values.getValue( 0, Instant.class );
		final ZoneOffset zoneOffset = values.getValue( 1, ZoneOffset.class );
		return instant == null || zoneOffset == null ? null : ZonedDateTime.ofInstant( instant, zoneOffset );
	}

	@Override
	public Class<?> embeddable() {
		return ZonedDateTimeEmbeddable.class;
	}

	@Override
	public Class<ZonedDateTime> returnedClass() {
		return ZonedDateTime.class;
	}

	public static class ZonedDateTimeEmbeddable {
		private Instant instant;
		@JdbcTypeCode( SqlTypes.INTEGER )
		private ZoneOffset zoneOffset;
	}
}
