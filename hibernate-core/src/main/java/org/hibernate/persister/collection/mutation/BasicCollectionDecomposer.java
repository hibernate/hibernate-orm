/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.meta.CollectionTableDescriptor;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableDeleteBuilderStandard;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableInsertBuilderStandard;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableUpdateBuilderStandard;
import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static org.hibernate.cfg.FlushSettings.BUNDLE_COLLECTION_OPERATIONS;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/// Decomposition support for [BasicCollectionPersister] which managed inserts, updates and deletes
/// into the collection table.
///
/// @author Steve Ebersole
public class BasicCollectionDecomposer extends AbstractCollectionDecomposer {
	private final BasicCollectionPersister persister;
	private final CollectionTableDescriptor tableDescriptor;
	private final boolean shouldBundleCollectionOperations;
	private final CollectionJdbcOperations jdbcOperations;

	public BasicCollectionDecomposer(
			BasicCollectionPersister persister,
			CollectionTableDescriptor tableDescriptor,
			SessionFactoryImplementor factory) {
		assert persister != null;
		assert tableDescriptor != null;

		this.persister = persister;
		this.tableDescriptor = tableDescriptor;

		var configurationService = factory.getServiceRegistry().requireService( ConfigurationService.class );
		shouldBundleCollectionOperations = configurationService.getSetting(
				BUNDLE_COLLECTION_OPERATIONS,
				BOOLEAN,
				false
		);

		this.jdbcOperations = buildJdbcOperations( factory );
	}

	public List<PlannedOperation> decomposeRecreate(
			CollectionRecreateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		// Register callback to handle post-execution work (afterAction, cache, events, stats)
		final Object cacheKey = lockCacheItem( action, session );
		postExecCallbackRegistry.accept( new PostCollectionRecreateHandling( action, cacheKey ) );

		return planRecreateOperation( action.getCollection(), action.getKey(), ordinalBase, session );
	}

