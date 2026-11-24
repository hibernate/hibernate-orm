package org.hibernate.action.queue.internal.decompose.entity;

import java.util.function.Consumer;

import org.hibernate.action.queue.internal.decompose.collection.DecompositionSupport;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.bind.GeneratedValuesCollector;
import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport.IdentityInsertCommands;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.internal.TenantIdHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;

import static org.hibernate.action.queue.internal.decompose.entity.EntityTemporalEndBindPlan.isIncludedInOptimisticLock;
import static org.hibernate.generator.EventType.UPDATE;

/// Graph mutation plan contributor for single-table temporal entity mutations.
///
/// @author Steve Ebersole
public class TemporalEntityMutationPlanContributor implements EntityMutationPlanContributor {
	private final EntityPersister entityPersister;
	private final SessionFactoryImplementor sessionFactory;
	private final TemporalMapping temporalMapping;
	private final EntityInsertMutationPlanner insertMutationPlanner;
	private final MutationOperation staticEndOperation;
	private final IdentityInsertCommands identityInsertCommands;

	public TemporalEntityMutationPlanContributor(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister;
		this.sessionFactory = sessionFactory;
		this.temporalMapping = entityPersister.getTemporalMapping();
		this.identityInsertCommands = entityPersister.getGenerator().requiresIdentityColumn()
				? sessionFactory.getJdbcServices().getDialect().getIdentityColumnSupport()
						.getExplicitIdentityInsertCommands( entityPersister.getIdentifierTableName() )
				: null;
		this.insertMutationPlanner = new EntityInsertMutationPlanner( entityPersister, sessionFactory );
		this.staticEndOperation = buildEndOperation( null, null, effectiveOptimisticLockStyle( null, null ), null );
	}

