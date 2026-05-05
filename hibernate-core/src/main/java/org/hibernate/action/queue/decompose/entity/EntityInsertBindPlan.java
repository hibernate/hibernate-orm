/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.cyclebreak.CycleBreakPatcher;
import org.hibernate.action.queue.exec.BindPlan;
import org.hibernate.action.queue.exec.Checkers;
import org.hibernate.action.queue.exec.ExecutionContext;
import org.hibernate.action.queue.exec.GeneratedValuesCollector;
import org.hibernate.action.queue.exec.JdbcValueBindings;
import org.hibernate.action.queue.exec.OperationResultChecker;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;
import java.sql.SQLException;

import static org.hibernate.generator.EventType.INSERT;

/**
 * Bind plan for entity insert operations.
 * Uses on-demand decomposition to minimize allocation overhead.
 *
 * @author Steve Ebersole
 */
public class EntityInsertBindPlan implements BindPlan, OperationResultChecker {
	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final Object entity;
	private final Object identifier;
	private final Object[] state;
	private final boolean[] insertable;
	private final AbstractEntityInsertAction action;
	private final GeneratedValuesCollector generatedValuesCollector;
	private final DecompositionContext decompositionContext;

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
		this.action = action;
		this.generatedValuesCollector = generatedValuesCollector;
		this.decompositionContext = decompositionContext;
	}

	@Override
	public Object getEntityId() {
		return action.getId();
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
	public void execute(
			ExecutionContext context,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		context.executeRow(
				flushOperation,
				(jdbcValueBindings, s) -> bindValues( jdbcValueBindings, flushOperation, session ),
				this
		);
	}

	private void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		decomposeForInsert( valueBindings, identifier, session );

		if ( flushOperation.getBindingPatch() != null ) {
			CycleBreakPatcher.applyFixupPatch( valueBindings, flushOperation, flushOperation.getBindingPatch() );
		}
	}

	private void decomposeForInsert(JdbcValueBindings valueBindings, Object identifier, SharedSessionContractImplementor session) {
		// Decompose attribute values on-demand during binding
		tableDescriptor.attributes().forEach( attribute -> {
			if ( !attribute.isPluralAttributeMapping() && insertable[attribute.getStateArrayPosition()] ) {
				final Object attributeValue = resolveInsertAttributeValue( attribute, session );
				attribute.decompose(
						attributeValue,
						(valueIndex, jdbcValue, jdbcValueMapping) -> {
							if ( jdbcValueMapping.isInsertable()
									&& shouldBindInsertValue( attribute, valueIndex, session ) ) {
								final Object valueToBind = jdbcValue == null && decompositionContext != null
										? generatedIdentifierHandleOrNull( attributeValue )
										: jdbcValue;
								valueBindings.bindValue(
										valueToBind,
										jdbcValueMapping.getSelectionExpression(),
										ParameterUsage.SET
								);
							}
						},
						session
				);
			}
		} );

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
		final var generator = attribute.getGenerator();
		if ( !( generator instanceof OnExecutionGenerator onExecutionGenerator )
				|| !generator.generatesOnInsert()
				|| !generator.generatedOnExecution( entity, session ) ) {
			return true;
		}

		final var dialect = entityPersister.getFactory().getJdbcServices().getDialect();
		final boolean[] columnInclusions = onExecutionGenerator.getColumnInclusions( dialect, INSERT );
		if ( columnInclusions != null
				&& selectableIndex < columnInclusions.length
				&& !columnInclusions[selectableIndex] ) {
			return false;
		}

		final String[] columnValues = onExecutionGenerator.getReferencedColumnValues( dialect, INSERT );
		return columnValues == null
				|| selectableIndex >= columnValues.length
				|| "?".equals( columnValues[selectableIndex] );
	}

	private Object generatedIdentifierHandleOrNull(Object attributeValue) {
		final var handle = decompositionContext.getGeneratedIdentifierHandle( attributeValue );
		return handle == null ? null : handle;
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
					session.getCurrentTransactionIdentifier(),
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

		entityPersister.getIdentifierMapping().breakDownJdbcValues(
				identifier,
				(index, jdbcValue, jdbcValueMapping) -> {
					// Use the table's key column name, not the identifier column name
					final var keyColumn = keyColumns.get(index);
					final String columnName = keyColumn.selectionExpression();
					if ( valueBindings.hasBinding( columnName, ParameterUsage.SET ) ) {
						valueBindings.replaceValue( columnName, ParameterUsage.SET, jdbcValue );
					}
					else {
						valueBindings.bindValue(
								jdbcValue,
								columnName,
								ParameterUsage.SET
						);
					}
				},
				session
		);
	}

	@Override
	public OperationResultChecker getOperationResultChecker() {
		return this;
	}

	@Override
	public boolean checkResult(
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
