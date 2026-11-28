package org.hibernate.persister.collection;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.WrongClassException;
import org.hibernate.action.queue.internal.PreparedCollectionMutation;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.spi.decompose.collection.CollectionDecomposer;
import org.hibernate.action.queue.internal.decompose.collection.StandardOneToManyDecomposer;
import org.hibernate.action.queue.internal.decompose.collection.TablePerSubclassOneToManyDecomposer;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.loader.ast.internal.CollectionElementLoader;
import org.hibernate.loader.ast.internal.CollectionIndexSnapshotLoader;
import org.hibernate.loader.ast.internal.EntityConcreteTypeLoader;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.OneToManyCollectionPart;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.OperationProducer;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.RowMutationOperations;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.collection.mutation.WriteIndexCoordinator;
import org.hibernate.persister.collection.mutation.WriteIndexCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.WriteIndexCoordinatorStandard;
import org.hibernate.persister.collection.mutation.WriteIndexCoordinatorTablePerSubclass;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.persister.entity.mutation.HistoryRevisionWriter;
import org.hibernate.persister.entity.mutation.PartitionKeyValue;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.persister.state.spi.StateManagement;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameterList;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.RestrictedTableMutation;
import org.hibernate.sql.ast.spi.model.TableUpdate;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.hibernate.internal.util.collections.ArrayHelper.isAnyTrue;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.persister.collection.mutation.RowMutationOperations.DEFAULT_RESTRICTOR;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;
import static org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder.NULL;

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
	private final StateManagement stateManagement;

	private final InsertRowsCoordinator insertRowsCoordinator;
	private final UpdateRowsCoordinator updateRowsCoordinator;
	private final DeleteRowsCoordinator deleteRowsCoordinator;
	private final RemoveCoordinator removeCoordinator;
	private final WriteIndexCoordinator writeIndexCoordinator;

	private boolean useTablePerSubclassDecomposition;
	@Nullable
	private EntityConcreteTypeLoader concreteElementTypeLoader;
	private final boolean keyIsNullable;

	private final boolean auditIndex;
	private CollectionIndexSnapshotLoader indexSnapshotLoader;

	/// Whether the attribute which holds this collection is excluded from temporal versioning.
	private final boolean temporalExcluded;
	/// The columns of the row of an element which are written by this collection.
	private final List<String> writtenElementColumns;
	private List<SelectableMapping> writtenElementSelectables;
	private boolean elementRevisions;
	private CollectionElementLoader elementLoader;
	final boolean doWriteEvenWhenInverse; // contrary to intent of JPA

	public OneToManyPersister(
			@Nonnull Collection collectionBinding,
			@Nullable CollectionDataAccess cacheAccessStrategy,
			@Nonnull RuntimeModelCreationContext creationContext)
					throws MappingException, CacheException {
		super( collectionBinding, cacheAccessStrategy, creationContext );
		keyIsNullable = collectionBinding.getKey().isNullable();

		// the index is written by an update of its column alone when the key is not written by
		// the collection, because the association is inverse, or its join column not updatable
		doWriteEvenWhenInverse =
				( isInverse || !collectionBinding.getKey().isUpdateable() )
					&& hasIndex()
					&& !indexContainsFormula
					&& isAnyTrue( indexColumnIsSettable )
					&& !getElementPersister().managesColumns( castNonNull( indexColumnNames ) );

		if ( isIndexNeverWritten( collectionBinding ) ) {
			throw new UnsupportedMappingException( "Collection '" + collectionBinding.getRole()
					+ "' is a one-to-many association with an order column which is not updatable"
					+ " (the order column of a one-to-many association is written by an SQL 'update' statement,"
					+ " so it would never be written)" );
		}

		auditIndex = doWriteEvenWhenInverse
				&& collectionBinding.getCollectionTable().getAuditedInverseCollections().contains( collectionBinding );
		writtenElementColumns = writtenElementColumns();
		temporalExcluded = isHistoryStrategy() && isTemporalExcluded( collectionBinding );
		rowMutationOperations = buildRowMutationOperations();
		stateManagement = collectionBinding.getStateManagement();
		final var legacyIntegration = stateManagement.getLegacyIntegration();
		insertRowsCoordinator = legacyIntegration.createInsertRowsCoordinator( this );
		updateRowsCoordinator = legacyIntegration.createUpdateRowsCoordinator( this );
		deleteRowsCoordinator = legacyIntegration.createDeleteRowsCoordinator( this );
		removeCoordinator = legacyIntegration.createRemoveCoordinator( this );
		writeIndexCoordinator = buildWriteIndexCoordinator();
	}

	@Override
	public void prepareMappingModel(@Nonnull MappingModelCreationProcess creationProcess, @Nonnull Collection bootCollectionDescriptor) {
		super.prepareMappingModel( creationProcess, bootCollectionDescriptor );

		useTablePerSubclassDecomposition = isElementTablePerSubclass();
	}

	private boolean isElementTablePerSubclass() {
		final var elementPersister = getElementPersister();
		return elementPersister instanceof UnionSubclassEntityPersister
			&& elementPersister.hasSubclasses();
	}

	/**
	 * The persister of the concrete subclass of the given element of this collection.
	 * <p>
	 * An uninitialized proxy obtained through a type with subclasses does not know its
	 * concrete subclass, and might belong to another session, so its concrete subclass
	 * is resolved from its identifier using the given session.
	 */
	@Nonnull
	public EntityPersister getConcreteElementPersister(
			@Nonnull Object element,
			@Nonnull SharedSessionContractImplementor session) {
		final var mappingMetamodel = getFactory().getMappingMetamodel();
		final var lazyInitializer = extractLazyInitializer( element );
		if ( lazyInitializer != null && lazyInitializer.isUninitialized() ) {
			final var proxiedPersister = mappingMetamodel.getEntityDescriptor( lazyInitializer.getEntityName() );
			if ( !proxiedPersister.hasSubclasses() ) {
				return proxiedPersister;
			}
			final Object id = lazyInitializer.getInternalIdentifier();
			final var concretePersister = resolveConcreteElementPersister( id, session );
			if ( !concretePersister.isTypeOrSuperType( proxiedPersister ) ) {
				throw new WrongClassException(
						"object was of wrong type " + concretePersister.getEntityName(),
						id,
						proxiedPersister.getEntityName()
				);
			}
			return concretePersister;
		}
		else {
			return mappingMetamodel.getEntityDescriptor( session.bestGuessEntityName( element ) );
		}
	}

	/**
	 * The persisters of the element type of this collection and of each of its subclasses
	 * which have a table of their own. An abstract class with subclasses has none.
	 */
	@Nonnull
	public List<EntityPersister> getElementPersistersWithTable() {
		final var elementPersister = getElementPersister();
		// the tables of the element type, of its superclasses, and of its subclasses
		final Set<String> tableNames = new HashSet<>();
		elementPersister.visitConstraintOrderedTables(
				(tableName, keyColumnVisitationSupplier) -> tableNames.add( tableName ) );
		final List<EntityPersister> persisters = new ArrayList<>();
		if ( tableNames.contains( elementPersister.getMappedTableDetails().getTableName() ) ) {
			persisters.add( elementPersister );
		}
		for ( var subMappingType : elementPersister.getSubMappingTypes() ) {
			final var subclassPersister = subMappingType.getEntityPersister();
			if ( tableNames.contains( subclassPersister.getMappedTableDetails().getTableName() ) ) {
				persisters.add( subclassPersister );
			}
		}
		return persisters;
	}

	@Nonnull
	private EntityPersister resolveConcreteElementPersister(
			@Nonnull Object id,
			@Nonnull SharedSessionContractImplementor session) {
		final var elementPersister = getElementPersister();
		final Object entity =
				session.getPersistenceContextInternal()
						.getEntity( session.generateEntityKey( id, elementPersister ) );
		if ( entity != null ) {
			return getFactory().getMappingMetamodel()
					.getEntityDescriptor( session.bestGuessEntityName( entity ) );
		}
		else {
			var concreteElementTypeLoader = this.concreteElementTypeLoader;
			if ( concreteElementTypeLoader == null ) {
				this.concreteElementTypeLoader = concreteElementTypeLoader =
						new EntityConcreteTypeLoader( elementPersister, getFactory() );
			}
			return concreteElementTypeLoader.getConcreteType( id, session );
		}
	}

	private List<String> writtenElementColumns() {
		final boolean keyWritten = !isInverse && isRowInsertEnabled();
		final List<String> columns = new ArrayList<>();
		if ( keyWritten ) {
			addColumns( keyColumnNames, columns );
		}
		if ( hasIndex() && !indexContainsFormula && ( keyWritten || doWriteEvenWhenInverse ) ) {
			addColumns( castNonNull( indexColumnNames ), columns );
		}
		return columns;
	}

	private static boolean isTemporalExcluded(Collection collectionBinding) {
		final String propertyPath =
				collectionBinding.getRole().substring( collectionBinding.getOwnerEntityName().length() + 1 );
		try {
			return collectionBinding.getOwner().getRecursiveProperty( propertyPath ).isTemporalExcluded();
		}
		catch (MappingException e) {
			// the collection is not held by a property which can be annotated
			return false;
		}
	}

	private List<SelectableMapping> writtenElementSelectables() {
		final List<SelectableMapping> selectables = new ArrayList<>();
		final SelectableConsumer consumer = (index, selectable) -> {
			if ( writtenElementColumns.contains( selectable.getSelectionExpression() ) ) {
				selectables.add( selectable );
			}
		};
		final var attributeMapping = getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().forEachSelectable( consumer );
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.forEachSelectable( consumer );
		}
		return selectables;
	}

	private static void addColumns(String[] columnNames, List<String> columns) {
		for ( String columnName : columnNames ) {
			// a formula has no column
			if ( columnName != null ) {
				columns.add( columnName );
			}
		}
	}

	@Override
	public void postInstantiate() throws MappingException {
		super.postInstantiate();
		if ( hasAuditedIndex() ) {
			indexSnapshotLoader = new CollectionIndexSnapshotLoader(
					getAttributeMapping(), new LoadQueryInfluencers( getFactory() )
			);
		}
		checkElementRevisions( getElementPersister() );
		for ( var subMappingType : getElementPersister().getSubMappingTypes() ) {
			checkElementRevisions( subMappingType.getEntityPersister() );
		}
		if ( elementRevisions ) {
			writtenElementSelectables = writtenElementSelectables();
			if ( needsRemove() ) {
				elementLoader = new CollectionElementLoader( getAttributeMapping(), new LoadQueryInfluencers( getFactory() ) );
			}
		}

		// Initialize JDBC operations after entity persisters have completed their initialization
		decomposer = buildCollectionDecomposer();
	}

	@Nonnull
	protected CollectionDecomposer buildCollectionDecomposer() {
		final var mutationPlanContributor = stateManagement.getGraphIntegration()
				.createCollectionMutationPlanContributor( this );
		return useTablePerSubclassDecomposition
				? new TablePerSubclassOneToManyDecomposer( this, getFactory(), mutationPlanContributor )
				: new StandardOneToManyDecomposer( this, getFactory(), mutationPlanContributor );
	}

	public boolean hasAuditedIndex() {
		return auditIndex && getElementPersister().getAuditMapping() != null;
	}

	/// Notes whether a revision of the given element type holds a column written
	/// by this collection. A version of an entity has one revision, and this
	/// collection does not increment the version of an element, so it can only
	/// update the open revision of a versioned element.
	private void checkElementRevisions(EntityPersister elementPersister) {
		final var writer = elementPersister.getHistoryRevisionWriter();
		if ( writer != null && writer.copiesAny( writtenElementColumns ) ) {
			if ( elementPersister.isVersioned() && !temporalExcluded ) {
				throw new UnsupportedMappingException( "Collection '" + getRole()
						+ "' is a one-to-many association which writes the row of versioned entity '"
						+ elementPersister.getEntityName()
						+ "' and must be annotated '@Temporal.Excluded' for HISTORY_TABLE"
						+ " (the association writes its join column or order column without incrementing"
						+ " the version of the entity, which has one revision for each version)" );
			}
			elementRevisions = true;
		}
	}

	/// Whether a revision of an element in its
	/// [history table][org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE]
	/// holds a column written by this collection. If so, the history of an element
	/// must record what this collection writes to its row.
	public boolean hasElementRevisions() {
		return elementRevisions;
	}

	/// The elements whose rows are written when the given collection is created
	/// or updated: every element of a collection which is new to its role and key,
	/// or else each element which was added, moved, or removed since the collection
	/// was last written.
	public List<Object> getWrittenElements(
			PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session) {
		final boolean hasOwnSnapshot = hasOwnSnapshot( collection, key, session );
		final List<Object> elements = new ArrayList<>();
		final var entries = collection.wasInitialized() ? collection.entries( this ) : collection.queuedAdditionIterator();
		for ( int i = 0; entries.hasNext(); i++ ) {
			final var entry = entries.next();
			if ( entry != null && collection.entryExists( entry, i )
					&& !( hasOwnSnapshot && isUnchanged( collection, entry, i ) ) ) {
				elements.add( collection.getElement( entry ) );
			}
		}
		if ( hasOwnSnapshot && needsRemove() ) {
			addDisplacedElements( collection, elements );
		}
		return elements;
	}

	/// The elements whose rows are written when every row of the collection
	/// with the given key is removed. They are read from the database, since
	/// the removal is not restricted by a filter, and so it writes rows which
	/// a collection loaded with the filter enabled does not hold.
	public List<Object> getRemovedElements(Object key, SharedSessionContractImplementor session) {
		return elementLoader.load( key, session );
	}

	/// Adds the elements of the snapshot of the given collection which were
	/// removed from the collection, or replaced by another element.
	private void addDisplacedElements(PersistentCollection<?> collection, List<Object> elements) {
		collection.getDeletes( this, true ).forEachRemaining( elements::add );
		final var entries = collection.entries( this );
		for ( int i = 0; entries.hasNext(); i++ ) {
			final var entry = entries.next();
			if ( entry != null && collection.entryExists( entry, i )
					&& collection.needsUpdating( entry, i, getAttributeMapping() ) ) {
				elements.add( collection.getSnapshotElement( entry, i ) );
			}
		}
	}

	/// Record that the rows of the given elements are written by this collection,
	/// so that the history of each of them holds what was written.
	public void recordElementRevisions(List<Object> elements, SharedSessionContractImplementor session) {
		for ( Object element : elements ) {
			if ( element != null ) {
				recordElementRevision( element, session );
			}
		}
	}

	private void recordElementRevision(Object element, SharedSessionContractImplementor session) {
		final var lazyInitializer = extractLazyInitializer( element );
		final boolean uninitialized = lazyInitializer != null && lazyInitializer.isUninitialized();
		// only a class with a table of its own has a history table of its own
		final var concretePersister = uninitialized && !useTablePerSubclassDecomposition
				? getFactory().getMappingMetamodel().getEntityDescriptor( lazyInitializer.getEntityName() )
				: getConcreteElementPersister( element, session );
		final var writer = concretePersister.getHistoryRevisionWriter();
		if ( writer == null || !writer.copiesAny( writtenElementColumns ) ) {
			return;
		}
		if ( uninitialized ) {
			recordElementRevision( lazyInitializer.getInternalIdentifier(), null, writer, session );
		}
		else {
			final Object entity = lazyInitializer == null ? element : lazyInitializer.getImplementation();
			final var entityEntry = session.getPersistenceContextInternal().getEntry( entity );
			if ( entityEntry == null ) {
				recordElementRevision( concretePersister.getIdentifier( entity, session ), null, writer, session );
			}
			else if ( !entityEntry.getStatus().isDeletedOrGone() ) {
				recordElementRevision( entityEntry.getId(), entityEntry.getLoadedState(), writer, session );
			}
		}
	}

	/// @param state the state of the row before the changeset, if it is known
	private void recordElementRevision(
			@Nullable Object id, @Nullable Object[] state,
			HistoryRevisionWriter writer, SharedSessionContractImplementor session) {
		if ( id != null ) {
			session.getHistoryWorkQueue().entityRowWritten( id, !temporalExcluded, writtenElementSelectables,
					PartitionKeyValue.fromState( writer.getEntityPersister(), state, session ), writer, session );
		}
	}

	/// Whether the given collection holds a snapshot of the rows with the given key.
	private boolean hasOwnSnapshot(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var collectionEntry = persistenceContext.getCollectionEntry( collection );
		return collection.wasInitialized()
			&& collectionEntry != null
			&& collectionEntry.getLoadedPersister() == this
			&& collectionEntry.getSnapshot() != null
			&& persistenceContext.getCollection( session.generateCollectionKey( this, key ) ) == collection;
	}

	/// Whether the given entry of a collection has the row it had in the snapshot.
	private boolean isUnchanged(PersistentCollection<?> collection, Object entry, int position) {
		return !collection.includeInInsert( entry, position, collection, getAttributeMapping() )
			&& !collection.needsUpdating( entry, position, getAttributeMapping() );
	}

	/// Capture the destination's indexes before a replacement writes them.
	public @Nullable Object getReplacementIndexSnapshot(
			PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session) {
		final var previous = session.getPersistenceContextInternal().getCollection( session.generateCollectionKey( this, key ) );
		if ( previous == null || previous == collection || !collection.wasInitialized() || collection.empty() ) {
			return null;
		}
		if ( previous.wasInitialized() ) {
			return previous.getStoredSnapshot();
		}
		// A child inserted by an earlier flush may still have a null order column.
		// Read index/element pairs without trying to assemble the old list.
		final var influencers = session.getLoadQueryInfluencers();
		final var loader = getAttributeMapping().isAffectedByInfluencers( influencers, true )
				? new CollectionIndexSnapshotLoader( getAttributeMapping(), influencers )
				: indexSnapshotLoader;
		return loader.load( key, session );
	}

	/** Visit the elements which need a new audit row for their changed order column or map key. */
	public void visitElementsWithChangedIndex(
			PersistentCollection<?> collection, Object key, @Nullable Object previousSnapshot,
			SharedSessionContractImplementor session, BiConsumer<EntityEntry, Object> consumer) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final boolean compareOwnSnapshot = hasOwnSnapshot( collection, key, session );
		final var entries = collection.wasInitialized() ? collection.entries( this ) : collection.queuedAdditionIterator();
		for ( int i = 0; entries.hasNext(); i++ ) {
			final var entry = entries.next();
			if ( !collection.entryExists( entry, i ) ) {
				continue;
			}
			final Object element = collection.getElement( entry );
			if ( previousSnapshot != null && getElementType().isEqual(
					snapshotElement( previousSnapshot, collection, entry, i ), element, getFactory() ) ) {
				continue;
			}
			if ( compareOwnSnapshot && isUnchanged( collection, entry, i ) ) {
				continue;
			}
			final var entity = Hibernate.unproxy( element );
			final var entityEntry = persistenceContext.getEntry( entity );
			if ( entityEntry != null && !entityEntry.getStatus().isDeletedOrGone() ) {
				consumer.accept( entityEntry, entity );
			}
		}
	}

	private Object snapshotElement(Object snapshot, PersistentCollection<?> collection, Object entry, int position) {
		if ( snapshot instanceof List<?> list ) {
			return position < list.size() ? list.get( position ) : null;
		}
		if ( snapshot instanceof Map<?, ?> map ) {
			return map.get( collection.getIndex( entry, position, this ) );
		}
		if ( snapshot instanceof Object[] array && position < array.length ) {
			return array[position];
		}
		return null;
	}

	public boolean isDoWriteEvenWhenInverse() {
		return doWriteEvenWhenInverse;
	}

	/// Whether the update which writes the index of an element is also restricted by
	/// the key of the collection. The index of an owned list whose join column is not
	/// updatable is written by an update of its own, but the key is never changed, so
	/// an element which belongs to the list of another owner keeps its row, and its
	/// index, in that list.
	public boolean isIndexWrittenWithKeyRestriction() {
		return doWriteEvenWhenInverse && !isInverse;
	}

	@Nonnull
	@Override
	public RowMutationOperations getRowMutationOperations() {
		return rowMutationOperations;
	}

	@Nonnull
	public InsertRowsCoordinator getInsertRowsCoordinator() {
		return insertRowsCoordinator;
	}

	@Nonnull
	public UpdateRowsCoordinator getUpdateRowsCoordinator() {
		return updateRowsCoordinator;
	}

	@Nonnull
	public DeleteRowsCoordinator getDeleteRowsCoordinator() {
		return deleteRowsCoordinator;
	}

	@Nonnull
	@Override
	public RemoveCoordinator getRemoveCoordinator() {
		return removeCoordinator;
	}

	@Nonnull
	public WriteIndexCoordinator getWriteIndexCoordinator() {
		return writeIndexCoordinator;
	}

	@Override
	public boolean isRowDeleteEnabled() {
		return super.isRowDeleteEnabled() && keyIsNullable;
	}

	@Override
	public void recreate(@Nonnull PersistentCollection<?> collection, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session)
			throws HibernateException {
		getInsertRowsCoordinator().insertRows( collection, id, collection::includeInRecreate, session );
		writeIndex( collection, collection.entries( this ), id, true, session );
		elementRowsWritten( collection, id, session );
	}

	/// The last step of an update of the rows of a collection.
	@Override
	public void insertRows(@Nonnull PersistentCollection<?> collection, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session)
			throws HibernateException {
		getInsertRowsCoordinator().insertRows( collection, id, collection::includeInInsert, session );
		writeIndex( collection, collection.entries( this ), id, true, session );
		elementRowsWritten( collection, id, session );
	}

	@Override
	public void remove(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) throws HibernateException {
		if ( elementRevisions && needsRemove() ) {
			// the elements cannot be identified once their rows are written
			final var elements = getRemovedElements( id, session );
			super.remove( id, session );
			elementRowsWritten( elements, session );
		}
		else {
			super.remove( id, session );
		}
	}

	@Override
	public void updateRows(@Nonnull PersistentCollection<?> collection, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		getUpdateRowsCoordinator().updateRows( id, collection, session );
//		oldUpdateRows( collection, id, session );
	}

	@Override
	public void deleteRows(@Nonnull PersistentCollection<?> collection, @Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		getDeleteRowsCoordinator().deleteRows( collection, key, session );
	}

	@Override
	protected void doProcessQueuedOps(@Nonnull PersistentCollection<?> collection, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session)
			throws HibernateException {
		writeIndex( collection, collection.queuedAdditionIterator(), id, false, session );
		elementRowsWritten( collection, id, session );
	}

	private void elementRowsWritten(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session) {
		if ( elementRevisions ) {
			// the snapshot of the collection is not replaced until the action completes
			elementRowsWritten( getWrittenElements( collection, key, session ), session );
		}
	}

	private void elementRowsWritten(List<Object> elements, SharedSessionContractImplementor session) {
		recordElementRevisions( elements, session );
		session.getHistoryWorkQueue().currentRowsWritten( session );
	}

	private void writeIndex(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Iterator<?> entries,
			@Nonnull Object key,
			boolean resetIndex,
			@Nonnull SharedSessionContractImplementor session) {
		final var snapshot = hasAuditedIndex() ? getReplacementIndexSnapshot( collection, key, session ) : null;
		writeIndexCoordinator.writeIndex( collection, entries, key, resetIndex, session );
		if ( hasAuditedIndex() ) {
			visitElementsWithChangedIndex( collection, key, snapshot, session,
					(entry, element) -> session.getAuditWorkQueue().indexedElementChanged( entry, element, session ) );
		}
	}

	public boolean isOneToMany() {
		return true;
	}

	@Override
	public boolean isManyToMany() {
		return false;
	}

	@Nonnull
	@Override
	public String getTableName() {
		return getElementPersister().getTableName();
	}

	protected void applyWhereFragments(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nullable String alias,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nullable SqlAstCreationState astCreationState) {
		super.applyWhereFragments( predicateConsumer, alias, tableGroup, useQualifier, astCreationState );
		if ( astCreationState != null && !astCreationState.supportsEntityNameUsage() ) {
			// We only need to apply discriminator for loads, since queries with joined
			// inheritance subtypes are already filtered by the entity name usage logic
			getElementPersister()
					.applyDiscriminator( predicateConsumer, alias, tableGroup, astCreationState );
		}
	}

	@Nonnull
	@Override
	public FilterAliasGenerator getFilterAliasGenerator(@Nonnull String rootAlias) {
		return getElementPersister().getFilterAliasGenerator( rootAlias );
	}

	@Nonnull
	@Override
	public FilterAliasGenerator getFilterAliasGenerator(@Nonnull TableGroup rootTableGroup) {
		return getElementPersister().getFilterAliasGenerator( rootTableGroup );
	}


	@Nonnull
	@Override
	public RestrictedTableMutation<JdbcMutationOperation> generateDeleteAllAst(@Nonnull MutatingTableReference tableReference) {
		final var attributeMapping = getAttributeMapping();
		assert attributeMapping != null;

		final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
		assert foreignKeyDescriptor != null;

		final int keyColumnCount = foreignKeyDescriptor.getJdbcTypeCount();
		final int valuesCount = hasIndex()
				? keyColumnCount + castNonNull( indexColumnNames ).length
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


	@Nonnull
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
		final RowMutationOperations.DeleteAllRowsBindings deleteAllRowsBindings;
		if ( !isInverse() && isRowDeleteEnabled() ) {
			deleteAllEntriesOperationProducer = this::buildDeleteAllOperation;
			deleteAllRowsBindings = this::applyDeleteAllRowsBindings;
		}
		else {
			deleteAllEntriesOperationProducer = null;
			deleteAllRowsBindings = null;
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
				deleteAllEntriesOperationProducer,
				deleteAllRowsBindings
		);
	}

	/**
	 * The index of a list is written by an SQL {@code update} of the table of
	 * the element. So an index column which is insertable but not updatable is
	 * never written, unless the element entity writes the column itself.
	 */
	private boolean isIndexNeverWritten(@Nonnull Collection collectionBinding) {
		return hasIndex()
			&& !indexContainsFormula
			// the key of a map is written if it is insertable or updatable
			&& !collectionBinding.isMap()
			&& isAnyTrue( indexColumnIsSettable )
			&& !isAnyTrue( indexColumnIsUpdatable )
			// the element of an owned list with a non-null key inserts the index as a backref
			&& ( isInverse || keyIsNullable )
			&& !getElementPersister().managesColumns( castNonNull( indexColumnNames ) );
	}

	@Nonnull
	private WriteIndexCoordinator buildWriteIndexCoordinator() {
		if ( !doWriteEvenWhenInverse ) {
			return new WriteIndexCoordinatorNoOp( this );
		}
		else if ( isElementTablePerSubclass() ) {
			return new WriteIndexCoordinatorTablePerSubclass( this, rowMutationOperations, getFactory() );
		}
		else {
			return new WriteIndexCoordinatorStandard( this, rowMutationOperations, getFactory() );
		}
	}

	@Nonnull
	private JdbcMutationOperation generateDeleteRowOperation(@Nonnull MutatingTableReference tableReference) {
		return getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.ModelMutation<>( getFactory(), generateDeleteRowAst( tableReference ) ) )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	@Nonnull
	public RestrictedTableMutation<JdbcMutationOperation> generateDeleteRowAst(@Nonnull MutatingTableReference tableReference) {
		// note that custom SQL delete row details are handled by CollectionRowUpdateBuilder
		final var updateBuilder = new org.hibernate.sql.ast.internal.model.builder.CollectionRowDeleteByUpdateSetNullBuilder(
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
				// restrict - use Leniently to include nullable columns
				// (nullable FKs still need WHERE clause restrictions)
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
		entityId.forEachColumn( (index, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );

		//noinspection unchecked,rawtypes
		return (RestrictedTableMutation) updateBuilder.buildMutation();
	}

	private void applyDeleteRowRestrictions(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object keyValue,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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


	@Nonnull
	private JdbcMutationOperation generateInsertRowOperation(@Nonnull MutatingTableReference tableReference) {
		// NOTE: `TableUpdateBuilderStandard` and `TableUpdate` already handle custom-sql
		return buildTableUpdate( tableReference )
				.createMutationOperation( null, getFactory() );
	}

	@Nonnull
	private TableUpdate<JdbcMutationOperation> buildTableUpdate(@Nonnull MutatingTableReference tableReference) {
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
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object keyValue,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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


	@Nonnull
	private JdbcMutationOperation generateWriteIndexOperation(@Nonnull MutatingTableReference tableReference) {
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
		if ( isIndexWrittenWithKeyRestriction() ) {
			updateBuilder.addKeyRestrictionsLeniently( attributeMapping.getKeyDescriptor().getKeyPart() );
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
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object entry,
			int entryPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object entry,
			int entryPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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

		if ( isIndexWrittenWithKeyRestriction() ) {
			attributeMapping.getKeyDescriptor().getKeyPart().decompose(
					key,
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
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// GraphBasedActionQueue / FlushCoordinator support

	@SuppressWarnings("NullAway.Init") // Initialized during mapping model creation.
	private CollectionDecomposer decomposer;

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
