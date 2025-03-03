/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype.internal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.HibernateException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.SqlTypes;

/**
 * @author Christian Beikov
 */
public class OffsetDateTimeCompositeUserType extends AbstractTimeZoneStorageCompositeUserType<OffsetDateTime> {

	@Override
	public Object getPropertyValue(OffsetDateTime component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component.toInstant();
			case 1 -> component.getOffset();
			default -> null;
		};
	}

	@Override
	public OffsetDateTime instantiate(ValueAccess values) {
		final Instant instant = values.getValue( 0, Instant.class );
		final ZoneOffset zoneOffset = values.getValue( 1, ZoneOffset.class );
		return instant == null || zoneOffset == null ? null : OffsetDateTime.ofInstant( instant, zoneOffset );
	}

	@Override
	public Class<?> embeddable() {
		return OffsetDateTimeEmbeddable.class;
	}

	@Override
	public Class<OffsetDateTime> returnedClass() {
		return OffsetDateTime.class;
	}

	public static class OffsetDateTimeEmbeddable {
		private Instant instant;
		@JdbcTypeCode( SqlTypes.INTEGER )
		private ZoneOffset zoneOffset;
	}
}
