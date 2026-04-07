/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.constraint;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.EntityUpdateBindPlan;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.persister.entity.EntityPersister;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Extracts unique constraint slot values from PlannedOperations.
 * Used by graph builder to create precise DELETE → INSERT edges based on actual values.
 *
 * @author Steve Ebersole
 */
public class UniqueSlotExtractor {
	private final ConstraintModel constraintModel;
	private final SharedSessionContractImplementor session;
	private final Map<String, EntityPersister> persistersByTable;

	public UniqueSlotExtractor(
			ConstraintModel constraintModel,
			SharedSessionContractImplementor session,
			Map<String, EntityPersister> persistersByTable) {
		this.constraintModel = constraintModel;
		this.session = session;
		this.persistersByTable = persistersByTable;
	}

	/**
	 * Extract all unique slots affected by this operation.
	 * For INSERT/DELETE operations, extracts values from entity state.
	 * For UPDATE operations (Phase 3), extracts both old and new values.
	 */
	public List<UniqueSlot> extractSlots(PlannedOperation operation) {
		List<UniqueSlot> slots = new ArrayList<>();

		// Phase 3: Handle UPDATE operations differently
		if (operation.getKind() == MutationKind.UPDATE) {
			return extractUpdateSlots(operation);
		}

		// Only extract for INSERT and DELETE
		if (operation.getKind() != MutationKind.INSERT && operation.getKind() != MutationKind.DELETE) {
			return slots;
		}

		String tableName = operation.getTableExpression();
		List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable(tableName);

		if (constraints.isEmpty()) {
			return slots;
		}

		// Get entity instance and state from BindPlan
		var bindPlan = operation.getBindPlan();
		if (bindPlan == null) {
			return slots;
		}

		Object entityInstance = bindPlan.getEntityInstance();
		Object[] loadedState = bindPlan.getLoadedState();

		if (entityInstance == null && loadedState == null) {
			return slots;
		}

		// Get entity persister for this table
		EntityPersister persister = persistersByTable.get(tableName);
		if (persister == null) {
			return slots;
		}

		// Extract values for each unique constraint
		for (UniqueConstraint constraint : constraints) {
			Object[] values = entityInstance != null
					? extractValues(persister, entityInstance, constraint)
					: extractValuesFromState(persister, loadedState, constraint);
			if (values != null) {
				slots.add(new UniqueSlot(tableName, values, constraint));
			}
		}

		return slots;
	}

	/**
	 * Extract unique slots for UPDATE operations.
	 * Phase 3: UPDATEs that change unique constraint values are treated as:
	 * - Releasing the old slot (like DELETE)
	 * - Occupying the new slot (like INSERT)
	 *
	 * Returns slots with the NEW values only. Slot comparison will detect conflicts.
	 */
	private List<UniqueSlot> extractUpdateSlots(PlannedOperation operation) {
		List<UniqueSlot> slots = new ArrayList<>();

		String tableName = operation.getTableExpression();
		List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable(tableName);

		if (constraints.isEmpty()) {
			return slots;
		}

		var bindPlan = operation.getBindPlan();
		if (!(bindPlan instanceof EntityUpdateBindPlan updateBindPlan)) {
			return slots;  // Not an UpdateBindPlan, can't extract state
		}

		EntityPersister persister = persistersByTable.get(tableName);
		if (persister == null) {
			return slots;
		}

		// Extract unique constraint values from both old and new state
		for (UniqueConstraint constraint : constraints) {
			Object[] oldValues = extractValuesFromState(persister, updateBindPlan.getPreviousState(), constraint);
			Object[] newValues = extractValuesFromState(persister, updateBindPlan.getState(), constraint);

			// Only create slots if the value changed
			// If unchanged, UPDATE doesn't affect this unique constraint
			if (!Objects.deepEquals(oldValues, newValues)) {
				// Return the NEW value - this is what the UPDATE will set
				if (newValues != null) {
					slots.add(new UniqueSlot(tableName, newValues, constraint));
				}
			}
		}

		return slots;
	}

	/**
	 * Extract the values for a specific unique constraint from entity instance.
	 */
	private Object[] extractValues(
			EntityPersister persister,
			Object entityInstance,
			UniqueConstraint constraint) {

		try {
			// Handle primary key
			if (constraint.isPrimaryKey()) {
				return extractPrimaryKeyValues(persister, entityInstance);
			}

			// Phase 4: Handle other unique constraints using property names
			if (constraint.propertyNames() != null && constraint.propertyNames().length > 0) {
				return extractPropertyValues(persister, entityInstance, constraint.propertyNames());
			}

			// Fallback: constraints without property names metadata (shouldn't happen in Phase 4)
			return null;
		}
		catch (Exception e) {
			// If we can't extract values, don't create a slot
			return null;
		}
	}

