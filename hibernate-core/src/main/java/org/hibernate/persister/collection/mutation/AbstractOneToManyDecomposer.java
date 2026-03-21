/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.ast.builder.CollectionRowDeleteByUpdateSetNullBuilder;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableUpdateBuilderStandard;
import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.List;
import java.util.function.Consumer;

import static org.hibernate.sql.model.ast.builder.TableMutationBuilder.NULL;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractOneToManyDecomposer extends AbstractCollectionDecomposer implements OneToManyDecomposer {
	protected final OneToManyPersister persister;
	protected final SessionFactoryImplementor factory;

	public AbstractOneToManyDecomposer(
			OneToManyPersister persister,
			SessionFactoryImplementor factory) {
		this.persister = persister;
		this.factory = factory;
	}


	///  Apply removals/deletes based on a [org.hibernate.action.internal.CollectionUpdateAction].
	protected abstract void applyUpdateRemovals(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer);


	protected List<PlannedOperation> decomposeRemove(
			CollectionJdbcOperations jdbcOperations,
			CollectionRemoveAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		if ( !persister.needsRemove() ) {
			return List.of();
		}

		// Register callback to handle post-execution work (afterAction, cache, events, stats)
		final Object cacheKey = lockCacheItem( action, session );
		postExecCallbackRegistry.accept( new PostCollectionRemoveHandling( action, cacheKey ) );

		var removeOperation = jdbcOperations.getRemoveOperation();
		return List.of( new PlannedOperation(
				removeOperation.getTableDescriptor(),
				// technically an UPDATE
				MutationKind.UPDATE,
				removeOperation,
				new RemoveBindPlan( action.getKey(), persister ),
				ordinalBase * 1_000,
				"RemoveAllRows(" + persister.getRolePath() + ")"
		) );
	}


	protected static class RemoveBindPlan implements BindPlan {
		private final Object key;
		private final OneToManyPersister mutationTarget;

		public RemoveBindPlan(Object key, OneToManyPersister mutationTarget) {
			this.key = key;
			this.mutationTarget = mutationTarget;
		}

		@Override
		public void execute(
				org.hibernate.action.queue.exec.ExecutionContext context,
				PlannedOperation plannedOperation,
				SharedSessionContractImplementor session) {
			context.executeRow(
					plannedOperation,
					valueBindings -> {
						var fkDescriptor = mutationTarget.getAttributeMapping().getKeyDescriptor();
						fkDescriptor.getKeyPart().decompose(
								key,
								(valueIndex, value, jdbcValueMapping) -> {
									valueBindings.bindValue(
											value,
											jdbcValueMapping.getSelectableName(),
											ParameterUsage.RESTRICT
									);
								},
								session
						);
					},
					null
			);
		}
	}


	protected CollectionJdbcOperations buildJdbcOperations(TableDescriptor tableDescriptor, SessionFactoryImplementor factory) {
		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = buildInsertRowPlan( tableDescriptor );
		final CollectionJdbcOperations.UpdateRowPlan updateRowPlan = buildUpdateRowPlan( tableDescriptor );
		final CollectionJdbcOperations.DeleteRowPlan deleteRowPlan = buildDeleteRowPlan( tableDescriptor );

		return new CollectionJdbcOperations(
				persister,
				insertRowPlan,
				updateRowPlan,
				deleteRowPlan,
				buildRemoveOperation( tableDescriptor )
		);
	}

	/// Generates operation to perform "insert" SQL in the form -
	///
	/// ```sql
	/// update orders set customer_fk = ?, order_number = ? where id = ?
	/// ```
	///
	/// Which, in the case of a unidirectional one-to-many, actually "adds the collection row"
	private CollectionJdbcOperations.InsertRowPlan buildInsertRowPlan(TableDescriptor tableDescriptor) {
		if ( persister.isInverse() || !persister.isRowInsertEnabled() ) {
			return null;
		}

		final GraphTableUpdateBuilderStandard updateBuilder = new GraphTableUpdateBuilderStandard(
				persister,
				tableDescriptor,
				persister.getSqlWhereString(),
				factory
		);

		final var attributeMapping = persister.getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().forEachUpdatable( (index, selectableMapping) -> {
			updateBuilder.addValueColumn( selectableMapping );
		} );

		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.forEachUpdatable( (index, selectableMapping) -> {
				updateBuilder.addValueColumn( selectableMapping );
			} );
		}

		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var elementType = elementDescriptor.getAssociatedEntityMappingType();
		elementType.getIdentifierMapping().forEachColumn( (index, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );


		return new CollectionJdbcOperations.InsertRowPlan(
				updateBuilder.buildMutation().createMutationOperation(),
				this::applyInsertRowValues
		);
	}

	private void applyInsertRowValues(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = persister.getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				keyValue,
				jdbcValueBindings::bindAssignment,
				session
		);
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.decompose(
					persister.incrementIndexByBase( collection.getIndex( rowValue, rowPosition, persister ) ),
					jdbcValueBindings::bindAssignment,
					session
			);
		}

		final Object elementValue = collection.getElement( rowValue );
		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var identifierMapping = elementDescriptor.getAssociatedEntityMappingType().getIdentifierMapping();
		identifierMapping.decompose(
				identifierMapping.getIdentifier( elementValue ),
				jdbcValueBindings::bindRestriction,
				session
		);
	}

	private CollectionJdbcOperations.UpdateRowPlan buildUpdateRowPlan(TableDescriptor collectionTableDescriptor) {
		if ( !persister.isDoWriteEvenWhenInverse() ) {
			return null;
		}

		final var updateBuilder = new GraphTableUpdateBuilderStandard(
				persister,
				collectionTableDescriptor,
				persister.getSqlWhereString(),
				factory
		);

		final var attributeMapping = persister.getAttributeMapping();

		attributeMapping.getIndexDescriptor().forEachUpdatable( (index, selectableMapping) -> {
			updateBuilder.addValueColumn( selectableMapping );
		} );

		// Add element identifier restrictions (WHERE clause for element's PK)
		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var elementType = elementDescriptor.getAssociatedEntityMappingType();
		elementType.getIdentifierMapping().forEachColumn( (index, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );

		return new CollectionJdbcOperations.UpdateRowPlan(
				updateBuilder.buildMutation().createMutationOperation(),
				this::applyWriteIndexValues,
				this::applyWriteIndexRestrictions
		);
	}

	private void applyWriteIndexValues(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		persister.getAttributeMapping().getIndexDescriptor().decompose(
				collection.getIndex( entry, entryPosition, persister ),
				(valueIndex, jdbcValue, jdbcValueMapping) -> {
					if ( jdbcValueMapping.isUpdateable() ) {
						jdbcValueBindings.bindValue(
								jdbcValue,
								jdbcValueMapping.getSelectableName(),
								ParameterUsage.SET
						);
					}
				},
				session
		);
	}

	private void applyWriteIndexRestrictions(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = persister.getAttributeMapping();
		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var associatedType = elementDescriptor.getAssociatedEntityMappingType();
		final Object element = collection.getElement( entry );
		final var identifierMapping = associatedType.getIdentifierMapping();
		identifierMapping.decompose(
				identifierMapping.getIdentifier( element ),
				jdbcValueBindings::bindRestriction,
				session
		);
	}

	/// Generates operation to perform "delete" SQL in the form -
	///
	/// ```sql
	/// update orders set customer_fk = null, order_number = null where id = ?
	/// ```
	///
	/// Which, in the case of a unidirectional one-to-many, actually "removes the collection row"
	private CollectionJdbcOperations.DeleteRowPlan buildDeleteRowPlan(TableDescriptor collectionTableDescriptor) {
		if ( !persister.needsRemove() ) {
			return null;
		}

		final var updateBuilder = new CollectionRowDeleteByUpdateSetNullBuilder(
				persister,
				collectionTableDescriptor,
				persister.getSqlWhereString(),
				factory
		);

		final var foreignKeyDescriptor = persister.getAttributeMapping().getKeyDescriptor();
		foreignKeyDescriptor.getKeyPart().forEachUpdatable( (index, selectableMapping) -> {
			// set null
			updateBuilder.addValueColumn( NULL, selectableMapping );
		} );

		// set the value for each index column to null
		if ( persister.hasPhysicalIndexColumn() ) {
			final var indexDescriptor = persister.getAttributeMapping().getIndexDescriptor();
			assert indexDescriptor != null;
			indexDescriptor.forEachUpdatable( (index, selectableMapping) -> {
				updateBuilder.addValueColumn( NULL, selectableMapping );
			} );
		}

		// for one-to-many, we know the element is an entity and need to restrict the update
		// based on the element's id
		final var entityPart = (EntityCollectionPart) persister.getAttributeMapping().getElementDescriptor();
		entityPart.getAssociatedEntityMappingType().getIdentifierMapping().forEachColumn(  (index, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );

		return new CollectionJdbcOperations.DeleteRowPlan(
				updateBuilder.buildMutation().createMutationOperation(),
				this::applyDeleteRowRestrictions
		);
	}

	private void applyDeleteRowRestrictions(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var entityPart = (EntityCollectionPart) persister.getAttributeMapping().getElementDescriptor();
		entityPart.getAssociatedEntityMappingType().getIdentifierMapping().decompose(
				keyValue,
				(index, val, jdbcMapping) -> jdbcValueBindings.bindValue(
						val,
						( jdbcMapping.getSelectableName() ),
						ParameterUsage.RESTRICT
				),
				session
		);
	}

	private JdbcOperation buildRemoveOperation(TableDescriptor tableDescriptor) {
		var builder = new GraphTableUpdateBuilderStandard(
				persister,
				tableDescriptor,
				persister.getSqlWhereString(),
				factory
		);

		final var attributeMapping = persister.getAttributeMapping();
		assert attributeMapping != null;

		final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
		assert foreignKeyDescriptor != null;

		foreignKeyDescriptor.getKeyPart().forEachSelectable( (selectionIndex, selectableMapping) -> {
			builder.addValueColumn( NULL, selectableMapping );
			builder.addKeyRestriction( selectableMapping );
		} );

		if ( persister.hasPhysicalIndexColumn() ) {
			attributeMapping.getIndexDescriptor().forEachColumn( (selectionIndex, selectableMapping) -> {
				if ( selectableMapping.isUpdateable() ) {
					builder.addValueColumn( NULL, selectableMapping );
				}
			} );
		}

		return builder.buildMutation().createMutationOperation();
	}

}
