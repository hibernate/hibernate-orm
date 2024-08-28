/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;

import jakarta.persistence.TemporalType;

import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTemporalJavaType<T>
		extends AbstractClassJavaType<T>
		implements TemporalJavaType<T> {

	protected AbstractTemporalJavaType(Class<? extends T> type) {
		super( type );
	}

	protected AbstractTemporalJavaType(Class<? extends T> type, MutabilityPlan<? extends T> mutabilityPlan) {
		super( type, mutabilityPlan );
	}

	public AbstractTemporalJavaType(
			Class<? extends T> type,
			MutabilityPlan<? extends T> mutabilityPlan,
			Comparator<? extends T> comparator) {
		super( type, mutabilityPlan, comparator );
	}

	@Override
	public final <X> TemporalJavaType<X> resolveTypeForPrecision(
			TemporalType precision,
			TypeConfiguration typeConfiguration) {
		if ( precision == null ) {
			return forMissingPrecision( typeConfiguration );
		}

		switch ( precision ) {
			case DATE: {
				return forDatePrecision( typeConfiguration );
			}
			case TIME: {
				return forTimePrecision( typeConfiguration );
			}
			case TIMESTAMP: {
				return forTimestampPrecision( typeConfiguration );
			}
		}

		throw new IllegalArgumentException( "Unrecognized JPA TemporalType precision [" + precision + "]" );
	}

	private <X> TemporalJavaType<X> forMissingPrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked,rawtypes
		return (TemporalJavaType) this;
	}

	public static Time millisToSqlTime(long millis) {
		final LocalTime localTime = Instant.ofEpochMilli( millis ).atZone( ZoneId.systemDefault() ).toLocalTime();
		final Time time = Time.valueOf( localTime );
		time.setTime( time.getTime() + localTime.getNano() / 1_000_000 );
		return time;
	}

	protected <X> TemporalJavaType<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException(
				this + " as `jakarta.persistence.TemporalType.TIMESTAMP` not supported"
		);
	}

	protected <X> TemporalJavaType<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException(
				this + " as `jakarta.persistence.TemporalType.DATE` not supported"
		);
	}

	protected <X> TemporalJavaType<X> forTimePrecision(TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException(
				this + " as `jakarta.persistence.TemporalType.TIME` not supported"
		);
	}

	@Override
	public String toString() {
		return "TemporalJavaType(javaType=" + getTypeName() + ")";
	}
}
