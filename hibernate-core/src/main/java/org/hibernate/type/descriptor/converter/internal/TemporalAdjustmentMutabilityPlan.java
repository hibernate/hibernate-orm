package org.hibernate.type.descriptor.converter.internal;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/// Applies mapping adjustment to cache values without canonicalizing precision
/// or changing dirty-checking snapshots of immutable domain values.
///
/// @see org.hibernate.annotations.TimeZoneAdjustment
///
/// @since 8.1
/// @author Steve Ebersole
public final class TemporalAdjustmentMutabilityPlan<T> implements MutabilityPlan<T> {
	private final TemporalAdjustmentConverter<T> converter;
	private final MutabilityPlan<T> delegate;

	public TemporalAdjustmentMutabilityPlan(TemporalAdjustmentConverter<T> converter, MutabilityPlan<T> delegate) {
		this.converter = converter;
		this.delegate = delegate;
	}

	@Override
	public boolean isMutable() {
		return delegate.isMutable();
	}

	@Override
	public T deepCopy(T value) {
		return delegate.deepCopy( value );
	}

	@Override
	public Serializable disassemble(T value, SharedSessionContract session) {
		return delegate.disassemble( converter.toRelationalValue( value ), session );
	}

	@Override
	public T assemble(Serializable cached, SharedSessionContract session) {
		return converter.toDomainValue( delegate.assemble( cached, session ) );
	}
}
