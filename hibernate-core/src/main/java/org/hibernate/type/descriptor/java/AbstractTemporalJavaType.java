/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.util.Comparator;

import jakarta.persistence.TemporalType;

import org.hibernate.SPI;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Base Java descriptor for temporal values with precision-specific
/// resolution hooks.
///
/// @param <T> the represented temporal value type
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public abstract class AbstractTemporalJavaType<T>
		extends AbstractClassJavaType<T>
		implements TemporalJavaType<T> {

	/// Initialize an immutable temporal descriptor.
	@SPI(IMPLEMENT)
	protected AbstractTemporalJavaType(Class<T> type) {
		super( type );
	}

	/// Initialize a temporal descriptor with explicit mutability semantics.
	@SPI(IMPLEMENT)
	protected AbstractTemporalJavaType(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		super( type, mutabilityPlan );
	}

	/// Initialize a temporal descriptor with explicit mutability and comparison
	/// semantics.
	@SPI(IMPLEMENT)
	public AbstractTemporalJavaType(
			Class<T> type,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator) {
		super( type, mutabilityPlan, comparator );
	}

	@Override
	public TemporalJavaType<T> resolveTypeForPrecision(
			TemporalType precision,
			TypeConfiguration typeConfiguration) {
		if ( precision == null ) {
			return forMissingPrecision( typeConfiguration );
		}
		else {
			return switch ( precision ) {
				case DATE -> forDatePrecision( typeConfiguration );
				case TIME -> forTimePrecision( typeConfiguration );
				case TIMESTAMP -> forTimestampPrecision( typeConfiguration );
			};
		}
	}

	private TemporalJavaType<T> forMissingPrecision(TypeConfiguration typeConfiguration) {
		return this;
	}

	protected TemporalJavaType<T> forTimestampPrecision(TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException(
				getTypeName() + " as TemporalType.TIMESTAMP not supported"
		);
	}

	protected TemporalJavaType<T> forDatePrecision(TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException(
				getTypeName() + " as TemporalType.DATE not supported"
		);
	}

	protected TemporalJavaType<T> forTimePrecision(TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException(
				getTypeName() + " as TemporalType.TIME not supported"
		);
	}

	@Override
	public String toString() {
		return "TemporalJavaType(javaType=" + getTypeName() + ")";
	}
}
