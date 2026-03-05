/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * DeleteRowsCoordinator for audited collections.
 */
public class DeleteRowsCoordinatorAudit implements DeleteRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final DeleteRowsCoordinator currentDeleteCoordinator;
	private final SessionFactoryImplementor sessionFactory;
	private final boolean deleteByIndex;
	private final MutationExecutorService mutationExecutorService;
	private final BasicBatchKey auditBatchKey;
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	private MutationOperationGroup auditOperationGroup;
	private AuditCollectionHelper auditHelper;

	public DeleteRowsCoordinatorAudit(
			CollectionMutationTarget mutationTarget,
			DeleteRowsCoordinator currentDeleteCoordinator,
			boolean deleteByIndex,
			boolean[] indexColumnIsSettable,
			boolean[] elementColumnIsSettable,
			UnaryOperator<Object> indexIncrementer,
			SessionFactoryImplementor sessionFactory) {
		this.mutationTarget = mutationTarget;
		this.currentDeleteCoordinator = currentDeleteCoordinator;
		this.sessionFactory = sessionFactory;
		this.deleteByIndex = deleteByIndex;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.auditBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#AUDIT_INSERT" );
		this.mutationExecutorService = sessionFactory.getServiceRegistry().getService( MutationExecutorService.class );
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void deleteRows(
			PersistentCollection<?> collection,
			Object key,
			SharedSessionContractImplementor session) {
		final var collectionDescriptor = mutationTarget.getTargetPart().getCollectionDescriptor();
		final var deletions = collectDeletions( collection, collectionDescriptor );

		currentDeleteCoordinator.deleteRows( collection, key, session );

		if ( !deletions.isEmpty() ) {
			if ( auditOperationGroup == null ) {
				auditOperationGroup = getAuditHelper().getAuditInsertOperationGroup();
			}
			if ( auditOperationGroup != null ) {
				final var auditExecutor = mutationExecutorService.createExecutor(
						() -> auditBatchKey,
						auditOperationGroup,
						session
				);
				try {
					final var bindings = getAuditHelper().getRowMutationHelper();
					for ( int i = 0; i < deletions.size(); i++ ) {
						final Object removal = deletions.get( i );
						bindings.bindInsertValues(
								collection,
								key,
								removal,
								i,
								AuditStateManagement.ModificationType.DEL,
								session,
								auditExecutor.getJdbcValueBindings()
						);
						auditExecutor.execute( removal, null, null, null, session );
					}
				}
				finally {
					auditExecutor.release();
				}
			}
		}
	}

	private AuditCollectionHelper getAuditHelper() {
		if ( auditHelper == null ) {
			auditHelper = new AuditCollectionHelper(
					mutationTarget,
					sessionFactory,
					indexColumnIsSettable,
					elementColumnIsSettable,
					indexIncrementer,
					mutationTarget.getTargetPart().getAuditMapping()
			);
		}
		return auditHelper;
	}

	private List<Object> collectDeletions(
			PersistentCollection<?> collection,
			CollectionPersister persister) {
		final List<Object> deletions = new ArrayList<>();
		final var deletes = collection.getDeletes( persister, !deleteByIndex );
		while ( deletes.hasNext() ) {
			deletions.add( deletes.next() );
		}
		return deletions;
	}
}
