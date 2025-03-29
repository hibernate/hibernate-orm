/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.jdbc.Expectations;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.OneToManyCollectionPart;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorStandard;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorTablePerSubclass;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorStandard;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorTablePerSubclass;
import org.hibernate.persister.collection.mutation.OperationProducer;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorStandard;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorTablePerSubclass;
import org.hibernate.persister.collection.mutation.RowMutationOperations;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorOneToMany;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorTablePerSubclass;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.ColumnValueParameterList;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.TableUpdate;
import org.hibernate.sql.model.ast.builder.CollectionRowDeleteByUpdateSetNullBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationOperationGroupFactory;
import org.hibernate.sql.model.internal.TableUpdateStandard;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import static org.hibernate.internal.util.NullnessHelper.areAllNonNull;
import static org.hibernate.internal.util.collections.ArrayHelper.isAnyTrue;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
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

	private final boolean keyIsNullable;
	private final MutationExecutorService mutationExecutorService;

	final boolean doWriteEvenWhenInverse; // contrary to intent of JPA

	public OneToManyPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) throws MappingException, CacheException {
		super( collectionBinding, cacheAccessStrategy, creationContext );
		keyIsNullable = collectionBinding.getKey().isNullable();

		doWriteEvenWhenInverse = isInverse
				&& hasIndex()
				&& !indexContainsFormula
				&& isAnyTrue( indexColumnIsSettable )
				&& !getElementPersisterInternal().managesColumns( indexColumnNames );

		rowMutationOperations = buildRowMutationOperations();

		insertRowsCoordinator = buildInsertCoordinator();
		updateRowsCoordinator = buildUpdateCoordinator();
		deleteRowsCoordinator = buildDeleteCoordinator();
		removeCoordinator = buildDeleteAllCoordinator();
		mutationExecutorService = creationContext.getServiceRegistry().getService(	MutationExecutorService.class );
	}

	@Override
	protected RowMutationOperations getRowMutationOperations() {
		return rowMutationOperations;
	}

	protected InsertRowsCoordinator getInsertRowsCoordinator() {
		return insertRowsCoordinator;
	}

	protected UpdateRowsCoordinator getUpdateRowsCoordinator() {
		return updateRowsCoordinator;
	}

	protected DeleteRowsCoordinator getDeleteRowsCoordinator() {
		return deleteRowsCoordinator;
	}

	@Override
	protected RemoveCoordinator getRemoveCoordinator() {
		return removeCoordinator;
	}

	@Override
	protected boolean isRowDeleteEnabled() {
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
		// See HHH-5732 and HHH-18830.
		// Claim: "If one-to-many and inverse, still need to create the index."
		// In fact this is wrong: JPA is very clear that bidirectional associations
		// are persisted from the owning side. However, since this is a very ancient
		// mistake, I have fixed it in a backward-compatible way, by writing to the
		// order column if there is no mapping at all for it on the other side.
		// But if the owning entity does have a mapping for the order column, don't
		// do superfluous SQL UPDATEs here from the unowned side, no matter how many
		// users complain.
		if ( doWriteEvenWhenInverse && entries.hasNext() ) {

			final JdbcMutationOperation updateRowOperation = rowMutationOperations.getUpdateRowOperation();
			final RowMutationOperations.Values updateRowValues = rowMutationOperations.getUpdateRowValues();
			final RowMutationOperations.Restrictions updateRowRestrictions = rowMutationOperations.getUpdateRowRestrictions();
			assert areAllNonNull( updateRowOperation, updateRowValues, updateRowRestrictions );

			final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
					() -> new BasicBatchKey( getNavigableRole() + "#INDEX" ),
					MutationOperationGroupFactory.singleOperation( MutationType.UPDATE, this, updateRowOperation ),
					session
			);

			final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			try {
				int nextIndex = ( resetIndex ? 0 : getSize( key, session ) ) +
						Math.max( getAttributeMapping().getIndexMetadata().getListIndexBase(), 0 );
				while ( entries.hasNext() ) {
					final Object entry = entries.next();
					if ( entry != null && collection.entryExists( entry, nextIndex ) ) {
						updateRowValues.applyValues(
								collection,
								key,
								entry,
								nextIndex,
								session,
								jdbcValueBindings
						);
						updateRowRestrictions.applyRestrictions(
								collection,
								key,
								entry,
								nextIndex,
								session,
								jdbcValueBindings
						);
						mutationExecutor.execute( collection, null, null, null, session );
						nextIndex++;
					}
				}
			}
			finally {
				mutationExecutor.release();
			}
		}

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
			getElementPersisterInternal().applyDiscriminator(
					predicateConsumer,
					alias,
					tableGroup,
					astCreationState
			);
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
		assert getAttributeMapping() != null;

		final ForeignKeyDescriptor fkDescriptor = getAttributeMapping().getKeyDescriptor();
		assert fkDescriptor != null;

		final int keyColumnCount = fkDescriptor.getJdbcTypeCount();
		final int valuesCount = hasIndex()
				? keyColumnCount + indexColumnNames.length
				: keyColumnCount;

		final ColumnValueParameterList parameterBinders = new ColumnValueParameterList(
				tableReference,
				ParameterUsage.RESTRICT,
				keyColumnCount
		);
		final List<ColumnValueBinding> keyRestrictionBindings = arrayList( keyColumnCount );
		final List<ColumnValueBinding> valueBindings = arrayList( valuesCount );
		fkDescriptor.getKeyPart().forEachSelectable( parameterBinders );
		for ( ColumnValueParameter columnValueParameter : parameterBinders ) {
			final ColumnReference columnReference = columnValueParameter.getColumnReference();
			keyRestrictionBindings.add(
					new ColumnValueBinding(
							columnReference,
							new ColumnWriteFragment(
									"?",
									columnValueParameter,
									columnReference.getJdbcMapping()
							)
					)
			);
			valueBindings.add(
					new ColumnValueBinding(
							columnReference,
							new ColumnWriteFragment( "null", columnReference.getJdbcMapping() )
					)
			);
		}

		if ( hasIndex() && !indexContainsFormula ) {
			getAttributeMapping().getIndexDescriptor().forEachSelectable( (selectionIndex, selectableMapping) -> {
				if ( ! selectableMapping.isUpdateable() ) {
					return;
				}
				valueBindings.add(
						new ColumnValueBinding( new ColumnReference( tableReference, selectableMapping ),
						new ColumnWriteFragment( "null", selectableMapping.getJdbcMapping() )
				) );
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
				Expectations.NONE
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

		return new RowMutationOperations(
				this,
				insertRowOperationProducer,
				insertRowValues,
				writeIndexOperationProducer,
				writeIndexValues,
				writeIndexRestrictions,
				deleteEntryOperationProducer,
				deleteEntryRestrictions
		);
	}


	private InsertRowsCoordinator buildInsertCoordinator() {
		if ( isInverse() || !isRowInsertEnabled() ) {
			if ( MODEL_MUTATION_LOGGER.isDebugEnabled() ) {
				MODEL_MUTATION_LOGGER.debugf( "Skipping collection (re)creation - %s", getRolePath() );
			}
			return new InsertRowsCoordinatorNoOp( this );
		}
		else {
			final ServiceRegistryImplementor serviceRegistry = getFactory().getServiceRegistry();
			final EntityPersister elementPersister = getElementPersisterInternal();
			return elementPersister != null && elementPersister.hasSubclasses()
						&& elementPersister instanceof UnionSubclassEntityPersister
					? new InsertRowsCoordinatorTablePerSubclass( this, rowMutationOperations, serviceRegistry )
					: new InsertRowsCoordinatorStandard( this, rowMutationOperations, serviceRegistry );
		}
	}

	private UpdateRowsCoordinator buildUpdateCoordinator() {
		if ( !isRowDeleteEnabled() && !isRowInsertEnabled() ) {
			if ( MODEL_MUTATION_LOGGER.isDebugEnabled() ) {
				MODEL_MUTATION_LOGGER.debugf( "Skipping collection row updates - %s", getRolePath() );
			}
			return new UpdateRowsCoordinatorNoOp( this );
		}
		else {
			final EntityPersister elementPersister = getElementPersisterInternal();
			return elementPersister != null && elementPersister.hasSubclasses()
						&& elementPersister instanceof UnionSubclassEntityPersister
					? new UpdateRowsCoordinatorTablePerSubclass( this, rowMutationOperations, getFactory() )
					: new UpdateRowsCoordinatorOneToMany( this, rowMutationOperations, getFactory() );
		}
	}

	private DeleteRowsCoordinator buildDeleteCoordinator() {
		if ( !needsRemove() ) {
			if ( MODEL_MUTATION_LOGGER.isDebugEnabled() ) {
				MODEL_MUTATION_LOGGER.debugf( "Skipping collection row deletions - %s", getRolePath() );
			}
			return new DeleteRowsCoordinatorNoOp( this );
		}
		else {
			final EntityPersister elementPersister = getElementPersisterInternal();
			final ServiceRegistryImplementor serviceRegistry = getFactory().getServiceRegistry();
			return elementPersister != null && elementPersister.hasSubclasses()
						&& elementPersister instanceof UnionSubclassEntityPersister
					// never delete by index for one-to-many
					? new DeleteRowsCoordinatorTablePerSubclass( this, rowMutationOperations, false, serviceRegistry )
					: new DeleteRowsCoordinatorStandard( this, rowMutationOperations, false, serviceRegistry );
		}
	}

	private RemoveCoordinator buildDeleteAllCoordinator() {
		if ( ! needsRemove() ) {
			if ( MODEL_MUTATION_LOGGER.isDebugEnabled() ) {
				MODEL_MUTATION_LOGGER.debugf( "Skipping collection removals - %s", getRolePath() );
			}
			return new RemoveCoordinatorNoOp( this );
		}
		else {
			final ServiceRegistryImplementor serviceRegistry = getFactory().getServiceRegistry();
			final EntityPersister elementPersister = getElementPersisterInternal();
			return elementPersister != null && elementPersister.hasSubclasses()
						&& elementPersister instanceof UnionSubclassEntityPersister
					? new RemoveCoordinatorTablePerSubclass( this, this::buildDeleteAllOperation, serviceRegistry )
					: new RemoveCoordinatorStandard( this, this::buildDeleteAllOperation, serviceRegistry );
		}
	}

	private JdbcMutationOperation generateDeleteRowOperation(MutatingTableReference tableReference) {
		return getFactory().getJdbcServices().getDialect().getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( generateDeleteRowAst( tableReference ), getFactory() )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	public RestrictedTableMutation<JdbcMutationOperation> generateDeleteRowAst(MutatingTableReference tableReference) {
		// note that custom sql delete row details are handled by CollectionRowUpdateBuilder
		final CollectionRowDeleteByUpdateSetNullBuilder<MutationOperation> updateBuilder =
				new CollectionRowDeleteByUpdateSetNullBuilder<>( this, tableReference, getFactory(), sqlWhereString );

		// for each key column -
		// 		1) set the value to null
		// 		2) restrict based on key value
		final ForeignKeyDescriptor keyDescriptor = getAttributeMapping().getKeyDescriptor();
		final int keyTypeCount = keyDescriptor.getJdbcTypeCount();
		for ( int i = 0; i < keyTypeCount; i++ ) {
			final SelectableMapping selectable = keyDescriptor.getSelectable( i );
			if ( !selectable.isFormula() ) {
				if ( selectable.isUpdateable() ) {
					// set null
					updateBuilder.addValueColumn(
							selectable.getSelectionExpression(),
							NULL,
							selectable.getJdbcMapping(),
							selectable.isLob()
					);
				}
				// restrict
				updateBuilder.addKeyRestrictionLeniently( selectable );
			}
		}

		// set the value for each index column to null
		if ( hasIndex() && !indexContainsFormula ) {
			final CollectionPart indexDescriptor = getAttributeMapping().getIndexDescriptor();
			assert indexDescriptor != null;
			final int indexTypeCount = indexDescriptor.getJdbcTypeCount();
			for ( int i = 0; i < indexTypeCount; i++ ) {
				final SelectableMapping selectable = indexDescriptor.getSelectable( i );
				if ( selectable.isUpdateable() ) {
					updateBuilder.addValueColumn(
							selectable.getSelectionExpression(),
							NULL,
							selectable.getJdbcMapping(),
							selectable.isLob()
					);
				}
			}
		}

		// for one-to-many, we know the element is an entity and need to restrict the update
		// based on the element's id
		final EntityCollectionPart entityPart = (EntityCollectionPart) getAttributeMapping().getElementDescriptor();
		final EntityIdentifierMapping entityId = entityPart.getAssociatedEntityMappingType().getIdentifierMapping();
		updateBuilder.addKeyRestrictionsLeniently( entityId );

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
		final PluralAttributeMapping pluralAttribute = getAttributeMapping();
		pluralAttribute.getKeyDescriptor().decompose(
				keyValue,
				0,
				jdbcValueBindings,
				null,
				RowMutationOperations.DEFAULT_RESTRICTOR,
				session
		);
		pluralAttribute.getElementDescriptor().decompose(
				rowValue,
				0,
				jdbcValueBindings,
				null,
				RowMutationOperations.DEFAULT_RESTRICTOR,
				session
		);
	}


	private JdbcMutationOperation generateInsertRowOperation(MutatingTableReference tableReference) {
		// NOTE : `TableUpdateBuilderStandard` and `TableUpdate` already handle custom-sql
		final TableUpdate<JdbcMutationOperation> tableUpdate = buildTableUpdate( tableReference );
		return tableUpdate.createMutationOperation( null, getFactory() );
	}

	private TableUpdate<JdbcMutationOperation> buildTableUpdate(MutatingTableReference tableReference) {
		final TableUpdateBuilderStandard<JdbcMutationOperation> updateBuilder =
				new TableUpdateBuilderStandard<>( this, tableReference, getFactory(), sqlWhereString );

		final PluralAttributeMapping attributeMapping = getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().forEachSelectable( updateBuilder );

		final CollectionPart indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.forEachUpdatable( updateBuilder );
		}

		final EntityCollectionPart elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final EntityMappingType elementType = elementDescriptor.getAssociatedEntityMappingType();
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
		final PluralAttributeMapping attributeMapping = getAttributeMapping();

		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				keyValue,
				0,
				jdbcValueBindings,
				null,
				RowMutationOperations.DEFAULT_VALUE_SETTER,
				session
		);

		final CollectionPart indexDescriptor = attributeMapping.getIndexDescriptor();

		if ( indexDescriptor != null ) {
			indexDescriptor.decompose(
					incrementIndexByBase( collection.getIndex( rowValue, rowPosition, this ) ),
					0,
					jdbcValueBindings,
					null,
					(valueIndex, bindings, noop, value, jdbcValueMapping) -> {
						if ( !jdbcValueMapping.isUpdateable() ) {
							return;
						}
						bindings.bindValue( value, jdbcValueMapping, ParameterUsage.SET );
					},
					session
			);
		}

		final Object elementValue = collection.getElement( rowValue );
		final EntityCollectionPart elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final EntityIdentifierMapping identifierMapping = elementDescriptor.getAssociatedEntityMappingType().getIdentifierMapping();
		identifierMapping.decompose(
				identifierMapping.getIdentifier( elementValue ),
				0,
				jdbcValueBindings,
				null,
				RowMutationOperations.DEFAULT_RESTRICTOR,
				session
		);
	}


	private JdbcMutationOperation generateWriteIndexOperation(MutatingTableReference tableReference) {
		// note that custom sql update details are handled by TableUpdateBuilderStandard
		final TableUpdateBuilderStandard<JdbcMutationOperation> updateBuilder =
				new TableUpdateBuilderStandard<>( this, tableReference, getFactory(), sqlWhereString );

		final OneToManyCollectionPart elementDescriptor = (OneToManyCollectionPart) getAttributeMapping().getElementDescriptor();
		updateBuilder.addKeyRestrictionsLeniently( elementDescriptor.getAssociatedEntityMappingType().getIdentifierMapping() );

		// if the collection has an identifier, add its column as well
		if ( getAttributeMapping().getIdentifierDescriptor() != null ) {
			updateBuilder.addKeyRestrictionsLeniently( getAttributeMapping().getIdentifierDescriptor() );
		}

		// for each index column:
		// 		* add a restriction based on the previous value
		//		* add an assignment for the new value
		getAttributeMapping().getIndexDescriptor().forEachUpdatable( updateBuilder );

		final RestrictedTableMutation<JdbcMutationOperation> tableUpdate = updateBuilder.buildMutation();
		return tableUpdate.createMutationOperation( null, getFactory() );
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
					if ( !jdbcValueMapping.isUpdateable() ) {
						return;
					}
					bindings.bindValue( jdbcValue, jdbcValueMapping, ParameterUsage.SET );
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
		final OneToManyCollectionPart elementDescriptor = (OneToManyCollectionPart) getAttributeMapping().getElementDescriptor();
		final EntityMappingType associatedType = elementDescriptor.getAssociatedEntityMappingType();
		final Object element = collection.getElement( entry );
		final Object elementIdentifier = associatedType.getIdentifierMapping().getIdentifier( element );
		associatedType.getIdentifierMapping().decompose(
				elementIdentifier,
				0,
				jdbcValueBindings,
				null,
				RowMutationOperations.DEFAULT_RESTRICTOR,
				session
		);

		if ( getAttributeMapping().getIdentifierDescriptor() != null ) {
			final Object identifier = collection.getIdentifier( entry, entryPosition );
			getAttributeMapping().getIdentifierDescriptor().decompose(
					identifier,
					0,
					jdbcValueBindings,
					null,
					RowMutationOperations.DEFAULT_RESTRICTOR,
					session
			);
		}
	}

}