	private List<PlannedOperation> planRecreateOperation(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = jdbcOperations.getInsertRowPlan();
		if ( insertRowPlan == null ) {
			return List.of();
		}

		// Pre-insert callback once for the whole collection
		collection.preInsert( persister );

		final var entries = collection.entries( persister );
		if ( !entries.hasNext() ) {
			return List.of();
		}

		final List<PlannedOperation> operations = new ArrayList<>();

		if ( shouldBundleCollectionOperations ) {
			// Bundled: all rows in a single PlannedOperation with a bundled BindPlan
			final List<Object> entryList = new ArrayList<>();
			final List<Integer> entryIndices = new ArrayList<>();
			int entryCount = 0;

			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				boolean include = collection.includeInRecreate( entry, entryCount, collection, persister.getAttributeMapping() );

				if ( include ) {
					entryList.add( entry );
					entryIndices.add( entryCount );
				}

				entryCount++;
			}

			if ( !entryList.isEmpty() ) {
				final BindPlan bundledBindPlan = new BundledCollectionInsertBindPlan(
						insertRowPlan.values(),
						collection,
						key,
						entryList,
						entryIndices
				);

				operations.add( new PlannedOperation(
						tableDescriptor,
						MutationKind.INSERT,
						insertRowPlan.jdbcOperation(),
						bundledBindPlan,
						ordinalBase,
						"InsertRows(" + persister.getRolePath() + ")"
				) );
			}
		}
		else {
			// Non-bundled: one operation per row
			int entryCount = 0;
			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				boolean include = collection.includeInRecreate( entry, entryCount, collection, persister.getAttributeMapping() );

				if ( include ) {
					final BindPlan bindPlan = new SingleRowInsertBindPlan(
							persister,
							jdbcOperations.getInsertRowPlan().values(),
							collection,
							key,
							entry,
							entryCount
					);

					final PlannedOperation plannedOp = new PlannedOperation(
							tableDescriptor,
							MutationKind.INSERT,
							jdbcOperations.getInsertRowPlan().jdbcOperation(),
							bindPlan,
							ordinalBase * 1_000 + entryCount,
							"InsertRow[" + entryCount + "](" + persister.getRolePath() + ")"
					);

					operations.add( plannedOp );
				}

				entryCount++;
			}
		}

		return operations;
	}

	public List<PlannedOperation> decomposeUpdate(
			CollectionUpdateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		preUpdate( action, session );

		final Object cacheKey = lockCacheItem(action, session);

		var collection = action.getCollection();
		var key = action.getKey();
		var attribute = persister.getAttributeMapping();

		final List<PlannedOperation> operations = new ArrayList<>();

		if ( !collection.wasInitialized() ) {
			// If there were queued operations, they would have
			// been processed and cleared by now.
			if ( !collection.isDirty() ) {
				// The collection should still be dirty.
				throw new AssertionFailure( "collection is not dirty" );
			}
			// Do nothing - we only need to notify the cache
		}
		else {
			final boolean affectedByFilters = persister.isAffectedByEnabledFilters( session );
			final var eventMonitor = session.getEventMonitor();
			final var event = eventMonitor.beginCollectionUpdateEvent();
			boolean success = false;
			try {
				if ( !affectedByFilters && collection.empty() ) {
					if ( !action.isEmptySnapshot() ) {
						operations.addAll( planRemoveOperation( key, ordinalBase, session ) );
					}
				}
				else if ( collection.needsRecreate( persister ) ) {
					if ( affectedByFilters ) {
						throw new HibernateException( String.format( Locale.ROOT,
								"cannot recreate collection while filter is enabled [%s : %s]",
								persister.getRole(),
								key
						) );
					}
					if ( !action.isEmptySnapshot() ) {
						operations.addAll( planRemoveOperation( key, ordinalBase, session ) );
					}
					operations.addAll( planRecreateOperation(  collection, key, ordinalBase, session ) );
				}
				else {
					planDeleteRowOperations( collection, key, ordinalBase, session, operations::add );

					if ( shouldBundleCollectionOperations ) {
						planBundledChangeAndAdditionOperations( collection, key, ordinalBase, session, operations::add );
					}
					else {
						planUpdateRowOperations( collection, key, ordinalBase, session, operations::add );
						planInsertRowOperations( collection, key, ordinalBase, session, operations::add );
					}
				}
				success = true;
			}
			finally {
				eventMonitor.completeCollectionUpdateEvent( event, key, persister.getRole(), success, session );
			}
		}

		postExecCallbackRegistry.accept( new PostCollectionUpdateHandling(
				persister,
				collection,
				key,
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				cacheKey
		) );

		return operations;
	}

	private void planDeleteRowOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		var deleteRowPlan = jdbcOperations.getDeleteRowPlan();
		final var deletes = collection.getDeletes( persister, !persister.hasPhysicalIndexColumn() );
		if ( deleteRowPlan == null || !deletes.hasNext() ) {
			MODEL_MUTATION_LOGGER.noRowsToDelete();
			return;
		}

		if ( shouldBundleCollectionOperations ) {
			// Bundle all rows into a single PlannedOperation with a bundled BindPlan
			final List<Object> deletionList = new ArrayList<>();

			while ( deletes.hasNext() ) {
				deletionList.add( deletes.next() );
			}

			if ( !deletionList.isEmpty() ) {
				final BindPlan bundledBindPlan = new BundledCollectionDeleteBindPlan(
						collection,
						key,
						deleteRowPlan.restrictions(),
						deletionList
				);

				operationConsumer.accept( new PlannedOperation(
						tableDescriptor,
						MutationKind.DELETE,
						deleteRowPlan.jdbcOperation(),
						bundledBindPlan,
						ordinalBase,
						"DeleteRows(" + persister.getRolePath() + ")"
				) );
			}
		}
		else {
			// Original behavior: one operation per row
			int deletionCount = 0;

			while ( deletes.hasNext() ) {
				final Object removal = deletes.next();

				final BindPlan bindPlan = new SingleRowDeleteBindPlan(
						collection,
						key,
						removal,
						deleteRowPlan.restrictions()
				);

				operationConsumer.accept( new PlannedOperation(
						tableDescriptor,
						MutationKind.DELETE,
						deleteRowPlan.jdbcOperation(),
						bindPlan,
						ordinalBase * 1_000 + deletionCount,
						"DeleteRow[" + deletionCount + "](" + persister.getRolePath() + ")"
				) );

				deletionCount++;
			}
		}
	}

	private void planBundledChangeAndAdditionOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		assert shouldBundleCollectionOperations;

		var updateRowPlan = jdbcOperations.getUpdateRowPlan();
		var insertRowPlan = jdbcOperations.getInsertRowPlan();
		var entries = collection.entries( persister );

		if ( (updateRowPlan != null || insertRowPlan != null) && entries.hasNext() ) {
			var changeEntries = updateRowPlan == null ? null : new ArrayList<BundledBindPlanEntry>();
			var additionEntries = insertRowPlan == null ? null : new ArrayList<BundledBindPlanEntry>();
			int entryCount = 0;

			while ( entries.hasNext() ) {
				final Object entry = entries.next();

				var isAddition = collection.needsInserting( entry, entryCount, persister.getElementType() );
				var isChange = collection.needsUpdating( entry, entryCount, persister.getAttributeMapping() );

				if ( isAddition && isChange ) {
					// Log a warning?  This typically means bad equals/hashCode, though can happen I guess
					// with UserCollectionType too...
				}
				if ( updateRowPlan != null && isChange ) {
					changeEntries.add( new BundledBindPlanEntry( entry, entryCount ) );
				}
				if ( insertRowPlan != null && isAddition ) {
					additionEntries.add( new BundledBindPlanEntry( entry, entryCount ) );
				}

				entryCount++;
			}

			// UPDATE modified entries
			applyBundledUpdateChanges( collection, key, ordinalBase + 1, changeEntries, updateRowPlan, operationConsumer );

			// INSERT entries
			applyBundledUpdateAdditions( collection, key, ordinalBase + 2, additionEntries, insertRowPlan, operationConsumer );
		}
	}

	protected void applyBundledUpdateChanges(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			List<BundledBindPlanEntry> changeEntries,
			CollectionJdbcOperations.UpdateRowPlan updateRowPlan,
			Consumer<PlannedOperation> operationConsumer) {
		if ( CollectionHelper.isEmpty( changeEntries ) ) {
			return;
		}

		final BindPlan bundledBindPlan = new BundledCollectionUpdateBindPlan(
				collection,
				key,
				updateRowPlan.values(),
				updateRowPlan.restrictions(),
				changeEntries
		);

		operationConsumer.accept( new PlannedOperation(
				persister.getCollectionTableDescriptor(),
				MutationKind.UPDATE,
				updateRowPlan.jdbcOperation(),
				bundledBindPlan,
				ordinalBase,
				"BundledUpdateRows(" + persister.getRolePath() + ")"
		) );
	}

	protected void applyBundledUpdateAdditions(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			List<BundledBindPlanEntry> additionEntries,
			CollectionJdbcOperations.InsertRowPlan insertRowPlan,
			Consumer<PlannedOperation> operationConsumer) {
		if ( CollectionHelper.isEmpty( additionEntries ) ) {
			return;
		}

		// Pre-insert callback once for the whole collection
		collection.preInsert( persister );

		final BindPlan bundledBindPlan = new BundledCollectionInsertBindPlan(
				insertRowPlan.values(),
				collection,
				key,
				additionEntries
		);

		operationConsumer.accept( new PlannedOperation(
				persister.getCollectionTableDescriptor(),
				MutationKind.INSERT,
				insertRowPlan.jdbcOperation(),
				bundledBindPlan,
				ordinalBase,
				"BundledInsertRows(" + persister.getRolePath() + ")"
		) );
	}

	private void planUpdateRowOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		var updateRowPlan = jdbcOperations.getUpdateRowPlan();
		final var entries = collection.entries( persister );

		if ( updateRowPlan == null || !entries.hasNext() ) {
			// EARLY EXIT!!
			return;
		}

		// One operation per row
		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();

			if ( collection.needsUpdating( entry, entryCount, persister.getAttributeMapping() ) ) {
				final BindPlan bindPlan = new SingleRowUpdateBindPlan(
						collection,
						key,
						entry,
						entryCount,
						updateRowPlan.values(),
						updateRowPlan.restrictions()
				);

				operationConsumer.accept( new PlannedOperation(
						tableDescriptor,
						MutationKind.UPDATE,
						updateRowPlan.jdbcOperation(),
						bindPlan,
						ordinalBase * 1_000 + entryCount,
						"UpdateRow[" + entryCount + "](" + persister.getRolePath() + ")"
				) );
			}

			entryCount++;
		}
	}

	private void planInsertRowOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		// Pre-insert callback once for the whole collection
		collection.preInsert( persister );

		var insertRowPlan = jdbcOperations.getInsertRowPlan();
		final var entries = collection.entries( persister );

		if ( insertRowPlan == null || !entries.hasNext() ) {
			// EARLY EXIT!!
			return;
		}

		// One operation per row
		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();

			if ( collection.includeInInsert( entry, entryCount, collection, persister.getAttributeMapping() ) ) {
				final BindPlan bindPlan = new SingleRowInsertBindPlan(
						persister,
						insertRowPlan.values(),
						collection,
						key,
						entry,
						entryCount
				);

				operationConsumer.accept( new PlannedOperation(
						tableDescriptor,
						MutationKind.INSERT,
						insertRowPlan.jdbcOperation(),
						bindPlan,
						ordinalBase * 1_000 + entryCount,
						"InsertRow[" + entryCount + "](" + persister.getRolePath() + ")"
				) );
			}

			entryCount++;
		}
	}

	public List<PlannedOperation> decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		// Register callback to handle post-execution work (afterAction, cache, events, stats)
		final Object cacheKey = lockCacheItem( action, session );
		postExecCallbackRegistry.accept( new PostCollectionRemoveHandling( action, cacheKey ) );

		return planRemoveOperation( action.getKey(), ordinalBase,  session );
	}

	private List<PlannedOperation> planRemoveOperation(Object key, int ordinalBase, SharedSessionContractImplementor session) {
		final var jdbcOperation = jdbcOperations.getRemoveOperation();
		if ( jdbcOperation == null ) {
			return List.of();
		}

		final PlannedOperation plannedOp = new PlannedOperation(
				tableDescriptor,
				MutationKind.DELETE,
				jdbcOperation,
				new RemoveBindPlan( key, persister ),
				ordinalBase * 1_000,
				"RemoveAllRows(" + persister.getRolePath() + ")"
		);

		return List.of( plannedOp );
	}

	private static class RemoveBindPlan implements BindPlan {
		private final Object key;
		private final BasicCollectionPersister mutationTarget;

		public RemoveBindPlan(Object key, BasicCollectionPersister mutationTarget) {
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





	private CollectionJdbcOperations buildJdbcOperations(
			SessionFactoryImplementor factory) {
		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = buildInsertRowPlan( factory );

		final CollectionJdbcOperations.UpdateRowPlan updateRowPlan = buildUpdateRowPlan( factory );

		final CollectionJdbcOperations.DeleteRowPlan deleteRowPlan = buildDeleteRowPlan( factory );

		return new CollectionJdbcOperations(
				persister,
				insertRowPlan,
				updateRowPlan,
				deleteRowPlan,
				buildRemoveOperation( factory )
		);
	}

	private CollectionJdbcOperations.InsertRowPlan buildInsertRowPlan(
			SessionFactoryImplementor factory) {
		if ( persister.isInverse() || !persister.isRowInsertEnabled() ) {
			return null;
		}

		// Ignore custom SQL for now...
		var builder = new GraphTableInsertBuilderStandard(
				persister,
				tableDescriptor,
				factory
		);

		applyInsertDetails( builder, persister, factory );

		return new CollectionJdbcOperations.InsertRowPlan(
				builder.buildMutation().createMutationOperation(),
				this::bindInsertRowValues
		);
	}

	private void applyInsertDetails(
			GraphTableInsertBuilderStandard insertBuilder,
			BasicCollectionPersister persister,
			SessionFactoryImplementor factory) {
		final var attributeMapping = persister.getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().forEachInsertable( (i, columnMapping) -> {
			insertBuilder.addValueColumn( columnMapping );
		});

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.forEachInsertable( (i, columnMapping) -> {
				insertBuilder.addValueColumn( columnMapping );
			} );
		}
		else if ( indexDescriptor != null ) {
			indexDescriptor.forEachInsertable( (i, columnMapping) -> {
				insertBuilder.addValueColumn( columnMapping );
			} );
		}

		// Add element columns
		attributeMapping.getElementDescriptor().forEachInsertable( (i, columnMapping) -> {
			insertBuilder.addValueColumn( columnMapping );
		} );

		final var softDeleteMapping = attributeMapping.getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			final var columnReference = new ColumnReference( insertBuilder.getTableReference(), softDeleteMapping );
			insertBuilder.addValueColumn( softDeleteMapping.createNonDeletedValueBinding( columnReference ) );
		}
	}

	private void bindInsertRowValues(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + persister.getNavigableRole().getFullPath() );
		}

		final var attributeMapping = persister.getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				key,
				jdbcValueBindings::bindAssignment,
				session
		);

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					collection.getIdentifier( rowValue, rowPosition ),
					jdbcValueBindings::bindAssignment,
					session
			);
		}
		else {
			final var indexDescriptor = attributeMapping.getIndexDescriptor();
			if ( indexDescriptor != null ) {
				indexDescriptor.decompose(
						persister.incrementIndexByBase( collection.getIndex( rowValue, rowPosition, persister ) ),
						jdbcValueBindings::bindAssignment,
						session
				);
			}
		}

		attributeMapping.getElementDescriptor().decompose(
				collection.getElement( rowValue ),
				jdbcValueBindings::bindAssignment,
				session
		);
	}

	private CollectionJdbcOperations.UpdateRowPlan buildUpdateRowPlan(
			SessionFactoryImplementor factory) {
		if ( !persister.isPerformingUpdates() ) {
			return null;
		}

		var attribute = persister.getAttributeMapping();

		var builder = new GraphTableUpdateBuilderStandard(
				persister,
				tableDescriptor,
				persister.getSqlWhereString(),
				factory
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SET clause: element columns (and possibly index columns for lists)

		final var indexDescriptor = attribute.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.forEachUpdatable(
				(selectionIndex, jdbcMapping) -> {
					builder.addValueColumn( jdbcMapping );
				}
			);
		}

		attribute.getElementDescriptor().forEachUpdatable(
			(selectionIndex, jdbcMapping) -> {
				builder.addValueColumn( jdbcMapping );
			}
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// WHERE clause: key columns (restrict by owner FK)

		attribute.getKeyDescriptor().getKeyPart().forEachColumn(
			(selectionIndex, jdbcMapping) -> {
				builder.addKeyRestriction( jdbcMapping );
			}
		);

		return new CollectionJdbcOperations.UpdateRowPlan(
				builder.buildMutation().createMutationOperation(),
				this::bindUpdateRowValues,
				this::bindUpdateRowRestrictions
		);
	}

	private void bindUpdateRowValues(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + persister.getNavigableRole().getFullPath() );
		}

		var attribute = persister.getAttributeMapping();
		var indexDescriptor = attribute.getIndexDescriptor();
		var elementDescriptor = attribute.getElementDescriptor();

		if ( indexDescriptor != null ) {
			indexDescriptor.decompose(
					collection.getIndex( rowValue, rowPosition, persister ),
					jdbcValueBindings::bindAssignment,
					session
			);
		}

		elementDescriptor.decompose(
				rowValue,
				jdbcValueBindings::bindAssignment,
				session
		);
	}

	private void bindUpdateRowRestrictions(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + persister.getNavigableRole().getFullPath() );
		}

		final var attribute = persister.getAttributeMapping();

		attribute.getKeyDescriptor().getKeyPart().decompose(
				key,
				jdbcValueBindings::bindRestriction,
				session
		);
	}

	private CollectionJdbcOperations.DeleteRowPlan buildDeleteRowPlan(SessionFactoryImplementor factory) {
		if ( persister.isInverse() || !persister.isRowDeleteEnabled() ) {
			return null;
		}

		var attribute = persister.getAttributeMapping();

		var builder = new GraphTableDeleteBuilderStandard(
				persister,
				tableDescriptor,
				persister.getSqlWhereString(),
				factory
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// WHERE clause: restrict by
		// 		- key columns (restrict by owner FK)
		//		-  element/index

		attribute.getKeyDescriptor().getKeyPart().forEachSelectable( (index, jdbcMapping) -> {
			builder.addKeyRestriction( jdbcMapping );
		} );

		// For row-based deletion, also restrict by element/index
		// This differentiates deleteRows (specific rows) from remove (entire collection)
		final var indexDescriptor = attribute.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			// For indexed collections (lists, maps), restrict by index
			indexDescriptor.forEachSelectable( (index, jdbcMapping) -> {
				builder.addKeyRestriction( jdbcMapping );
			} );
		}
		else {
			// For non-indexed collections (sets, bags), restrict by element
			attribute.getElementDescriptor().forEachSelectable((index, jdbcMapping) -> {
				builder.addKeyRestriction( jdbcMapping );
			} );
		}

		return new CollectionJdbcOperations.DeleteRowPlan(
				builder.buildMutation().createMutationOperation(),
				this::bindDeleteRestrictions
		);
	}

	private void bindDeleteRestrictions(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		var attribute = persister.getAttributeMapping();

		attribute.getKeyDescriptor().getKeyPart().decompose(
				key,
				jdbcValueBindings::bindRestriction,
				session
		);

		// For row-based deletion, also restrict by element/index
		// This differentiates deleteRows (specific rows) from remove (entire collection)
		final var indexDescriptor = attribute.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			// For indexed collections (lists, maps), restrict by index
			indexDescriptor.decompose(
					collection.getIndex( rowValue, rowPosition, persister ),
					jdbcValueBindings::bindRestriction,
					session
			);
		}
		else {
			// For non-indexed collections (sets, bags), restrict by element
			attribute.getElementDescriptor().decompose(
					rowValue,
					jdbcValueBindings::bindRestriction,
					session
			);
		}
	}

	private JdbcOperation buildRemoveOperation(
			SessionFactoryImplementor factory) {
		var tableDescriptor = persister.getCollectionTableDescriptor();
		var attribute = persister.getAttributeMapping();

		var builder = new GraphTableDeleteBuilderStandard(
				persister,
				tableDescriptor,
				persister.getSqlWhereString(),
				factory
		);

		attribute.getKeyDescriptor().getKeyPart().forEachSelectable( (index, jdbcMapping) -> {
			builder.addKeyRestriction( jdbcMapping );
		} );

		return builder.buildMutation().createMutationOperation();
	}

}
