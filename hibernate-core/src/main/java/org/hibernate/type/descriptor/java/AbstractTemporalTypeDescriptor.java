/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import jakarta.persistence.TemporalType;

import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTemporalTypeDescriptor<T> extends AbstractClassTypeDescriptor<T> implements TemporalJavaTypeDescriptor<T> {
	protected AbstractTemporalTypeDescriptor(Class<T> type) {
		super( type );
	}

	protected AbstractTemporalTypeDescriptor(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		super( type, mutabilityPlan );
	}

	@Override
	public final <X> TemporalJavaTypeDescriptor<X> resolveTypeForPrecision(
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

	private <X> TemporalJavaTypeDescriptor<X> forMissingPrecision(TypeConfiguration typeConfiguration) {
		//noinspection unchecked,rawtypes
		return (TemporalJavaTypeDescriptor) this;
	}

	protected <X> TemporalJavaTypeDescriptor<X> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException(
				toString() + " as `jakarta.persistence.TemporalType.TIMESTAMP` not supported"
		);
	}

	protected <X> TemporalJavaTypeDescriptor<X> forDatePrecision(TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException(
				toString() + " as `jakarta.persistence.TemporalType.DATE` not supported"
		);
	}

	protected <X> TemporalJavaTypeDescriptor<X> forTimePrecision(TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException(
				toString() + " as `jakarta.persistence.TemporalType.TIME` not supported"
		);
	}

	@Override
	public String toString() {
		return "TemporalJavaTypeDescriptor(javaType=" + getJavaType().getTypeName() + ")";
	}
}
