/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.ast.builder.CollectionRowDeleteByUpdateSetNullBuilder;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableUpdateBuilderStandard;
import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.OneToManyCollectionPart;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.mutation.BundledBindPlanEntry;
import org.hibernate.persister.collection.mutation.BundledCollectionDeleteBindPlan;
import org.hibernate.persister.collection.mutation.BundledCollectionInsertBindPlan;
import org.hibernate.persister.collection.mutation.BundledCollectionUpdateBindPlan;
import org.hibernate.persister.collection.mutation.CollectionJdbcOperations;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorStandard;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorTablePerSubclass;
import org.hibernate.persister.collection.mutation.OperationProducer;
import org.hibernate.persister.collection.mutation.PostCollectionRecreateHandling;
import org.hibernate.persister.collection.mutation.PostCollectionRemoveHandling;
import org.hibernate.persister.collection.mutation.PostCollectionUpdateHandling;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.RowMutationOperations;
import org.hibernate.persister.collection.mutation.SingleRowDeleteBindPlan;
import org.hibernate.persister.collection.mutation.SingleRowInsertBindPlan;
import org.hibernate.persister.collection.mutation.SingleRowUpdateBindPlan;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorOneToMany;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorTablePerSubclass;
import org.hibernate.persister.collection.mutation.WriteIndexCoordinator;
import org.hibernate.persister.collection.mutation.WriteIndexCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.WriteIndexCoordinatorStandard;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameterList;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.TableUpdate;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.TableUpdateStandard;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static org.hibernate.internal.util.collections.ArrayHelper.isAnyTrue;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.persister.collection.mutation.RowMutationOperations.DEFAULT_RESTRICTOR;
import static org.hibernate.persister.collection.mutation.RowMutationOperations.DEFAULT_VALUE_SETTER;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.ast.builder.TableUpdateBuilder.NULL;

/**
 * A {@link CollectionPersister} for {@linkplain jakarta.persistence.OneToMany
 * one-to-many associations}.
 *
 * @see BasicCollectionPersister
 *
 * @author Gavin King
 * @author Brett Meyer
 */
@Internal
public class OneToManyPersister extends AbstractCollectionPersister {
	private final RowMutationOperations rowMutationOperations;

	private final InsertRowsCoordinator insertRowsCoordinator;
	private final UpdateRowsCoordinator updateRowsCoordinator;
	private final DeleteRowsCoordinator deleteRowsCoordinator;
	private final RemoveCoordinator removeCoordinator;
	private final WriteIndexCoordinator writeIndexCoordinator;

	private boolean isAssociationTablePerSubclass;
	private final boolean keyIsNullable;

	final boolean doWriteEvenWhenInverse; // contrary to intent of JPA

