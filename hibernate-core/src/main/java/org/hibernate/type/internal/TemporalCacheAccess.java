package org.hibernate.type.internal;

import java.io.Serializable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;

/// Explicit occurrence context for persistent cache operations. This dispatch is
/// deliberately separate from query-key disassembly, which consumes relational
/// comparison values. Unrelated types always receive their original owner.
///
/// @author Steve Ebersole
public final class TemporalCacheAccess {
	private TemporalCacheAccess() {
	}

	public static Serializable disassemble(Type type, Object value, SharedSessionContractImplementor session,
			Object owner, TemporalPreparedState state) {
		if ( state != null ) {
			if ( type instanceof BasicType<?> basic ) {
				final var temporal = TemporalPrecisionBasicType.find( basic );
				if ( temporal != null ) {
					if ( state.contains( temporal ) ) {
						return state.get( temporal );
					}
					if ( temporal.getValueConverter() instanceof JpaAttributeConverter<?, ?> ) {
						throw temporal.missingPreparedState( "collection element/member occurrence" );
					}
				}
			}
			else if ( type.getClass() == ComponentType.class ) {
				return ((ComponentType) type).disassemble( value, session, owner, state );
			}
		}
		return type.disassemble( value, session, owner );
	}

	public static Object assemble(Type type, Serializable value, SharedSessionContractImplementor session,
			Object owner, TemporalPreparedState state) {
		if ( state != null ) {
			if ( type instanceof BasicType<?> basic ) {
				final var temporal = TemporalPrecisionBasicType.find( basic );
				if ( temporal != null ) {
					return temporal.assemble( value, session, owner, state );
				}
			}
			else if ( type.getClass() == ComponentType.class ) {
				return ((ComponentType) type).assemble( value, session, owner, state );
			}
		}
		return type.assemble( value, session, owner );
	}
}
