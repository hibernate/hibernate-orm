/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.action.queue.spi.meta.CollectionTableDescriptor;
import org.hibernate.audit.ModificationType;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AuditMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.mutation.EntityAuditSupport;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.type.Type;

/// Shared diffing, operation planning, and binding support for audited collection row mutations.
///
/// Like [EntityAuditSupport], this type keeps audit operation construction
/// queue-neutral. Legacy collection audit coordinators consume operation groups
/// through the normal mutation executor, while the graph queue materializes
/// execution-agnostic plans as `FlushOperation`s during transaction completion.
/// This type also owns collection-row diffing and owner-entity audit-change
/// resolution because those are audit semantics shared by both queues.
///
/// The main invariants guarded here are:
///
/// - Collection audit rows are derived from the collection's original snapshot
/// and final in-memory state.
/// - Row identity is delegated to `AuditCollectionRowMutationHelper` so keyed,
/// indexed, identifier, element, and one-to-many join-column shapes bind
/// consistently.
/// - Validity-strategy transaction-end updates use the same row identity as
/// audit inserts and restrict on `REVEND is null`.
/// - Owner entity `MOD` audit changes are resolved here as logical audit
/// changes, not by teaching collection coordinators about graph execution.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public class CollectionAuditSupport {

	/// A resolved collection audit mutation operation.
	///
	/// The descriptor identifies the audit collection table as seen by the graph
	/// queue, while the mutation operation is the SQL-model operation shared with
	/// legacy mutation execution infrastructure.
	public record AuditCollectionOperation(
			CollectionTableDescriptor tableDescriptor,
			MutationOperation operation) {
	}

	/// Execution-agnostic plan for inserting collection audit rows.
	public record AuditInsertPlan(
			AuditCollectionOperation operation) {
	}

	/// Execution-agnostic plan for closing previous validity audit rows.
	public record TransactionEndPlan(
			AuditCollectionOperation operation) {
	}

	/// One row-level collection audit change.
	///
	/// The raw entry is the collection entry used by the collection wrapper and
	/// row mutation helper. For snapshot-map deletes, such as identifier bags, it
	/// may be the snapshot `Map.Entry` so the original row identifier remains
	/// available for transaction-end binding.
	///
	/// The position is the collection-entry iteration position used by collection
	/// wrappers when resolving identifiers or indexes.
	public record AuditCollectionChange(
			Object rawEntry,
			int position,
			ModificationType modificationType) {
	}

	/// Owning entity audit change implied by a collection mutation.
	///
	/// Collection row audit changes are accompanied by a logical owner `MOD`
	/// audit change when the owner is itself audited and can be resolved from the
	/// session.
	public record OwnerAuditChange(
			EntityKey entityKey,
			Object entity,
			Object[] values,
			EntityAuditSupport ownerMutationSupport) {
	}


	private final CollectionMutationTarget mutationTarget;
	private final AuditCollectionHelper auditHelper;
	private final @Nullable EntityAuditSupport ownerMutationSupport;

	public CollectionAuditSupport(
			CollectionMutationTarget mutationTarget,
			SessionFactoryImplementor sessionFactory,
			boolean[] indexColumnIsSettable,
			boolean[] elementColumnIsSettable,
			UnaryOperator<Object> indexIncrementer,
			AuditMapping auditMapping) {
		this.mutationTarget = mutationTarget;
		this.auditHelper = new AuditCollectionHelper(
				mutationTarget,
				sessionFactory,
				indexColumnIsSettable,
				elementColumnIsSettable,
				indexIncrementer,
				auditMapping
		);
		final var ownerPersister = mutationTarget.getTargetPart().getCollectionDescriptor().getOwnerEntityPersister();
		this.ownerMutationSupport = ownerPersister.getAuditMapping() == null
				? null
				: new EntityAuditSupport( ownerPersister, sessionFactory );
	}

	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	public @Nullable EntityAuditSupport getOwnerMutationSupport() {
		return ownerMutationSupport;
	}

	/// Resolve the owning entity change implied by an audited collection change.
	///
	/// Collection audit rows record the row-level ADD/DEL changes, while the
	/// owning entity receives a MOD audit row so revision queries can see that the
	/// association changed. The owner is resolved from the persistence context
	/// first, then from the collection wrapper, and finally by entity key lookup.
	/// If the owner cannot be resolved, no owner MOD change is produced; the
	/// collection row audit work can still be recorded.
	public OwnerAuditChange resolveOwnerAuditChange(
			Object ownerId,
			PersistentCollection<?> collection,
			SharedSessionContractImplementor session) {
		final var collectionDescriptor = mutationTarget.getTargetPart().getCollectionDescriptor();
		final var ownerPersister = collectionDescriptor.getOwnerEntityPersister();
		if ( ownerMutationSupport == null ) {
			return null;
		}
		final var ownerEntityKey = session.generateEntityKey( ownerId, ownerPersister );

		final var persistenceContext = session.getPersistenceContextInternal();
		final var persistenceContextOwner = persistenceContext.getCollectionOwner( ownerId, collectionDescriptor );
		final var collectionOwner = persistenceContextOwner != null ? persistenceContextOwner : collection.getOwner();
		final var owner = collectionOwner != null ? collectionOwner : persistenceContext.getEntity( ownerEntityKey );
		if ( owner == null ) {
			return null;
		}

		return new OwnerAuditChange(
				ownerEntityKey,
				owner,
				ownerPersister.getValues( owner ),
				ownerMutationSupport
		);
	}

	AuditCollectionRowMutationHelper getRowMutationHelper() {
		return auditHelper.getRowMutationHelper();
	}

	public MutationOperationGroup getAuditInsertOperationGroup() {
		return auditHelper.getAuditInsertOperationGroup();
	}

	public MutationOperationGroup getTransactionEndUpdateGroup() {
		return auditHelper.getTransactionEndUpdateGroup();
	}

	private AuditCollectionOperation resolveAuditInsertOperation() {
		final var group = getAuditInsertOperationGroup();
		return group == null ? null : createOperation( group.getSingleOperation() );
	}

	/// Resolve the audit insert plan for row-level collection audit changes.
	///
	/// The returned plan is execution-agnostic. It identifies the JDBC mutation
	/// operation that inserts audit rows, while queue-specific code decides how
	/// to materialize and execute that operation.
	public AuditInsertPlan resolveAuditInsertPlan() {
		final var operation = resolveAuditInsertOperation();
		return operation == null ? null : new AuditInsertPlan( operation );
	}

	private AuditCollectionOperation resolveTransactionEndUpdateOperation() {
		final var group = getTransactionEndUpdateGroup();
		return group == null ? null : createOperation( group.getSingleOperation() );
	}

	/// Resolve the validity-strategy transaction-end plan for collection audit rows.
	///
	/// The returned plan describes the update operation used to close previous
	/// audit rows for changed collection-row identities. A {@code null} result
	/// means the active audit strategy does not require this update.
	public TransactionEndPlan resolveTransactionEndUpdatePlan() {
		final var operation = resolveTransactionEndUpdateOperation();
		return operation == null ? null : new TransactionEndPlan( operation );
	}

	private AuditCollectionOperation createOperation(MutationOperation operation) {
		return new AuditCollectionOperation(
				createAuditTableDescriptor( operation ),
				operation
		);
	}

	/// Create the graph queue's descriptor for the collection audit table.
	///
	/// Audit collection rows are executed after flush planning, so they do not
	/// participate in graph dependency analysis. The descriptor therefore carries
	/// only the stable table identity, mutation details, and collection key
	/// metadata needed for batching and binding.
	private CollectionTableDescriptor createAuditTableDescriptor(MutationOperation operation) {
		final var sourceDescriptor = mutationTarget.getCollectionTableDescriptor();
		final var tableMapping = operation.getTableDetails();
		return new CollectionTableDescriptor(
				auditHelper.getAuditTableMapping().getTableName(),
				sourceDescriptor.navigableRole(),
				sourceDescriptor.isJoinTable(),
				sourceDescriptor.isInverse(),
				sourceDescriptor.isSelfReferential(),
				sourceDescriptor.hasUniqueConstraints(),
				sourceDescriptor.cascadeDeleteEnabled(),
				tableMapping.getInsertDetails(),
				tableMapping.getUpdateDetails(),
				tableMapping.getDeleteDetails(),
				sourceDescriptor.deleteAllDetails(),
				sourceDescriptor.keyDescriptor()
		);
	}

	public List<AuditCollectionChange> resolveChanges(
			PersistentCollection<?> collection,
			Object originalSnapshot) {
		final var collectionDescriptor = mutationTarget.getTargetPart().getCollectionDescriptor();
		if ( originalSnapshot == null ) {
			final List<AuditCollectionChange> changes = new ArrayList<>();
			final var entries = collection.entries( collectionDescriptor );
			int entryCount = 0;
			while ( entries.hasNext() ) {
				changes.add( new AuditCollectionChange( entries.next(), entryCount++, ModificationType.ADD ) );
			}
			return changes;
		}
		return computeCollectionChanges( collection, collectionDescriptor, originalSnapshot );
	}

	/// Bind values for one legacy collection audit INSERT.
	///
	/// The [AuditCollectionChange#rawEntry()] is passed through unchanged so
	/// the row mutation helper can use the collection wrapper's normal row access
	/// methods for list, map, id-bag, element, and association rows.
	public void bindAuditInsertValues(
			PersistentCollection<?> collection,
			Object ownerId,
			AuditCollectionChange change,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		getRowMutationHelper().bindInsertValues(
				collection,
				ownerId,
				change.rawEntry(),
				change.position(),
				change.modificationType(),
				session,
				jdbcValueBindings
		);
	}

	/// Bind values for one graph-queue collection audit INSERT.
	///
	/// This mirrors the legacy binding variant while using graph queue bindings,
	/// which are keyed by column name because the surrounding flush operation
	/// already carries the mutating audit table descriptor.
	public void bindAuditInsertValues(
			PersistentCollection<?> collection,
			Object ownerId,
			AuditCollectionChange change,
			Object changesetId,
			SharedSessionContractImplementor session,
			org.hibernate.action.queue.spi.bind.JdbcValueBindings jdbcValueBindings) {
		getRowMutationHelper().bindInsertValues(
				collection,
				ownerId,
				change.rawEntry(),
				change.position(),
				change.modificationType(),
				changesetId,
				session,
				jdbcValueBindings
		);
	}

	/// Bind values for one legacy validity-strategy transaction-end UPDATE.
	///
	/// The UPDATE closes the previous audit row for the same collection row
	/// identity. Zero affected rows are tolerated by callers because a row being
	/// added for the first time has no previous audit row to close.
	public void bindTransactionEndValues(
			PersistentCollection<?> collection,
			Object ownerId,
			AuditCollectionChange change,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var tableName = auditHelper.getAuditTableMapping().getTableName();
		final var txId = session.getCurrentChangesetIdentifier();
		final var auditMapping = mutationTarget.getTargetPart().getAuditMapping();
		final var collectionTableName = mutationTarget.getCollectionTableMapping().getTableName();
		final var revEndMapping = auditMapping.getInvalidatingChangesetIdMapping( collectionTableName );

		if ( !auditHelper.useServerTransactionTimestamps() ) {
			jdbcValueBindings.bindValue( txId, tableName, revEndMapping.getSelectionExpression(), ParameterUsage.SET );
		}

		getRowMutationHelper().bindRestrictValues(
				collection,
				ownerId,
				change.rawEntry(),
				change.position(),
				session,
				jdbcValueBindings
		);
	}

	/// Bind values for one graph-queue validity-strategy transaction-end UPDATE.
	///
	/// @see #bindTransactionEndValues(PersistentCollection, Object, AuditCollectionChange, SharedSessionContractImplementor, JdbcValueBindings)
	public void bindTransactionEndValues(
			PersistentCollection<?> collection,
			Object ownerId,
			AuditCollectionChange change,
			Object changesetId,
			SharedSessionContractImplementor session,
			org.hibernate.action.queue.spi.bind.JdbcValueBindings jdbcValueBindings) {
		final var auditMapping = mutationTarget.getTargetPart().getAuditMapping();
		final var collectionTableName = mutationTarget.getCollectionTableMapping().getTableName();
		final var revEndMapping = auditMapping.getInvalidatingChangesetIdMapping( collectionTableName );

		if ( !auditHelper.useServerTransactionTimestamps() ) {
			jdbcValueBindings.bindValue(
					changesetId,
					revEndMapping.getSelectionExpression(),
					ParameterUsage.SET
			);
		}

		getRowMutationHelper().bindRestrictValues(
				collection,
				ownerId,
				change.rawEntry(),
				change.position(),
				session,
				jdbcValueBindings
		);
	}

	/// Compute row-level audit changes for an existing collection.
	///
	/// Indexed collections compare by index/key, while unindexed collections match
	/// by element type equality and consume matched snapshot entries. The resulting
	/// ADD/DEL changes intentionally describe audit rows, not SQL DML operations;
	/// replacements are represented as both a DEL of the old row and an ADD of
	/// the new row.
	private List<AuditCollectionChange> computeCollectionChanges(
			PersistentCollection<?> collection,
			CollectionPersister collectionDescriptor,
			Object snapshot) {
		final Type elementType = collectionDescriptor.getElementType();
		if ( collectionDescriptor.hasIndex() ) {
			return snapshot instanceof Map<?, ?> ?
					computeMapChanges( collection, collectionDescriptor, (Map<?, ?>) snapshot, elementType ) :
					computeListChanges( collection, collectionDescriptor, snapshot, elementType );
		}
		else if ( snapshot instanceof Map<?, ?> snapshotMap ) {
			return computeUnindexedMapChanges( collection, collectionDescriptor, snapshotMap, elementType );
		}
		return computeUnindexedChanges( collection, collectionDescriptor, (Collection<?>) snapshot, elementType );
	}

	private List<AuditCollectionChange> computeMapChanges(
			PersistentCollection<?> collection,
			CollectionPersister collectionDescriptor,
			Map<?, ?> snapshot,
			Type elementType) {
		final List<AuditCollectionChange> changes = new ArrayList<>();
		final var currentMap = (Map<?, ?>) collection;

		final var entries = collection.entries( collectionDescriptor );
		int i = 0;
		while ( entries.hasNext() ) {
			final var entry = (Map.Entry<?, ?>) entries.next();
			if ( entry.getValue() != null ) {
				final Object snapshotValue = snapshot.get( entry.getKey() );
				if ( snapshotValue == null || !elementType.isSame( entry.getValue(), snapshotValue ) ) {
					changes.add( new AuditCollectionChange( entry, i, ModificationType.ADD ) );
				}
			}
			i++;
		}

		for ( var entry : snapshot.entrySet() ) {
			if ( entry.getValue() != null ) {
				final Object currentValue = currentMap.get( entry.getKey() );
				if ( currentValue == null || !elementType.isSame( entry.getValue(), currentValue ) ) {
					changes.add( new AuditCollectionChange( entry, i++, ModificationType.DEL ) );
				}
			}
		}

		return changes;
	}

	private List<AuditCollectionChange> computeListChanges(
			PersistentCollection<?> collection,
			CollectionPersister collectionDescriptor,
			Object snapshot,
			Type elementType) {
		final List<AuditCollectionChange> changes = new ArrayList<>();
		final List<?> snapshotList = snapshot instanceof List<?> list ? list : null;
		final int snapshotSize = snapshotList != null ? snapshotList.size() : Array.getLength( snapshot );

		final var entries = collection.entries( collectionDescriptor );
		int i = 0;
		while ( entries.hasNext() ) {
			final Object current = collection.getElement( entries.next() );
			final Object old = i < snapshotSize ? ( snapshotList != null ? snapshotList.get( i ) : Array.get( snapshot, i ) ) : null;
			final boolean same = current != null && old != null && elementType.isSame( current, old );
			if ( current != null && !same ) {
				changes.add( new AuditCollectionChange( current, i, ModificationType.ADD ) );
			}
			if ( old != null && !same ) {
				changes.add( new AuditCollectionChange( old, i, ModificationType.DEL ) );
			}
			i++;
		}

		for ( ; i < snapshotSize; i++ ) {
			final Object old = snapshotList != null ? snapshotList.get( i ) : Array.get( snapshot, i );
			if ( old != null ) {
				changes.add( new AuditCollectionChange( old, i, ModificationType.DEL ) );
			}
		}

		return changes;
	}

	private List<AuditCollectionChange> computeUnindexedChanges(
			PersistentCollection<?> collection,
			CollectionPersister collectionDescriptor,
			Collection<?> snapshotElements,
			Type elementType) {
		final var remaining = new ArrayList<>( snapshotElements );
		final List<AuditCollectionChange> changes = new ArrayList<>();

		final var entries = collection.entries( collectionDescriptor );
		int i = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			final Object element = collection.getElement( entry );
			if ( element != null ) {
				boolean matched = false;
				for ( var it = remaining.iterator(); it.hasNext(); ) {
					if ( elementType.isSame( element, it.next() ) ) {
						it.remove();
						matched = true;
						break;
					}
				}
				if ( !matched ) {
					changes.add( new AuditCollectionChange( entry, i, ModificationType.ADD ) );
				}
			}
			i++;
		}

		for ( var element : remaining ) {
			changes.add( new AuditCollectionChange( element, i++, ModificationType.DEL ) );
		}

		return changes;
	}

	private List<AuditCollectionChange> computeUnindexedMapChanges(
			PersistentCollection<?> collection,
			CollectionPersister collectionDescriptor,
			Map<?, ?> snapshot,
			Type elementType) {
		final var remaining = new ArrayList<>( snapshot.entrySet() );
		final List<AuditCollectionChange> changes = new ArrayList<>();

		final var entries = collection.entries( collectionDescriptor );
		int i = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			final Object element = collection.getElement( entry );
			if ( element != null ) {
				boolean matched = false;
				for ( var it = remaining.iterator(); it.hasNext(); ) {
					if ( elementType.isSame( element, it.next().getValue() ) ) {
						it.remove();
						matched = true;
						break;
					}
				}
				if ( !matched ) {
					changes.add( new AuditCollectionChange( entry, i, ModificationType.ADD ) );
				}
			}
			i++;
		}

		for ( var entry : remaining ) {
			changes.add( new AuditCollectionChange( entry, i++, ModificationType.DEL ) );
		}

		return changes;
	}
}