	@Override
	public void applyUpdateRestriction(EntityTableDescriptor table, TableUpdateBuilder<?> builder) {
		final var ending = temporalMapping.getEndingColumnMapping();
		if ( table.name().equals( entityPersister.physicalTableNameForMutation( ending ) ) ) {
			final var endingColumn = new ColumnReference( builder.getMutatingTable(), ending );
			builder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumn ) );
		}
	}

	@Override
	public boolean contributeReplacementUpdate(
			UpdateContext context,
			Consumer<FlushOperation> operationConsumer) {
		if ( context.isBeingDeleted() ) {
			// The delete closes this row in place. Preserve its historical association values.
			operationConsumer.accept( DecompositionSupport.createNoOpCallbackCarrier(
					entityPersister.getIdentifierTableDescriptor(), context.ordinalBase() * 1_000,
					new PostUpdateHandling( context.action(), context.cacheUpdate(), context.previousVersion(),
							null, context.entityEntry(), true ) ) );
			return true;
		}
		if ( !context.requiresNewTemporalVersion() ) {
			return false;
		}

		context.initializeStateForRevisionInsert( entityPersister.getIdentifierTableDescriptor() );

		final OptimisticLockStyle effectiveOptLockStyle = effectiveOptimisticLockStyle(
				context.previousVersion(),
				context.previousState()
		);
		final MutationOperation endOperation = needsDynamicEndOperation(
				context.rowId(),
				context.previousState(),
				effectiveOptLockStyle
		)
				? buildEndOperation(
						context.rowId(),
						context.previousState(),
						effectiveOptLockStyle,
						context.session()
				)
				: staticEndOperation;

		final var end = createEndOperation(
				context.ordinalBase(),
				"EntityUpdateAction(" + entityPersister.getEntityName() + ")",
				context.identifier(),
				context.rowId(),
				context.previousVersion(),
				context.previousState(),
				effectiveOptLockStyle,
				endOperation
		);
		operationConsumer.accept( end );
		contributeReplacementInsert( context, insert -> {
			// Inserts may be batched with a different entity's insert from the same flush.
			insert.addExecutionPrerequisite( end );
			operationConsumer.accept( insert );
		} );
		return true;
	}

	@Override
	public boolean contributeReplacementDelete(
			DeleteContext context,
			Consumer<FlushOperation> operationConsumer) {
		final var entityEntry = context.session().getPersistenceContextInternal().getEntry( context.action().getInstance() );
		final Object[] loadedState = entityEntry == null ? context.state() : entityEntry.getLoadedState();
		final Object rowId = entityEntry == null ? null : entityEntry.getRowId();
		final OptimisticLockStyle effectiveOptLockStyle = effectiveOptimisticLockStyle( context.version(), loadedState );

		final MutationOperation operation = needsDynamicEndOperation( rowId, loadedState, effectiveOptLockStyle )
				? buildEndOperation( rowId, loadedState, effectiveOptLockStyle, context.session() )
				: staticEndOperation;

		final FlushOperation flushOperation = createEndOperation(
				context.ordinalBase(),
				"EntityDeleteAction(" + entityPersister.getEntityName() + ")",
				context.identifier(),
				rowId,
				context.version(),
				loadedState,
				effectiveOptLockStyle,
				operation
		);
		flushOperation.setPreExecutionCallback( context.postDeleteHandling().getPreDeleteHandling() );
		flushOperation.setPostExecutionCallback( context.postDeleteHandling() );
		operationConsumer.accept( flushOperation );
		return true;
	}

	private FlushOperation createEndOperation(
			int ordinalBase,
			String description,
			Object identifier,
			Object rowId,
			Object version,
			Object[] loadedState,
			OptimisticLockStyle effectiveOptLockStyle,
			MutationOperation operation) {
		final var flushOperation = new FlushOperation(
				entityPersister.getIdentifierTableDescriptor(),
				MutationKind.UPDATE,
				operation,
				new EntityTemporalEndBindPlan(
						entityPersister.getIdentifierTableDescriptor(),
						entityPersister,
						temporalMapping,
						identifier,
						rowId,
						version,
						loadedState,
						effectiveOptLockStyle
				),
				ordinalBase * 1_000,
				description
		);
		// An ending update does not collect values generated by the mapped update delegate.
		if ( entityPersister.getUpdateDelegate() != null ) {
			flushOperation.disableBatching();
		}
		return flushOperation;
	}

	private void contributeReplacementInsert(
			UpdateContext context,
			Consumer<FlushOperation> operationConsumer) {
		generateReplacementValues( context );
		final boolean[] effectiveInsertability = context.revisionInsertability(
				insertMutationPlanner.resolveInsertability( context.state() ) );
		final boolean reusingGeneratedIdentifier = entityPersister.isIdentifierAssignedByInsert();
		final var generatedValuesCollector = reusingGeneratedIdentifier
				? GeneratedValuesCollector.forReplacementInsert( entityPersister, sessionFactory )
				: GeneratedValuesCollector.forInsert( entityPersister, sessionFactory );
		final var insertGroup = insertMutationPlanner.resolveInsertOperations(
				effectiveInsertability,
				context.entity(),
				context.identifier(),
				effectiveInsertability != entityPersister.getPropertyInsertability(),
				context.session(),
				!reusingGeneratedIdentifier || generatedValuesCollector != null
		);

		final var insertValuesAnalysis = new InsertValuesAnalysis( entityPersister, context.state() );
		final var postUpdateHandling = new PostUpdateHandling(
				context.action(),
				context.cacheUpdate(),
				context.previousVersion(),
				generatedValuesCollector,
				context.entityEntry()
		);

		int localOrd = 1;
		FlushOperation previousOperation = null;
		for ( var tableDescriptor : entityPersister.getTableDescriptors() ) {
			if ( !tableDescriptor.isInverse() && insertValuesAnalysis.include( tableDescriptor ) ) {
				final var operation = insertGroup.get( tableDescriptor.name() )
						.createMutationOperation( insertValuesAnalysis, sessionFactory );
				final var op = new FlushOperation(
						tableDescriptor,
						MutationKind.INSERT,
						operation,
						insertMutationPlanner.createInsertBindPlan(
								tableDescriptor,
								context.entity(),
								context.identifier(),
								context.state(),
								effectiveInsertability,
								null,
								generatedValuesCollector,
								context.decompositionContext()
						),
						context.ordinalBase() * 1_000 + (localOrd++),
						"EntityUpdateAction(" + entityPersister.getEntityName() + ")"
				);

				if ( reusingGeneratedIdentifier && tableDescriptor.isIdentifierTable() ) {
					if ( identityInsertCommands != null ) {
						op.disableBatching();
						op.setExecutionMonitor( new ExplicitIdentityInsert( identityInsertCommands ) );
					}
					else if ( entityPersister.getInsertDelegate() != null ) {
						op.disableBatching();
					}
				}

				if ( previousOperation != null ) {
					operationConsumer.accept( previousOperation );
				}
				previousOperation = op;
			}
		}

		if ( previousOperation != null ) {
			context.recordRevisionAfterInsert( previousOperation );
			final var rowIdMapping = entityPersister.getRowIdMapping();
			if ( rowIdMapping != null ) {
				// A replacement is a different physical row. Fall back to the entity key if
				// the insert delegate did not return its row ID.
				previousOperation.setPostExecutionCallback( session -> {
					final var rowId = generatedValuesCollector == null ? null
							: generatedValuesCollector.generatedValues().getGeneratedValue( rowIdMapping );
					session.getPersistenceContextInternal().replaceEntityEntryRowId( context.entity(), rowId );
				} );
			}
			previousOperation.setPostExecutionCallback( postUpdateHandling );
			operationConsumer.accept( previousOperation );
		}
	}

	private void generateReplacementValues(UpdateContext context) {
		if ( !entityPersister.hasPreUpdateGeneratedProperties() ) {
			return;
		}
		final var generators = entityPersister.getGenerators();
		for ( int i = 0; i < generators.length; i++ ) {
			final var generator = generators[i];
			if ( generator instanceof BeforeExecutionGenerator beforeExecution
					&& generator.generatesOnUpdate()
					&& generator.generatedBeforeExecution( context.entity(), context.session() ) ) {
				// The physical insert represents an update to an existing entity.
				final Object value = beforeExecution.generate( context.session(), context.entity(), context.state()[i], UPDATE );
				context.state()[i] = value;
				entityPersister.setValue( context.entity(), i, value );
			}
		}
	}

	private boolean needsDynamicEndOperation(
			Object rowId,
			Object[] loadedState,
			OptimisticLockStyle effectiveOptLockStyle) {
		return rowId != null
			|| effectiveOptLockStyle.isAllOrDirty()
			|| entityPersister.hasPartitionedSelectionMapping() && loadedState != null;
	}

	private OptimisticLockStyle effectiveOptimisticLockStyle(Object version, Object[] loadedState) {
		final var optimisticLockStyle = entityPersister.optimisticLockStyle();
		if ( optimisticLockStyle.isVersion() ) {
			return version == null || entityPersister.getVersionMapping() == null
					? OptimisticLockStyle.NONE
					: optimisticLockStyle;
		}
		return optimisticLockStyle.isAllOrDirty() && loadedState == null
				? OptimisticLockStyle.NONE
				: optimisticLockStyle;
	}

	private MutationOperation buildEndOperation(
			Object rowId,
			Object[] loadedState,
			OptimisticLockStyle effectiveOptLockStyle,
			SharedSessionContractImplementor session) {
		final var tableDescriptor = entityPersister.getIdentifierTableDescriptor();
		final var tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				tableDescriptor.getRelativePosition(),
				true,
				false
		);
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference( tableMapping ),
				sessionFactory
		);

		addKeyRestriction( updateBuilder, tableDescriptor, rowId );
		addTemporalEnding( updateBuilder );
		addVersionRestriction( updateBuilder, tableDescriptor );
		addNonVersionOptimisticLockRestrictions( updateBuilder, tableDescriptor, loadedState, effectiveOptLockStyle, session );
		addPartitionedSelectionRestrictions( updateBuilder, tableDescriptor, loadedState );
		TenantIdHelper.applyTenantRestriction( entityPersister, updateBuilder );

		return updateBuilder.buildMutation().createMutationOperation( null, sessionFactory );
	}

	private void addKeyRestriction(
			TableUpdateBuilderStandard<MutationOperation> updateBuilder,
			EntityTableDescriptor tableDescriptor,
			Object rowId) {
		if ( rowId != null && entityPersister.getRowIdMapping() != null ) {
			updateBuilder.addKeyRestrictionLeniently( entityPersister.getRowIdMapping() );
		}
		else {
			updateBuilder.addKeyRestrictions( tableDescriptor.keyDescriptor() );
		}
	}

	private void addTemporalEnding(TableUpdateBuilderStandard<MutationOperation> updateBuilder) {
		final var endingColumn = new ColumnReference(
				updateBuilder.getMutatingTable(),
				temporalMapping.getEndingColumnMapping()
		);
		updateBuilder.addValueColumn( temporalMapping.createEndingValueBinding( endingColumn ) );
		updateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumn ) );
	}

	private void addVersionRestriction(
			TableUpdateBuilderStandard<MutationOperation> updateBuilder,
			EntityTableDescriptor tableDescriptor) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null
				&& entityPersister.optimisticLockStyle().isVersion()
				&& tableDescriptor.name().equals( versionMapping.getContainingTableExpression() ) ) {
			updateBuilder.addOptimisticLockRestriction( versionMapping );
		}
	}

	private void addPartitionedSelectionRestrictions(
			TableUpdateBuilderStandard<MutationOperation> updateBuilder,
			EntityTableDescriptor tableDescriptor,
			Object[] loadedState) {
		if ( loadedState != null && entityPersister.hasPartitionedSelectionMapping() ) {
			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				final var attribute = tableDescriptor.attributes().get( i );
				attribute.forEachSelectable( (selectionIndex, selectableMapping) -> {
					if ( selectableMapping.isPartitioned() ) {
						updateBuilder.addKeyRestrictionLeniently( selectableMapping );
					}
				} );
			}
		}
	}

	private void addNonVersionOptimisticLockRestrictions(
			TableUpdateBuilderStandard<MutationOperation> updateBuilder,
			EntityTableDescriptor tableDescriptor,
			Object[] loadedState,
			OptimisticLockStyle effectiveOptLockStyle,
			SharedSessionContractImplementor session) {
		if ( loadedState != null && effectiveOptLockStyle.isAllOrDirty() ) {
			final boolean[] versionability = entityPersister.getPropertyVersionability();
			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				final var attribute = tableDescriptor.attributes().get( i );
				if ( isIncludedInOptimisticLock( attribute, versionability, loadedState ) ) {
					attribute.breakDownJdbcValues(
							loadedState[attribute.getStateArrayPosition()],
							(valueIndex, jdbcValue, jdbcValueMapping) -> {
								if ( !jdbcValueMapping.isFormula() ) {
									if ( jdbcValue == null ) {
										updateBuilder.addNullOptimisticLockRestriction( jdbcValueMapping );
									}
									else {
										updateBuilder.addOptimisticLockRestriction( jdbcValueMapping );
									}
								}
							},
							session
					);
				}
			}
		}
	}
}
