package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.engine.internal.TenantIdHelper;
import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.PartitionKeyValue;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableMutation;
import org.hibernate.sql.ast.spi.model.TableUpdate;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.spi.mutation.TableMapping;

/// Graph mutation plan contributor for soft-delete entity deletes.
///
/// @author Steve Ebersole
public class SoftDeleteEntityMutationPlanContributor implements EntityMutationPlanContributor {
	private final EntityPersister entityPersister;
	private final SessionFactoryImplementor sessionFactory;
	private final boolean versioned;
	private final TableUpdate<?> softDeleteOperation;
	private final TableUpdate<?> unversionedSoftDeleteOperation;

	public SoftDeleteEntityMutationPlanContributor(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister instanceof UnionSubclassEntityPersister
				? entityPersister
				: entityPersister.getRootEntityDescriptor().getEntityPersister();
		this.sessionFactory = sessionFactory;
		this.versioned = this.entityPersister.optimisticLockStyle().isVersion()
				&& this.entityPersister.getVersionMapping() != null;
		this.softDeleteOperation = generateSoftDeleteOperation( versioned, null );
		this.unversionedSoftDeleteOperation = versioned
				? generateSoftDeleteOperation( false, null )
				: softDeleteOperation;
	}

	@Override
	public Map<String, ? extends TableMutation<?>> getStaticDeleteOperations() {
		return Map.of( softDeleteOperation.getTableName(), softDeleteOperation );
	}

	@Override
	public boolean contributeReplacementDelete(
			DeleteContext context,
			Consumer<FlushOperation> operationConsumer) {
		final Object identifier = context.identifier();
		// An entity which is deleted without being loaded has no version to check.
		// An entity with a partition key is always loaded before it is deleted.
		final boolean applyVersion = versioned && context.action().getInstance() != null;
		final var loadedState = loadedState( context );
		final var partitionKeys = PartitionKeyValue.fromState( entityPersister, loadedState, context.session() );
		final TableUpdate<?> update;
		if ( PartitionKeyValue.needsDynamicRestriction( entityPersister, partitionKeys ) ) {
			update = generateSoftDeleteOperation( applyVersion, partitionKeys );
		}
		else {
			update = applyVersion ? softDeleteOperation : unversionedSoftDeleteOperation;
		}
		final var mutation = update.createMutationOperation( null, sessionFactory );
		final var tableMapping = (TableDescriptorAsTableMapping) mutation.getTableDetails();
		final var tableDescriptor = (EntityTableDescriptor) tableMapping.descriptor();
		final var operation = new FlushOperation(
				tableDescriptor,
				MutationKind.UPDATE,
				mutation,
				new EntitySoftDeleteBindPlan(
						tableDescriptor,
						entityPersister,
						identifier,
						applyVersion ? context.version() : null,
						loadedState,
						partitionKeys,
						applyVersion ? OptimisticLockStyle.VERSION : OptimisticLockStyle.NONE
				),
				context.ordinalBase() * 1_000,
				"EntityDeleteAction(" + entityPersister.getEntityName() + ")"
		);
		operation.setPreExecutionCallback( context.postDeleteHandling().getPreDeleteHandling() );
		operation.setPostExecutionCallback( context.postDeleteHandling() );
		operationConsumer.accept( operation );
		return true;
	}

	/// The state of the entity when it was loaded, or `null` if there is no snapshot
	/// of it, as for a read-only entity, whose current state may differ from the
	/// persistent state
	private static Object[] loadedState(DeleteContext context) {
		final Object instance = context.action().getInstance();
		final var entityEntry = instance == null ? null
				: context.session().getPersistenceContextInternal().getEntry( instance );
		return entityEntry == null ? null : entityEntry.getLoadedState();
	}

	/// @param partitionKeys the partition key of the instance, or `null` for a
	///                      statement with a parameter for each column
	private TableUpdate<?> generateSoftDeleteOperation(boolean applyVersion, List<PartitionKeyValue> partitionKeys) {
		final var softDeleteMapping = entityPersister.getSoftDeleteMapping();
		assert softDeleteMapping != null;

		final var rootTableDescriptor = entityPersister.getIdentifierTableDescriptor();
		final var tableMapping = new TableDescriptorAsTableMapping(
				rootTableDescriptor,
				0,
				true,
				false
		);

		final TableUpdateBuilder<?> tableUpdateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference( tableMapping ),
				sessionFactory
		);

		tableUpdateBuilder.addKeyRestrictions( rootTableDescriptor.keyDescriptor() );
		applySoftDelete( softDeleteMapping, tableUpdateBuilder );
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			// only the partition key columns of the root table restrict the soft delete
			PartitionKeyValue.addRestrictions( entityPersister,
					Map.of( rootTableDescriptor.name(), tableUpdateBuilder )::get, partitionKeys,
					TableMapping::getUpdateDetails );
		}
		if ( applyVersion ) {
			tableUpdateBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}
		TenantIdHelper.applyTenantRestriction( entityPersister, tableUpdateBuilder );

		return (TableUpdate<?>) tableUpdateBuilder.buildMutation();
	}

	private void applySoftDelete(
			SoftDeleteMapping softDeleteMapping,
			TableUpdateBuilder<?> tableUpdateBuilder) {
		final var softDeleteColumnReference = new ColumnReference(
				tableUpdateBuilder.getMutatingTable(),
				softDeleteMapping
		);

		tableUpdateBuilder.addColumnAssignment( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );
		tableUpdateBuilder.addNonKeyRestriction( softDeleteMapping.createNonDeletedValueBinding( softDeleteColumnReference ) );
	}
}
