/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype.internal;

import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.hibernate.HibernateException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.SqlTypes;

/**
 * @author Christian Beikov
 */
public class OffsetTimeCompositeUserType extends AbstractTimeZoneStorageCompositeUserType<OffsetTime> {

	public static final String LOCAL_TIME_NAME = "utcTime";

	@Override
	public Object getPropertyValue(OffsetTime component, int property) throws HibernateException {
		switch ( property ) {
			case 0:
				return component.withOffsetSameInstant( ZoneOffset.UTC );
			case 1:
				return component.getOffset();
		}
		return null;
	}

	@Override
	public OffsetTime instantiate(ValueAccess values, SessionFactoryImplementor sessionFactory) {
		final OffsetTime utcTime = values.getValue( 0, OffsetTime.class );
		final ZoneOffset zoneOffset = values.getValue( 1, ZoneOffset.class );
		return utcTime == null || zoneOffset == null ? null : utcTime.withOffsetSameInstant( zoneOffset );
	}

	@Override
	public Class<?> embeddable() {
		return OffsetTimeEmbeddable.class;
	}

	@Override
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
