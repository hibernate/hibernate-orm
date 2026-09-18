/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.action.queue.internal.PreparedCollectionMutation;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.internal.decompose.collection.BasicCollectionDecomposer;
import org.hibernate.action.queue.spi.decompose.collection.CollectionDecomposer;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.OperationProducer;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.RowMutationOperations;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.persister.filter.internal.StaticFilterAliasGenerator;
import org.hibernate.persister.state.spi.StateManagement;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameterList;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.RestrictedTableMutation;
import org.hibernate.sql.ast.spi.model.TableMutation;
import org.hibernate.sql.ast.internal.model.builder.CollectionRowDeleteBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilderStandard;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;
import org.hibernate.type.EntityType;

import java.util.List;
import java.util.function.Consumer;

import static org.hibernate.temporal.TemporalTableStrategy.NATIVE;
import static org.hibernate.temporal.TemporalTableStrategy.SINGLE_TABLE;
import static org.hibernate.internal.util.collections.ArrayHelper.isAnyTrue;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.persister.collection.mutation.RowMutationOperations.DEFAULT_RESTRICTOR;
import static org.hibernate.persister.collection.mutation.RowMutationOperations.DEFAULT_VALUE_SETTER;

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
	private final StateManagement stateManagement;

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private CollectionDecomposer decomposer;

	private final InsertRowsCoordinator insertRowsCoordinator;
	private final UpdateRowsCoordinator updateCoordinator;
	private final DeleteRowsCoordinator deleteRowsCoordinator;
	private final RemoveCoordinator removeCoordinator;

	public BasicCollectionPersister(
			@Nonnull Collection collectionBinding,
			@Nullable CollectionDataAccess cacheAccessStrategy,
			@Nonnull RuntimeModelCreationContext creationContext)
					throws MappingException, CacheException {
		super( collectionBinding, cacheAccessStrategy, creationContext );

		this.rowMutationOperations = buildRowMutationOperations();

		stateManagement = collectionBinding.getStateManagement();
		final var legacyIntegration = stateManagement.getLegacyIntegration();
		this.insertRowsCoordinator = legacyIntegration.createInsertRowsCoordinator( this );
		this.updateCoordinator = legacyIntegration.createUpdateRowsCoordinator( this );
		this.deleteRowsCoordinator = legacyIntegration.createDeleteRowsCoordinator( this );
		this.removeCoordinator = legacyIntegration.createRemoveCoordinator( this );
	}

	@Override
	public void postInstantiate() throws MappingException {
		super.postInstantiate();

		// Build JDBC operations after collectionTableDescriptor is initialized
		decomposer = buildCollectionDecomposer();
	}

	@Nonnull
	protected CollectionDecomposer buildCollectionDecomposer() {
		return new BasicCollectionDecomposer(
				this,
				getFactory(),
				stateManagement.getGraphIntegration().createCollectionMutationPlanContributor( this )
		);
	}

	@Nonnull
	public RowMutationOperations getRowMutationOperations() {
		return rowMutationOperations;
	}

	@Nonnull
	public InsertRowsCoordinator getCreateEntryCoordinator() {
		return insertRowsCoordinator;
	}

	@Nonnull
	public InsertRowsCoordinator getInsertRowsCoordinator() {
		return insertRowsCoordinator;
	}
	@Override
	public void recreate(@Nonnull PersistentCollection<?> collection, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		getCreateEntryCoordinator().insertRows( collection, id, collection::includeInRecreate, session );
	}

	@Override
	public void insertRows(@Nonnull PersistentCollection<?> collection, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session)
			throws HibernateException {
		getCreateEntryCoordinator().insertRows( collection, id, collection::includeInInsert, session );
	}

	@Nonnull
	public UpdateRowsCoordinator getUpdateEntryCoordinator() {
		return updateCoordinator;
	}

	@Nonnull
	public UpdateRowsCoordinator getUpdateRowsCoordinator() {
		return updateCoordinator;
	}

	@Override
	public void updateRows(@Nonnull PersistentCollection<?> collection, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		getUpdateEntryCoordinator().updateRows( id, collection, session );
	}

	@Nonnull
	public DeleteRowsCoordinator getRemoveEntryCoordinator() {
		return deleteRowsCoordinator;
	}

	@Nonnull
	public DeleteRowsCoordinator getDeleteRowsCoordinator() {
		return deleteRowsCoordinator;
	}

	@Override
	public void deleteRows(@Nonnull PersistentCollection<?> collection, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		getRemoveEntryCoordinator().deleteRows( collection, id, session );
	}

	@Nonnull
	@Override
	public RemoveCoordinator getRemoveCoordinator() {
		return removeCoordinator;
	}

	@Override
	protected void doProcessQueuedOps(@Nonnull PersistentCollection<?> collection, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		// nothing to do
	}

	public boolean isPerformingUpdates() {
		return !isInverse()
			&& getCollectionSemantics().getCollectionClassification().isRowUpdatePossible()
			&& isAnyTrue( elementColumnIsSettable );
	}

	@Nonnull
	@Override
	public RestrictedTableMutation<JdbcMutationOperation> generateDeleteAllAst(@Nonnull MutatingTableReference tableReference) {
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

	@Nonnull
	protected RestrictedTableMutation<JdbcMutationOperation> generateTemporalDeleteAllAst(@Nonnull MutatingTableReference tableReference) {
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

	@Nonnull
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




	@Nonnull
	private JdbcMutationOperation generateInsertRowOperation(@Nonnull MutatingTableReference tableReference) {
		return getIdentifierTableMapping().getInsertDetails().getCustomSql() != null
				? buildCustomSqlInsertRowOperation( tableReference )
				: buildGeneratedInsertRowOperation( tableReference );

	}

	@Nonnull
	private JdbcMutationOperation buildCustomSqlInsertRowOperation(@Nonnull MutatingTableReference tableReference) {
		final var factory = getFactory();
		final var insertBuilder = new TableInsertBuilderStandard( this, tableReference, factory );
		applyInsertDetails( insertBuilder );
		return insertBuilder.buildMutation().createMutationOperation( null, factory );
	}

	private void applyInsertDetails(@Nonnull TableInsertBuilderStandard insertBuilder) {
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

	@Nonnull
	private JdbcMutationOperation buildGeneratedInsertRowOperation(@Nonnull MutatingTableReference tableReference) {
		return getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.ModelMutation<>( getFactory(), generateInsertRowAst( tableReference ) ) )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	@Nonnull
	private TableMutation<JdbcMutationOperation> generateInsertRowAst(@Nonnull MutatingTableReference tableReference) {
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
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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
					session.getCurrentChangesetIdentifier(),
					temporalMapping.getStartingColumnMapping(),
					ParameterUsage.SET
			);
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Update handling

	@Nonnull
	private JdbcMutationOperation generateUpdateRowOperation(@Nonnull MutatingTableReference tableReference) {
		return getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.ModelMutation<>( getFactory(), generateUpdateRowAst( tableReference ) ) )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	@Nonnull
	private RestrictedTableMutation<JdbcMutationOperation> generateUpdateRowAst(@Nonnull MutatingTableReference tableReference) {
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
			if ( indexDescriptor != null && !indexContainsFormula ) {
				updateBuilder.addKeyRestrictionsLeniently( indexDescriptor );
			}
			else {
				updateBuilder.addKeyRestrictions( attribute.getElementDescriptor() );
			}
		}

		//noinspection unchecked,rawtypes
		return (RestrictedTableMutation) updateBuilder.buildMutation();
	}

	private void applyUpdateRowValues(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object entry,
			int entryPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object entry,
			int entryPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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
			if ( indexDescriptor != null && !indexContainsFormula ) {
				final Object index =
						collection.getIndex( entry, entryPosition,
								attributeMapping.getCollectionDescriptor() );
				indexDescriptor.decompose(
						incrementIndexByBase( index ),
						0,
						jdbcValueBindings,
						null,
						DEFAULT_RESTRICTOR,
						session
				);
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

	@Nonnull
	private JdbcMutationOperation generateDeleteRowOperation(@Nonnull MutatingTableReference tableReference) {
		return getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.ModelMutation<>( getFactory(), generateDeleteRowAst( tableReference ) ) )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	@Nonnull
	private RestrictedTableMutation<JdbcMutationOperation> generateDeleteRowAst(@Nonnull MutatingTableReference tableReference) {
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

	@Nonnull
	protected RestrictedTableMutation<JdbcMutationOperation> generateSoftDeleteRowsAst(@Nonnull MutatingTableReference tableReference) {
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

	@Nonnull
	protected RestrictedTableMutation<JdbcMutationOperation> generateTemporalDeleteRowsAst(@Nonnull MutatingTableReference tableReference) {
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
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object keyValue,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = getAttributeMapping();
		final var temporalMapping = attributeMapping.getTemporalMapping();
		if ( temporalMapping != null && isUsingTransactionIdParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getCurrentChangesetIdentifier(),
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

	private static boolean isUsingTransactionIdParameters(@Nonnull SharedSessionContractImplementor session) {
		final var factory = session.getFactory();
		return factory.getSessionFactoryOptions().getTemporalTableStrategy() == SINGLE_TABLE
			&& !factory.getChangesetCoordinator().useServerTimestamp( session.getDialect() );
	}

	private boolean isNativeTemporalTablesEnabled() {
		return getFactory().getSessionFactoryOptions().getTemporalTableStrategy() == NATIVE;
	}

	private boolean shouldApplyTemporalOperations(@Nonnull MutatingTableReference tableReference) {
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

	@Nonnull
	@Override
	public FilterAliasGenerator getFilterAliasGenerator(@Nonnull String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}

	@Nonnull
	@Override
	public FilterAliasGenerator getFilterAliasGenerator(@Nonnull TableGroup tableGroup) {
		return getFilterAliasGenerator( tableGroup.getPrimaryTableReference().getIdentificationVariable() );
	}

	@Override
	public void decompose(
			@Nonnull PreparedCollectionMutation mutation,
			int ordinalBase,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull DecompositionContext decompositionContext,
			@Nonnull Consumer<FlushOperation> operationConsumer) {
		switch ( mutation.kind() ) {
			case CREATE -> decomposer.decomposeRecreate(
					mutation, ordinalBase, session, decompositionContext, operationConsumer );
			case REMOVE -> decomposer.decomposeRemove(
					mutation, ordinalBase, session, decompositionContext, operationConsumer );
			case UPDATE -> decomposer.decomposeUpdate(
					mutation, ordinalBase, session, decompositionContext, operationConsumer );
			case QUEUED_OPERATIONS -> decomposer.decomposeQueuedOperations(
					mutation, ordinalBase, session, operationConsumer );
		}
	}

}