	/**
	 * Extract primary key values from entity instance.
	 */
	private Object[] extractPrimaryKeyValues(EntityPersister persister, Object entityInstance) {
		Object id = persister.getIdentifier(entityInstance, session);
		if (id == null) {
			return null;
		}

		// Handle composite keys
		EntityIdentifierMapping idMapping = persister.getIdentifierMapping();
		SelectableMappings selectables = idMapping;

		int columnCount = selectables.getJdbcTypeCount();
		Object[] values = new Object[columnCount];

		if (columnCount == 1) {
			// Simple ID
			values[0] = id;
		}
		else {
			// Composite ID - need to decompose
			// For now, use the ID object itself as single value
			// Full implementation would decompose composite IDs
			return new Object[] { id };
		}

		return values;
	}

	/**
	 * Extract property values from entity instance.
	 * Phase 4: Generic extraction using property names from UniqueConstraint metadata.
	 */
	private Object[] extractPropertyValues(
			EntityPersister persister,
			Object entityInstance,
			String[] propertyNames) {

		Object[] values = new Object[propertyNames.length];

		for (int i = 0; i < propertyNames.length; i++) {
			String propertyName = propertyNames[i];
			Object value = persister.getPropertyValue(entityInstance, propertyName);

			// If the value is an entity (association), extract its ID
			if (value != null) {
				int propertyIndex = persister.getEntityMetamodel().getPropertyIndex(propertyName);
				if (propertyIndex >= 0 && persister.getEntityMetamodel().getPropertyTypes()[propertyIndex].isEntityType()) {
					var entityType = (org.hibernate.type.EntityType) persister.getEntityMetamodel().getPropertyTypes()[propertyIndex];
					var associatedPersister = session.getFactory().getMappingMetamodel().getEntityDescriptor(entityType.getAssociatedEntityName());
					if (associatedPersister != null) {
						value = associatedPersister.getIdentifier(value, session);
					}
				}
			}

			values[i] = value;
		}

		return values;
	}

	/**
	 * Extract values from entity state array.
	 * Phase 3: Used for UPDATE operations to extract old/new values.
	 */
	private Object[] extractValuesFromState(
			EntityPersister persister,
			Object[] state,
			UniqueConstraint constraint) {

		if (state == null) {
			return null;
		}

		try {
			// Handle primary key
			if (constraint.isPrimaryKey()) {
				return extractPrimaryKeyValuesFromState(persister, state);
			}

			// Phase 4: Handle other unique constraints using property names
			if (constraint.propertyNames() != null && constraint.propertyNames().length > 0) {
				return extractPropertyValuesFromState(persister, state, constraint.propertyNames());
			}

			return null;
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Extract primary key values from entity state array.
	 */
	private Object[] extractPrimaryKeyValuesFromState(EntityPersister persister, Object[] state) {
		// The ID is not in the state array - it's separate
		// For UPDATEs, the ID doesn't change, so we can't extract it from state
		// We need to get it differently - from the identifier property
		EntityIdentifierMapping idMapping = persister.getIdentifierMapping();

		// For simple IDs, get from property index
		if (idMapping.getJdbcTypeCount() == 1) {
			int idPropertyIndex = persister.getEntityMetamodel().getPropertyIndex(
				persister.getIdentifierPropertyName()
			);
			if (idPropertyIndex >= 0 && idPropertyIndex < state.length) {
				return new Object[] { state[idPropertyIndex] };
			}
		}

		// For embedded IDs or when ID is not in state, we can't extract from state array
		// This is OK - primary keys don't change in UPDATEs anyway
		return null;
	}

	/**
	 * Extract property values from entity state array.
	 */
	private Object[] extractPropertyValuesFromState(
			EntityPersister persister,
			Object[] state,
			String[] propertyNames) {

		Object[] values = new Object[propertyNames.length];

		for (int i = 0; i < propertyNames.length; i++) {
			String propertyName = propertyNames[i];
			int propertyIndex = persister.getEntityMetamodel().getPropertyIndex(propertyName);

			if (propertyIndex >= 0 && propertyIndex < state.length) {
				Object value = state[propertyIndex];

				// If the value is an entity (association), extract its ID
				if (value != null && persister.getEntityMetamodel().getPropertyTypes()[propertyIndex].isEntityType()) {
					var entityType = (org.hibernate.type.EntityType) persister.getEntityMetamodel().getPropertyTypes()[propertyIndex];
					var associatedPersister = session.getFactory().getMappingMetamodel().getEntityDescriptor(entityType.getAssociatedEntityName());
					if (associatedPersister != null) {
						value = associatedPersister.getIdentifier(value, session);
					}
				}

				values[i] = value;
			}
			else {
				return null;  // Property not found in state
			}
		}

		return values;
	}

	/**
	 * Build a map of table names to entity persisters for quick lookup.
	 */
	public static Map<String, EntityPersister> buildPersisterMap(SharedSessionContractImplementor session) {
		Map<String, EntityPersister> map = new java.util.HashMap<>();

		session.getFactory().getMappingMetamodel().forEachEntityDescriptor(persister -> {
			// Map primary table
			map.put(persister.getTableName(), persister);

			// Could also map secondary tables, but for Phase 2 we focus on primary tables
		});

		return map;
	}
}
