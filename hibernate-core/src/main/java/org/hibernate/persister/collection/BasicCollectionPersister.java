/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.exec.ExecutionContext;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableDeleteBuilderStandard;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableInsertBuilderStandard;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableUpdateBuilderStandard;
import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.mutation.BundledBindPlanEntry;
import org.hibernate.persister.collection.mutation.BundledCollectionDeleteBindPlan;
import org.hibernate.persister.collection.mutation.BundledCollectionInsertBindPlan;
import org.hibernate.persister.collection.mutation.BundledCollectionUpdateBindPlan;
import org.hibernate.persister.collection.mutation.CollectionJdbcOperations;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
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
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorStandard;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.persister.filter.internal.StaticFilterAliasGenerator;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameterList;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.builder.CollectionRowDeleteBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.TableUpdateStandard;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;
import org.hibernate.type.EntityType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static org.hibernate.temporal.TemporalTableStrategy.NATIVE;
import static org.hibernate.temporal.TemporalTableStrategy.SINGLE_TABLE;
import static org.hibernate.internal.util.collections.ArrayHelper.isAnyTrue;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.persister.collection.mutation.RowMutationOperations.DEFAULT_RESTRICTOR;
import static org.hibernate.persister.collection.mutation.RowMutationOperations.DEFAULT_VALUE_SETTER;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * A {@link CollectionPersister} for {@linkplain jakarta.persistence.ElementCollection
 * collections of values} and {@linkplain jakarta.persistence.ManyToMany many-to-many
 * associations}.
 *
 * @see OneToManyPersister
 *
 * @author Gavin King
 */
@Internal
public class BasicCollectionPersister extends AbstractCollectionPersister {
	private final RowMutationOperations rowMutationOperations;

//	private BasicCollectionDecomposer decomposer;
	private CollectionJdbcOperations jdbcOperations;

	private final InsertRowsCoordinator insertRowsCoordinator;
	private final UpdateRowsCoordinator updateCoordinator;
	private final DeleteRowsCoordinator deleteRowsCoordinator;
	private final RemoveCoordinator removeCoordinator;

