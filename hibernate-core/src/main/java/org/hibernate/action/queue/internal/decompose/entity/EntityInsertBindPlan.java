/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;


import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.queue.internal.cyclebreak.CycleBreakPatcher;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.Checkers;
import org.hibernate.action.queue.spi.bind.GeneratedValuesCollector;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.spi.meta.ColumnDescriptor;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ModelPart.JdbcValueBiConsumer;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;

import java.sql.SQLException;
import java.util.List;

import static org.hibernate.action.queue.internal.decompose.entity.BindPlanHelper.shouldBindJdbcValue;
import static org.hibernate.generator.EventType.INSERT;

/// Bind plan for entity insert operations.
/// Uses on-demand decomposition to minimize allocation overhead.
///
/// @author Steve Ebersole
public class EntityInsertBindPlan implements BindPlan, OperationResultChecker {

	/// Static, non-capturing [JdbcValueBiConsumer] for binding attribute values for insert assignment (VALUES).
	private static final JdbcValueBiConsumer<EntityInsertBindPlan, AttributeMapping> INSERT_SET_BINDER =
			(valueIndex, bindPlan, attribute, jdbcValue, jdbcValueMapping) -> {
				if ( jdbcValueMapping.isInsertable()
						&& bindPlan.shouldBindInsertValue( attribute, valueIndex, bindPlan.bindingSession ) ) {
					final Object valueToBind = jdbcValue == null && bindPlan.decompositionContext != null
							? bindPlan.generatedIdentifierHandleOrNull( bindPlan.bindingAttributeValue )
							: jdbcValue;
					bindPlan.valueBindings.bindValue(
							valueToBind,
							jdbcValueMapping.getSelectionExpression(),
							ParameterUsage.SET
					);
				}
			};


	/// Static, non-capturing [JdbcValueBiConsumer] for binding key values for insert assignment (VALUES).
	private static final JdbcValueBiConsumer<EntityInsertBindPlan, List<ColumnDescriptor>> KEY_SET_BINDER =
			(index, bindPlan, keyColumns, jdbcValue, jdbcValueMapping) -> {
				final String columnName = keyColumns.get( index ).selectionExpression();
				final Object valueToBind = jdbcValueMapping instanceof BasicEntityIdentifierMapping
						? jdbcValueMapping.getJdbcMapping().convertToRelationalValue( jdbcValue )
						: jdbcValue;
				if ( bindPlan.valueBindings.hasBinding( columnName, ParameterUsage.SET ) ) {
					bindPlan.valueBindings.replaceValue( columnName, ParameterUsage.SET, valueToBind );
				}
				else {
					bindPlan.valueBindings.bindValue(
							valueToBind,
							columnName,
							ParameterUsage.SET
					);
				}
			};

	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final Object entity;
	private final Object identifier;
	private final Object[] state;
	private final boolean[] insertable;
	private final GeneratedValuesCollector generatedValuesCollector;
	private final DecompositionContext decompositionContext;

	// temporary state used during decomposition to allow model-part decomposition to be non-capturing
	private JdbcValueBindings valueBindings;
	private SharedSessionContractImplementor bindingSession;
	private FlushOperation bindingFlushOperation;
	private Object bindingAttributeValue;

	public EntityInsertBindPlan(
			EntityTableDescriptor tableDescriptor,
			EntityPersister entityPersister,
			Object entity,
			Object identifier,
			Object[] state,
			boolean[] insertable,
			AbstractEntityInsertAction action,
			GeneratedValuesCollector generatedValuesCollector,
			DecompositionContext decompositionContext) {
		this.tableDescriptor = tableDescriptor;
		this.entityPersister = entityPersister;
		this.entity = entity;
		this.identifier = identifier;
		this.state = state;
		this.insertable = insertable;
		this.generatedValuesCollector = generatedValuesCollector;
		this.decompositionContext = decompositionContext;
	}

	@Override
	public Object getEntityId() {
		return identifier;
	}

	@Override
	public Object getEntityInstance() {
		return entity;
	}

	@Override
	public GeneratedValuesCollector getGeneratedValuesCollector() {
		return generatedValuesCollector;
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		decomposeForInsert( valueBindings, identifier, flushOperation, session );

		if ( flushOperation.getBindingPatch() != null ) {
			CycleBreakPatcher.applyFixupPatch( valueBindings, flushOperation, flushOperation.getBindingPatch() );
		}
	}

