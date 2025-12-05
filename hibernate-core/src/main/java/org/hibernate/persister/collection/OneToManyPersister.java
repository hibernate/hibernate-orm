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
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.jdbc.Expectation;
import org.hibernate.mapping.Collection;
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
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameterList;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.TableUpdate;
import org.hibernate.sql.model.ast.builder.CollectionRowDeleteByUpdateSetNullBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.TableUpdateStandard;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import static org.hibernate.internal.util.NullnessHelper.areAllNonNull;
import static org.hibernate.internal.util.collections.ArrayHelper.isAnyTrue;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.persister.collection.mutation.RowMutationOperations.DEFAULT_RESTRICTOR;
import static org.hibernate.persister.collection.mutation.RowMutationOperations.DEFAULT_VALUE_SETTER;
import static org.hibernate.sql.model.ast.builder.TableUpdateBuilder.NULL;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

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

		insertRowsCoordinator = buildInsertCoordinator();
		updateRowsCoordinator = buildUpdateCoordinator();
		deleteRowsCoordinator = buildDeleteCoordinator();
		removeCoordinator = buildDeleteAllCoordinator();
		mutationExecutorService = creationContext.getServiceRegistry().getService( MutationExecutorService.class );
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

			final var updateRowOperation = rowMutationOperations.getUpdateRowOperation();
			final var updateRowValues = rowMutationOperations.getUpdateRowValues();
			final var updateRowRestrictions = rowMutationOperations.getUpdateRowRestrictions();
			assert areAllNonNull( updateRowOperation, updateRowValues, updateRowRestrictions );

			final var mutationExecutor = mutationExecutorService.createExecutor(
					() -> new BasicBatchKey( getNavigableRole() + "#INDEX" ),
					singleOperation( MutationType.UPDATE, this, updateRowOperation ),
					session
			);

			final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			try {
				int nextIndex = baseIndex() + ( resetIndex ? 0 : getSize( key, session ) );
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
//			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
//				MODEL_MUTATION_LOGGER.tracef( "Skipping collection (re)creation - %s", getRolePath() );
//			}
			return new InsertRowsCoordinatorNoOp( this );
		}
		else {
			final var registry = getFactory().getServiceRegistry();
			final var elementPersister = getElementPersisterInternal();
			return elementPersister != null && elementPersister.hasSubclasses()
						&& elementPersister instanceof UnionSubclassEntityPersister
					? new InsertRowsCoordinatorTablePerSubclass( this, rowMutationOperations, registry )
					: new InsertRowsCoordinatorStandard( this, rowMutationOperations, registry );
		}
	}

	private UpdateRowsCoordinator buildUpdateCoordinator() {
		if ( !isRowDeleteEnabled() && !isRowInsertEnabled() ) {
//			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
//				MODEL_MUTATION_LOGGER.tracef( "Skipping collection row updates - %s", getRolePath() );
//			}
			return new UpdateRowsCoordinatorNoOp( this );
		}
		else {
			final var elementPersister = getElementPersisterInternal();
			final var factory = getFactory();
			return elementPersister != null && elementPersister.hasSubclasses()
				&& elementPersister instanceof UnionSubclassEntityPersister
					? new UpdateRowsCoordinatorTablePerSubclass( this, rowMutationOperations, factory )
					: new UpdateRowsCoordinatorOneToMany( this, rowMutationOperations, factory );
		}
	}

	private DeleteRowsCoordinator buildDeleteCoordinator() {
		if ( !needsRemove() ) {
//			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
//				MODEL_MUTATION_LOGGER.tracef( "Skipping collection row deletions - %s", getRolePath() );
//			}
			return new DeleteRowsCoordinatorNoOp( this );
		}
		else {
			final var elementPersister = getElementPersisterInternal();
			final var registry = getFactory().getServiceRegistry();
			return elementPersister != null && elementPersister.hasSubclasses()
				&& elementPersister instanceof UnionSubclassEntityPersister
					// never delete by index for one-to-many
					? new DeleteRowsCoordinatorTablePerSubclass( this, rowMutationOperations, false, registry )
					: new DeleteRowsCoordinatorStandard( this, rowMutationOperations, false, registry );
		}
	}

	private RemoveCoordinator buildDeleteAllCoordinator() {
		if ( ! needsRemove() ) {
//			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
//				MODEL_MUTATION_LOGGER.tracef( "Skipping collection removals - %s", getRolePath() );
//			}
			return new RemoveCoordinatorNoOp( this );
		}
		else {
			final var registry = getFactory().getServiceRegistry();
			final var elementPersister = getElementPersisterInternal();
			return elementPersister != null && elementPersister.hasSubclasses()
				&& elementPersister instanceof UnionSubclassEntityPersister
					? new RemoveCoordinatorTablePerSubclass( this, this::buildDeleteAllOperation, registry )
					: new RemoveCoordinatorStandard( this, this::buildDeleteAllOperation, registry );
		}
	}

	private JdbcMutationOperation generateDeleteRowOperation(MutatingTableReference tableReference) {
		return getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( generateDeleteRowAst( tableReference ), getFactory() )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	public RestrictedTableMutation<JdbcMutationOperation> generateDeleteRowAst(MutatingTableReference tableReference) {
		// note that custom SQL delete row details are handled by CollectionRowUpdateBuilder
		final var updateBuilder =
				new CollectionRowDeleteByUpdateSetNullBuilder<>( this, tableReference, getFactory(), sqlWhereString );

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
					updateBuilder.addValueColumn(
							NULL,
							selectable
					);
				}
				// restrict
				updateBuilder.addKeyRestrictionLeniently( selectable );
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
		final var pluralAttribute = getAttributeMapping();
		pluralAttribute.getKeyDescriptor().decompose(
				keyValue,
				0,
				jdbcValueBindings,
				null,
				DEFAULT_RESTRICTOR,
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
		attributeMapping.getKeyDescriptor().getKeyPart().forEachSelectable( updateBuilder );
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
				DEFAULT_VALUE_SETTER,
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
		// 		* add a restriction based on the previous value
		//		* add an assignment for the new value
		attributeMapping.getIndexDescriptor().forEachUpdatable( updateBuilder );
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

}
