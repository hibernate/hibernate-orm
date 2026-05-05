/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.constraint;

import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.internal.decompose.entity.EntityUpdateBindPlan;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityTableMappingImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hibernate.action.queue.internal.constraint.ConstraintModel.normalizeIdentifier;
import static org.hibernate.action.queue.internal.constraint.ConstraintModel.normalizeTableExpression;

/// Extracts unique constraint slot values from Flush operations.
///
/// The graph builder uses these runtime values to model release/occupy facts for
/// unique constraints. DELETE and UPDATE operations can release slots, while INSERT
/// and UPDATE operations can occupy slots.
///
/// @author Steve Ebersole
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

	/// Extract all unique slots affected by this operation.
	///
	/// INSERT and DELETE operations use the operation state. UPDATE operations return
	/// only the changed new values; callers that need released values should use
	/// [#extractOldSlots(FlushOperation)].
	public List<UniqueSlot> extractSlots(FlushOperation operation) {
		List<UniqueSlot> slots = new ArrayList<>();

		if ( operation.getKind() == MutationKind.UPDATE || operation.getKind() == MutationKind.UPDATE_ORDER ) {
			return extractUpdateSlots(operation);
		}

		if (operation.getKind() != MutationKind.INSERT && operation.getKind() != MutationKind.DELETE) {
			return slots;
		}

		String tableName = operation.getTableExpression();
		List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable(tableName);

		if (constraints.isEmpty()) {
			return slots;
		}

		var bindPlan = operation.getBindPlan();
		if (bindPlan == null) {
			return slots;
		}

		for (UniqueConstraint constraint : constraints) {
			Object[] collectionValues = bindPlan.getUniqueConstraintValues( constraint, session );
			if (collectionValues != null) {
				slots.add(new UniqueSlot(tableName, collectionValues, constraint));
			}
		}
		if (!slots.isEmpty()) {
			return slots;
		}

		Object entityInstance = bindPlan.getEntityInstance();
		Object[] loadedState = bindPlan.getLoadedState();

		if (entityInstance == null && loadedState == null) {
			return slots;
		}

		EntityPersister persister = findPersisterForTable( tableName );
		if (persister == null) {
			return slots;
		}

		for (UniqueConstraint constraint : constraints) {
			Object[] values = entityInstance != null
					? extractValues(persister, entityInstance, constraint)
					: extractValuesFromState(persister, bindPlan.getEntityId(), loadedState, constraint);
			if (values != null) {
				slots.add(new UniqueSlot(tableName, values, constraint));
			}
		}

		return slots;
	}

	/// Extract the unique slots occupied by an UPDATE operation.
	///
	/// Only changed unique constraint values are returned. Unchanged unique values do
	/// not participate in release/occupy ordering for the UPDATE.
	private List<UniqueSlot> extractUpdateSlots(FlushOperation operation) {
		List<UniqueSlot> slots = new ArrayList<>();

		String tableName = operation.getTableExpression();
		List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable(tableName);

		if (constraints.isEmpty()) {
			return slots;
		}

		var bindPlan = operation.getBindPlan();
		if ( bindPlan == null ) {
			return slots;
		}
		if ( operation.getKind() == MutationKind.UPDATE_ORDER
				|| !( bindPlan instanceof EntityUpdateBindPlan updateBindPlan ) ) {
			for (UniqueConstraint constraint : constraints) {
				if ( operation.getKind() != MutationKind.UPDATE_ORDER && constraint.isPrimaryKey() ) {
					continue;
				}
				Object[] collectionValues = bindPlan.getUniqueConstraintValues( constraint, session );
				if (collectionValues != null) {
					slots.add(new UniqueSlot(tableName, collectionValues, constraint));
				}
			}
			return slots;
		}

		EntityPersister persister = findPersisterForTable( tableName );
		if (persister == null) {
			return slots;
		}

		for (UniqueConstraint constraint : constraints) {
			if ( constraint.isPrimaryKey() ) {
				continue;
			}

			Object[] oldValues = extractValuesFromState(persister, bindPlan.getEntityId(), updateBindPlan.getPreviousState(), constraint);
			Object[] newValues = extractValuesFromState(persister, bindPlan.getEntityId(), updateBindPlan.getState(), constraint);

			if (!Objects.deepEquals(oldValues, newValues)) {
				if (newValues != null) {
					slots.add(new UniqueSlot(tableName, newValues, constraint));
				}
			}
		}

		return slots;
	}

	/// Extract the unique slots released by an UPDATE operation.
	public List<UniqueSlot> extractOldSlots(FlushOperation operation) {
		List<UniqueSlot> slots = new ArrayList<>();

		if ( operation.getKind() != MutationKind.UPDATE && operation.getKind() != MutationKind.UPDATE_ORDER ) {
			return slots;
		}

		final String tableName = operation.getTableExpression();
		final List<UniqueConstraint> constraints = constraintModel.getUniqueConstraintsForTable( tableName );
		if ( constraints.isEmpty() ) {
			return slots;
		}

		final var bindPlan = operation.getBindPlan();
		if ( bindPlan == null ) {
			return slots;
		}
		if ( operation.getKind() == MutationKind.UPDATE_ORDER
				|| !( bindPlan instanceof EntityUpdateBindPlan updateBindPlan ) ) {
			for ( UniqueConstraint constraint : constraints ) {
				if ( operation.getKind() != MutationKind.UPDATE_ORDER && constraint.isPrimaryKey() ) {
					continue;
				}
				final Object[] values = bindPlan.getPreviousUniqueConstraintValues( constraint, session );
				if ( values != null ) {
					slots.add( new UniqueSlot( tableName, values, constraint ) );
				}
			}
			return slots;
		}

		final EntityPersister persister = findPersisterForTable( tableName );
		if ( persister == null ) {
			return slots;
		}

		for ( UniqueConstraint constraint : constraints ) {
			if ( constraint.isPrimaryKey() ) {
				continue;
			}

			final Object[] values = extractValuesFromState(
					persister,
					bindPlan.getEntityId(),
					updateBindPlan.getPreviousState(),
					constraint
			);
			if ( values != null ) {
				slots.add( new UniqueSlot( tableName, values, constraint ) );
			}
		}

		return slots;
	}

	/// Extract the values for a specific unique constraint from entity instance.
	private Object[] extractValues(
			EntityPersister persister,
			Object entityInstance,
			UniqueConstraint constraint) {

		try {
			return extractColumnValues(
					persister,
					persister.getIdentifier( entityInstance, session ),
					entityInstance,
					null,
					constraint
			);
		}
		catch (Exception e) {
			// If we can't extract values, don't create a slot
			return null;
		}
	}

	private Object[] extractPrimaryKeyValues(
			EntityPersister persister,
			Object id,
			SelectableMappings constraintColumns) {
		if ( id == null ) {
			return null;
		}

		if ( constraintColumns instanceof EntityTableMappingImpl.KeyMapping keyMapping ) {
			Object[] values = new Object[keyMapping.getJdbcTypeCount()];
			keyMapping.breakDownKeyJdbcValues(
					id,
					(jdbcValue, keyColumn) -> {
						for ( int i = 0; i < keyMapping.getJdbcTypeCount(); i++ ) {
							if ( keyMapping.getSelectable( i ) == keyColumn ) {
								values[i] = jdbcValue;
								return;
							}
						}
					},
					session
			);
			return hasNullValue( values ) ? null : values;
		}

		Object[] values = new Object[constraintColumns.getJdbcTypeCount()];
		persister.getIdentifierMapping().breakDownJdbcValues(
				id,
				(index, jdbcValue, jdbcValueMapping) -> {
					if ( index < values.length ) {
						values[index] = jdbcValue;
					}
				},
				session
		);

		return hasNullValue( values ) ? null : values;
	}

	private boolean hasNullValue(Object[] values) {
		for ( Object value : values ) {
			if ( value == null ) {
				return true;
			}
		}
		return false;
	}

	private Object[] extractColumnValues(
			EntityPersister persister,
			Object entityId,
			Object entityInstance,
			Object[] state,
			UniqueConstraint constraint) {
		if ( constraint.isPrimaryKey() ) {
			return extractPrimaryKeyValues( persister, entityId, constraint.columns() );
		}

		final Map<ColumnKey, Object> valuesByColumn = new HashMap<>();
		collectAttributeColumnValues( persister, entityInstance, state, valuesByColumn );
		return valuesInConstraintColumnOrder( constraint, valuesByColumn );
	}

	/// Extract values from entity state array.
	private Object[] extractValuesFromState(
			EntityPersister persister,
			Object entityId,
			Object[] state,
			UniqueConstraint constraint) {

		if (state == null) {
			return null;
		}

		try {
			return extractColumnValues( persister, entityId, null, state, constraint );
		}
		catch (Exception e) {
			return null;
		}
	}

	private void collectAttributeColumnValues(
			EntityPersister persister,
			Object entityInstance,
			Object[] state,
			Map<ColumnKey, Object> valuesByColumn) {
		persister.forEachAttributeMapping( attributeMapping -> {
			if ( attributeMapping.isPluralAttributeMapping() ) {
				return;
			}
			if ( isInverseOneToOne( attributeMapping ) ) {
				return;
			}

			final Object attributeValue = resolveAttributeValue( attributeMapping, entityInstance, state );
			if ( attributeValue == UnresolvedAttributeValue.INSTANCE ) {
				return;
			}

			collectModelPartColumnValues( attributeMapping, attributeValue, valuesByColumn );
		} );
	}

	private boolean isInverseOneToOne(AttributeMapping attributeMapping) {
		return attributeMapping instanceof ToOneAttributeMapping toOneAttributeMapping
				&& toOneAttributeMapping.getCardinality() == ToOneAttributeMapping.Cardinality.ONE_TO_ONE
				&& toOneAttributeMapping.getSideNature() == ForeignKeyDescriptor.Nature.TARGET;
	}

	private Object resolveAttributeValue(
			AttributeMapping attributeMapping,
			Object entityInstance,
			Object[] state) {
		if ( state != null ) {
			final int statePosition = attributeMapping.getStateArrayPosition();
			if ( statePosition < 0 || statePosition >= state.length ) {
				return UnresolvedAttributeValue.INSTANCE;
			}
			return state[statePosition];
		}
		if ( entityInstance != null ) {
			return attributeMapping.getValue( entityInstance );
		}
		return UnresolvedAttributeValue.INSTANCE;
	}

	private void collectModelPartColumnValues(
			AttributeMapping attributeMapping,
			Object attributeValue,
			Map<ColumnKey, Object> valuesByColumn) {
		try {
			attributeMapping.decompose(
					attributeValue,
					(valueIndex, value, selectable) -> valuesByColumn.put( ColumnKey.from( selectable ), value ),
					session
			);
		}
		catch (RuntimeException | AssertionError ignored) {
			// Incomplete decomposition for one attribute simply prevents matching constraints from being slotted.
		}
	}

	private Object[] valuesInConstraintColumnOrder(
			UniqueConstraint constraint,
			Map<ColumnKey, Object> valuesByColumn) {
		final Object[] values = new Object[constraint.columns().getJdbcTypeCount()];
		for ( int i = 0; i < constraint.columns().getJdbcTypeCount(); i++ ) {
			final ColumnKey columnKey = ColumnKey.from( constraint.columns().getSelectable( i ) );
			if ( !valuesByColumn.containsKey( columnKey ) ) {
				return null;
			}
			values[i] = valuesByColumn.get( columnKey );
		}
		return values;
	}

	/// Build a map of table names to entity persisters for quick lookup.
	public static Map<String, EntityPersister> buildPersisterMap(SharedSessionContractImplementor session) {
		Map<String, EntityPersister> map = new java.util.HashMap<>();

		session.getFactory().getMappingMetamodel().forEachEntityDescriptor(persister -> {
			for ( var tableMapping : persister.getTableMappings() ) {
				map.put( tableMapping.getTableName(), persister );
				map.put( normalizeTableExpression( tableMapping.getTableName() ), persister );
			}
		});

		return map;
	}

	private EntityPersister findPersisterForTable(String tableName) {
		EntityPersister persister = persistersByTable.get( tableName );
		if ( persister != null ) {
			return persister;
		}
		return persistersByTable.get( normalizeTableExpression( tableName ) );
	}

	private enum UnresolvedAttributeValue {
		INSTANCE
	}

	private record ColumnKey(String tableName, String columnName) {
		static ColumnKey from(SelectableMapping selectableMapping) {
			return new ColumnKey(
					normalizeTableExpression( selectableMapping.getContainingTableExpression() ),
					normalizeIdentifier( selectableMapping.getSelectableName() )
			);
		}
	}
}