	public OneToManyPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			RuntimeModelCreationContext creationContext)
					throws MappingException, CacheException {
		super( collectionBinding, cacheAccessStrategy, creationContext );
		keyIsNullable = collectionBinding.getKey().isNullable();

		doWriteEvenWhenInverse =
				isInverse
					&& hasIndex()
					&& !indexContainsFormula
					&& isAnyTrue( indexColumnIsSettable )
					&& !getElementPersisterInternal().managesColumns( indexColumnNames );

		rowMutationOperations = buildRowMutationOperations();
		final var stateManagement = collectionBinding.getStateManagement();
		insertRowsCoordinator = stateManagement.createInsertRowsCoordinator( this );
		updateRowsCoordinator = stateManagement.createUpdateRowsCoordinator( this );
		deleteRowsCoordinator = stateManagement.createDeleteRowsCoordinator( this );
		removeCoordinator = stateManagement.createRemoveCoordinator( this );
		writeIndexCoordinator = buildWriteIndexCoordinator();
	}

	@Override
	public void prepareMappingModel(MappingModelCreationProcess creationProcess) {
		super.prepareMappingModel( creationProcess );

		if ( getElementPersister() instanceof UnionSubclassEntityPersister ) {
			isAssociationTablePerSubclass = true;
		}
		else {
			isAssociationTablePerSubclass = false;
		}
	}

	@Override
	public void postInstantiate() throws MappingException {
		super.postInstantiate();

		// Initialize JDBC operations after entity persisters have completed their initialization
		if ( isAssociationTablePerSubclass ) {
//			decomposer = new TablePerSubclassOneToManyDecomposer( this, getFactory() );
			jdbcOperationsSelector = new TablePerSubclassJdbcOperationsSelector();
		}
		else {
//			var cfgSvc = getFactory().getServiceRegistry().requireService( ConfigurationService.class );
//			var shouldBundleOperations = cfgSvc.getSetting(
//					FlushSettings.BUNDLE_COLLECTION_OPERATIONS,
//					BOOLEAN,
//					false
//			);
//			decomposer = shouldBundleOperations
//					? new BundledOneToManyDecomposer( this, getFactory() )
//					: new StandardOneToManyDecomposer( this, getFactory() );
			jdbcOperationsSelector = new StandardJdbcOperationsSelector();
		}
	}

	public boolean isDoWriteEvenWhenInverse() {
		return doWriteEvenWhenInverse;
	}

	@Override
	public RowMutationOperations getRowMutationOperations() {
		return rowMutationOperations;
	}

	public InsertRowsCoordinator getInsertRowsCoordinator() {
		return insertRowsCoordinator;
	}

	public UpdateRowsCoordinator getUpdateRowsCoordinator() {
		return updateRowsCoordinator;
	}

	public DeleteRowsCoordinator getDeleteRowsCoordinator() {
		return deleteRowsCoordinator;
	}

	@Override
	public RemoveCoordinator getRemoveCoordinator() {
		return removeCoordinator;
	}

	public WriteIndexCoordinator getWriteIndexCoordinator() {
		return writeIndexCoordinator;
	}

	@Override
	public boolean isRowDeleteEnabled() {
		return super.isRowDeleteEnabled() && keyIsNullable;
	}

	@Override
	public void recreate(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session)
			throws HibernateException {
		getInsertRowsCoordinator().insertRows( collection, id, collection::includeInRecreate, session );
		writeIndex( collection, collection.entries( this ), id, true, session );
	}

	@Override
	public void insertRows(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session)
			throws HibernateException {
		getInsertRowsCoordinator().insertRows( collection, id, collection::includeInInsert, session );
		writeIndex( collection, collection.entries( this ), id, true, session );
	}

	@Override
	public void updateRows(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session) {
		getUpdateRowsCoordinator().updateRows( id, collection, session );
//		oldUpdateRows( collection, id, session );
	}

	@Override
	public void deleteRows(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session) {
		getDeleteRowsCoordinator().deleteRows( collection, key, session );
	}

	@Override
	protected void doProcessQueuedOps(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session)
			throws HibernateException {
		writeIndex( collection, collection.queuedAdditionIterator(), id, false, session );
	}

	private void writeIndex(
			PersistentCollection<?> collection,
			Iterator<?> entries,
			Object key,
			boolean resetIndex,
			SharedSessionContractImplementor session) {
		writeIndexCoordinator.writeIndex( collection, entries, key, resetIndex, session );
	}

	public boolean isOneToMany() {
		return true;
	}

	@Override
	public boolean isManyToMany() {
		return false;
	}

	@Override
	public String getTableName() {
		return getElementPersister().getTableName();
	}

	protected void applyWhereFragments(
			Consumer<Predicate> predicateConsumer,
			String alias,
			TableGroup tableGroup,
			SqlAstCreationState astCreationState) {
		super.applyWhereFragments( predicateConsumer, alias, tableGroup, astCreationState );
		if ( !astCreationState.supportsEntityNameUsage() ) {
			// We only need to apply discriminator for loads, since queries with joined
			// inheritance subtypes are already filtered by the entity name usage logic
			getElementPersisterInternal()
					.applyDiscriminator( predicateConsumer, alias, tableGroup, astCreationState );
		}
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return getElementPersister().getFilterAliasGenerator( rootAlias );
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(TableGroup rootTableGroup) {
		return getElementPersister().getFilterAliasGenerator( rootTableGroup );
	}


	@Override
	public RestrictedTableMutation<JdbcMutationOperation> generateDeleteAllAst(MutatingTableReference tableReference) {
		final var attributeMapping = getAttributeMapping();
		assert attributeMapping != null;

		final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
		assert foreignKeyDescriptor != null;

		final int keyColumnCount = foreignKeyDescriptor.getJdbcTypeCount();
		final int valuesCount = hasIndex()
				? keyColumnCount + indexColumnNames.length
				: keyColumnCount;

		final var parameterBinders =
				new ColumnValueParameterList( tableReference, ParameterUsage.RESTRICT, keyColumnCount );
		final List<ColumnValueBinding> keyRestrictionBindings = arrayList( keyColumnCount );
		final List<ColumnValueBinding> valueBindings = arrayList( valuesCount );
		foreignKeyDescriptor.getKeyPart().forEachSelectable( (selectionIndex, selectableMapping) -> {
			final var columnValueParameter = parameterBinders.addColumValueParameter( selectableMapping );
			final var columnReference = columnValueParameter.getColumnReference();
			keyRestrictionBindings.add(
					new ColumnValueBinding( columnReference,
							new ColumnWriteFragment( "?", columnValueParameter, selectableMapping ) )
			);
			valueBindings.add(
					new ColumnValueBinding( columnReference,
							new ColumnWriteFragment( "null", selectableMapping ) )
			);
		} );

		if ( hasIndex() && !indexContainsFormula ) {
			attributeMapping.getIndexDescriptor().forEachSelectable( (selectionIndex, selectableMapping) -> {
				if ( selectableMapping.isUpdateable() ) {
					valueBindings.add(
							new ColumnValueBinding(
									new ColumnReference( tableReference, selectableMapping ),
									new ColumnWriteFragment( "null", selectableMapping )
							)
					);
				}
			} );
		}

		return new TableUpdateStandard(
				tableReference,
				this,
				"one-shot delete for " + getRolePath(),
				valueBindings,
				keyRestrictionBindings,
				null,
				parameterBinders,
				sqlWhereString,
				Expectation.None.INSTANCE
		);
	}


	private RowMutationOperations buildRowMutationOperations() {
		final OperationProducer insertRowOperationProducer;
		final RowMutationOperations.Values insertRowValues;
		if ( !isInverse() && isRowInsertEnabled() ) {
			insertRowOperationProducer = this::generateInsertRowOperation;
			insertRowValues = this::applyInsertRowValues;
		}
		else {
			insertRowOperationProducer = null;
			insertRowValues = null;
		}

		final OperationProducer writeIndexOperationProducer;
		final RowMutationOperations.Values writeIndexValues;
		final RowMutationOperations.Restrictions writeIndexRestrictions;
		if ( doWriteEvenWhenInverse ) {
			writeIndexOperationProducer = this::generateWriteIndexOperation;
			writeIndexValues = this::applyWriteIndexValues;
			writeIndexRestrictions = this::applyWriteIndexRestrictions;
		}
		else {
			writeIndexOperationProducer = null;
			writeIndexValues = null;
			writeIndexRestrictions = null;
		}

		final OperationProducer deleteEntryOperationProducer;
		final RowMutationOperations.Restrictions deleteEntryRestrictions;
		if ( !isInverse() && isRowDeleteEnabled() ) {
			deleteEntryOperationProducer = this::generateDeleteRowOperation;
			deleteEntryRestrictions = this::applyDeleteRowRestrictions;
		}
		else {
			deleteEntryOperationProducer = null;
			deleteEntryRestrictions = null;
		}

		final OperationProducer deleteAllEntriesOperationProducer;
		if ( !isInverse() && isRowDeleteEnabled() ) {
			deleteAllEntriesOperationProducer = this::buildDeleteAllOperation;
		}
		else {
			deleteAllEntriesOperationProducer = null;
		}

		return new RowMutationOperations(
				this,
				insertRowOperationProducer,
				insertRowValues,
				writeIndexOperationProducer,
				writeIndexValues,
				writeIndexRestrictions,
				deleteEntryOperationProducer,
				deleteEntryRestrictions,
				deleteAllEntriesOperationProducer
		);
	}

	private WriteIndexCoordinator buildWriteIndexCoordinator() {
		if ( doWriteEvenWhenInverse ) {
			return new WriteIndexCoordinatorStandard( this, rowMutationOperations, getFactory() );
		}
		else {
			return new WriteIndexCoordinatorNoOp( this );
		}
	}

	private JdbcMutationOperation generateDeleteRowOperation(MutatingTableReference tableReference) {
		return getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( generateDeleteRowAst( tableReference ), getFactory() )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	public RestrictedTableMutation<JdbcMutationOperation> generateDeleteRowAst(MutatingTableReference tableReference) {
		// note that custom SQL delete row details are handled by CollectionRowUpdateBuilder
		final var updateBuilder = new org.hibernate.sql.model.ast.builder.CollectionRowDeleteByUpdateSetNullBuilder(
				this,
				tableReference,
				getFactory(),
				sqlWhereString
		);

		// for each key column -
		// 		1) set the value to null
		// 		2) restrict based on key value
		final var foreignKeyDescriptor = getAttributeMapping().getKeyDescriptor();
		final int keyTypeCount = foreignKeyDescriptor.getJdbcTypeCount();
		for ( int i = 0; i < keyTypeCount; i++ ) {
			final var selectable = foreignKeyDescriptor.getSelectable( i );
			if ( !selectable.isFormula() ) {
				if ( selectable.isUpdateable() ) {
					// set null
					updateBuilder.addValueColumn( NULL, selectable );
				}
				// restrict
				updateBuilder.addKeyRestriction( selectable );
			}
		}

		// set the value for each index column to null
		if ( hasIndex() && !indexContainsFormula ) {
			final var indexDescriptor = getAttributeMapping().getIndexDescriptor();
			assert indexDescriptor != null;
			final int indexTypeCount = indexDescriptor.getJdbcTypeCount();
			for ( int i = 0; i < indexTypeCount; i++ ) {
				final var selectable = indexDescriptor.getSelectable( i );
				if ( selectable.isUpdateable() ) {
					updateBuilder.addValueColumn( NULL, selectable );
				}
			}
		}

		// for one-to-many, we know the element is an entity and need to restrict the update
		// based on the element's id
		final var entityPart = (EntityCollectionPart) getAttributeMapping().getElementDescriptor();
		final var entityId = entityPart.getAssociatedEntityMappingType().getIdentifierMapping();
		entityId.forEachColumn( (index, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );

		//noinspection unchecked,rawtypes
		return (RestrictedTableMutation) updateBuilder.buildMutation();
	}

	private void applyDeleteRowRestrictions(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var pluralAttribute = getAttributeMapping();
		pluralAttribute.getKeyDescriptor().decompose(
				keyValue,
				0,
				jdbcValueBindings,
				null,
				(valueIndex, bindings, noop, value, jdbcValueMapping) -> {
					if ( !jdbcValueMapping.isFormula() ) {
						bindings.bindValue( value, jdbcValueMapping, ParameterUsage.RESTRICT );
					}
				},
				session
		);
		pluralAttribute.getElementDescriptor().decompose(
				rowValue,
				0,
				jdbcValueBindings,
				null,
				DEFAULT_RESTRICTOR,
				session
		);
	}


	private JdbcMutationOperation generateInsertRowOperation(MutatingTableReference tableReference) {
		// NOTE: `TableUpdateBuilderStandard` and `TableUpdate` already handle custom-sql
		return buildTableUpdate( tableReference )
				.createMutationOperation( null, getFactory() );
	}

	private TableUpdate<JdbcMutationOperation> buildTableUpdate(MutatingTableReference tableReference) {
		final TableUpdateBuilderStandard<JdbcMutationOperation> updateBuilder =
				new TableUpdateBuilderStandard<>( this, tableReference, getFactory(), sqlWhereString );
		final var attributeMapping = getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().forEachUpdatable( updateBuilder );
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.forEachUpdatable( updateBuilder );
		}
		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var elementType = elementDescriptor.getAssociatedEntityMappingType();
		assert elementType.containsTableReference( tableReference.getTableName() );
		updateBuilder.addKeyRestrictionsLeniently( elementType.getIdentifierMapping() );
		return (TableUpdate<JdbcMutationOperation>) updateBuilder.buildMutation();
	}

	private void applyInsertRowValues(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				keyValue,
				0,
				jdbcValueBindings,
				null,
				(valueIndex, bindings, noop, value, jdbcValueMapping) -> {
					if ( jdbcValueMapping.isUpdateable() && !jdbcValueMapping.isFormula() ) {
						bindings.bindValue( value, jdbcValueMapping, ParameterUsage.SET );
					}
				},
				session
		);
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.decompose(
					incrementIndexByBase( collection.getIndex( rowValue, rowPosition, this ) ),
					0,
					jdbcValueBindings,
					null,
					(valueIndex, bindings, noop, value, jdbcValueMapping) -> {
						if ( jdbcValueMapping.isUpdateable() ) {
							bindings.bindValue( value, jdbcValueMapping, ParameterUsage.SET );
						}
					},
					session
			);
		}

		final Object elementValue = collection.getElement( rowValue );
		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var identifierMapping = elementDescriptor.getAssociatedEntityMappingType().getIdentifierMapping();
		identifierMapping.decompose(
				identifierMapping.getIdentifier( elementValue ),
				0,
				jdbcValueBindings,
				null,
				DEFAULT_RESTRICTOR,
				session
		);
	}


	private JdbcMutationOperation generateWriteIndexOperation(MutatingTableReference tableReference) {
		// note that custom SQL update details are handled by TableUpdateBuilderStandard
		final var factory = getFactory();
		final TableUpdateBuilderStandard<JdbcMutationOperation> updateBuilder =
				new TableUpdateBuilderStandard<>( this, tableReference, factory, sqlWhereString );
		final var attributeMapping = getAttributeMapping();
		final var elementDescriptor = (OneToManyCollectionPart) attributeMapping.getElementDescriptor();
		updateBuilder.addKeyRestrictionsLeniently( elementDescriptor.getAssociatedEntityMappingType().getIdentifierMapping() );
		// if the collection has an identifier, add its column as well
		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			updateBuilder.addKeyRestrictionsLeniently( identifierDescriptor );
		}
		// for each index column:
		// 		* add a restriction based on the previous value (WHERE clause)
		//		* add an assignment for the new value (SET clause)
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
// DISABLED: 		updateBuilder.addKeyRestrictionsLeniently( indexDescriptor );
		indexDescriptor.forEachUpdatable( updateBuilder );
		return updateBuilder.buildMutation()
				.createMutationOperation( null, factory );
	}

	private void applyWriteIndexValues(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		getAttributeMapping().getIndexDescriptor().decompose(
				collection.getIndex( entry, entryPosition, this ),
				0,
				jdbcValueBindings,
				null,
				(valueIndex, bindings, noop, jdbcValue, jdbcValueMapping) -> {
					if ( jdbcValueMapping.isUpdateable() ) {
						bindings.bindValue( jdbcValue, jdbcValueMapping, ParameterUsage.SET );
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
		final var attributeMapping = getAttributeMapping();
		final var elementDescriptor = (OneToManyCollectionPart) attributeMapping.getElementDescriptor();
		final var associatedType = elementDescriptor.getAssociatedEntityMappingType();
		final Object element = collection.getElement( entry );
		final var identifierMapping = associatedType.getIdentifierMapping();
		identifierMapping.decompose(
				identifierMapping.getIdentifier( element ),
				0,
				jdbcValueBindings,
				null,
				DEFAULT_RESTRICTOR,
				session
		);

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					collection.getIdentifier( entry, entryPosition ),
					0,
					jdbcValueBindings,
					null,
					DEFAULT_RESTRICTOR,
					session
			);
		}

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// GraphBasedActionQueue / FlushCoordinator support
	//
	// IMPORTANT!!!
	//    	For whatever reason (polymorphism maybe?), benchmarking shows that splitting
	//		all of this out into delegates, while much cleaner code-wise, significantly
	//		hurts performance.  Go figure.

//	private OneToManyDecomposer decomposer;
	private JdbcOperationsSelector jdbcOperationsSelector;

	/// Accounts for differences in mutation handling between one-to-many collections
	/// targeting table-per-subclass (union) hierarchies and others.  Table-per-subclass
	/// targets need very different behavior since the target table changes depending
	/// on the actual entity instance.
	private interface JdbcOperationsSelector {
		CollectionJdbcOperations selectOperations(Object entity, SharedSessionContractImplementor session);
		CollectionJdbcOperations selectOperations(int subclassId);
		void forEachJdbcOperations(Consumer<CollectionJdbcOperations> consumer);
	}



	@Override
	public List<PlannedOperation> decompose(
			CollectionRecreateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		// NOTE : bundled operations are only valid for non-table-per-subclass associations...
		if ( shouldBundleCollectionOperations && !isAssociationTablePerSubclass ) {
			return planBundledRecreation( action, ordinalBase, postExecCallbackRegistry, session );
		}
		else {
			return planNonBundledRecreation( action, ordinalBase, postExecCallbackRegistry, session );
		}
	}

	private List<PlannedOperation> planBundledRecreation(
			CollectionRecreateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		var jdbcOperations = jdbcOperationsSelector.selectOperations( -1 );

		var collection = action.getCollection();
		var key = action.getKey();

		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = jdbcOperations.getInsertRowPlan();
		final CollectionJdbcOperations.UpdateRowPlan updateRowPlan = jdbcOperations.getUpdateRowPlan();

		if ( insertRowPlan == null && updateRowPlan == null ) {
			return List.of();
		}

		var operations = new ArrayList<PlannedOperation>();

		// Pre-insert callback once for the whole collection
		collection.preInsert( this );

		final var entries = collection.entries( this );
		if ( !entries.hasNext() ) {
			return List.of();
		}

		final List<BundledBindPlanEntry> entryList = new ArrayList<>();
		int entryCount = 0;

		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			boolean include = collection.includeInRecreate(
					entry,
					entryCount,
					collection,
					getAttributeMapping()
			);

			if ( include ) {
				entryList.add( new BundledBindPlanEntry( entry, entryCount ) );
			}

			entryCount++;
		}

		if ( !entryList.isEmpty() ) {
			if ( insertRowPlan != null ) {
				var bundledBindPlan = new BundledCollectionInsertBindPlan(
						insertRowPlan.values(),
						collection,
						action.getKey(),
						entryList
				);

				operations.add( new PlannedOperation(
						getCollectionTableDescriptor(),
						// actually an update...
						MutationKind.UPDATE,
						insertRowPlan.jdbcOperation(),
						bundledBindPlan,
						ordinalBase,
						"BundledInsertRows(" + getRolePath() + ")"
				) );
			}

			if ( updateRowPlan != null ) {
				var bundledBindPlan = new BundledCollectionUpdateBindPlan(
						collection,
						action.getKey(),
						updateRowPlan.values(),
						updateRowPlan.restrictions(),
						entryList
				);

				var  writeIndexPlannedOp = new PlannedOperation(
						jdbcOperations.getUpdateRowPlan().jdbcOperation().getTableDescriptor(),
						MutationKind.UPDATE,
						jdbcOperations.getUpdateRowPlan().jdbcOperation(),
						bundledBindPlan,
						ordinalBase * 2_000 + entryCount,
						"BundledWriteIndex[" + entryCount + "](" + getRolePath() + ")"
				);

				operations.add( writeIndexPlannedOp );
			}
		}

		// Only register callback if we actually have operations to execute
		if ( !operations.isEmpty() ) {
			final Object cacheKey = lockCacheItem( action, session );
			postExecCallbackRegistry.accept( new PostCollectionRecreateHandling( action, cacheKey ) );
		}

		return operations;
	}

	private List<PlannedOperation> planNonBundledRecreation(
			CollectionRecreateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		var attribute = getAttributeMapping();
		var collection = action.getCollection();
		var key = action.getKey();

		final var entries = collection.entries( this );
		if ( !entries.hasNext() ) {
			return List.of();
		}

		collection.preInsert( this );

		final List<PlannedOperation> operations = new ArrayList<>();

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			var jdbcOperations = jdbcOperationsSelector.selectOperations( entry, session );
			var insertRowPlan = jdbcOperations.getInsertRowPlan();

			// For inverse one-to-many collections, insertRowPlan will be null (inserts are managed by the owning side)
			if ( insertRowPlan != null && collection.includeInRecreate( entry, entryCount, collection, attribute ) ) {
				var bindPlan = new SingleRowInsertBindPlan(
						this,
						insertRowPlan.values(),
						collection,
						key,
						entry,
						entryCount
				);

				// For one-to-many collections, the "insert" is actually an UPDATE that sets the FK
				// Use MutationKind.UPDATE so it's ordered AFTER entity INSERTs via FK edges
				final PlannedOperation plannedOp = new PlannedOperation(
						insertRowPlan.jdbcOperation().getTableDescriptor(),
						// actually an update...
						MutationKind.UPDATE,
						insertRowPlan.jdbcOperation(),
						bindPlan,
						ordinalBase * 1_000 + entryCount,
						"InsertRow[" + entryCount + "](" + getRolePath() + ")"
				);

				operations.add( plannedOp );
			}

			if ( jdbcOperations.getUpdateRowPlan() != null ) {
				var writeIndexBindPlan = new SingleRowUpdateBindPlan(
						collection,
						key,
						entry,
						entryCount,
						jdbcOperations.getUpdateRowPlan().values(),
						jdbcOperations.getUpdateRowPlan().restrictions()
				);


				var  writeIndexPlannedOp = new PlannedOperation(
						jdbcOperations.getUpdateRowPlan().jdbcOperation().getTableDescriptor(),
						MutationKind.UPDATE,
						jdbcOperations.getUpdateRowPlan().jdbcOperation(),
						writeIndexBindPlan,
						ordinalBase * 2_000 + entryCount,
						"WriteIndex[" + entryCount + "](" + getRolePath() + ")"
				);

				//postExecCallbackRegistry.accept(... );
				// on recreate we should be able to write the index as a normal planned-op
				operations.add( writeIndexPlannedOp );
			}

			entryCount++;
		}

		// Only register callback if we actually have operations to execute
		if ( !operations.isEmpty() ) {
			final Object cacheKey = lockCacheItem( action, session );
			postExecCallbackRegistry.accept( new PostCollectionRecreateHandling( action, cacheKey ) );
		}

		return operations;
	}

	@Override
	public List<PlannedOperation> decompose(
			CollectionUpdateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		if ( !action.getCollection().wasInitialized() ) {
			return List.of();
		}

		if ( shouldBundleCollectionOperations && !isAssociationTablePerSubclass ) {
			return planBundledUpdate( action, ordinalBase, postExecCallbackRegistry, session );
		}
		else {
			return planNonBundledUpdate( action, ordinalBase, postExecCallbackRegistry, session );
		}
	}

	private List<PlannedOperation> planBundledUpdate(
			CollectionUpdateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		action.preUpdate();

		var collection = action.getCollection();
		var key = action.getKey();

		var operations = new ArrayList<PlannedOperation>();

		// DELETE removed entries
		applyBundledUpdateRemovals( collection, key, ordinalBase, session, operations::add );

		var jdbcOperations = jdbcOperationsSelector.selectOperations( -1, session );
		var updateRowPlan = jdbcOperations.getUpdateRowPlan();
		var insertRowPlan = jdbcOperations.getInsertRowPlan();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create bundles for changes and additions at the same time to save iterations
		// since they both iterate the same set of elements using on
		// PersistenceCollection.entires()
		var entries = collection.entries( this );

		if ( (updateRowPlan != null || insertRowPlan != null) && entries.hasNext() ) {
			var changeEntries = updateRowPlan == null ? null : new ArrayList<BundledBindPlanEntry>();
			var additionEntries = insertRowPlan == null ? null : new ArrayList<BundledBindPlanEntry>();
			int entryCount = 0;

			while ( entries.hasNext() ) {
				final Object entry = entries.next();

				var isAddition = collection.needsInserting( entry, entryCount, getElementType() );
				var isChange = collection.needsUpdating( entry, entryCount, getAttributeMapping() );

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
			applyBundledUpdateChanges( collection, key, ordinalBase + 1, changeEntries, updateRowPlan, operations::add );

			// INSERT entries
			applyBundledUpdateAdditions( collection, key, ordinalBase + 2, additionEntries, insertRowPlan, operations::add );
		}

		// Only register callback if we actually have operations to execute
		if ( !operations.isEmpty() ) {
			var cacheKey = lockCacheItem( action, session );
			postExecCallbackRegistry.accept( new PostCollectionUpdateHandling(
					this,
					collection,
					key,
					cacheKey,
					action.getAffectedOwner(),
					action.getAffectedOwnerId()
			) );
		}

		return operations;
	}

	private void applyBundledUpdateRemovals(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		var deleteRowPlan = jdbcOperationsSelector.selectOperations( -1 ).getDeleteRowPlan();
		final var deletes = collection.getDeletes( this, !hasPhysicalIndexColumn() );
		if ( deleteRowPlan == null || !deletes.hasNext() ) {
			MODEL_MUTATION_LOGGER.noRowsToDelete();
			return;
		}

		// Bundle all rows into a single PlannedOperation with a bundled BindPlan
		final List<Object> deletionList = new ArrayList<>();

		while ( deletes.hasNext() ) {
			deletionList.add( deletes.next() );
		}

		if ( !deletionList.isEmpty() ) {
			var bundledBindPlan = new BundledCollectionDeleteBindPlan(
					collection,
					key,
					deleteRowPlan.restrictions(),
					deletionList
			);

			operationConsumer.accept( new PlannedOperation(
					getCollectionTableDescriptor(),
					MutationKind.DELETE,
					deleteRowPlan.jdbcOperation(),
					bundledBindPlan,
					ordinalBase,
					"BundledDeleteRows(" + getRolePath() + ")"
			) );
		}
	}

	private void applyBundledUpdateChanges(
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
				getCollectionTableDescriptor(),
				MutationKind.UPDATE,
				updateRowPlan.jdbcOperation(),
				bundledBindPlan,
				ordinalBase,
				"BundledUpdateRows(" + getRolePath() + ")"
		) );
	}

	private void applyBundledUpdateAdditions(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			List<BundledBindPlanEntry> additionEntries,
			CollectionJdbcOperations.InsertRowPlan insertRowPlan,
			Consumer<PlannedOperation> operationConsumer) {
		if ( CollectionHelper.isEmpty( additionEntries ) ) {
			return;
		}

		collection.preInsert( this );

		final BindPlan bundledBindPlan = new BundledCollectionInsertBindPlan(
				insertRowPlan.values(),
				collection,
				key,
				additionEntries
		);

		operationConsumer.accept( new PlannedOperation(
				getCollectionTableDescriptor(),
				MutationKind.INSERT,
				insertRowPlan.jdbcOperation(),
				bundledBindPlan,
				ordinalBase,
				"BundledInsertRows(" + getRolePath() + ")"
		) );
	}

	private List<PlannedOperation> planNonBundledUpdate(
			CollectionUpdateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		action.preUpdate();

		var collection = action.getCollection();
		var key = action.getKey();

		var operations = new ArrayList<PlannedOperation>();

		// DELETE removed entries
		applyNonBundledUpdateRemovals( collection, key, ordinalBase, session, operations::add );

		// UPDATE modified entries
		applyNonBundledUpdateChanges( collection, key, ordinalBase + 1, session, operations::add );

		// INSERT entries
		applyNonBundledUpdateAdditions( collection, key, ordinalBase + 2, session, operations::add );

		// Only register callback if we actually have operations to execute
		if ( !operations.isEmpty() ) {
			var cacheKey = lockCacheItem( action, session );
			postExecCallbackRegistry.accept( new PostCollectionUpdateHandling(
					this,
					collection,
					key,
					cacheKey,
					action.getAffectedOwner(),
					action.getAffectedOwnerId()
			) );
		}

		return operations;
	}

	private void applyNonBundledUpdateRemovals(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		if ( !needsRemove() ) {
			// EARLY EXIT!!
			return;
		}

		final var deletes = collection.getDeletes( this, !hasPhysicalIndexColumn() );
		if ( !deletes.hasNext() ) {
			MODEL_MUTATION_LOGGER.noRowsToDelete();
			// EARLY EXIT!!
			return;
		}

		int deletionCount = 0;
		while ( deletes.hasNext() ) {
			var removal = deletes.next();

			var jdbcOperations = jdbcOperationsSelector.selectOperations( removal, session );
			assert jdbcOperations != null;
			var deleteRowPlan = jdbcOperations.getDeleteRowPlan();

			final BindPlan bindPlan = new SingleRowDeleteBindPlan(
					collection,
					key,
					removal,
					deleteRowPlan.restrictions()
			);

			final PlannedOperation plannedOp = new PlannedOperation(
					deleteRowPlan.jdbcOperation().getTableDescriptor(),
					// actually an update...
					MutationKind.UPDATE,
					deleteRowPlan.jdbcOperation(),
					bindPlan,
					ordinalBase * 1_000 + deletionCount,
					"DeleteRow[" + deletionCount + "](" + getRole() + ")"
			);

			operationConsumer.accept( plannedOp );
			deletionCount++;
		}
	}

	private void applyNonBundledUpdateChanges(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		if ( !isDoWriteEvenWhenInverse() ) {
			// EARLY EXIT!!
			return;
		}

		final var entries = collection.entries( this );

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();

			var jdbcOperations = jdbcOperationsSelector.selectOperations( entry, session );
			assert jdbcOperations != null;
			var updateRowPlan = jdbcOperations.getUpdateRowPlan();

			// For inverse collections, updateRowPlan will be null
			if ( updateRowPlan != null && collection.needsUpdating( entry, entryCount, getAttributeMapping() ) ) {
				final BindPlan bindPlan = new SingleRowUpdateBindPlan(
						collection,
						key,
						entry,
						entryCount,
						updateRowPlan.values(),
						updateRowPlan.restrictions()
				);

				final PlannedOperation plannedOp = new PlannedOperation(
						updateRowPlan.jdbcOperation().getTableDescriptor(),
						MutationKind.UPDATE,
						updateRowPlan.jdbcOperation(),
						bindPlan,
						ordinalBase * 1_000 + entryCount,
						"UpdateRow[" + entryCount + "](" + getRolePath() + ")"
				);

				operationConsumer.accept( plannedOp );
			}

			entryCount++;
		}
	}

	private void applyNonBundledUpdateAdditions(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		if ( !isRowInsertEnabled() ) {
			// EARLY EXIT!!
			return;
		}

		final var entries = collection.entries( this );
		if ( !entries.hasNext() ) {
			MODEL_MUTATION_LOGGER.noCollectionRowsToInsert( getRolePath(), key );
			return;
		}

		collection.preInsert( this );

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			if ( collection.includeInInsert( entry, entryCount, collection, getAttributeMapping() ) ) {

				var jdbcOperations = jdbcOperationsSelector.selectOperations( entry, session );
				assert jdbcOperations != null;
				var insertRowPlan = jdbcOperations.getInsertRowPlan();

				// For inverse one-to-many collections, insertRowPlan will be null
				// (inserts are managed by the owning side)
				if ( insertRowPlan != null ) {
					final BindPlan bindPlan = new SingleRowInsertBindPlan(
							this,
							insertRowPlan.values(),
							collection,
							key,
							entry,
							entryCount
					);

					// For one-to-many collections, the "insert" is actually an UPDATE that sets the FK
					// Use MutationKind.UPDATE so it's ordered AFTER entity INSERTs via FK edges
					final PlannedOperation plannedOp = new PlannedOperation(
							insertRowPlan.jdbcOperation().getTableDescriptor(),
							// actually an update...
							MutationKind.UPDATE,
							insertRowPlan.jdbcOperation(),
							bindPlan,
							ordinalBase * 1_000 + entryCount,
							"InsertRow[" + entryCount + "](" + getRolePath() + ")"
					);

					operationConsumer.accept( plannedOp );
				}
			}

			entryCount++;
		}

		MODEL_MUTATION_LOGGER.doneInsertingCollectionRows( entryCount, getRolePath() );
	}

	@Override
	public List<PlannedOperation> decompose(
			CollectionRemoveAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		if ( !needsRemove() ) {
			// EARLY EXIT!!
			return List.of();
		}

		if ( isAssociationTablePerSubclass ) {
			var operations = new ArrayList<PlannedOperation>();
			jdbcOperationsSelector.forEachJdbcOperations( (jdbcOperations) -> {
				var removeOperation = jdbcOperations.getRemoveOperation();
				operations.add( new PlannedOperation(
						removeOperation.getTableDescriptor(),
						// actually an update...
						MutationKind.UPDATE,
						removeOperation,
						new RemoveBindPlan( action.getKey(), this ),
						ordinalBase * 1_000,
						"RemoveAllRows(" + getRolePath() + ")"
				) );
			} );

			// Only register callback if we actually have operations to execute
			if ( !operations.isEmpty() ) {
				final Object cacheKey = lockCacheItem( action, session );
				postExecCallbackRegistry.accept( new PostCollectionRemoveHandling( action, cacheKey ) );
			}

			return operations;
		}
		else {
			var jdbcOperations = jdbcOperationsSelector.selectOperations( -1 );
			var removeOperation = jdbcOperations.getRemoveOperation();
			if ( removeOperation == null ) {
				// EARLY EXIT!!
				return List.of();
			}

			var operations = List.of( new PlannedOperation(
					removeOperation.getTableDescriptor(),
					// actually an update...
					MutationKind.UPDATE,
					removeOperation,
					new RemoveBindPlan( action.getKey(), this ),
					ordinalBase * 1_000,
					"RemoveAllRows(" + getRolePath() + ")"
			) );

			// Only register callback if we actually have operations to execute
			if ( !operations.isEmpty() ) {
				final Object cacheKey = lockCacheItem( action, session );
				postExecCallbackRegistry.accept( new PostCollectionRemoveHandling( action, cacheKey ) );
			}

			return operations;
		}
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


	private class StandardJdbcOperationsSelector implements JdbcOperationsSelector {
		private final CollectionJdbcOperations jdbcOperations;

		private StandardJdbcOperationsSelector() {
			jdbcOperations = buildJdbcOperations( getCollectionTableDescriptor() );
		}

		@Override
		public CollectionJdbcOperations selectOperations(Object entity, SharedSessionContractImplementor session) {
			return jdbcOperations;
		}

		@Override
		public CollectionJdbcOperations selectOperations(int subclassId) {
			return jdbcOperations;
		}

		@Override
		public void forEachJdbcOperations(Consumer<CollectionJdbcOperations> consumer) {
			consumer.accept( jdbcOperations );
		}
	}

	private class TablePerSubclassJdbcOperationsSelector implements JdbcOperationsSelector {
		private final CollectionJdbcOperations[] operationsBySubclassId;

		public TablePerSubclassJdbcOperationsSelector() {
			var subclassMappings = ( (EntityCollectionPart) getAttributeMapping().getElementDescriptor() )
					.getAssociatedEntityMappingType()
					.getRootEntityDescriptor()
					.getSubMappingTypes();
			operationsBySubclassId = new CollectionJdbcOperations[subclassMappings.size()];

			subclassMappings.forEach( (subclassMapping) -> {
				var tableDescriptor = subclassMapping.getEntityPersister().getIdentifierTableDescriptor();
				operationsBySubclassId[subclassMapping.getSubclassId()] = buildJdbcOperations( tableDescriptor );
			} );
		}

		@Override
		public CollectionJdbcOperations selectOperations(Object entity, SharedSessionContractImplementor session) {
			final var entityEntry = session.getPersistenceContextInternal().getEntry( entity );
			final int subclassId = entityEntry.getPersister().getSubclassId();
			return operationsBySubclassId[subclassId];
		}

		@Override
		public CollectionJdbcOperations selectOperations(int subclassId) {
			return operationsBySubclassId[subclassId];
		}

		@Override
		public void forEachJdbcOperations(Consumer<CollectionJdbcOperations> consumer) {
			for ( int i = 0; i < operationsBySubclassId.length; i++ ) {
				consumer.accept( operationsBySubclassId[i] );
			}
		}
	}

	private CollectionJdbcOperations buildJdbcOperations(TableDescriptor tableDescriptor) {
		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = buildInsertRowPlan( tableDescriptor );
		final CollectionJdbcOperations.UpdateRowPlan updateRowPlan = buildUpdateRowPlan( tableDescriptor );
		final CollectionJdbcOperations.DeleteRowPlan deleteRowPlan = buildDeleteRowPlan( tableDescriptor );

		return new CollectionJdbcOperations(
				this,
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
		if ( isInverse() || !isRowInsertEnabled() ) {
			return null;
		}

		var updateBuilder = new GraphTableUpdateBuilderStandard(
				this,
				tableDescriptor,
				getSqlWhereString(),
				getFactory()
		);

		final var attributeMapping = getAttributeMapping();
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
			org.hibernate.action.queue.bind.JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				keyValue,
				jdbcValueBindings::bindAssignment,
				session
		);
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.decompose(
					incrementIndexByBase( collection.getIndex( rowValue, rowPosition, this ) ),
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
		if ( !isDoWriteEvenWhenInverse() ) {
			return null;
		}

		final var updateBuilder = new GraphTableUpdateBuilderStandard(
				this,
				collectionTableDescriptor,
				getSqlWhereString(),
				getFactory()
		);

		final var attributeMapping = getAttributeMapping();

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
			org.hibernate.action.queue.bind.JdbcValueBindings jdbcValueBindings) {
		getAttributeMapping().getIndexDescriptor().decompose(
				collection.getIndex( entry, entryPosition, this ),
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
			org.hibernate.action.queue.bind.JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = getAttributeMapping();
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
		if ( !needsRemove() ) {
			return null;
		}

		final var updateBuilder = new CollectionRowDeleteByUpdateSetNullBuilder(
				this,
				collectionTableDescriptor,
				getSqlWhereString(),
				getFactory()
		);

		final var foreignKeyDescriptor = getAttributeMapping().getKeyDescriptor();
		foreignKeyDescriptor.getKeyPart().forEachUpdatable( (index, selectableMapping) -> {
			// set null
			updateBuilder.addValueColumn( NULL, selectableMapping );
		} );

		// set the value for each index column to null
		if ( hasPhysicalIndexColumn() ) {
			final var indexDescriptor = getAttributeMapping().getIndexDescriptor();
			assert indexDescriptor != null;
			indexDescriptor.forEachUpdatable( (index, selectableMapping) -> {
				updateBuilder.addValueColumn( NULL, selectableMapping );
			} );
		}

		// for one-to-many, we know the element is an entity and need to restrict the update
		// based on the element's id
		final var entityPart = (EntityCollectionPart) getAttributeMapping().getElementDescriptor();
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
			org.hibernate.action.queue.bind.JdbcValueBindings jdbcValueBindings) {
		final var entityPart = (EntityCollectionPart) getAttributeMapping().getElementDescriptor();
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
				this,
				tableDescriptor,
				getSqlWhereString(),
				getFactory()
		);

		final var attributeMapping = getAttributeMapping();
		assert attributeMapping != null;

		final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
		assert foreignKeyDescriptor != null;

		foreignKeyDescriptor.getKeyPart().forEachSelectable( (selectionIndex, selectableMapping) -> {
			builder.addValueColumn( NULL, selectableMapping );
			builder.addKeyRestriction( selectableMapping );
		} );

		if ( hasPhysicalIndexColumn() ) {
			attributeMapping.getIndexDescriptor().forEachColumn( (selectionIndex, selectableMapping) -> {
				if ( selectableMapping.isUpdateable() ) {
					builder.addValueColumn( NULL, selectableMapping );
				}
			} );
		}

		return builder.buildMutation().createMutationOperation();
	}
}
