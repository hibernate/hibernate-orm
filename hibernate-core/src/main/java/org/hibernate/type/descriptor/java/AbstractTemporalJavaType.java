/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

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
