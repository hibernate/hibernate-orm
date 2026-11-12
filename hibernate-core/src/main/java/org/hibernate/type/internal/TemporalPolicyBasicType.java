package org.hibernate.type.internal;

import org.hibernate.type.AdjustableBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.TemporalPolicySettings.Selection;

/// Mapping-local policy metadata with execution, equality and cache behavior delegated unchanged.
/// Never registered in the shared basic type registry.
///
/// @author Steve Ebersole
public final class TemporalPolicyBasicType<T> extends AbstractDelegatingBasicType<T> {
	private final Selection policy;

	public TemporalPolicyBasicType(BasicType<T> delegate, Selection policy) {
		super( delegate );
		this.policy = policy;
	}

	public Selection getPolicy() { return policy; }

	@Override
	@SuppressWarnings("unchecked")
	public <X> BasicType<X> resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<X> javaType) {
		if ( delegate instanceof AdjustableBasicType<?> adjustable ) {
			final var resolved = adjustable.resolveIndicatedType( indicators, javaType );
			return resolved == delegate ? (BasicType<X>) this : new TemporalPolicyBasicType<>( resolved, policy );
		}
		return (BasicType<X>) this;
	}
}
