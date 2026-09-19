/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;

/**
 * Stored keys of associations whose targets were excluded while hydrating an entity.
 * A null Java attribute with an entry here must not erase its stored reference.
 */
public final class FilteredAssociationState implements EntityEntryExtraState, Serializable {
	private final Map<Integer, Key> keys = new HashMap<>();
	private final Map<Integer, FilteredAssociationState> components = new HashMap<>();
	private transient EntityEntryExtraState next;

	/** A physical association key, used only during JDBC mutation binding. */
	public record Key(Object value) implements Serializable {
	}

	public static FilteredAssociationState from(
			DomainResultAssembler<?>[] assemblers, Object[] values, RowProcessingState rowProcessingState) {
		FilteredAssociationState state = null;
		for ( int i = 0; i < assemblers.length; i++ ) {
			final var assembler = assemblers[i];
			if ( assembler != null ) {
				final var initializer = assembler.getInitializer();
				if ( values[i] == null && initializer instanceof EntityInitializer<?> entityInitializer ) {
					final Object key = entityInitializer.getFilteredAssociationKey( rowProcessingState );
					if ( key != null ) {
						if ( state == null ) {
							state = new FilteredAssociationState();
						}
						state.keys.put( i, new Key( key ) );
					}
				}
				else if ( initializer instanceof EmbeddableInitializer<?> embeddableInitializer ) {
					final var component = embeddableInitializer.getFilteredAssociationState( rowProcessingState );
					if ( component != null ) {
						if ( state == null ) {
							state = new FilteredAssociationState();
						}
						state.components.put( i, component );
					}
				}
			}
		}
		return state;
	}

	public static void register(EntityEntry entry, FilteredAssociationState state) {
		if ( state != null ) {
			final var existing = entry.getExtraState( FilteredAssociationState.class );
			if ( existing == null ) {
				entry.addExtraState( state );
			}
			else {
				existing.keys.putAll( state.keys );
				existing.components.putAll( state.components );
			}
		}
	}

	public static boolean hasFilteredAssociations(EntityEntry entry) {
		final var state = entry == null ? null : entry.getExtraState( FilteredAssociationState.class );
		return state != null && !state.isEmpty();
	}

	public boolean isEmpty() {
		return keys.isEmpty() && components.isEmpty();
	}

	public boolean isFiltered(int position) {
		return keys.containsKey( position );
	}

	public Object[] physicalState(Object[] state, ManagedMappingType mapping) {
		if ( state == null || isEmpty() ) {
			return state;
		}
		final Object[] result = state.clone();
		keys.forEach( (position, key) -> {
			if ( result[position] == null ) {
				result[position] = key;
			}
		} );
		components.forEach( (position, component) -> {
			if ( result[position] != UNFETCHED_PROPERTY ) {
				final var componentMapping = (EmbeddableMappingType) mapping.getAttributeMapping( position ).getMappedType();
				result[position] = component.physicalState( componentValues( result[position], componentMapping ), componentMapping );
			}
		} );
		return result;
	}

	private static Object[] componentValues(Object value, EmbeddableMappingType mapping) {
		if ( value instanceof Object[] values ) {
			return values;
		}
		final Object[] values = value == null
				? new Object[mapping.getNumberOfAttributeMappings()] : mapping.getValues( value );
		if ( mapping.isPolymorphic() ) {
			final Object[] withDiscriminator = Arrays.copyOf( values, values.length + 1 );
			if ( value != null ) {
				withDiscriminator[values.length] = mapping.findSubtypeBySubclass( value.getClass().getName() ).getDiscriminatorValue();
			}
			return withDiscriminator;
		}
		return values;
	}

	public void afterUpdate(Object[] state, ManagedMappingType mapping) {
		keys.keySet().removeIf( position -> state[position] != null && state[position] != UNFETCHED_PROPERTY );
		components.entrySet().removeIf( entry -> {
			final Object value = state[entry.getKey()];
			if ( value != UNFETCHED_PROPERTY ) {
				final var componentMapping = (EmbeddableMappingType) mapping.getAttributeMapping( entry.getKey() ).getMappedType();
				entry.getValue().afterUpdate( componentValues( value, componentMapping ), componentMapping );
			}
			return entry.getValue().isEmpty();
		} );
	}

	@Override
	public void addExtraState(EntityEntryExtraState extraState) {
		if ( next == null ) {
			next = extraState;
		}
		else {
			next.addExtraState( extraState );
		}
	}

	@Override
	public <T extends EntityEntryExtraState> T getExtraState(Class<T> type) {
		return next == null ? null : type.isInstance( next ) ? type.cast( next ) : next.getExtraState( type );
	}
}
