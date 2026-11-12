package org.hibernate.type.internal;

import java.util.function.Supplier;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.internal.EmbeddableCompositeUserTypeInstantiator;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.usertype.internal.ContextualTemporalCompositeUserType;

/// Assignment-only COLUMN tuple preparation. Immutable tuple arrays are scoped to
/// one decomposition; retained cache payloads belong to the entity or collection
/// occurrence. Generic decomposition, restrictions and custom composites keep
/// their existing semantics.
///
/// @author Steve Ebersole
public final class TemporalColumnPrecision {
	private TemporalColumnPrecision() {
	}

	public static ContextualTemporalCompositeUserType<?> find(ModelPart part) {
		if ( part instanceof EmbeddableValuedModelPart embedded
				&& !embedded.getEmbeddableTypeDescriptor().isPolymorphic()
				&& embedded.getEmbeddableTypeDescriptor().getRepresentationStrategy().getInstantiator() instanceof EmbeddableCompositeUserTypeInstantiator wrapper
				&& wrapper.getUserType() instanceof ContextualTemporalCompositeUserType<?> temporal && temporal.hasPrecision() ) {
			return temporal;
		}
		return null;
	}

	public static Object prepare(Object value, ModelPart part, Object entity, SharedSessionContractImplementor session, boolean insert) {
		return part instanceof EmbeddableValuedModelPart
				? prepare( value, part, () -> TemporalPreparedState.forEntity( entity, session ), insert ) : value;
	}

	public static Object prepare(Object value, ModelPart part, TemporalPreparedState state, boolean insert) {
		return prepare( value, part, () -> state, insert );
	}

	private static Object prepare(Object value, ModelPart part, Supplier<TemporalPreparedState> stateAccess, boolean insert) {
		if ( part instanceof AttributeMapping attribute
				&& !(insert ? attribute.getAttributeMetadata().isInsertable() : attribute.getAttributeMetadata().isUpdatable()) ) {
			return value;
		}
		final var temporal = find( part );
		if ( temporal != null ) {
			final var tuple = temporal.prepareTuple( value );
			final var state = stateAccess.get();
			if ( state != null ) {
				state.recordCacheCandidate( temporal, temporal.disassembleTuple( tuple ) );
			}
			return tuple;
		}
		if ( value != null && part instanceof EmbeddableValuedModelPart embedded ) {
			final var mapping = embedded.getEmbeddableTypeDescriptor();
			// Aggregate encoding and polymorphic component decomposition own their
			// representation; this path handles ordinary flattened embeddables.
			if ( mapping.shouldBindAggregateMapping() || mapping.isPolymorphic()
					|| mapping.getRepresentationStrategy().getInstantiator() instanceof EmbeddableCompositeUserTypeInstantiator ) {
				return value;
			}
			Object[] prepared = null;
			for ( int i = 0; i < mapping.getNumberOfAttributeMappings(); i++ ) {
				final var member = mapping.getAttributeMapping( i );
				if ( !(member instanceof EmbeddableValuedModelPart) ) { continue; }
				final Object original = value instanceof Object[] values ? values[i] : member.getValue( value );
				final Object replacement = prepare( original, member, stateAccess, insert );
				if ( replacement != original ) {
					if ( prepared == null ) {
						prepared = value instanceof Object[] values ? values.clone() : mapping.getValues( value ).clone();
					}
					prepared[i] = replacement;
				}
			}
			return prepared == null ? value : prepared;
		}
		return value;
	}
}