	private void decomposeForInsert(
			JdbcValueBindings valueBindings,
			Object identifier,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		this.bindingFlushOperation = flushOperation;
		try {
			// Decompose attribute values on-demand during binding
			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				final var attribute = tableDescriptor.attributes().get( i );
				if ( !attribute.isPluralAttributeMapping() && insertable[attribute.getStateArrayPosition()] ) {
					final Object attributeValue = resolveInsertAttributeValue( attribute, session );
					this.valueBindings = valueBindings;
					this.bindingSession = session;
					this.bindingAttributeValue = attributeValue;
					try {
						attribute.decompose(
								attributeValue,
								0,
								this,
								attribute,
								INSERT_SET_BINDER,
								session
						);
					}
					finally {
						this.valueBindings = null;
						this.bindingSession = null;
						this.bindingAttributeValue = null;
					}
				}
			}

			if ( tableDescriptor.isIdentifierTable() ) {
				// Bind discriminator, if needed
				entityPersister.bindDiscriminatorForInsert( valueBindings );
			}

			bindTemporalStartingValue( valueBindings, session );

			// Bind the key columns (identifier for root table, FK for joined subclass tables)
			// unless using identity generation (identifier == null)
			if ( identifier != null ) {
				breakDownKeyJdbcValue( valueBindings, session );
			}
			else {
				assert entityPersister.getInsertDelegate() != null;
			}
		}
		finally {
			this.bindingFlushOperation = null;
		}
	}

	private Object resolveInsertAttributeValue(
			AttributeMapping attribute,
			SharedSessionContractImplementor session) {
		final Object attributeValue = state[attribute.getStateArrayPosition()];
		if ( attribute instanceof ToOneAttributeMapping
				&& attributeValue != null
				&& attribute.getValue( entity ) == null ) {
			return null;
		}
		if ( isDeletedEntityReference( attributeValue, session ) ) {
			return null;
		}
		return attributeValue;
	}

	private boolean isDeletedEntityReference(
			Object attributeValue,
			SharedSessionContractImplementor session) {
		if ( attributeValue == null ) {
			return false;
		}
		if ( decompositionContext != null && decompositionContext.isBeingDeletedInCurrentFlush( attributeValue ) ) {
			return true;
		}
		final var entry = session.getPersistenceContextInternal().getEntry( attributeValue );
		return entry != null && entry.getStatus().isDeletedOrGone();
	}

	private boolean shouldBindInsertValue(
			AttributeMapping attribute,
			int selectableIndex,
			SharedSessionContractImplementor session) {
		return shouldBindJdbcValue(
				attribute,
				selectableIndex,
				bindingFlushOperation,
				entityPersister,
				INSERT,
				entity,
				session
		);
	}

	private Object generatedIdentifierHandleOrNull(Object attributeValue) {
		return decompositionContext.getGeneratedIdentifierHandle( attributeValue );
	}

	private void bindTemporalStartingValue(
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		final TemporalMapping temporalMapping = entityPersister.getTemporalMapping();
		if ( temporalMapping == null || !TemporalMutationHelper.isUsingParameters( session ) ) {
			return;
		}

		final String temporalTableName = entityPersister.physicalTableNameForMutation(
				temporalMapping.getStartingColumnMapping()
		);
		if ( tableDescriptor.name().equals( temporalTableName ) ) {
			valueBindings.bindValue(
					session.getCurrentChangesetIdentifier(),
					temporalMapping.getStartingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	private void breakDownKeyJdbcValue(
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		// For joined subclasses, use the table's key columns (which may be FK columns)
		// rather than the entity's identifier columns
		final var keyColumns = tableDescriptor.keyDescriptor().columns();

		this.valueBindings = valueBindings;
		try {
			entityPersister.getIdentifierMapping().breakDownJdbcValues(
					identifier,
					0,
					this,
					keyColumns,
					KEY_SET_BINDER,
					session
			);
		}
		finally {
			this.valueBindings = null;
		}
	}

	@Override
	public OperationResultChecker getOperationResultChecker() {
		return this;
	}

	@Override
	public boolean checkResult(
			FlushOperation flushOperation,
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		return checkResult(
				(EntityTableDescriptor) flushOperation.getMutatingTableDescriptor(),
				affectedRowCount,
				batchPosition,
				sqlString,
				sessionFactory
		);
	}

	@Override
	public boolean checkResult(
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		return checkResult( tableDescriptor, affectedRowCount, batchPosition, sqlString, sessionFactory );
	}

	private boolean checkResult(
			EntityTableDescriptor tableDescriptor,
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		return Checkers.identifiedResultsCheck(
				tableDescriptor.insertDetails().getExpectation(),
				affectedRowCount,
				batchPosition,
				entityPersister,
				tableDescriptor,
				identifier,
				sqlString,
				sessionFactory
		);
	}

}
