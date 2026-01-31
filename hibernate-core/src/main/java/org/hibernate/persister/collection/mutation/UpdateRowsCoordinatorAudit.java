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
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * UpdateRowsCoordinator for audited collections.
 */
public class UpdateRowsCoordinatorAudit implements UpdateRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final UpdateRowsCoordinator currentUpdateCoordinator;
	private final SessionFactoryImplementor sessionFactory;
	private final MutationExecutorService mutationExecutorService;
	private final BasicBatchKey auditBatchKey;
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	private MutationOperationGroup auditOperationGroup;
	private AuditCollectionHelper auditHelper;

	public UpdateRowsCoordinatorAudit(
			CollectionMutationTarget mutationTarget,
			UpdateRowsCoordinator currentUpdateCoordinator,
			boolean[] indexColumnIsSettable,
			boolean[] elementColumnIsSettable,
			UnaryOperator<Object> indexIncrementer,
			SessionFactoryImplementor sessionFactory) {
		this.mutationTarget = mutationTarget;
		this.currentUpdateCoordinator = currentUpdateCoordinator;
		this.sessionFactory = sessionFactory;
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
	public void updateRows(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		final var attribute = mutationTarget.getTargetPart();
		final var collectionDescriptor = attribute.getCollectionDescriptor();
		final var rowsToAudit = collectUpdatedRows( collection, attribute, collectionDescriptor );

		currentUpdateCoordinator.updateRows( key, collection, session );

		if ( !rowsToAudit.isEmpty() ) {
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
					for ( var row : rowsToAudit ) {
						bindings.bindInsertValues(
								collection,
								key,
								row.entry,
								row.position,
								AuditStateManagement.ModificationType.MOD,
								session,
								auditExecutor.getJdbcValueBindings()
						);
						auditExecutor.execute( row.entry, null, null, null, session );
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

	private List<RowReference> collectUpdatedRows(
			PersistentCollection<?> collection,
			PluralAttributeMapping attribute,
			CollectionPersister persister) {
		final List<RowReference> rows = new ArrayList<>();
		final var entries = collection.entries( persister );
		if ( !entries.hasNext() ) {
			return rows;
		}

		if ( collection.isElementRemoved() ) {
			final List<Object> elements = new ArrayList<>();
			while ( entries.hasNext() ) {
				elements.add( entries.next() );
			}
			for ( int i = elements.size() - 1; i >= 0; i-- ) {
				final Object entry = elements.get( i );
				if ( collection.needsUpdating( entry, i, attribute ) ) {
					rows.add( new RowReference( entry, i ) );
				}
			}
		}
		else {
			int position = 0;
			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				if ( collection.needsUpdating( entry, position, attribute ) ) {
					rows.add( new RowReference( entry, position ) );
				}
				position++;
			}
		}

		return rows;
	}

	private static final class RowReference {
		private final Object entry;
		private final int position;

		private RowReference(Object entry, int position) {
			this.entry = entry;
			this.position = position;
		}
	}
}
