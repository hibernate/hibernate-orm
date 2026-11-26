package org.hibernate.action.queue.internal.decompose.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.internal.FilteredAssociationState;
import org.hibernate.engine.internal.TenantIdHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ModelPart.JdbcValueConsumer;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.persister.entity.mutation.PartitionKeyValue;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableMutation;
import org.hibernate.sql.ast.spi.model.TableUpdate;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.TableMapping;

import static org.hibernate.action.queue.internal.decompose.entity.DecompositionHelper.isIncludedInOptimisticLock;
import static org.hibernate.internal.util.collections.ArrayHelper.contains;

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
		this.softDeleteOperation = buildSoftDelete( softDeleteBuilder( versioned, null ) );
		this.unversionedSoftDeleteOperation = versioned
				? buildSoftDelete( softDeleteBuilder( false, null ) )
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
		final boolean lockLoadedState = loadedState != null
				&& context.entityPersister().optimisticLockStyle().isAllOrDirty();
		final Map<String, Object> optimisticLockBindings;
		final TableUpdate<?> update;
		if ( lockLoadedState || PartitionKeyValue.needsDynamicRestriction( entityPersister, partitionKeys ) ) {
			final var builder = softDeleteBuilder( applyVersion, partitionKeys );
			optimisticLockBindings = lockLoadedState
					? addOptimisticLockRestrictions( builder, context, loadedState )
					: Map.of();
			update = buildSoftDelete( builder );
		}
		else {
			optimisticLockBindings = Map.of();
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
						applyVersion,
						context.version(),
						optimisticLockBindings,
						partitionKeys
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
		if ( entityEntry == null ) {
			return null;
		}
		final var filteredState = entityEntry.getExtraState( FilteredAssociationState.class );
		return filteredState == null ? entityEntry.getLoadedState()
				: filteredState.physicalState( entityEntry.getLoadedState(), context.entityPersister() );
	}

	/// Add the predicates and collect their JDBC parameter values from the same snapshot.
	private static Map<String, Object> addOptimisticLockRestrictions(
			TableUpdateBuilder<?> builder, DeleteContext context, Object[] loadedState) {
		final var persister = context.entityPersister();
		final var table = builder.getMutatingTable().getTableName();
		final var lockRestrictions = builder.getOptimisticLockBindings();
		// The identifier, partition key and soft-delete indicator already restrict this row.
		final var restrictedColumns = new HashSet<String>();
		builder.getKeyRestrictionBindings().forEach( binding ->
				restrictedColumns.add( binding.getColumnReference().getColumnExpression() ) );
		lockRestrictions.forEach( binding ->
				restrictedColumns.add( binding.getColumnReference().getColumnExpression() ) );
		final Map<String, Object> bindings = new HashMap<>();
		final JdbcValueConsumer addRestriction = (index, value, selectable) -> {
			final var column = selectable.getSelectionExpression();
			if ( !selectable.isFormula() && table.equals( persister.physicalTableNameForMutation( selectable ) )
					&& restrictedColumns.add( column ) ) {
				lockRestrictions.consume( index, value, selectable );
				if ( value != null ) {
					bindings.put( column, value );
				}
			}
		};
		final var updatedAttributes = context.decompositionContext() == null ? null
				: context.decompositionContext().getUpdatedAttributeIndexesForDeletedEntity( context.action().getInstance() );
		final var versionability = persister.getPropertyVersionability();
		for ( int i = 0; i < versionability.length; i++ ) {
			final var attribute = persister.getAttributeMapping( i );
			if ( isIncludedInOptimisticLock( attribute, versionability, loadedState )
					&& ( updatedAttributes == null || !contains( updatedAttributes, i ) ) ) {
				attribute.breakDownJdbcValues( loadedState[i], addRestriction, context.session() );
			}
		}
		return bindings;
	}

	/// @param partitionKeys the partition key of the instance, or `null` for a
	///                      statement with a parameter for each column
	private TableUpdateBuilder<?> softDeleteBuilder(boolean applyVersion, List<PartitionKeyValue> partitionKeys) {
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
		return tableUpdateBuilder;
	}

	private TableUpdate<?> buildSoftDelete(TableUpdateBuilder<?> builder) {
		TenantIdHelper.applyTenantRestriction( entityPersister, builder );
		return (TableUpdate<?>) builder.buildMutation();
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
