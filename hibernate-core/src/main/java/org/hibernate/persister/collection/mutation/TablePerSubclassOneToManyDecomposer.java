/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * One-to-many decomposer for table-per-subclass inheritance where each concrete subclass
 * has its own table, requiring different JDBC operations per subclass.
 * @author Steve Ebersole
 */
public class TablePerSubclassOneToManyDecomposer extends AbstractNonBundledOneToManyDecomposer {
	private final CollectionJdbcOperations[] operationsBySubclassId;

	public TablePerSubclassOneToManyDecomposer(OneToManyPersister persister, SessionFactoryImplementor factory) {
		super( persister, factory );

		var elementDescriptor = (EntityCollectionPart) persister.getAttributeMapping().getElementDescriptor();
		var subclassMappings = elementDescriptor.getAssociatedEntityMappingType().getRootEntityDescriptor().getSubMappingTypes();
		operationsBySubclassId = new CollectionJdbcOperations[subclassMappings.size()];

		subclassMappings.forEach( (subclassMapping) -> {
			var tableDescriptor = subclassMapping.getEntityPersister().getIdentifierTableDescriptor();
			operationsBySubclassId[subclassMapping.getSubclassId()] = buildJdbcOperations( tableDescriptor, factory );
		} );
	}

	@Override
	protected CollectionJdbcOperations selectJdbcOperations(Object entry, SharedSessionContractImplementor session) {
		final var entityEntry = session.getPersistenceContextInternal().getEntry( entry );
		final int subclassId = entityEntry.getPersister().getSubclassId();
		return operationsBySubclassId[subclassId];
	}

	@Override
	public List<PlannedOperation> decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		// Register callback to handle post-execution work (afterAction, cache, events, stats)
		final Object cacheKey = lockCacheItem( action, session );
		postExecCallbackRegistry.accept( new PostCollectionRemoveHandling( action, cacheKey ) );

		var operations = new ArrayList<PlannedOperation>();

		for ( int i = 0; i < operationsBySubclassId.length; i++ ) {
			final CollectionJdbcOperations operation = operationsBySubclassId[i];

			operations.add( new PlannedOperation(
					persister.getCollectionTableDescriptor(),
					// technically an UPDATE
					MutationKind.UPDATE,
					operation.getRemoveOperation(),
					new RemoveBindPlan( action.getKey(), persister ),
					ordinalBase * 1_000,
					"RemoveAllRows(" + persister.getRolePath() + ")"
			) );
		}

		return operations;
	}
}
