/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;

/** Shared slot and column metadata. No mapping references are stored in an entity's bitmap. */
public final class FilteredAssociationMapping {
	public static final FilteredAssociationMapping NONE =
			new FilteredAssociationMapping( new Slot[0], false );
	private final Slot[] slots;
	private final boolean retainKeys;
	private final Map<String, Slot> byRole = new HashMap<>();
	private final Map<String, Map<String, Slot>> byColumn = new HashMap<>();

	private FilteredAssociationMapping(Slot[] slots, boolean retainKeys) {
		this.slots = slots;
		this.retainKeys = retainKeys;
		for ( Slot slot : slots ) {
			byRole.put( slot.association.getNavigableRole().getFullPath(), slot );
			slot.association.forEachUpdatable( (index, column) -> {
				if ( !column.isFormula() ) {
					byColumn.computeIfAbsent( column.getContainingTableExpression(), key -> new HashMap<>() )
							.put( column.getSelectionExpression(), slot );
				}
			} );
		}
	}

	public static FilteredAssociationMapping create(EntityPersister persister) {
		final List<Slot> slots = new ArrayList<>();
		collectSlots( persister, new AttributeMapping[0], slots );
		if ( slots.isEmpty() ) {
			return NONE;
		}
		else {
			boolean retainKeys = persister.optimisticLockStyle().isAllOrDirty();
			for ( var table : persister.getTableMappings() ) {
				retainKeys |= table.getUpdateDetails().getCustomSql() != null;
			}
			return new FilteredAssociationMapping( slots.toArray( Slot[]::new ), retainKeys );
		}
	}

	public static boolean isRestricted(ToOneAttributeMapping association) {
		return association.hasWhereRestrictions() || association.hasFilterForLoadByKey();
	}

	public static boolean hasRestrictedAssociations(ManagedMappingType mapping) {
		for ( int i = 0; i < mapping.getNumberOfAttributeMappings(); i++ ) {
			final var attribute = mapping.getAttributeMapping( i );
			if ( attribute instanceof ToOneAttributeMapping toOne && isRestricted( toOne )
					|| attribute instanceof EmbeddableValuedModelPart embedded
							&& hasRestrictedAssociations( embedded.getEmbeddableTypeDescriptor() ) ) {
				return true;
			}
		}
		return false;
	}

	private static void collectSlots(ManagedMappingType mapping, AttributeMapping[] prefix, List<Slot> slots) {
		for ( int i = 0; i < mapping.getNumberOfAttributeMappings(); i++ ) {
			final var attribute = mapping.getAttributeMapping( i );
			if ( attribute instanceof ToOneAttributeMapping association ) {
				if ( isRestricted( association ) ) {
					slots.add( new Slot( slots.size(), append( prefix, attribute ), association ) );
				}
			}
			else if ( attribute instanceof EmbeddableValuedModelPart embedded ) {
				collectSlots( embedded.getEmbeddableTypeDescriptor(), append( prefix, attribute ), slots );
			}
		}
	}

	private static AttributeMapping[] append(AttributeMapping[] prefix, AttributeMapping attribute) {
		final var result = Arrays.copyOf( prefix, prefix.length + 1 );
		result[prefix.length] = attribute;
		return result;
	}

	public FilteredAssociationState record(FilteredAssociationState state, ToOneAttributeMapping association, Object key) {
		final var slot = byRole.get( association.getNavigableRole().getFullPath() );
		if ( slot == null ) {
			throw new IllegalStateException( "No filtered association slot for " + association.getNavigableRole() );
		}
		if ( state == null ) {
			state = FilteredAssociationState.create( slots.length, retainKeys );
		}
		state.set( slot.index, key );
		return state;
	}

	public boolean isFiltered(FilteredAssociationState state, ToOneAttributeMapping association) {
		final var slot = byRole.get( association.getNavigableRole().getFullPath() );
		return state != null && slot != null && state.contains( slot.index );
	}

