/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.persister.filter.internal.StaticFilterAliasGenerator;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorStandard;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorStandard;
import org.hibernate.persister.collection.mutation.OperationProducer;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorStandard;
import org.hibernate.persister.collection.mutation.RowMutationOperations;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorStandard;
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

import java.util.List;

import static org.hibernate.internal.util.collections.ArrayHelper.isAnyTrue;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

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
		this.insertRowsCoordinator = buildInsertRowCoordinator();
		this.updateCoordinator = buildUpdateRowCoordinator();
		this.deleteRowsCoordinator = buildDeleteRowCoordinator();
		this.removeCoordinator = buildDeleteAllCoordinator();
	}

	protected RowMutationOperations getRowMutationOperations() {
		return rowMutationOperations;
	}

	protected InsertRowsCoordinator getCreateEntryCoordinator() {
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

	protected UpdateRowsCoordinator getUpdateEntryCoordinator() {
		return updateCoordinator;
	}

	@Override
	public void updateRows(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session) {
		getUpdateEntryCoordinator().updateRows( id, collection, session );
	}

	protected DeleteRowsCoordinator getRemoveEntryCoordinator() {
		return deleteRowsCoordinator;
	}

	@Override
	public void deleteRows(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session) {
		getRemoveEntryCoordinator().deleteRows( collection, id, session );
	}

	@Override
	protected RemoveCoordinator getRemoveCoordinator() {
		return removeCoordinator;
	}

	@Override
	protected void doProcessQueuedOps(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session) {
		// nothing to do
	}


	private UpdateRowsCoordinator buildUpdateRowCoordinator() {
		final boolean performUpdates =
				getCollectionSemantics().getCollectionClassification().isRowUpdatePossible()
						&& isAnyTrue( elementColumnIsSettable )
						&& !isInverse();

		if ( !performUpdates ) {
//			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
//				MODEL_MUTATION_LOGGER.tracef(
//						"Skipping collection row updates - %s",
//						getRolePath()
//				);
//			}
			return new UpdateRowsCoordinatorNoOp( this );
		}
		else {
			return new UpdateRowsCoordinatorStandard(
					this,
					rowMutationOperations,
					getFactory()
			);
		}
	}

	private InsertRowsCoordinator buildInsertRowCoordinator() {
		if ( isInverse() || !isRowInsertEnabled() ) {
//			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
//				MODEL_MUTATION_LOGGER.tracef(
//						"Skipping collection inserts - %s",
//						getRolePath()
//				);
//			}
			return new InsertRowsCoordinatorNoOp( this );
		}
		else {
			return new InsertRowsCoordinatorStandard(
					this,
					rowMutationOperations,
					getFactory().getServiceRegistry()
			);
		}
	}

	private DeleteRowsCoordinator buildDeleteRowCoordinator() {
		if ( !needsRemove() ) {
//			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
//				MODEL_MUTATION_LOGGER.tracef(
//						"Skipping collection row deletions - %s",
//						getRolePath()
//				);
//			}
			return new DeleteRowsCoordinatorNoOp( this );
		}
		else {
			return new DeleteRowsCoordinatorStandard(
					this,
					rowMutationOperations,
					hasPhysicalIndexColumn(),
					getFactory().getServiceRegistry()
			);
		}
	}

	private RemoveCoordinator buildDeleteAllCoordinator() {
		if ( !needsRemove() ) {
//			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
//				MODEL_MUTATION_LOGGER.tracef(
//						"Skipping collection removals - %s",
//						getRolePath()
//				);
//			}
			return new RemoveCoordinatorNoOp( this );
		}
		else {
			return new RemoveCoordinatorStandard(
					this,
					this::buildDeleteAllOperation,
					getFactory().getServiceRegistry()
			);
		}
	}


	@Override
	public RestrictedTableMutation<JdbcMutationOperation> generateDeleteAllAst(MutatingTableReference tableReference) {
		final var attributeMapping = getAttributeMapping();
		assert attributeMapping != null;
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
		if ( getCollectionSemantics().getCollectionClassification().isRowUpdatePossible()
				&& isAnyTrue( elementColumnIsSettable )
				&& !isInverse() ) {
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

		return new RowMutationOperations(
				this,
				insertRowOperationProducer,
				insertRowValues,
				updateRowOperationProducer,
				updateRowValues,
				updateRowRestrictions,
				deleteRowOperationProducer,
				deleteRowRestrictions
		);
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Insert handling

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
		final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
		foreignKeyDescriptor.getKeyPart().decompose(
				key,
				0,
				jdbcValueBindings,
				null,
				RowMutationOperations.DEFAULT_VALUE_SETTER,
				session
		);

		if ( attributeMapping.getIdentifierDescriptor() != null ) {
			attributeMapping.getIdentifierDescriptor().decompose(
					collection.getIdentifier( rowValue, rowPosition ),
					0,
					jdbcValueBindings,
					null,
					RowMutationOperations.DEFAULT_VALUE_SETTER,
					session
			);
		}
		else if ( attributeMapping.getIndexDescriptor() != null ) {
			// todo (mutation) : this would be more efficient if we exposed the "containing table"
			//		per value-mapping model-parts which is what we effectively support anyway.
			//
			// this would need to kind of like a union of ModelPart and ValueMapping, except:
			// 		1) not the managed-type structure from ModelPart
			//		2) not BasicType from ValueMapping
			//	essentially any basic or composite mapping of column(s)
			attributeMapping.getIndexDescriptor().decompose(
					incrementIndexByBase( collection.getIndex( rowValue, rowPosition, this ) ),
					0,
					indexColumnIsSettable,
					jdbcValueBindings,
					(valueIndex, settable, bindings, jdbcValue, jdbcValueMapping) -> {
						if ( jdbcValueMapping.getContainingTableExpression().equals( getTableName() ) ) {
							if ( settable[valueIndex] ) {
								bindings.bindValue( jdbcValue, jdbcValueMapping, ParameterUsage.SET );
							}
						}
						// otherwise a many-to-many mapping and the index is defined
						// on the associated entity table - we skip it here
					},
					session
			);
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

		if ( attribute.getIdentifierDescriptor() != null ) {
			updateBuilder.addKeyRestrictionsLeniently( attribute.getIdentifierDescriptor() );
		}
		else {
			updateBuilder.addKeyRestrictionsLeniently( attribute.getKeyDescriptor().getKeyPart() );
			if ( attribute.getIndexDescriptor() != null && !indexContainsFormula ) {
				updateBuilder.addKeyRestrictionsLeniently( attribute.getIndexDescriptor() );
			}
			else {
				updateBuilder.addKeyRestrictions( attribute.getElementDescriptor() );
			}
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
		if ( attributeMapping.getIdentifierDescriptor() != null ) {
			attributeMapping.getIdentifierDescriptor().decompose(
					collection.getIdentifier( entry, entryPosition ),
					0,
					jdbcValueBindings,
					null,
					RowMutationOperations.DEFAULT_RESTRICTOR,
					session
			);
		}
		else {
			attributeMapping.getKeyDescriptor().getKeyPart().decompose(
					key,
					0,
					jdbcValueBindings,
					null,
					RowMutationOperations.DEFAULT_RESTRICTOR,
					session
			);

			if ( attributeMapping.getIndexDescriptor() != null && !indexContainsFormula ) {
				final Object index =
						collection.getIndex( entry, entryPosition,
								attributeMapping.getCollectionDescriptor() );
				final Object adjustedIndex = incrementIndexByBase( index );
				attributeMapping.getIndexDescriptor().decompose(
						adjustedIndex,
						0,
						jdbcValueBindings,
						null,
						RowMutationOperations.DEFAULT_RESTRICTOR,
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

	private JdbcMutationOperation generateDeleteRowOperation(MutatingTableReference tableReference) {
		return getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( generateDeleteRowAst( tableReference ), getFactory() )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	private RestrictedTableMutation<JdbcMutationOperation> generateDeleteRowAst(MutatingTableReference tableReference) {
		final var pluralAttribute = getAttributeMapping();
		assert pluralAttribute != null;
		final var softDeleteMapping = pluralAttribute.getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			return generateSoftDeleteRowsAst( tableReference );
		}
		else {
			final var foreignKeyDescriptor = pluralAttribute.getKeyDescriptor();
			assert foreignKeyDescriptor != null;
			// note that custom sql delete row details are handled by CollectionRowDeleteBuilder
			final var deleteBuilder = new CollectionRowDeleteBuilder(
					this,
					tableReference,
					getFactory(),
					sqlWhereString
			);
			if ( pluralAttribute.getIdentifierDescriptor() != null ) {
				deleteBuilder.addKeyRestrictionsLeniently( pluralAttribute.getIdentifierDescriptor() );
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
		if ( attributeMapping.getIdentifierDescriptor() != null ) {
			updateBuilder.addKeyRestrictionsLeniently( attributeMapping.getIdentifierDescriptor() );
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

	private void applyDeleteRowRestrictions(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = getAttributeMapping();
		if ( attributeMapping.getIdentifierDescriptor() != null ) {
			attributeMapping.getIdentifierDescriptor().decompose(
					rowValue,
					0,
					jdbcValueBindings,
					null,
					RowMutationOperations.DEFAULT_RESTRICTOR,
					session
			);
		}
		else {
			attributeMapping.getKeyDescriptor().getKeyPart().decompose(
					keyValue,
					0,
					jdbcValueBindings,
					null,
					RowMutationOperations.DEFAULT_RESTRICTOR,
					session
			);
			if ( hasPhysicalIndexColumn() ) {
				attributeMapping.getIndexDescriptor().decompose(
						incrementIndexByBase( rowValue ),
						0,
						jdbcValueBindings,
						null,
						RowMutationOperations.DEFAULT_RESTRICTOR,
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

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(TableGroup tableGroup) {
		return getFilterAliasGenerator( tableGroup.getPrimaryTableReference().getIdentificationVariable() );
	}
}
