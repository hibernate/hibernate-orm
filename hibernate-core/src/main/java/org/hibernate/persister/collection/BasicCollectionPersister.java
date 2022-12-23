/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.mutation.CollectionTableMapping;
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
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.model.ast.ColumnValueParameterList;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.jdbc.JdbcDeleteMutation;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;
import org.hibernate.sql.model.jdbc.JdbcUpdateMutation;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER_DEBUG_ENABLED;

/**
 * A {@link CollectionPersister} for {@linkplain jakarta.persistence.ElementCollection
 * collections of values} and {@linkplain jakarta.persistence.ManyToMany many-to-many
 * associations}.
 *
 * @see OneToManyPersister
 *
 * @author Gavin King
 */
public class BasicCollectionPersister extends AbstractCollectionPersister {
	private final RowMutationOperations rowMutationOperations;
	private final InsertRowsCoordinator insertRowsCoordinator;
	private final UpdateRowsCoordinator updateCoordinator;
	private final DeleteRowsCoordinator deleteRowsCoordinator;
	private final RemoveCoordinator removeCoordinator;

	public boolean isCascadeDeleteEnabled() {
		return false;
	}

	@Deprecated(since = "6.0")
	public BasicCollectionPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			PersisterCreationContext creationContext) throws MappingException, CacheException {
		this( collectionBinding, cacheAccessStrategy, (RuntimeModelCreationContext) creationContext );
	}

	public BasicCollectionPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) throws MappingException, CacheException {
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
		final boolean performUpdates = getCollectionSemantics().getCollectionClassification().isRowUpdatePossible()
				&& ArrayHelper.isAnyTrue( elementColumnIsSettable )
				&& !isInverse();

		if ( !performUpdates ) {
			if ( MODEL_MUTATION_LOGGER_DEBUG_ENABLED ) {
				MODEL_MUTATION_LOGGER.debugf(
						"Skipping collection row updates - %s",
						getRolePath()
				);
			}
			return new UpdateRowsCoordinatorNoOp( this );
		}

		return new UpdateRowsCoordinatorStandard( this, rowMutationOperations, getFactory() );
	}

	private InsertRowsCoordinator buildInsertRowCoordinator() {
		if ( isInverse() || !isRowInsertEnabled() ) {
			if ( MODEL_MUTATION_LOGGER_DEBUG_ENABLED ) {
				MODEL_MUTATION_LOGGER.debugf(
						"Skipping collection inserts - %s",
						getRolePath()
				);
			}
			return new InsertRowsCoordinatorNoOp( this );
		}

		return new InsertRowsCoordinatorStandard( this, rowMutationOperations );
	}

	private DeleteRowsCoordinator buildDeleteRowCoordinator() {
		if ( ! needsRemove() ) {
			if ( MODEL_MUTATION_LOGGER_DEBUG_ENABLED ) {
				MODEL_MUTATION_LOGGER.debugf(
						"Skipping collection row deletions - %s",
						getRolePath()
				);
			}
			return new DeleteRowsCoordinatorNoOp( this );
		}

		return new DeleteRowsCoordinatorStandard( this, rowMutationOperations, hasPhysicalIndexColumn() );
	}

	private RemoveCoordinator buildDeleteAllCoordinator() {
		if ( ! needsRemove() ) {
			if ( MODEL_MUTATION_LOGGER_DEBUG_ENABLED ) {
				MODEL_MUTATION_LOGGER.debugf(
						"Skipping collection removals - %s",
						getRolePath()
				);
			}
			return new RemoveCoordinatorNoOp( this );
		}

		return new RemoveCoordinatorStandard( this, this::buildDeleteAllOperation );
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
				&& ArrayHelper.isAnyTrue( elementColumnIsSettable )
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
		if ( getIdentifierTableMapping().getInsertDetails().getCustomSql() != null ) {
			return buildCustomSqlInsertRowOperation( tableReference );
		}

		return buildGeneratedInsertRowOperation( tableReference );

	}

	private JdbcMutationOperation buildCustomSqlInsertRowOperation(MutatingTableReference tableReference) {
		final TableInsertBuilderStandard insertBuilder = new TableInsertBuilderStandard( this, tableReference, getFactory() );
		applyInsertDetails( insertBuilder );

		final TableInsert tableInsert = insertBuilder.buildMutation();
		return tableInsert.createMutationOperation( null, getFactory() );
	}

	private void applyInsertDetails(TableInsertBuilderStandard insertBuilder) {
		final PluralAttributeMapping attributeMapping = getAttributeMapping();

		final ForeignKeyDescriptor foreignKey = attributeMapping.getKeyDescriptor();
		foreignKey.getKeyPart().forEachSelectable( insertBuilder );

		final CollectionIdentifierDescriptor identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		final CollectionPart indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.forEachSelectable( insertBuilder );
		}
		else if ( indexDescriptor != null ) {
			indexDescriptor.forEachInsertable( insertBuilder );
		}

		attributeMapping.getElementDescriptor().forEachInsertable( insertBuilder );
	}

	private JdbcMutationOperation buildGeneratedInsertRowOperation(MutatingTableReference tableReference) {
		final TableMutation<JdbcMutationOperation> sqlAst = generateInsertRowAst( tableReference );

		final SqlAstTranslator<JdbcMutationOperation> translator = getFactory().getJdbcServices()
				.getDialect()
				.getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( sqlAst, getFactory() );

		return translator.translate( null, MutationQueryOptions.INSTANCE );
	}

	private TableMutation<JdbcMutationOperation> generateInsertRowAst(MutatingTableReference tableReference) {
		final PluralAttributeMapping pluralAttribute = getAttributeMapping();
		assert pluralAttribute != null;

		final ForeignKeyDescriptor fkDescriptor = pluralAttribute.getKeyDescriptor();
		assert fkDescriptor != null;

		final TableInsertBuilderStandard insertBuilder = new TableInsertBuilderStandard( this, tableReference, getFactory() );
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
			RowMutationOperations.ValuesBindingConsumer jdbcValueConsumer) {
		final PluralAttributeMapping attributeMapping = getAttributeMapping();

		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + getNavigableRole().getFullPath() );
		}

		final ForeignKeyDescriptor foreignKey = attributeMapping.getKeyDescriptor();
		foreignKey.getKeyPart().decompose( key, jdbcValueConsumer, session );

		final MutableInteger columnPositionCount = new MutableInteger();

		if ( attributeMapping.getIdentifierDescriptor() != null ) {
			getAttributeMapping().getIdentifierDescriptor().decompose(
					collection.getIdentifier( rowValue, rowPosition ),
					jdbcValueConsumer,
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

			getAttributeMapping().getIndexDescriptor().decompose(
					incrementIndexByBase( collection.getIndex( rowValue, rowPosition, this ) ),
					(jdbcValue, jdbcValueMapping) -> {
						if ( !jdbcValueMapping.getContainingTableExpression().equals( getTableName() ) ) {
							// indicates a many-to-many mapping and the index is contained on the
							// associated entity table - we skip it here
							return;
						}
						final int columnPosition = columnPositionCount.getAndIncrement();
						if ( indexColumnIsSettable[columnPosition] ) {
							jdbcValueConsumer.consume( jdbcValue, jdbcValueMapping );
						}
					},
					session
			);

			columnPositionCount.set( 0 );
		}

		attributeMapping.getElementDescriptor().decompose(
				collection.getElement( rowValue ),
				(jdbcValue, jdbcValueMapping) -> {
					final int columnPosition = columnPositionCount.getAndIncrement();
					if ( elementColumnIsSettable[columnPosition] ) {
						jdbcValueConsumer.consume( jdbcValue, jdbcValueMapping );
					}
				},
				session
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Update handling

	private JdbcMutationOperation generateUpdateRowOperation(MutatingTableReference tableReference) {
		if ( getIdentifierTableMapping().getInsertDetails().getCustomSql() != null ) {
			return buildCustomSqlUpdateRowOperation( tableReference );
		}

		return buildGeneratedUpdateRowOperation( tableReference );

	}

	private JdbcMutationOperation buildCustomSqlUpdateRowOperation(MutatingTableReference tableReference) {
		final PluralAttributeMapping attribute = getAttributeMapping();
		assert attribute != null;

		final ForeignKeyDescriptor foreignKey = attribute.getKeyDescriptor();
		assert foreignKey != null;

		final int keyColumnCount = foreignKey.getJdbcTypeCount();
		final ColumnValueParameterList parameterBinders = new ColumnValueParameterList(
				tableReference,
				ParameterUsage.RESTRICT,
				keyColumnCount
		);
		foreignKey.getKeyPart().forEachSelectable( parameterBinders );

		return new JdbcUpdateMutation(
				getCollectionTableMapping(),
				this,
				getCollectionTableMapping().getDeleteDetails().getCustomSql(),
				getCollectionTableMapping().getDeleteDetails().isCallable(),
				getCollectionTableMapping().getDeleteDetails().getExpectation(),
				parameterBinders
		);
	}

	private JdbcMutationOperation buildGeneratedUpdateRowOperation(MutatingTableReference tableReference) {
		final RestrictedTableMutation<JdbcMutationOperation> sqlAst = generateUpdateRowAst( tableReference );

		final SqlAstTranslator<JdbcMutationOperation> translator = getFactory().getJdbcServices()
				.getDialect()
				.getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( sqlAst, getFactory() );

		return translator.translate( null, MutationQueryOptions.INSTANCE );
	}

	private RestrictedTableMutation<JdbcMutationOperation> generateUpdateRowAst(MutatingTableReference tableReference) {
		final PluralAttributeMapping attribute = getAttributeMapping();
		assert attribute != null;

		final TableUpdateBuilderStandard<?> updateBuilder = new TableUpdateBuilderStandard<>( this, tableReference, getFactory() );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SET

		attribute.getElementDescriptor().forEachUpdatable( updateBuilder );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// WHERE

		if ( attribute.getIdentifierDescriptor() != null ) {
			attribute.getIdentifierDescriptor().forEachSelectable( updateBuilder::addKeyRestriction );
		}
		else {
			attribute.getKeyDescriptor().getKeyPart().forEachSelectable( updateBuilder::addKeyRestriction );

			if ( attribute.getIndexDescriptor() != null && !indexContainsFormula ) {
				attribute.getIndexDescriptor().forEachSelectable( updateBuilder::addKeyRestriction );
			}
			else {
				attribute.getElementDescriptor().forEachSelectable( (selectionIndex, selectableMapping) -> {
					if ( selectableMapping.isFormula() || selectableMapping.isNullable() ) {
						return;
					}
					updateBuilder.addKeyRestriction( selectableMapping );
				} );
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
			RowMutationOperations.ValuesBindingConsumer jdbcValueConsumer) {
		final Object element = collection.getElement( entry );
		final CollectionPart elementDescriptor = getAttributeMapping().getElementDescriptor();
		elementDescriptor.decompose(
				element,
				(jdbcValue, jdbcValueMapping) -> {
					if ( !jdbcValueMapping.isUpdateable() || jdbcValueMapping.isFormula() ) {
						return;
					}
					jdbcValueConsumer.consumeJdbcValueBinding(
							jdbcValue,
							jdbcValueMapping,
							ParameterUsage.SET
					);
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
			ModelPart.JdbcValueConsumer restrictor) {
		if ( getAttributeMapping().getIdentifierDescriptor() != null ) {
			final CollectionIdentifierDescriptor identifierDescriptor = getAttributeMapping().getIdentifierDescriptor();
			final Object identifier = collection.getIdentifier( entry, entryPosition );
			identifierDescriptor.decompose( identifier, restrictor, session );
		}
		else {
			getAttributeMapping().getKeyDescriptor().getKeyPart().decompose( key, restrictor, session );

			if ( getAttributeMapping().getIndexDescriptor() != null && !indexContainsFormula ) {
				final Object index = collection.getIndex( entry, entryPosition, getAttributeMapping().getCollectionDescriptor() );
				final Object adjustedIndex = incrementIndexByBase( index );
				getAttributeMapping().getIndexDescriptor().decompose( adjustedIndex, restrictor, session );
			}
			else {
				final Object snapshotElement = collection.getSnapshotElement( entry, entryPosition );
				getAttributeMapping().getElementDescriptor().decompose(
						snapshotElement,
						(jdbcValue, jdbcValueMapping) -> {
							if ( jdbcValueMapping.isNullable() && jdbcValueMapping.isFormula() ) {
								return;
							}
							restrictor.consume( jdbcValue, jdbcValueMapping );
						},
						session
				);
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Delete handling

	private JdbcMutationOperation generateDeleteRowOperation(MutatingTableReference tableReference) {
		if ( getIdentifierTableMapping().getDeleteRowDetails().getCustomSql() != null ) {
			return buildCustomSqlDeleteRowOperation( tableReference );
		}

		return buildGeneratedDeleteRowOperation( tableReference );
	}

	private JdbcMutationOperation buildCustomSqlDeleteRowOperation(MutatingTableReference tableReference) {
		final PluralAttributeMapping attribute = getAttributeMapping();
		assert attribute != null;

		final ForeignKeyDescriptor foreignKey = attribute.getKeyDescriptor();
		assert foreignKey != null;

		final CollectionTableMapping tableMapping = (CollectionTableMapping) tableReference.getTableMapping();

		final int keyColumnCount = foreignKey.getJdbcTypeCount();
		final ColumnValueParameterList parameterBinders = new ColumnValueParameterList(
				tableReference,
				ParameterUsage.RESTRICT,
				keyColumnCount
		);
		foreignKey.getKeyPart().forEachSelectable( parameterBinders );

		return new JdbcDeleteMutation(
				tableMapping,
				this,
				tableMapping.getDeleteDetails().getCustomSql(),
				tableMapping.getDeleteDetails().isCallable(),
				tableMapping.getDeleteDetails().getExpectation(),
				parameterBinders
		);
	}

	private JdbcMutationOperation buildGeneratedDeleteRowOperation(MutatingTableReference tableReference) {
		final RestrictedTableMutation<JdbcMutationOperation> sqlAst = generateDeleteRowAst( tableReference );

		final SqlAstTranslator<JdbcMutationOperation> translator = getFactory().getJdbcServices()
				.getDialect()
				.getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( sqlAst, getFactory() );

		return translator.translate( null, MutationQueryOptions.INSTANCE );
	}

	private RestrictedTableMutation<JdbcMutationOperation> generateDeleteRowAst(MutatingTableReference tableReference) {
		final PluralAttributeMapping pluralAttribute = getAttributeMapping();
		assert pluralAttribute != null;

		final ForeignKeyDescriptor fkDescriptor = pluralAttribute.getKeyDescriptor();
		assert fkDescriptor != null;

		final TableDeleteBuilderStandard deleteBuilder = new TableDeleteBuilderStandard( this, tableReference, getFactory() );

		if ( pluralAttribute.getIdentifierDescriptor() != null ) {
			deleteBuilder.addKeyRestriction( pluralAttribute.getIdentifierDescriptor() );
		}
		else {
			pluralAttribute
					.getKeyDescriptor()
					.getKeyPart()
					.forEachSelectable( (index, selectable) -> deleteBuilder.addKeyRestriction( selectable ) );

			if ( hasIndex && !indexContainsFormula ) {
				assert pluralAttribute.getIndexDescriptor() != null;
				pluralAttribute
						.getIndexDescriptor()
						.forEachSelectable( (index, selectable) -> deleteBuilder.addKeyRestriction( selectable ) );
			}
			else {
				pluralAttribute
						.getElementDescriptor()
						.forEachSelectable( (index, selectable) -> deleteBuilder.addKeyRestriction( selectable ) );
			}
		}

		//noinspection unchecked,rawtypes
		return (RestrictedTableMutation) deleteBuilder.buildMutation();
	}

	private void applyDeleteRowRestrictions(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			ModelPart.JdbcValueConsumer restrictor) {
		final PluralAttributeMapping attributeMapping = getAttributeMapping();

		if ( attributeMapping.getIdentifierDescriptor() != null ) {
			attributeMapping.getIdentifierDescriptor().decompose( rowValue, restrictor, session );
		}
		else {
			getAttributeMapping().getKeyDescriptor().getKeyPart().decompose( keyValue, restrictor, session );

			if ( hasPhysicalIndexColumn() ) {
				attributeMapping.getIndexDescriptor().decompose(
						incrementIndexByBase( rowValue ),
						restrictor,
						session
				);
			}
			else {
				attributeMapping.getElementDescriptor().decompose( rowValue, restrictor, session );
			}
		}
	}

	@Override
	public boolean consumesEntityAlias() {
		return false;
	}

	@Override
	public boolean consumesCollectionAlias() {
		return true;
	}

	@Override
	public boolean isOneToMany() {
		return false;
	}

	@Override
	public boolean isManyToMany() {
		return elementType.isEntityType(); //instanceof AssociationType;
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