	/** Whether this column must retain its database value in the current update. */
	boolean preservesColumn(FilteredAssociationState state, Object[] values, SelectableMapping column) {
		if ( state == null ) {
			return false;
		}
		else {
			final var columns = byColumn.get( column.getContainingTableExpression() );
			final var slot = columns == null
					? null
					: columns.get( column.getSelectionExpression() );
			return slot != null
				&& state.omitsColumn( slot.index )
				&& slot.value( values ) == null;
		}
	}

	/** A hidden nonnull FK also proves that its containing table row existed when loaded. */
	boolean hasHiddenReference(FilteredAssociationState state, String table) {
		if ( state != null ) {
			for ( var slot : slots ) {
				if ( state.contains( slot.index ) && slot.table().equals( table ) ) {
					return true;
				}
			}
		}
		return false;
	}

	boolean preservesRow(FilteredAssociationState state, Object[] values, String table) {
		if ( state != null ) {
			for ( var slot : slots ) {
				if ( state.omitsColumn( slot.index )
						&& slot.table().equals( table )
						&& slot.value( values ) == null ) {
					return true;
				}
			}
		}
		return false;
	}

	BitSet omittedSlots(FilteredAssociationState state, Object[] values) {
		final var omitted = new BitSet( slots.length );
		for ( var slot : slots ) {
			if ( state.omitsColumn( slot.index ) && slot.value( values ) == null ) {
				omitted.set( slot.index );
			}
		}
		return omitted;
	}

	boolean hasHiddenAttribute(FilteredAssociationState state, AttributeMapping attribute) {
		if ( state != null ) {
			for ( var slot : slots ) {
				if ( state.contains( slot.index ) ) {
					for ( var part : slot.path ) {
						if ( part == attribute ) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	void afterUpdate(FilteredAssociationState state, Object[] values) {
		for ( var slot : slots ) {
			if ( state.contains( slot.index ) ) {
				final Object value = slot.value( values );
				if ( value != null && value != UNFETCHED_PROPERTY ) {
					state.clear( slot.index );
				}
			}
		}
	}

	Object[] physicalState(FilteredAssociationState state, Object[] values, IntFunction<Object> keys) {
		final Object[] result = values.clone();
		for ( var slot : slots ) {
			if ( state.contains( slot.index ) && slot.value( values ) == null ) {
				Object[] parent = result;
				for ( int depth = 0; depth < slot.path.length - 1; depth++ ) {
					final var attribute = slot.path[depth];
					final int position = attribute.getStateArrayPosition();
					parent[position] = parent[position] instanceof Object[] components
							? components.clone()
							: componentValues( parent[position], (EmbeddableMappingType) attribute.getMappedType() );
					parent = (Object[]) parent[position];
				}
				parent[slot.association.getStateArrayPosition()] = new FilteredAssociationState.Key( keys.apply( slot.index ) );
			}
		}
		return result;
	}

	private static Object[] componentValues(Object value, EmbeddableMappingType mapping) {
		final Object[] values = value == null ? new Object[mapping.getNumberOfAttributeMappings()] : mapping.getValues( value );
		if ( mapping.isPolymorphic() ) {
			final Object[] result = Arrays.copyOf( values, values.length + 1 );
			if ( value != null ) {
				result[values.length] = mapping.findSubtypeBySubclass( value.getClass().getName() ).getDiscriminatorValue();
			}
			return result;
		}
		return values;
	}

	private record Slot(int index, AttributeMapping[] path, ToOneAttributeMapping association) {
		String table() {
			return association.getForeignKeyDescriptor().getKeyTable();
		}

		Object value(Object[] values) {
			Object result = values[path[0].getStateArrayPosition()];
			for ( int i = 1; i < path.length && result != null && result != UNFETCHED_PROPERTY; i++ ) {
				final var mapping = (EmbeddableMappingType) path[i - 1].getMappedType();
				if ( result instanceof Object[] components ) {
					result = components[path[i].getStateArrayPosition()];
				}
				else if ( mapping.isPolymorphic()
						&& !mapping.findSubtypeBySubclass( result.getClass().getName() )
								.declaresAttribute( path[i] ) ) {
					return UNFETCHED_PROPERTY;
				}
				else {
					result = path[i].getValue( result );
				}
			}
			return result;
		}
	}
}
