/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype.internal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.HibernateException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.SqlTypes;

/**
 * @author Christian Beikov
 */
public class OffsetDateTimeCompositeUserType extends AbstractTimeZoneStorageCompositeUserType<OffsetDateTime> {

	@Override
	public Object getPropertyValue(OffsetDateTime component, int property) throws HibernateException {
		switch ( property ) {
			case 0:
				return component.toInstant();
			case 1:
				return component.getOffset();
		}
		return null;
	}

	@Override
	public OffsetDateTime instantiate(ValueAccess values, SessionFactoryImplementor sessionFactory) {
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
