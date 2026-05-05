/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.action.queue.spi.decompose.collection.CollectionJdbcOperations;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor;

import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.IdentityMap;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.EntityPersister;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

/// One-to-many decomposer for table-per-subclass inheritance where each concrete subclass
/// has its own table, requiring different JDBC operations per subclass.
/// @author Steve Ebersole
public class TablePerSubclassOneToManyDecomposer extends AbstractOneToManyDecomposer {
	private final IdentityMap<EntityPersister,CollectionJdbcOperations> operationsBySubclass;

	public TablePerSubclassOneToManyDecomposer(
			OneToManyPersister persister,
			SessionFactoryImplementor factory,
			CollectionMutationPlanContributor mutationPlanContributor) {
		super( persister, factory, mutationPlanContributor );

		var elementDescriptor = (EntityCollectionPart) persister.getAttributeMapping().getElementDescriptor();
		var associatedType = elementDescriptor.getAssociatedEntityMappingType();

		int count = associatedType.getSubMappingTypes().size() + 1;
		operationsBySubclass = IdentityMap.instantiateSequenced( count );

		final var associatedEntityPersister = associatedType.getEntityPersister();
		operationsBySubclass.put( associatedEntityPersister, buildJdbcOperations( associatedEntityPersister, factory ) );

		associatedType.getSubMappingTypes().forEach( subclassMapping -> {
			final var subclassPersister = subclassMapping.getEntityPersister();
			operationsBySubclass.put( subclassPersister, buildJdbcOperations( subclassPersister, factory ) );
		} );
	}

	private CollectionJdbcOperations buildJdbcOperations(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory) {
		return buildJdbcOperations( concreteTableDescriptor( entityPersister ), factory );
	}

	private TableDescriptor concreteTableDescriptor(EntityPersister subclassPersister) {
		final var tableDescriptor = subclassPersister.getIdentifierTableDescriptor();
		final var tableName = subclassPersister.getMappedTableDetails().getTableName();
		if ( tableDescriptor.name().equals( tableName ) ) {
			return tableDescriptor;
		}
		return new TableDescriptor() {
			@Override
			public String name() {
				return tableName;
			}

			@Override
			public boolean isOptional() {
				return tableDescriptor.isOptional();
			}

			@Override
			public org.hibernate.action.queue.spi.meta.TableKeyDescriptor keyDescriptor() {
				return tableDescriptor.keyDescriptor();
			}

			@Override
			public boolean isSelfReferential() {
				return tableDescriptor.isSelfReferential();
			}

			@Override
			public boolean hasUniqueConstraints() {
				return tableDescriptor.hasUniqueConstraints();
			}

			@Override
			public boolean cascadeDeleteEnabled() {
				return tableDescriptor.cascadeDeleteEnabled();
			}

			@Override
			public org.hibernate.sql.model.TableMapping.MutationDetails insertDetails() {
				return tableDescriptor.insertDetails();
			}

			@Override
			public org.hibernate.sql.model.TableMapping.MutationDetails updateDetails() {
				return tableDescriptor.updateDetails();
			}

			@Override
			public org.hibernate.sql.model.TableMapping.MutationDetails deleteDetails() {
				return tableDescriptor.deleteDetails();
			}

			@Override
			public int getRelativePosition() {
				return tableDescriptor.getRelativePosition();
			}
		};
	}

	@Override
	protected CollectionJdbcOperations selectJdbcOperations(Object entry, SharedSessionContractImplementor session) {
		final var element = entry instanceof Map.Entry<?,?> mapEntry ? mapEntry.getValue() : entry;
		final var entityPersister = session.getEntityPersister( null, element );
		final var jdbcOperations = operationsBySubclass.get( entityPersister );
		if ( jdbcOperations != null ) {
			return jdbcOperations;
		}
		final var entityEntry = session.getPersistenceContextInternal().getEntry( element );
		return operationsBySubclass.get( entityEntry.getPersister() );
	}

	@Override
	public void decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<FlushOperation> operationConsumer) {
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
			operationConsumer.accept( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					ordinalBase * 1_000,
					postRemoveHandling
			) );
			return;
		}

		var operations = new ArrayList<FlushOperation>();
		operationsBySubclass.forEach( (entityPersister, jdbcOperations) -> {
			operations.add( new FlushOperation(
					jdbcOperations.tableDescriptor(),
						// technically an UPDATE
						MutationKind.UPDATE,
						jdbcOperations.removeOperation(),
						new RemoveBindPlan( action.getKey(), persister, mutationPlanContributor ),
						ordinalBase * 1_000,
					"RemoveAllRows(" + persister.getRolePath() + ")"
			) );
		} );

		if ( !operations.isEmpty() ) {
			// Attach post-execution callback to the last operation
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postRemoveHandling );
			operations.forEach( operationConsumer );
		}
		else {
			// Operations unexpectedly empty - create no-op to defer POST callback
			operationConsumer.accept( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					ordinalBase * 1_000,
					postRemoveHandling
			) );
		}
	}
}
