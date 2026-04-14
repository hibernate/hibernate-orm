/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.IdentityMap;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.EntityPersister;

import java.util.ArrayList;
import java.util.List;

/**
 * One-to-many decomposer for table-per-subclass inheritance where each concrete subclass
 * has its own table, requiring different JDBC operations per subclass.
 * @author Steve Ebersole
 */
public class TablePerSubclassOneToManyDecomposer extends AbstractOneToManyDecomposer {
	private final IdentityMap<EntityPersister,CollectionJdbcOperations> operationsBySubclass;

	public TablePerSubclassOneToManyDecomposer(OneToManyPersister persister, SessionFactoryImplementor factory) {
		super( persister, factory );

		var elementDescriptor = (EntityCollectionPart) persister.getAttributeMapping().getElementDescriptor();
		var associatedType = elementDescriptor.getAssociatedEntityMappingType();

		int count = associatedType.getSubMappingTypes().size() + 1;
		operationsBySubclass = IdentityMap.instantiateSequenced( count );

		var baseTableDescriptor = associatedType.getEntityPersister().getIdentifierTableDescriptor();
		operationsBySubclass.put( associatedType.getEntityPersister(), buildJdbcOperations( baseTableDescriptor, factory ) );

		associatedType.getSubMappingTypes().forEach(  subclassMapping -> {
			var tableDescriptor = subclassMapping.getEntityPersister().getIdentifierTableDescriptor();
			operationsBySubclass.put( subclassMapping.getEntityPersister(), buildJdbcOperations( tableDescriptor, factory ) );
		} );
	}

	@Override
	protected CollectionJdbcOperations selectJdbcOperations(Object entry, SharedSessionContractImplementor session) {
		final var entityEntry = session.getPersistenceContextInternal().getEntry( entry );
		return operationsBySubclass.get( entityEntry.getPersister() );
	}

	@Override
	public List<PlannedOperation> decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			SharedSessionContractImplementor session, DecompositionContext decompositionContext) {
		// Always fire PRE event, even if no SQL operations will be needed
		DecompositionSupport.firePreRemove( persister, action.getCollection(), action.getAffectedOwner(), session );

		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		var postRemoveHandling = new PostCollectionRemoveHandling(
				persister,
				action.getCollection(),
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey( action, session )
		);

		if ( !persister.needsRemove() || action.isEmptySnapshot() ) {
			// No remove needed or collection is UNEQUIVOCALLY empty - create no-op to defer POST callback
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					ordinalBase * 1_000,
					postRemoveHandling
			) );
		}

		var operations = new ArrayList<PlannedOperation>();
		operationsBySubclass.forEach( (entityPersister, jdbcOperations) -> {
			operations.add( new PlannedOperation(
					persister.getCollectionTableDescriptor(),
					// technically an UPDATE
					MutationKind.UPDATE,
					jdbcOperations.removeOperation(),
					new RemoveBindPlan( action.getKey(), persister ),
					ordinalBase * 1_000,
					"RemoveAllRows(" + persister.getRolePath() + ")"
			) );
		} );

		if ( !operations.isEmpty() ) {
			// Attach post-execution callback to the last operation
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postRemoveHandling );
			return operations;
		}
		else {
			// Operations unexpectedly empty - create no-op to defer POST callback
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					ordinalBase * 1_000,
					postRemoveHandling
			) );
		}
	}
}
