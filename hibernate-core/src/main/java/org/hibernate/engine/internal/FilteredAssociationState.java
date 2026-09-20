/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.util.Arrays;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;

/**
 * Stored keys of associations whose targets were excluded while hydrating an entity.
 * Allocated only for hidden references. Each node holds one raw key or one nested
 * component, without maps, boxed positions, or retained mutation-binding wrappers.
 */
public final class FilteredAssociationState implements EntityEntryExtraState, Serializable {
	// A negative position encodes a component index as its bitwise complement.
	private int position;
	private Object value;
	private FilteredAssociationState sibling;
	private transient EntityEntryExtraState next;

	private FilteredAssociationState(int position, Object value, FilteredAssociationState sibling) {
		this.position = position;
		this.value = value;
		this.sibling = sibling;
	}

	/** A physical association key in temporary mutation or nullability-checking state. */
	public record Key(Object value) implements Serializable {
		public Key {
			assert value != null && !(value instanceof Key);
		}
	}

	/** Builds shared assembler metadata once, returning null for unaffected result graphs. */
	public static int[][] assemblerIndexes(DomainResultAssembler<?>[][] assemblers) {
		int[][] result = null;
		for ( int subtype = 0; subtype < assemblers.length; subtype++ ) {
			final var row = assemblers[subtype];
			if ( row != null ) {
				int[] indexes = null;
				int count = 0;
				for ( int i = 0; i < row.length; i++ ) {
					if ( row[i] != null ) {
						final var initializer = row[i].getInitializer();
						// An @Any initializer resolves its target type from each row's discriminator.
						// Only ordinary to-one mappings have a static target descriptor to inspect here.
						final boolean affected =
								initializer instanceof EntityInitializer<?> entity
										? hasFilterOrRestriction( entity )
										: hasRestrictedAssociations( initializer );
						if ( affected ) {
							if ( indexes == null ) {
								indexes = new int[row.length];
							}
							indexes[count++] = i;
						}
					}
				}
				if ( count != 0 ) {
					if ( result == null ) {
						result = new int[assemblers.length][];
					}
					result[subtype] = Arrays.copyOf( indexes, count );
				}
			}
		}
		return result;
	}

	private static boolean hasFilterOrRestriction(EntityInitializer<?> entity) {
		return entity.getInitializedPart() instanceof ToOneAttributeMapping
			&& ( entity.getEntityDescriptor().hasWhereRestrictions()
				   || entity.getEntityDescriptor().hasFilterForLoadByKey() );
	}

	private static boolean hasRestrictedAssociations(Initializer<?> initializer) {
		return initializer instanceof EmbeddableInitializer<?> embedded
			&& hasRestrictedAssociations( embedded.getInitializedPart().getEmbeddableTypeDescriptor() );
	}

	private static boolean hasRestrictedAssociations(ManagedMappingType mapping) {
		for ( int i = 0; i < mapping.getNumberOfAttributeMappings(); i++ ) {
			final var attribute = mapping.getAttributeMapping( i );
			if ( attribute instanceof ToOneAttributeMapping toOne ) {
				final var target = toOne.getEntityMappingType().getEntityPersister();
				if ( target.hasWhereRestrictions() || target.hasFilterForLoadByKey() ) {
					return true;
				}
			}
			else if ( attribute instanceof EmbeddableValuedModelPart embedded
					&& hasRestrictedAssociations( embedded.getEmbeddableTypeDescriptor() ) ) {
				return true;
			}
		}
		return false;
	}

