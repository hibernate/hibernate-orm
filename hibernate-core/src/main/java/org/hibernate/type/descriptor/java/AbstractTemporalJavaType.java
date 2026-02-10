/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	protected AbstractTemporalJavaType(Class<T> type) {
		super( type );
	}

	protected AbstractTemporalJavaType(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		super( type, mutabilityPlan );
	}

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
