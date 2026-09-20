/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.model.builder.AbstractTableUpdateBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;

/**
 * The mutation view of restricted associations. Clients ask about columns and rows,
 * without knowing whether hydration retained keys or a bitmap.
 */
public final class FilteredAssociationMutation {
	public static final FilteredAssociationMutation NONE = new FilteredAssociationMutation( null, null, null, null );
	private final FilteredAssociationState state;
	private final FilteredAssociationMapping mapping;
	private final EntityPersister persister;
	private final Object[] values;

	private FilteredAssociationMutation(
			FilteredAssociationState state,
			FilteredAssociationMapping mapping,
			EntityPersister persister,
			Object[] values) {
		this.state = state;
		this.mapping = mapping;
		this.persister = persister;
		this.values = values;
	}

	public static FilteredAssociationMutation forUpdate(
			Object entity, EntityPersister persister, Object[] values, SharedSessionContractImplementor session) {
		final var entry = session.getPersistenceContextInternal().getEntry( entity );
		final var state = entry == null ? null : entry.getExtraState( FilteredAssociationState.class );
		return state == null || state.isEmpty() ? NONE
				: new FilteredAssociationMutation( state, persister.getFilteredAssociationMapping(), persister, values );
	}

	public boolean isActive() {
		return state != null;
	}

	public boolean includesColumn(SelectableMapping column) {
		return state == null || !mapping.preservesColumn( state, values, column );
	}

	public boolean hasStoredRow(String table) {
		return state != null && mapping.hasHiddenReference( state, table );
	}

	/** Apply column omission and use an UPDATE when omitted keys cannot be supplied to an INSERT. */
	public void prepareUpdateBuilder(TableUpdateBuilder<?> builder, String table) {
		if ( state != null && !state.retainsKeys() ) {
			if ( builder instanceof AbstractTableUpdateBuilder<?> updateBuilder ) {
				updateBuilder.setColumnInclusion( this::includesColumn );
			}
			if ( builder instanceof TableUpdateBuilderStandard<?> standard ) {
				standard.setRowKnownToExist( mapping.preservesRow( state, values, table ) );
			}
		}
	}

	public Object[] physicalState(Object[] domainState) {
		return state == null ? domainState : state.physicalState( domainState, persister );
	}
}