	public static FilteredAssociationState from(
			DomainResultAssembler<?>[] assemblers, int[] indexes, Object[] values, RowProcessingState rowProcessingState) {
		FilteredAssociationState state = null;
		if ( indexes != null ) {
			for ( int i : indexes ) {
				final var initializer = assemblers[i].getInitializer();
				if ( values[i] == null
						&& initializer instanceof EntityInitializer<?> entityInitializer ) {
					final Object key =
							entityInitializer.getFilteredAssociationKey( rowProcessingState );
					if ( key != null ) {
						state = new FilteredAssociationState( i, key, state );
					}
				}
				else if ( initializer instanceof EmbeddableInitializer<?> embeddableInitializer ) {
					final var component =
							embeddableInitializer.getFilteredAssociationState( rowProcessingState );
					if ( component != null ) {
						state = new FilteredAssociationState( ~i, component, state );
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
				for ( var current = state; current != null; current = current.sibling ) {
					existing.merge( current.position, current.value );
				}
			}
		}
	}

	private void merge(int position, Object value) {
		for ( var current = this; ; current = current.sibling ) {
			if ( current.value == null || current.position == position ) {
				current.position = position;
				current.value = value;
				return;
			}
			if ( current.sibling == null ) {
				current.sibling = new FilteredAssociationState( position, value, null );
				return;
			}
		}
	}

	public static boolean hasFilteredAssociations(EntityEntry entry) {
		final var state = entry == null ? null
				: entry.getExtraState( FilteredAssociationState.class );
		return state != null && !state.isEmpty();
	}

	public boolean isEmpty() {
		return value == null;
	}

	public boolean isFiltered(int position) {
		for ( var current = this; current != null; current = current.sibling ) {
			if ( current.position == position && current.value != null ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates temporary storage state for update/delete decomposition and update
	 * nullability checks. Hidden to-one values become {@link Key} wrappers, and
	 * affected embeddables become nested {@code Object[]} arrays. Only consumers
	 * which decompose values through their attribute mappings may bind this state.
	 * It must never replace entity attribute values, dirty-checking snapshots, or
	 * second-level cache state. The supplied domain state is not modified.
	 */
	public Object[] physicalState(Object[] state, ManagedMappingType mapping) {
		if ( state == null || isEmpty() ) {
			return state;
		}
		else {
			final Object[] result = state.clone();
			for ( var current = this; current != null; current = current.sibling ) {
				final int position = current.position;
				if ( position >= 0 ) {
					assert mapping.getAttributeMapping( position ) instanceof ToOneAttributeMapping;
					if ( result[position] == null ) {
						result[position] = new Key( current.value );
					}
				}
				else if ( result[~position] != UNFETCHED_PROPERTY ) {
					final var componentMapping =
							(EmbeddableMappingType)
									mapping.getAttributeMapping( ~position )
											.getMappedType();
					result[~position] = ((FilteredAssociationState) current.value).physicalState(
							componentValues( result[~position], componentMapping ), componentMapping );
				}
			}
			return result;
		}
	}

	private static Object[] componentValues(Object value, EmbeddableMappingType mapping) {
		if ( value instanceof Object[] values ) {
			return values;
		}
		else {
			final Object[] values =
					value == null
							? new Object[mapping.getNumberOfAttributeMappings()]
							: mapping.getValues( value );
			if ( mapping.isPolymorphic() ) {
				final Object[] withDiscriminator =
						Arrays.copyOf( values, values.length + 1 );
				if ( value != null ) {
					withDiscriminator[values.length] =
							mapping.findSubtypeBySubclass( value.getClass().getName() )
									.getDiscriminatorValue();
				}
				return withDiscriminator;
			}
			return values;
		}
	}

	public void afterUpdate(Object[] state, ManagedMappingType mapping) {
		if ( !isEmpty() ) {
			for ( var current = this; current != null; current = current.sibling ) {
				if ( current.position >= 0 ) {
					final Object updated = state[current.position];
					if ( updated != null && updated != UNFETCHED_PROPERTY ) {
						current.value = null;
					}
				}
				else {
					final Object updated = state[~current.position];
					if ( updated != UNFETCHED_PROPERTY ) {
						final var componentMapping = (EmbeddableMappingType)
								mapping.getAttributeMapping( ~current.position )
										.getMappedType();
						final var component = (FilteredAssociationState) current.value;
						component.afterUpdate( componentValues( updated, componentMapping ), componentMapping );
						if ( component.isEmpty() ) {
							current.value = null;
						}
					}
				}
			}
			// Retain only the head if empty: EntityEntryExtraState has no removal operation.
			for ( var current = this; current != null; current = current.sibling ) {
				while ( current.sibling != null && current.sibling.value == null ) {
					current.sibling = current.sibling.sibling;
				}
			}
			if ( value == null && sibling != null ) {
				position = sibling.position;
				value = sibling.value;
				sibling = sibling.sibling;
			}
		}
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
		if ( next == null ) {
			return null;
		}
		else {
			return type.isInstance( next )
					? type.cast( next )
					: next.getExtraState( type );
		}
	}
}
