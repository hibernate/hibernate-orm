/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.BitSet;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;

/**
 * The mutation view of restricted associations. Clients ask about columns and rows,
 * without knowing whether hydration retained keys or a bitmap.
 */
public final class FilteredAssociationMutation {
	public static final FilteredAssociationMutation NONE = new FilteredAssociationMutation( null, null, null );
	private final FilteredAssociationState state;
	private final FilteredAssociationMapping mapping;
	private final Object[] values;

	private FilteredAssociationMutation(
			FilteredAssociationState state,
			FilteredAssociationMapping mapping,
			Object[] values) {
		this.state = state;
		this.mapping = mapping;
		this.values = values;
	}

	public static FilteredAssociationMutation forUpdate(EntityEntry entry, Object[] values) {
		final var state = entry == null ? null : entry.getExtraState( FilteredAssociationState.class );
		return state == null || state.isEmpty() ? NONE
				: new FilteredAssociationMutation( state, entry.getPersister().getFilteredAssociationMapping(), values );
	}

	public boolean isActive() {
		return state != null;
	}

	/** Combine update structure with this operation's column and row preservation requirements. */
	public FilteredUpdateCache.Key updateCacheKey(BitSet assignments, BitSet tables) {
		return new FilteredUpdateCache.Key(
				state == null ? new BitSet() : mapping.omittedSlots( state, values ), assignments, tables );
	}

	public boolean includesColumn(SelectableMapping column) {
		return state == null || !mapping.preservesColumn( state, values, column );
	}

	public boolean hasStoredRow(String table) {
		return state != null && mapping.hasHiddenReference( state, table );
	}

	/** Apply column omission and use an UPDATE when omitted keys cannot be supplied to an INSERT. */
	public void prepareUpdateBuilder(TableUpdateBuilder<?> builder, String table) {
		if ( state != null ) {
			builder.setColumnInclusion( this::includesColumn );
			builder.setRowKnownToExist( mapping.preservesRow( state, values, table ) );
		}
	}

	public Object[] physicalState(Object[] domainState) {
		return state == null ? domainState : state.physicalState( domainState, mapping );
	}
}
