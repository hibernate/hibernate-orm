/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
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
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.ast.SqlAstTranslator;
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

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.ast.builder.TableUpdateBuilder.NULL;

/**
 * A {@link CollectionPersister} for {@link jakarta.persistence.OneToMany one-to-one
 * associations}.
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

	@Deprecated(since = "6.0")
	public OneToManyPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			PersisterCreationContext creationContext) throws MappingException, CacheException {
		this( collectionBinding, cacheAccessStrategy, (RuntimeModelCreationContext) creationContext );
	}

	public OneToManyPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) throws MappingException, CacheException {
		super( collectionBinding, cacheAccessStrategy, creationContext );
		keyIsNullable = collectionBinding.getKey().isNullable();

		this.rowMutationOperations = buildRowMutationOperations();

		this.insertRowsCoordinator = buildInsertCoordinator();
		this.updateRowsCoordinator = buildUpdateCoordinator();
		this.deleteRowsCoordinator = buildDeleteCoordinator();
		this.removeCoordinator = buildDeleteAllCoordinator();
		this.mutationExecutorService = creationContext.getServiceRegistry().getService(	MutationExecutorService.class );
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
		if ( !entries.hasNext() ) {
			// no entries to update
			return;
		}

		// If one-to-many and inverse, still need to create the index.  See HHH-5732.
		final boolean doWrite = isInverse
				&& hasIndex()
				&& !indexContainsFormula
				&& ArrayHelper.countTrue( indexColumnIsSettable ) > 0;
		if ( !doWrite ) {
			return;
		}

		final JdbcMutationOperation updateRowOperation = rowMutationOperations.getUpdateRowOperation();
		final RowMutationOperations.Values updateRowValues = rowMutationOperations.getUpdateRowValues();
		final RowMutationOperations.Restrictions updateRowRestrictions = rowMutationOperations.getUpdateRowRestrictions();
		assert NullnessHelper.areAllNonNull( updateRowOperation, updateRowValues, updateRowRestrictions );

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

	public boolean consumesEntityAlias() {
		return true;
	}

	public boolean consumesCollectionAlias() {
		return true;
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
		return ( (Joinable) getElementPersister() ).getTableName();
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
		final boolean needsWriteIndex = isInverse
				&& hasIndex()
				&& !indexContainsFormula
				&& !ArrayHelper.isAllFalse( indexColumnIsSettable );
		if ( needsWriteIndex ) {
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
				MODEL_MUTATION_LOGGER.debugf(
						"Skipping collection (re)creation - %s",
						getRolePath()
				);
			}
			return new InsertRowsCoordinatorNoOp( this );
		}

		if ( getElementPersisterInternal() != null && getElementPersisterInternal().hasSubclasses()
				&& getElementPersisterInternal() instanceof UnionSubclassEntityPersister ) {
			return new InsertRowsCoordinatorTablePerSubclass( this, rowMutationOperations, getFactory().getServiceRegistry() );
		}
		return new InsertRowsCoordinatorStandard( this, rowMutationOperations, getFactory().getServiceRegistry() );
	}

	private UpdateRowsCoordinator buildUpdateCoordinator() {
		if ( !isRowDeleteEnabled() && !isRowInsertEnabled() ) {
			if ( MODEL_MUTATION_LOGGER.isDebugEnabled() ) {
				MODEL_MUTATION_LOGGER.debugf(
						"Skipping collection row updates - %s",
						getRolePath()
				);
			}
			return new UpdateRowsCoordinatorNoOp( this );
		}

		if ( getElementPersisterInternal() != null && getElementPersisterInternal().hasSubclasses()
				&& getElementPersisterInternal() instanceof UnionSubclassEntityPersister ) {
			return new UpdateRowsCoordinatorTablePerSubclass( this, rowMutationOperations, getFactory() );
		}
		return new UpdateRowsCoordinatorOneToMany( this, getRowMutationOperations(), getFactory() );
	}

	private DeleteRowsCoordinator buildDeleteCoordinator() {
		if ( !needsRemove() ) {
			if ( MODEL_MUTATION_LOGGER.isDebugEnabled() ) {
				MODEL_MUTATION_LOGGER.debugf(
						"Skipping collection row deletions - %s",
						getRolePath()
				);
			}
			return new DeleteRowsCoordinatorNoOp( this );
		}


		if ( getElementPersisterInternal() != null && getElementPersisterInternal().hasSubclasses()
				&& getElementPersisterInternal() instanceof UnionSubclassEntityPersister ) {
			return new DeleteRowsCoordinatorTablePerSubclass( this, rowMutationOperations, false, getFactory().getServiceRegistry() );
		}
		return new DeleteRowsCoordinatorStandard(
				this,
				rowMutationOperations,
				// never delete by index for one-to-many
				false,
				getFactory().getServiceRegistry()
		);
	}

	private RemoveCoordinator buildDeleteAllCoordinator() {
		if ( ! needsRemove() ) {
			if ( MODEL_MUTATION_LOGGER.isDebugEnabled() ) {
				MODEL_MUTATION_LOGGER.debugf(
						"Skipping collection removals - %s",
						getRolePath()
				);
			}
			return new RemoveCoordinatorNoOp( this );
		}

		if ( getElementPersisterInternal() != null && getElementPersisterInternal().hasSubclasses()
				&& getElementPersisterInternal() instanceof UnionSubclassEntityPersister ) {
			return new RemoveCoordinatorTablePerSubclass( this, this::buildDeleteAllOperation, getFactory().getServiceRegistry() );
		}
		return new RemoveCoordinatorStandard( this, this::buildDeleteAllOperation, getFactory().getServiceRegistry() );
	}

	private JdbcMutationOperation generateDeleteRowOperation(MutatingTableReference tableReference) {
		final RestrictedTableMutation<JdbcMutationOperation> sqlAst = generateDeleteRowAst( tableReference );

		final SqlAstTranslator<JdbcMutationOperation> translator = getFactory().getJdbcServices()
				.getDialect()
				.getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( sqlAst, getFactory() );

		return translator.translate( null, MutationQueryOptions.INSTANCE );
	}

	public RestrictedTableMutation<JdbcMutationOperation> generateDeleteRowAst(MutatingTableReference tableReference) {
		// note that custom sql delete row details are handled by CollectionRowUpdateBuilder
		final CollectionRowDeleteByUpdateSetNullBuilder<MutationOperation> updateBuilder = new CollectionRowDeleteByUpdateSetNullBuilder<>(
				this,
				tableReference,
				getFactory(),
				sqlWhereString
		);

		// for each key column -
		// 		1) set the value to null
		// 		2) restrict based on key value
		final ForeignKeyDescriptor keyDescriptor = getAttributeMapping().getKeyDescriptor();
		final int keyTypeCount = keyDescriptor.getJdbcTypeCount();
		for ( int i = 0; i < keyTypeCount; i++ ) {
			final SelectableMapping selectable = keyDescriptor.getSelectable( i );
			if ( selectable.isFormula() ) {
				continue;
			}

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

		// set the value for each index column to null
		if ( hasIndex() && !indexContainsFormula ) {
			final CollectionPart indexDescriptor = getAttributeMapping().getIndexDescriptor();
			assert indexDescriptor != null;

			final int indexTypeCount = indexDescriptor.getJdbcTypeCount();
			for ( int i = 0; i < indexTypeCount; i++ ) {
				final SelectableMapping selectable = indexDescriptor.getSelectable( i );
				if ( !selectable.isUpdateable() ) {
					continue;
				}

				updateBuilder.addValueColumn(
						selectable.getSelectionExpression(),
						NULL,
						selectable.getJdbcMapping(),
						selectable.isLob()
				);
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
		final TableUpdateBuilderStandard<JdbcMutationOperation> updateBuilder = new TableUpdateBuilderStandard<>(
				this,
				tableReference,
				getFactory(),
				sqlWhereString
		);

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
		final TableUpdateBuilderStandard<JdbcMutationOperation> updateBuilder = new TableUpdateBuilderStandard<>(
				this,
				tableReference,
				getFactory(),
				sqlWhereString
		);

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
		final Object index = collection.getIndex( entry, entryPosition, this );

		getAttributeMapping().getIndexDescriptor().decompose(
				index,
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