	public BasicCollectionPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			RuntimeModelCreationContext creationContext)
					throws MappingException, CacheException {
		super( collectionBinding, cacheAccessStrategy, creationContext );

		this.rowMutationOperations = buildRowMutationOperations();

final var stateManagement = collectionBinding.getStateManagement();
		this.insertRowsCoordinator = stateManagement.createInsertRowsCoordinator( this );
		this.updateCoordinator = stateManagement.createUpdateRowsCoordinator( this );
		this.deleteRowsCoordinator = stateManagement.createDeleteRowsCoordinator( this );
		this.removeCoordinator = stateManagement.createRemoveCoordinator( this );
	}

	@Override
	public void prepareMappingModel(MappingModelCreationProcess creationProcess) {
		super.prepareMappingModel( creationProcess );
	}

	@Override
	public void postInstantiate() throws MappingException {
		super.postInstantiate();

		// Build JDBC operations after collectionTableDescriptor is initialized
//		decomposer = new BasicCollectionDecomposer( this, getFactory() );
		jdbcOperations = buildJdbcOperations( getFactory() );
	}

	public RowMutationOperations getRowMutationOperations() {
		return rowMutationOperations;
	}

	public InsertRowsCoordinator getCreateEntryCoordinator() {
		return insertRowsCoordinator;
	}

	public InsertRowsCoordinator getInsertRowsCoordinator() {
		return insertRowsCoordinator;
	}
	@Override
	public void recreate(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session) {
		getCreateEntryCoordinator().insertRows( collection, id, collection::includeInRecreate, session );
	}

	@Override
	public void insertRows(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session)
			throws HibernateException {
		getCreateEntryCoordinator().insertRows( collection, id, collection::includeInInsert, session );
	}

	public UpdateRowsCoordinator getUpdateEntryCoordinator() {
		return updateCoordinator;
	}

	public UpdateRowsCoordinator getUpdateRowsCoordinator() {
		return updateCoordinator;
	}

	@Override
	public void updateRows(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session) {
		getUpdateEntryCoordinator().updateRows( id, collection, session );
	}

	public DeleteRowsCoordinator getRemoveEntryCoordinator() {
		return deleteRowsCoordinator;
	}

	public DeleteRowsCoordinator getDeleteRowsCoordinator() {
		return deleteRowsCoordinator;
	}

	@Override
	public void deleteRows(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session) {
		getRemoveEntryCoordinator().deleteRows( collection, id, session );
	}

	@Override
	public RemoveCoordinator getRemoveCoordinator() {
		return removeCoordinator;
	}

	@Override
	protected void doProcessQueuedOps(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session) {
		// nothing to do
	}

	public boolean isPerformingUpdates() {
		return !isInverse()
			&& getCollectionSemantics().getCollectionClassification().isRowUpdatePossible()
			&& isAnyTrue( elementColumnIsSettable );
	}

	@Override
	public RestrictedTableMutation<JdbcMutationOperation> generateDeleteAllAst(MutatingTableReference tableReference) {
		final var attributeMapping = getAttributeMapping();
		assert attributeMapping != null;
		final var temporalMapping = attributeMapping.getTemporalMapping();
		if ( temporalMapping != null && shouldApplyTemporalOperations( tableReference ) ) {
			return generateTemporalDeleteAllAst( tableReference );
		}
		final var softDeleteMapping = attributeMapping.getSoftDeleteMapping();
		if ( softDeleteMapping == null ) {
			return super.generateDeleteAllAst( tableReference );
		}
		else {
			final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
			assert foreignKeyDescriptor != null;
			final int keyColumnCount = foreignKeyDescriptor.getJdbcTypeCount();
			final var parameterBinders =
					new ColumnValueParameterList( tableReference, ParameterUsage.RESTRICT, keyColumnCount );
			final List<ColumnValueBinding> restrictionBindings = arrayList( keyColumnCount );
			applyKeyRestrictions( parameterBinders, restrictionBindings );
			final var softDeleteColumn = new ColumnReference( tableReference, softDeleteMapping );
			final var nonDeletedBinding = softDeleteMapping.createNonDeletedValueBinding( softDeleteColumn );
			final var deletedBinding = softDeleteMapping.createDeletedValueBinding( softDeleteColumn );
			return new TableUpdateStandard(
					tableReference,
					this,
					"soft-delete removal",
					List.of( deletedBinding ),
					restrictionBindings,
					List.of( nonDeletedBinding )
			);
		}
	}

	protected RestrictedTableMutation<JdbcMutationOperation> generateTemporalDeleteAllAst(MutatingTableReference tableReference) {
		final var attributeMapping = getAttributeMapping();
		final var temporalMapping = attributeMapping.getTemporalMapping();
		assert temporalMapping != null;
		final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
		assert foreignKeyDescriptor != null;
		final int keyColumnCount = foreignKeyDescriptor.getJdbcTypeCount();
		final var parameterBinders =
				new ColumnValueParameterList( tableReference, ParameterUsage.RESTRICT, keyColumnCount );
		final List<ColumnValueBinding> restrictionBindings = arrayList( keyColumnCount );
		applyKeyRestrictions( parameterBinders, restrictionBindings );
		final var endingColumn = new ColumnReference( tableReference, temporalMapping.getEndingColumnMapping() );
		final var endingBinding = temporalMapping.createEndingValueBinding( endingColumn );
		final var nullEndingBinding = temporalMapping.createNullEndingValueBinding( endingColumn );
		return new TableUpdateStandard(
				tableReference,
				this,
				"temporal removal",
				List.of( endingBinding ),
				restrictionBindings,
				List.of( nullEndingBinding )
		);
	}

	protected RowMutationOperations buildRowMutationOperations() {
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

		final OperationProducer updateRowOperationProducer;
		final RowMutationOperations.Values updateRowValues;
		final RowMutationOperations.Restrictions updateRowRestrictions;
		if ( isPerformingUpdates() ) {
			updateRowOperationProducer = this::generateUpdateRowOperation;
			updateRowValues = this::applyUpdateRowValues;
			updateRowRestrictions = this::applyUpdateRowRestrictions;
		}
		else {
			updateRowOperationProducer = null;
			updateRowValues = null;
			updateRowRestrictions = null;
		}


		final OperationProducer deleteRowOperationProducer;
		final RowMutationOperations.Restrictions deleteRowRestrictions;
		if ( !isInverse() && isRowDeleteEnabled() ) {
			deleteRowOperationProducer = this::generateDeleteRowOperation;
			deleteRowRestrictions = this::applyDeleteRowRestrictions;
		}
		else {
			deleteRowOperationProducer = null;
			deleteRowRestrictions = null;
		}

		final OperationProducer deleteAllRowsOperationProducer;
		if ( !isInverse() && isRowDeleteEnabled() ) {
			deleteAllRowsOperationProducer = this::buildDeleteAllOperation;
		}
		else {
			deleteAllRowsOperationProducer = null;
		}

		return new RowMutationOperations(
				this,
				insertRowOperationProducer,
				insertRowValues,
				updateRowOperationProducer,
				updateRowValues,
				updateRowRestrictions,
				deleteRowOperationProducer,
				deleteRowRestrictions,
				deleteAllRowsOperationProducer
		);
	}




	private JdbcMutationOperation generateInsertRowOperation(MutatingTableReference tableReference) {
		return getIdentifierTableMapping().getInsertDetails().getCustomSql() != null
				? buildCustomSqlInsertRowOperation( tableReference )
				: buildGeneratedInsertRowOperation( tableReference );

	}

	private JdbcMutationOperation buildCustomSqlInsertRowOperation(MutatingTableReference tableReference) {
		final var factory = getFactory();
		final var insertBuilder = new TableInsertBuilderStandard( this, tableReference, factory );
		applyInsertDetails( insertBuilder );
		return insertBuilder.buildMutation().createMutationOperation( null, factory );
	}

	private void applyInsertDetails(TableInsertBuilderStandard insertBuilder) {
		final var attributeMapping = getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().forEachSelectable( insertBuilder );
		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.forEachSelectable( insertBuilder );
		}
		else if ( indexDescriptor != null ) {
			indexDescriptor.forEachInsertable( insertBuilder );
		}
		attributeMapping.getElementDescriptor().forEachInsertable( insertBuilder );
		final var softDeleteMapping = attributeMapping.getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			final var columnReference = new ColumnReference( insertBuilder.getMutatingTable(), softDeleteMapping );
			insertBuilder.addValueColumn( softDeleteMapping.createNonDeletedValueBinding( columnReference ) );
		}
		final var temporalMapping = attributeMapping.getTemporalMapping();
		if ( temporalMapping != null && shouldApplyTemporalOperations( insertBuilder.getMutatingTable() ) ) {
			final var startingColumnReference =
					new ColumnReference( insertBuilder.getMutatingTable(), temporalMapping.getStartingColumnMapping() );
			insertBuilder.addValueColumn( temporalMapping.createStartingValueBinding( startingColumnReference ) );
			final var endingColumnReference =
					new ColumnReference( insertBuilder.getMutatingTable(), temporalMapping.getEndingColumnMapping() );
			insertBuilder.addValueColumn( temporalMapping.createNullEndingValueBinding( endingColumnReference ) );
		}
	}

	private JdbcMutationOperation buildGeneratedInsertRowOperation(MutatingTableReference tableReference) {
		return getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( generateInsertRowAst( tableReference ), getFactory() )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	private TableMutation<JdbcMutationOperation> generateInsertRowAst(MutatingTableReference tableReference) {
		final var pluralAttribute = getAttributeMapping();
		assert pluralAttribute != null;
		final var foreignKeyDescriptor = pluralAttribute.getKeyDescriptor();
		assert foreignKeyDescriptor != null;
		final var insertBuilder = new TableInsertBuilderStandard( this, tableReference, getFactory() );
		applyInsertDetails( insertBuilder );
		//noinspection unchecked,rawtypes
		return (TableMutation) insertBuilder.buildMutation();
	}

	private void applyInsertRowValues(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + getNavigableRole().getFullPath() );
		}
		final var attributeMapping = getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				key,
				0,
				jdbcValueBindings,
				null,
				DEFAULT_VALUE_SETTER,
				session
		);

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					collection.getIdentifier( rowValue, rowPosition ),
					0,
					jdbcValueBindings,
					null,
					DEFAULT_VALUE_SETTER,
					session
			);
		}
		else {
			final var indexDescriptor = attributeMapping.getIndexDescriptor();
			if ( indexDescriptor != null ) {
				// todo (mutation) : this would be more efficient if we exposed the "containing table"
				//		per value-mapping model-parts which is what we effectively support anyway.
				//
				// this would need to kind of like a union of ModelPart and ValueMapping, except:
				// 		1) not the managed-type structure from ModelPart
				//		2) not BasicType from ValueMapping
				//	essentially any basic or composite mapping of column(s)
				indexDescriptor.decompose(
						incrementIndexByBase( collection.getIndex( rowValue, rowPosition, this ) ),
						0,
						indexColumnIsSettable,
						jdbcValueBindings,
						(valueIndex, settable, bindings, jdbcValue, jdbcValueMapping) -> {
							if ( settable[valueIndex]
									&& jdbcValueMapping.getContainingTableExpression()
											.equals( getTableName() ) ) {
								bindings.bindValue( jdbcValue, jdbcValueMapping, ParameterUsage.SET );
							}
							// otherwise a many-to-many mapping and the index is defined
							// on the associated entity table - we skip it here
						},
						session
				);
			}
		}

		attributeMapping.getElementDescriptor().decompose(
				collection.getElement( rowValue ),
				0,
				elementColumnIsSettable,
				jdbcValueBindings,
				(valueIndex, settable, bindings, jdbcValue, jdbcValueMapping) -> {
					if ( settable[valueIndex] ) {
						bindings.bindValue( jdbcValue, jdbcValueMapping, ParameterUsage.SET );
					}
				},
				session
		);

		final var temporalMapping = attributeMapping.getTemporalMapping();
		if ( temporalMapping != null && isUsingTransactionIdParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getCurrentTransactionIdentifier(),
					temporalMapping.getStartingColumnMapping(),
					ParameterUsage.SET
			);
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Update handling

	private JdbcMutationOperation generateUpdateRowOperation(MutatingTableReference tableReference) {
		return getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( generateUpdateRowAst( tableReference ), getFactory() )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	private RestrictedTableMutation<JdbcMutationOperation> generateUpdateRowAst(MutatingTableReference tableReference) {
		final var attribute = getAttributeMapping();
		assert attribute != null;

		// note that custom SQL update row details are handled by TableUpdateBuilderStandard
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				this,
				tableReference,
				getFactory(),
				sqlWhereString
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SET

		attribute.getElementDescriptor().forEachUpdatable( updateBuilder );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// WHERE

		final var identifierDescriptor = attribute.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			updateBuilder.addKeyRestrictionsLeniently( identifierDescriptor );
		}
		else {
			updateBuilder.addKeyRestrictionsLeniently( attribute.getKeyDescriptor().getKeyPart() );
			final var indexDescriptor = attribute.getIndexDescriptor();
// DISABLED: 			if ( indexDescriptor != null && !indexContainsFormula ) {
// DISABLED: 				updateBuilder.addKeyRestrictionsLeniently( indexDescriptor );
// DISABLED: 			}
// DISABLED: 			else {
// DISABLED: 				updateBuilder.addKeyRestrictions( attribute.getElementDescriptor() );
// DISABLED: 			}
			// Always use element descriptor for restriction (not index)
			updateBuilder.addKeyRestrictions( attribute.getElementDescriptor() );
		}

		//noinspection unchecked,rawtypes
		return (RestrictedTableMutation) updateBuilder.buildMutation();
	}

	private void applyUpdateRowValues(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		getAttributeMapping().getElementDescriptor().decompose(
				collection.getElement( entry ),
				0,
				jdbcValueBindings,
				null,
				(valueIndex, bindings, y, jdbcValue, jdbcValueMapping) -> {
					if ( jdbcValueMapping.isUpdateable() && !jdbcValueMapping.isFormula() ) {
						bindings.bindValue( jdbcValue, jdbcValueMapping, ParameterUsage.SET );
					}
				},
				session
		);
	}

	private void applyUpdateRowRestrictions(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = getAttributeMapping();
		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					collection.getIdentifier( entry, entryPosition ),
					jdbcValueBindings::bindRestriction,
					session
			);
		}
		else {
			attributeMapping.getKeyDescriptor().getKeyPart().decompose(
					key,
					jdbcValueBindings::bindRestriction,
					session
			);

			final var indexDescriptor = attributeMapping.getIndexDescriptor();
		// For @OrderColumn updates, do NOT include index in WHERE clause
		// The index is what we're SETTING, not part of row identification
		// Row is uniquely identified by element and parent key
		// Including index fails for newly inserted rows where it's NULL
			if ( false && indexDescriptor != null && !indexContainsFormula ) { // DISABLED - see comment above
				// For WriteIndexCoordinator updates, we need to know if this row
				// existed before or was just inserted
				// - For existing rows: include the old index in WHERE clause
				// - For newly inserted rows: the index column is NULL, don't include in WHERE
				final Serializable snapshot = collection.getStoredSnapshot();
				final boolean includeIndexInWhere;

				if ( snapshot == null || (snapshot instanceof List && ((List<?>) snapshot).isEmpty()) ) {
					// Empty or null snapshot means this is a newly created collection
					// All rows were just inserted with NULL for the index column
					includeIndexInWhere = false;
				}
				else if ( snapshot instanceof List ) {
					// For Lists, check if this entry position existed in the snapshot
					// If position is beyond snapshot size, it's a newly inserted element
					includeIndexInWhere = entryPosition < ((List<?>) snapshot).size();
				}
				else {
					// For other indexed collections, include index if snapshot exists
					includeIndexInWhere = true;
				}
				if ( includeIndexInWhere ) {
					// This is an existing row - include the old index in WHERE clause
					// For Lists, the old index is the element's position in the snapshot
					indexDescriptor.decompose(
							incrementIndexByBase( entryPosition ),
							0,
							jdbcValueBindings,
							null,
							DEFAULT_RESTRICTOR,
							session
					);
				}
				// If newly inserted, don't add index to WHERE clause
				// The index column is still NULL in the database
			}
			else {
				attributeMapping.getElementDescriptor().decompose(
						collection.getSnapshotElement( entry, entryPosition ),
						0,
						jdbcValueBindings,
						null,
						(valueIndex, bindings, noop, jdbcValue, jdbcValueMapping) -> {
							if ( !jdbcValueMapping.isNullable() && !jdbcValueMapping.isFormula() ) {
								bindings.bindValue( jdbcValue, jdbcValueMapping, ParameterUsage.RESTRICT );
							}
						},
						session
				);
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Delete handling

	private JdbcMutationOperation generateDeleteRowOperation(MutatingTableReference tableReference) {
		return getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( generateDeleteRowAst( tableReference ), getFactory() )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	private RestrictedTableMutation<JdbcMutationOperation> generateDeleteRowAst(MutatingTableReference tableReference) {
		final var pluralAttribute = getAttributeMapping();
		assert pluralAttribute != null;
		final var temporalMapping = pluralAttribute.getTemporalMapping();
		if ( temporalMapping != null && shouldApplyTemporalOperations( tableReference ) ) {
			return generateTemporalDeleteRowsAst( tableReference );
		}
		final var softDeleteMapping = pluralAttribute.getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			return generateSoftDeleteRowsAst( tableReference );
		}
		else {
			final var foreignKeyDescriptor = pluralAttribute.getKeyDescriptor();
			assert foreignKeyDescriptor != null;
			// note that custom SQL delete row details are handled by CollectionRowDeleteBuilder
			final var deleteBuilder = new CollectionRowDeleteBuilder(
					this,
					tableReference,
					getFactory(),
					sqlWhereString
			);
			final var identifierDescriptor = pluralAttribute.getIdentifierDescriptor();
			if ( identifierDescriptor != null ) {
				deleteBuilder.addKeyRestrictionsLeniently( identifierDescriptor );
			}
			else {
				deleteBuilder.addKeyRestrictionsLeniently( foreignKeyDescriptor.getKeyPart() );
				if ( hasIndex() && !indexContainsFormula ) {
					assert pluralAttribute.getIndexDescriptor() != null;
					deleteBuilder.addKeyRestrictionsLeniently( pluralAttribute.getIndexDescriptor() );
				}
				else {
					deleteBuilder.addKeyRestrictions( pluralAttribute.getElementDescriptor() );
				}
			}
			//noinspection unchecked,rawtypes
			return (RestrictedTableMutation) deleteBuilder.buildMutation();
		}
	}

	protected RestrictedTableMutation<JdbcMutationOperation> generateSoftDeleteRowsAst(MutatingTableReference tableReference) {
		final var attributeMapping = getAttributeMapping();
		final var softDeleteMapping = attributeMapping.getSoftDeleteMapping();
		assert softDeleteMapping != null;
		final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
		assert foreignKeyDescriptor != null;
		final TableUpdateBuilderStandard<JdbcMutationOperation> updateBuilder = new TableUpdateBuilderStandard<>(
				this,
				tableReference,
				getFactory(),
				sqlWhereString
		);
		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			updateBuilder.addKeyRestrictionsLeniently( identifierDescriptor );
		}
		else {
			updateBuilder.addKeyRestrictionsLeniently( foreignKeyDescriptor.getKeyPart() );
			if ( hasIndex() && !indexContainsFormula ) {
				assert attributeMapping.getIndexDescriptor() != null;
				updateBuilder.addKeyRestrictionsLeniently( attributeMapping.getIndexDescriptor() );
			}
			else {
				updateBuilder.addKeyRestrictions( attributeMapping.getElementDescriptor() );
			}
		}

		final var softDeleteColumnReference = new ColumnReference( tableReference, softDeleteMapping );
		// apply the assignment
		updateBuilder.addValueColumn( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );
		// apply the restriction
		updateBuilder.addNonKeyRestriction( softDeleteMapping.createNonDeletedValueBinding( softDeleteColumnReference ) );
		return updateBuilder.buildMutation();
	}

	protected RestrictedTableMutation<JdbcMutationOperation> generateTemporalDeleteRowsAst(MutatingTableReference tableReference) {
		final var attributeMapping = getAttributeMapping();
		final var temporalMapping = attributeMapping.getTemporalMapping();
		assert temporalMapping != null;
		final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
		assert foreignKeyDescriptor != null;
		final TableUpdateBuilderStandard<JdbcMutationOperation> updateBuilder = new TableUpdateBuilderStandard<>(
				this,
				tableReference,
				getFactory(),
				sqlWhereString
		);
		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			updateBuilder.addKeyRestrictionsLeniently( identifierDescriptor );
		}
		else {
			updateBuilder.addKeyRestrictionsLeniently( foreignKeyDescriptor.getKeyPart() );
			if ( hasIndex() && !indexContainsFormula ) {
				assert attributeMapping.getIndexDescriptor() != null;
				updateBuilder.addKeyRestrictionsLeniently( attributeMapping.getIndexDescriptor() );
			}
			else {
				updateBuilder.addKeyRestrictions( attributeMapping.getElementDescriptor() );
			}
		}

		final var endingColumnReference = new ColumnReference( tableReference, temporalMapping.getEndingColumnMapping() );
		updateBuilder.addValueColumn( temporalMapping.createEndingValueBinding( endingColumnReference ) );
		updateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumnReference ) );
		return updateBuilder.buildMutation();
	}

	private void applyDeleteRowRestrictions(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = getAttributeMapping();
		final var temporalMapping = attributeMapping.getTemporalMapping();
		if ( temporalMapping != null && isUsingTransactionIdParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getCurrentTransactionIdentifier(),
					temporalMapping.getEndingColumnMapping(),
					ParameterUsage.SET
			);
		}
		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					rowValue,
					0,
					jdbcValueBindings,
					null,
					DEFAULT_RESTRICTOR,
					session
			);
		}
		else {
			attributeMapping.getKeyDescriptor().getKeyPart().decompose(
					keyValue,
					0,
					jdbcValueBindings,
					null,
					DEFAULT_RESTRICTOR,
					session
			);
			if ( hasPhysicalIndexColumn() ) {
				attributeMapping.getIndexDescriptor().decompose(
						incrementIndexByBase( rowValue ),
						0,
						jdbcValueBindings,
						null,
						DEFAULT_RESTRICTOR,
						session
				);
			}
			else {
				attributeMapping.getElementDescriptor().decompose(
						rowValue,
						0,
						jdbcValueBindings,
						null,
						(valueIndex, bindings, noop, jdbcValue, jdbcValueMapping) -> {
							if ( !jdbcValueMapping.isNullable() && !jdbcValueMapping.isFormula() ) {
								bindings.bindValue( jdbcValue, jdbcValueMapping, ParameterUsage.RESTRICT );
							}
						},
						session
				);
			}
		}
	}

	@Override
	public boolean isOneToMany() {
		return false;
	}

	@Override
	public boolean isManyToMany() {
		return elementType instanceof EntityType; //instanceof AssociationType;
	}

	private static boolean isUsingTransactionIdParameters(SharedSessionContractImplementor session) {
		final var factory = session.getFactory();
		return factory.getSessionFactoryOptions().getTemporalTableStrategy() == SINGLE_TABLE
			&& !factory.getTransactionIdentifierService().isDisabled();
	}

	private boolean isNativeTemporalTablesEnabled() {
		return getFactory().getSessionFactoryOptions().getTemporalTableStrategy() == NATIVE;
	}

	private boolean shouldApplyTemporalOperations(MutatingTableReference tableReference) {
		final var attributeMapping = getAttributeMapping();
		if ( attributeMapping == null ) {
			return false;
		}
		else {
			final var temporalMapping = attributeMapping.getTemporalMapping();
			return temporalMapping != null
				&& !isNativeTemporalTablesEnabled()
				&& ( !isHistoryStrategy() || temporalMapping.getTableName().equals( tableReference.getTableName() ) );
		}
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(TableGroup tableGroup) {
		return getFilterAliasGenerator( tableGroup.getPrimaryTableReference().getIdentificationVariable() );
	}

	@Override
	public List<PlannedOperation> decompose(
			CollectionRecreateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		var operations = planRecreateOperation( action.getCollection(), action.getKey(), ordinalBase, session );

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
		action.preUpdate();

		final Object cacheKey = lockCacheItem(action, session);

		var collection = action.getCollection();
		var key = action.getKey();

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
			final boolean affectedByFilters = isAffectedByEnabledFilters( session );
			final var eventMonitor = session.getEventMonitor();
			final var event = eventMonitor.beginCollectionUpdateEvent();
			boolean success = false;
			try {
				if ( !affectedByFilters && collection.empty() ) {
					if ( !action.isEmptySnapshot() ) {
						operations.addAll( planRemoveOperation( key, ordinalBase, session ) );
					}
				}
				else if ( collection.needsRecreate( this ) ) {
					if ( affectedByFilters ) {
						throw new HibernateException( String.format( Locale.ROOT,
								"cannot recreate collection while filter is enabled [%s : %s]",
								getRole(),
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
				eventMonitor.completeCollectionUpdateEvent( event, key, getRole(), success, session );
			}
		}

		// Only register callback if we actually have operations to execute
		if ( !operations.isEmpty() ) {
			postExecCallbackRegistry.accept( new PostCollectionUpdateHandling(
					this,
					collection,
					key,
					action.getAffectedOwner(),
					action.getAffectedOwnerId(),
					cacheKey
			) );
		}

		return operations;
	}

	@Override
	public List<PlannedOperation> decompose(
			CollectionRemoveAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		var operations = planRemoveOperation( action.getKey(), ordinalBase,  session );

		// Only register callback if we actually have operations to execute
		if ( !operations.isEmpty() ) {
			final Object cacheKey = lockCacheItem( action, session );
			postExecCallbackRegistry.accept( new PostCollectionRemoveHandling( action, cacheKey ) );
		}

		return operations;
	}


	private List<PlannedOperation> planRecreateOperation(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		var insertRowPlan = jdbcOperations.getInsertRowPlan();
		if ( insertRowPlan == null ) {
			return List.of();
		}

		// Pre-insert callback once for the whole collection
		collection.preInsert( this );

		final var entries = collection.entries( this );
		if ( !entries.hasNext() ) {
			return List.of();
		}

		var operations = new ArrayList<PlannedOperation>();

		if ( shouldBundleCollectionOperations ) {
			// Bundled: all rows in a single PlannedOperation with a bundled BindPlan
			final List<Object> entryList = new ArrayList<>();
			final List<Integer> entryIndices = new ArrayList<>();
			int entryCount = 0;

			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				boolean include = collection.includeInRecreate( entry, entryCount, collection, getAttributeMapping() );

				if ( include ) {
					entryList.add( entry );
					entryIndices.add( entryCount );
				}

				entryCount++;
			}

			if ( !entryList.isEmpty() ) {
				var bundledBindPlan = new BundledCollectionInsertBindPlan(
						insertRowPlan.values(),
						collection,
						key,
						entryList,
						entryIndices
				);

				operations.add( new PlannedOperation(
						getCollectionTableDescriptor(),
						MutationKind.INSERT,
						insertRowPlan.jdbcOperation(),
						bundledBindPlan,
						ordinalBase,
						"BundledInsertRows(" + getRolePath() + ")"
				) );
			}
		}
		else {
			// Non-bundled: one operation per row
			int entryCount = 0;
			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				boolean include = collection.includeInRecreate( entry, entryCount, collection, getAttributeMapping() );

				if ( include ) {
					var bindPlan = new SingleRowInsertBindPlan(
							this,
							insertRowPlan.values(),
							collection,
							key,
							entry,
							entryCount
					);

					final PlannedOperation plannedOp = new PlannedOperation(
							getCollectionTableDescriptor(),
							MutationKind.INSERT,
							jdbcOperations.getInsertRowPlan().jdbcOperation(),
							bindPlan,
							ordinalBase * 1_000 + entryCount,
							"InsertRow[" + entryCount + "](" + getRolePath() + ")"
					);

					operations.add( plannedOp );
				}

				entryCount++;
			}
		}

		return operations;
	}

	private void planDeleteRowOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		var deleteRowPlan = jdbcOperations.getDeleteRowPlan();
		final var deletes = collection.getDeletes( this, !hasPhysicalIndexColumn() );
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
						"DeleteRows(" + getRolePath() + ")"
				) );
			}
		}
		else {
			// Original behavior: one operation per row
			int deletionCount = 0;

			while ( deletes.hasNext() ) {
				final Object removal = deletes.next();

				var bindPlan = new SingleRowDeleteBindPlan(
						collection,
						key,
						removal,
						deleteRowPlan.restrictions()
				);

				operationConsumer.accept( new PlannedOperation(
						getCollectionTableDescriptor(),
						MutationKind.DELETE,
						deleteRowPlan.jdbcOperation(),
						bindPlan,
						ordinalBase * 1_000 + deletionCount,
						"DeleteRow[" + deletionCount + "](" + getRolePath() + ")"
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

		var bundledBindPlan = new BundledCollectionUpdateBindPlan(
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
		collection.preInsert( this );

		var bundledBindPlan = new BundledCollectionInsertBindPlan(
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

	private void planUpdateRowOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		var updateRowPlan = jdbcOperations.getUpdateRowPlan();
		final var entries = collection.entries( this );

		if ( updateRowPlan == null || !entries.hasNext() ) {
			// EARLY EXIT!!
			return;
		}

		// One operation per row
		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();

			if ( collection.needsUpdating( entry, entryCount, getAttributeMapping() ) ) {
				var bindPlan = new SingleRowUpdateBindPlan(
						collection,
						key,
						entry,
						entryCount,
						updateRowPlan.values(),
						updateRowPlan.restrictions()
				);

				operationConsumer.accept( new PlannedOperation(
						getCollectionTableDescriptor(),
						MutationKind.UPDATE,
						updateRowPlan.jdbcOperation(),
						bindPlan,
						ordinalBase * 1_000 + entryCount,
						"UpdateRow[" + entryCount + "](" + getRolePath() + ")"
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
		collection.preInsert( this );

		var insertRowPlan = jdbcOperations.getInsertRowPlan();
		final var entries = collection.entries( this );

		if ( insertRowPlan == null || !entries.hasNext() ) {
			// EARLY EXIT!!
			return;
		}

		// One operation per row
		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();

			if ( collection.includeInInsert( entry, entryCount, collection, getAttributeMapping() ) ) {
				var bindPlan = new SingleRowInsertBindPlan(
						this,
						insertRowPlan.values(),
						collection,
						key,
						entry,
						entryCount
				);

				operationConsumer.accept( new PlannedOperation(
						getCollectionTableDescriptor(),
						MutationKind.INSERT,
						insertRowPlan.jdbcOperation(),
						bindPlan,
						ordinalBase * 1_000 + entryCount,
						"InsertRow[" + entryCount + "](" + getRolePath() + ")"
				) );
			}

			entryCount++;
		}
	}

	private List<PlannedOperation> planRemoveOperation(Object key, int ordinalBase, SharedSessionContractImplementor session) {
		final var jdbcOperation = jdbcOperations.getRemoveOperation();
		if ( jdbcOperation == null ) {
			return List.of();
		}

		final PlannedOperation plannedOp = new PlannedOperation(
				getCollectionTableDescriptor(),
				MutationKind.DELETE,
				jdbcOperation,
				new RemoveBindPlan( key, this ),
				ordinalBase * 1_000,
				"RemoveAllRows(" + getRolePath() + ")"
		);

		return List.of( plannedOp );
	}




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CollectionJdbcOperations creation (used with action decomposition)

	private CollectionJdbcOperations buildJdbcOperations(
			SessionFactoryImplementor factory) {
		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = buildInsertRowPlan( factory );

		final CollectionJdbcOperations.UpdateRowPlan updateRowPlan = buildUpdateRowPlan( factory );

		final CollectionJdbcOperations.DeleteRowPlan deleteRowPlan = buildDeleteRowPlan( factory );

		return new CollectionJdbcOperations(
				this,
				insertRowPlan,
				updateRowPlan,
				deleteRowPlan,
				buildRemoveOperation( factory )
		);
	}

	private CollectionJdbcOperations.InsertRowPlan buildInsertRowPlan(SessionFactoryImplementor factory) {
		if ( isInverse() || !isRowInsertEnabled() ) {
			return null;
		}

		var builder = new GraphTableInsertBuilderStandard(
				this,
				getCollectionTableDescriptor(),
				factory
		);

		applyInsertDetails( builder, factory );

		return new CollectionJdbcOperations.InsertRowPlan(
				builder.buildMutation().createMutationOperation(),
				this::bindInsertRowValues
		);
	}

	private void applyInsertDetails(
			GraphTableInsertBuilderStandard insertBuilder,
			SessionFactoryImplementor factory) {
		final var attributeMapping = getAttributeMapping();
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
			org.hibernate.action.queue.bind.JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + getNavigableRole().getFullPath() );
		}

		final var attributeMapping = getAttributeMapping();
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
						incrementIndexByBase( collection.getIndex( rowValue, rowPosition, this ) ),
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
		if ( !isPerformingUpdates() ) {
			return null;
		}

		var attribute = getAttributeMapping();

		var builder = new GraphTableUpdateBuilderStandard(
				this,
				getCollectionTableDescriptor(),
				getSqlWhereString(),
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
			org.hibernate.action.queue.bind.JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + getNavigableRole().getFullPath() );
		}

		var attribute = getAttributeMapping();
		var indexDescriptor = attribute.getIndexDescriptor();
		var elementDescriptor = attribute.getElementDescriptor();

		if ( indexDescriptor != null ) {
			indexDescriptor.decompose(
					collection.getIndex( rowValue, rowPosition, this ),
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
			org.hibernate.action.queue.bind.JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + getNavigableRole().getFullPath() );
		}

		final var attribute = getAttributeMapping();

		attribute.getKeyDescriptor().getKeyPart().decompose(
				key,
				jdbcValueBindings::bindRestriction,
				session
		);
	}

	private CollectionJdbcOperations.DeleteRowPlan buildDeleteRowPlan(SessionFactoryImplementor factory) {
		if ( needsRemove() ) {
			return null;
		}

		var attribute = getAttributeMapping();

		var builder = new GraphTableDeleteBuilderStandard(
				this,
				getCollectionTableDescriptor(),
				getSqlWhereString(),
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
			org.hibernate.action.queue.bind.JdbcValueBindings jdbcValueBindings) {
		var attribute = getAttributeMapping();

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
					collection.getIndex( rowValue, rowPosition, this ),
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
		var tableDescriptor = getCollectionTableDescriptor();
		var attribute = getAttributeMapping();

		var builder = new GraphTableDeleteBuilderStandard(
				this,
				tableDescriptor,
				getSqlWhereString(),
				factory
		);

		attribute.getKeyDescriptor().getKeyPart().forEachSelectable( (index, jdbcMapping) -> {
			builder.addKeyRestriction( jdbcMapping );
		} );

		return builder.buildMutation().createMutationOperation();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BindPlan for collection removals (full deletion).

	public static class RemoveBindPlan implements BindPlan {
		private final Object key;
		private final BasicCollectionPersister mutationTarget;

		public RemoveBindPlan(Object key, BasicCollectionPersister mutationTarget) {
			this.key = key;
			this.mutationTarget = mutationTarget;
		}

		@Override
		public void execute(
				ExecutionContext context,
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
}
