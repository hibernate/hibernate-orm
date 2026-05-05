/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.MappingException;
import org.hibernate.audit.AuditException;
import org.hibernate.audit.ModificationType;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AuditMapping;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.MutationGroup;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;
import org.hibernate.sql.model.internal.MutationGroupStandard;

import static org.hibernate.persister.entity.mutation.AbstractMutationCoordinator.createAuxiliaryTableMapping;
import static org.hibernate.persister.entity.mutation.InsertCoordinatorStandard.getPropertiesToInsert;

/// Shared construction and binding support for audited entity JDBC mutations.
///
/// This support deliberately stops at the mutation-model boundary. Legacy
/// coordinators consume the generated mutation groups through the normal
/// mutation executor, while the graph queue turns the same groups into
/// `FlushOperation`s. Keeping the support here avoids teaching either
/// execution path about the other.
///
/// The main invariants guarded by this type are:
///
/// - Audit table resolution follows the entity table mappings, including
/// secondary tables and inheritance layouts.
/// - Validity-strategy transaction-end updates are generated before the new
/// audit insert and restrict on `REVEND is null`.
/// - Generated graph table descriptors describe the physical audit table used by
/// the JDBC mutation operation, not the original entity table.
///
/// @implSpec Used by both [graph][org.hibernate.action.queue.spi.QueueType#GRAPH] and
/// [legacy][org.hibernate.action.queue.spi.QueueType#LEGACY] action-queue paths.  This
/// is just some shared infrastructure.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public class EntityAuditSupport {

	/// A resolved per-table audit mutation operation.
	///
	/// The descriptor identifies the audit table as seen by the graph queue,
	/// while the mutation operation is the SQL-model operation shared with
	/// legacy mutation execution infrastructure.
	public record AuditMutationOperation(
			int tableIndex,
			EntityTableDescriptor tableDescriptor,
			MutationOperation operation) {
	}

	/// Execution-agnostic plan for one per-table audit insert operation belonging
	/// to a logical audited entity change.
	///
	/// The property inclusion mask is the already audit-masked set of entity
	/// properties that should be bound for this logical entity change. Dynamic
	/// insert planning may update this mask while resolving generated values, so
	/// the plan takes an owned copy after resolution.
	public record AuditInsertPlan(
			AuditMutationOperation operation,
			boolean[] propertyInclusions) {
		public AuditInsertPlan {
			propertyInclusions = propertyInclusions.clone();
		}

		@Override
		public boolean[] propertyInclusions() {
			return propertyInclusions.clone();
		}
	}

	/// Execution-agnostic plan for closing the previous validity audit row.
	public record TransactionEndPlan(
			AuditMutationOperation operation) {
	}


	private final EntityPersister entityPersister;
	private final SessionFactoryImplementor factory;
	private final AuditMapping auditMapping;
	private final EntityTableMapping[] auditTableMappings;
	private final boolean[] auditedPropertyMask;
	private final boolean useServerTransactionTimestamps;
	private final String currentTimestampFunctionName;
	private final MutationGroup staticAuditInsertMutationGroup;
	private final MutationGroup transactionEndUpdateMutationGroup;

	public EntityAuditSupport(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory) {
		this.entityPersister = entityPersister;
		this.factory = factory;
		this.auditMapping = entityPersister.getAuditMapping();
		if ( auditMapping == null ) {
			throw new MappingException( "No audit mapping available for " + entityPersister.getEntityName() );
		}
		this.auditTableMappings = buildAuditTableMappings();
		this.auditedPropertyMask = new boolean[entityPersister.getPropertySpan()];
		for ( int i = 0; i < auditedPropertyMask.length; i++ ) {
			auditedPropertyMask[i] = !entityPersister.isPropertyAuditedExcluded( i );
		}
		this.useServerTransactionTimestamps =
				factory.getChangesetCoordinator().useServerTimestamp( factory.getJdbcServices().getDialect() );
		this.currentTimestampFunctionName = useServerTransactionTimestamps
				? factory.getJdbcServices().getDialect().currentTimestamp()
				: null;
		this.staticAuditInsertMutationGroup = entityPersister.isDynamicInsert()
				? null
				: buildAuditInsertMutationGroup( applyAuditMask( entityPersister.getPropertyInsertability() ), null, null );
		this.transactionEndUpdateMutationGroup = buildTransactionEndUpdateMutationGroup();
	}

	public EntityPersister getEntityPersister() {
		return entityPersister;
	}

	/// The per-property audit inclusion mask.
	///
	/// The returned array is owned by this support instance and must be treated as
	/// read-only by callers. It is exposed directly because the legacy audit
	/// coordinator keeps a reference for compatibility with its existing shape.
	/// Code that needs to alter the mask should first clone it, as
	/// [#resolvePropertyInclusions(Object, Object[], SharedSessionContractImplementor)]
	/// does.
	public boolean[] getAuditedPropertyMask() {
		return auditedPropertyMask;
	}

	public MutationGroup getStaticAuditInsertMutationGroup() {
		return staticAuditInsertMutationGroup;
	}

	public MutationGroup getTransactionEndUpdateMutationGroup() {
		return transactionEndUpdateMutationGroup;
	}

	public boolean[] resolvePropertyInclusions(
			Object entity,
			Object[] values,
			SharedSessionContractImplementor session) {
		return applyAuditMask(
				entityPersister.isDynamicInsert()
						? getPropertiesToInsert( entityPersister, values )
						: entityPersister.getPropertyInsertability()
		);
	}

	public MutationGroup resolveAuditInsertMutationGroup(
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		return entityPersister.isDynamicInsert()
				? buildAuditInsertMutationGroup( propertyInclusions, entity, session )
				: staticAuditInsertMutationGroup;
	}

	private List<AuditMutationOperation> resolveAuditInsertOperations(
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		final var mutationGroup = resolveAuditInsertMutationGroup( propertyInclusions, entity, session );
		return mutationGroup == null
				? List.of()
				: createMutationOperations( mutationGroup );
	}

	/// Resolve the per-table audit insert plans for an audited entity change.
	///
	/// The returned plans are still execution-agnostic: they identify the JDBC
	/// mutation operation and the property inclusion mask needed to bind that
	/// operation, but they do not create graph queue [FlushOperation][org.hibernate.action.queue.spi.plan.FlushOperation]
	/// instances or legacy mutation executors. The returned insert plans retain
	/// an owned copy of the resolved property inclusion mask.
	public List<AuditInsertPlan> resolveAuditInsertPlans(
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		final var operations = resolveAuditInsertOperations( propertyInclusions, entity, session );
		if ( operations.isEmpty() ) {
			return List.of();
		}
		final List<AuditInsertPlan> plans = new ArrayList<>( operations.size() );
		for ( var operation : operations ) {
			plans.add( new AuditInsertPlan( operation, propertyInclusions ) );
		}
		return plans;
	}

	private List<AuditMutationOperation> resolveTransactionEndUpdateOperations() {
		return transactionEndUpdateMutationGroup == null
				? List.of()
				: createMutationOperations( transactionEndUpdateMutationGroup );
	}

	/// Resolve the per-table validity-strategy transaction-end update plans.
	///
	/// The returned plans describe the update operations needed to close the
	/// previous audit rows. Queue-specific code decides how to execute them.
	public List<TransactionEndPlan> resolveTransactionEndUpdatePlans() {
		final var operations = resolveTransactionEndUpdateOperations();
		if ( operations.isEmpty() ) {
			return List.of();
		}
		final List<TransactionEndPlan> plans = new ArrayList<>( operations.size() );
		for ( var operation : operations ) {
			plans.add( new TransactionEndPlan( operation ) );
		}
		return plans;
	}

	public void bindAuditInsertValues(
			int tableIndex,
			Object id,
			Object[] values,
			boolean[] propertyInclusions,
			ModificationType modificationType,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( auditTableMappings[tableIndex] == null ) {
			return;
		}
		final var attributeMappings = entityPersister.getAttributeMappings();
		final EntityTableMapping[] sourceMappings = entityPersister.getTableMappings();
		final String tableName = auditTableMappings[tableIndex].getTableName();

		sourceMappings[tableIndex].getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> jdbcValueBindings.bindValue(
						jdbcValue, tableName, columnMapping.getColumnName(), ParameterUsage.SET
				),
				session
		);

		for ( final int attributeIndex : sourceMappings[tableIndex].getAttributeIndexes() ) {
			if ( propertyInclusions[attributeIndex] ) {
				final var attributeMapping = attributeMappings.get( attributeIndex );
				if ( !( attributeMapping instanceof PluralAttributeMapping ) ) {
					attributeMapping.decompose(
							values[attributeIndex], 0, jdbcValueBindings, null,
							(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
								if ( selectableMapping.isInsertable() && !selectableMapping.isFormula() ) {
									bindings.bindValue(
											jdbcValue, tableName,
											selectableMapping.getSelectionExpression(), ParameterUsage.SET
									);
								}
							},
							session
					);
				}
			}
		}

		final String sourceTableName = sourceMappings[tableIndex].getTableName();
		if ( !useServerTransactionTimestamps ) {
			jdbcValueBindings.bindValue(
					session.getCurrentChangesetIdentifier(),
					tableName,
					auditMapping.getChangesetIdMapping( sourceTableName ).getSelectionExpression(),
					ParameterUsage.SET
			);
		}
		final var modTypeMapping = auditMapping.getModificationTypeMapping( sourceTableName );
		if ( modTypeMapping != null ) {
			jdbcValueBindings.bindValue(
					modificationType,
					tableName,
					modTypeMapping.getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	public void bindTransactionEndValues(
			int tableIndex,
			Object id,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( auditTableMappings[tableIndex] == null ) {
			return;
		}
		final EntityTableMapping[] sourceMappings = entityPersister.getTableMappings();
		final String tableName = auditTableMappings[tableIndex].getTableName();
		final String sourceTableName = sourceMappings[tableIndex].getTableName();
		final var revEndMapping = auditMapping.getInvalidatingChangesetIdMapping( sourceTableName );
		if ( revEndMapping == null ) {
			return;
		}

		if ( !useServerTransactionTimestamps ) {
			jdbcValueBindings.bindValue(
					session.getCurrentChangesetIdentifier(),
					tableName,
					revEndMapping.getSelectionExpression(),
					ParameterUsage.SET
			);
		}

		sourceMappings[tableIndex].getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> jdbcValueBindings.bindValue(
						jdbcValue, tableName, columnMapping.getColumnName(), ParameterUsage.RESTRICT
				),
				session
		);
	}

	public void bindAuditInsertValues(
			int tableIndex,
			Object id,
			Object[] values,
			boolean[] propertyInclusions,
			ModificationType modificationType,
			Object changesetId,
			SharedSessionContractImplementor session,
			org.hibernate.action.queue.spi.bind.JdbcValueBindings jdbcValueBindings) {
		if ( auditTableMappings[tableIndex] == null ) {
			return;
		}
		final var attributeMappings = entityPersister.getAttributeMappings();
		final EntityTableMapping[] sourceMappings = entityPersister.getTableMappings();

		sourceMappings[tableIndex].getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> jdbcValueBindings.bindValue(
						jdbcValue, columnMapping.getColumnName(), ParameterUsage.SET
				),
				session
		);

		for ( final int attributeIndex : sourceMappings[tableIndex].getAttributeIndexes() ) {
			if ( propertyInclusions[attributeIndex] ) {
				final var attributeMapping = attributeMappings.get( attributeIndex );
				if ( !( attributeMapping instanceof PluralAttributeMapping ) ) {
					attributeMapping.decompose(
							values[attributeIndex], 0, jdbcValueBindings, null,
							(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
								if ( selectableMapping.isInsertable() && !selectableMapping.isFormula() ) {
									bindings.bindValue(
											jdbcValue,
											selectableMapping.getSelectionExpression(),
											ParameterUsage.SET
									);
								}
							},
							session
					);
				}
			}
		}

		final String sourceTableName = sourceMappings[tableIndex].getTableName();
		if ( !useServerTransactionTimestamps ) {
			jdbcValueBindings.bindValue(
					changesetId,
					auditMapping.getChangesetIdMapping( sourceTableName ).getSelectionExpression(),
					ParameterUsage.SET
			);
		}
		final var modTypeMapping = auditMapping.getModificationTypeMapping( sourceTableName );
		if ( modTypeMapping != null ) {
			jdbcValueBindings.bindValue(
					modificationType,
					modTypeMapping.getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	public void bindTransactionEndValues(
			int tableIndex,
			Object id,
			Object changesetId,
			SharedSessionContractImplementor session,
			org.hibernate.action.queue.spi.bind.JdbcValueBindings jdbcValueBindings) {
		if ( auditTableMappings[tableIndex] == null ) {
			return;
		}
		final EntityTableMapping[] sourceMappings = entityPersister.getTableMappings();
		final String sourceTableName = sourceMappings[tableIndex].getTableName();
		final var revEndMapping = auditMapping.getInvalidatingChangesetIdMapping( sourceTableName );
		if ( revEndMapping == null ) {
			return;
		}

		if ( !useServerTransactionTimestamps ) {
			jdbcValueBindings.bindValue(
					changesetId,
					revEndMapping.getSelectionExpression(),
					ParameterUsage.SET
			);
		}

		sourceMappings[tableIndex].getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> jdbcValueBindings.bindValue(
						jdbcValue, columnMapping.getColumnName(), ParameterUsage.RESTRICT
				),
				session
		);
	}

	public static boolean verifyTransactionEndOutcome(
			int affectedRowCount,
			ModificationType modificationType,
			String entityName,
			Object id) {
		if ( affectedRowCount > 1
				|| affectedRowCount == 0 && modificationType != ModificationType.ADD ) {
			throw new AuditException(
					"Cannot update previous revision for entity "
							+ entityName + " and id " + id
							+ " (" + affectedRowCount + " rows modified)."
			);
		}
		return true;
	}

	private EntityTableMapping[] buildAuditTableMappings() {
		final EntityTableMapping[] sourceMappings = entityPersister.getTableMappings();
		final EntityTableMapping[] result = new EntityTableMapping[sourceMappings.length];
		for ( int i = 0; i < sourceMappings.length; i++ ) {
			final EntityTableMapping source = sourceMappings[i];
			if ( source.isInverse() ) {
				continue;
			}
			final String auditTableName = auditMapping.resolveTableName( source.getTableName() );
			result[i] = createAuxiliaryTableMapping( source, entityPersister, auditTableName );
		}
		return result;
	}

	private MutationGroup buildAuditInsertMutationGroup(
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		final EntityTableMapping[] sourceMappings = entityPersister.getTableMappings();
		final var attributeMappings = entityPersister.getAttributeMappings();
		final List<TableMutation<?>> mutations = new ArrayList<>( auditTableMappings.length );

		for ( int i = 0; i < auditTableMappings.length; i++ ) {
			if ( auditTableMappings[i] == null ) {
				continue;
			}
			final var insertBuilder =
					new TableInsertBuilderStandard( entityPersister, auditTableMappings[i], factory );
			final var sourceMapping = sourceMappings[i];
			for ( final int attributeIndex : sourceMapping.getAttributeIndexes() ) {
				final var attributeMapping = attributeMappings.get( attributeIndex );
				if ( propertyInclusions[attributeIndex] ) {
					attributeMapping.forEachInsertable( insertBuilder );
				}
				else {
					final var generator = attributeMapping.getGenerator();
					if ( isValueGenerated( generator ) ) {
						if ( entity != null && generator.generatedBeforeExecution( entity, session ) ) {
							propertyInclusions[attributeIndex] = true;
							attributeMapping.forEachInsertable( insertBuilder );
						}
						else if ( isValueGenerationInSql( generator ) ) {
							addSqlGeneratedValue( insertBuilder, attributeMapping, (OnExecutionGenerator) generator );
						}
					}
				}
			}

			if ( sourceMapping.isIdentifierTable() ) {
				final var discriminatorMapping = entityPersister.getDiscriminatorMapping();
				if ( discriminatorMapping != null && discriminatorMapping.hasPhysicalColumn()
						&& entityPersister.getDiscriminatorValue() instanceof DiscriminatorValue.Literal ) {
					insertBuilder.addColumnAssignment(
							discriminatorMapping,
							entityPersister.getDiscriminatorSQLValue()
					);
				}
			}

			final var sourceTableName = sourceMapping.getTableName();
			final var txIdMapping = auditMapping.getChangesetIdMapping( sourceTableName );
			final var modTypeMapping = auditMapping.getModificationTypeMapping( sourceTableName );
			insertBuilder.addColumnAssignment( txIdMapping, useServerTransactionTimestamps ? currentTimestampFunctionName : "?" );
			if ( modTypeMapping != null ) {
				insertBuilder.addColumnAssignment( modTypeMapping, "?" );
			}

			sourceMapping.getKeyMapping().forEachKeyColumn( insertBuilder::addColumnAssignment );
			mutations.add( insertBuilder.buildMutation() );
		}

		if ( mutations.isEmpty() ) {
			return null;
		}
		return mutations.size() == 1
				? new MutationGroupSingle( MutationType.INSERT, entityPersister, mutations.get( 0 ) )
				: new MutationGroupStandard( MutationType.INSERT, entityPersister, mutations );
	}

	private MutationGroup buildTransactionEndUpdateMutationGroup() {
		final EntityTableMapping[] sourceMappings = entityPersister.getTableMappings();
		final List<TableMutation<?>> mutations = new ArrayList<>();
		for ( int i = 0; i < auditTableMappings.length; i++ ) {
			if ( auditTableMappings[i] == null ) {
				continue;
			}
			final String sourceTableName = sourceMappings[i].getTableName();
			final var revEndMapping = auditMapping.getInvalidatingChangesetIdMapping( sourceTableName );
			if ( revEndMapping == null ) {
				continue;
			}
			final var updateBuilder = new TableUpdateBuilderStandard<>( entityPersister, auditTableMappings[i], factory );
			updateBuilder.addColumnAssignment( revEndMapping, useServerTransactionTimestamps ? currentTimestampFunctionName : "?" );

			sourceMappings[i].getKeyMapping().forEachKeyColumn(
					(position, keyColumn) -> updateBuilder.addKeyRestrictionBinding( keyColumn )
			);

			final var revEndColumnRef = new ColumnReference( updateBuilder.getMutatingTable(), revEndMapping );
			updateBuilder.addNonKeyRestriction( new ColumnValueBinding(
					revEndColumnRef,
					new ColumnWriteFragment( null, List.of(), revEndMapping )
			) );

			mutations.add( updateBuilder.buildMutation() );
		}
		if ( mutations.isEmpty() ) {
			return null;
		}
		return mutations.size() == 1
				? new MutationGroupSingle( MutationType.UPDATE, entityPersister, mutations.get( 0 ) )
				: new MutationGroupStandard( MutationType.UPDATE, entityPersister, mutations );
	}

	private List<AuditMutationOperation> createMutationOperations(MutationGroup mutationGroup) {
		final List<AuditMutationOperation> operations = new ArrayList<>( mutationGroup.getNumberOfTableMutations() );
		for ( int i = 0; i < mutationGroup.getNumberOfTableMutations(); i++ ) {
			final var tableMutation = mutationGroup.getTableMutation( i );
			final MutationOperation operation = tableMutation.createMutationOperation( null, factory );
			if ( operation != null ) {
				final int tableIndex = resolveAuditTableIndex( tableMutation.getTableName() );
				operations.add( new AuditMutationOperation(
						tableIndex,
						createAuditTableDescriptor( auditTableMappings[tableIndex], operation ),
						operation
				) );
			}
		}
		return operations;
	}

	private EntityTableDescriptor createAuditTableDescriptor(
			EntityTableMapping auditTableMapping,
			MutationOperation operation) {
		final var tableMapping = operation.getTableDetails();
		final var identifierTableDescriptor = entityPersister.getIdentifierTableDescriptor();
		return new EntityTableDescriptor(
				auditTableMapping.getTableName(),
				auditTableMapping.relativePosition(),
				auditTableMapping.isIdentifierTable(),
				auditTableMapping.isOptional(),
				auditTableMapping.isInverse(),
				false,
				false,
				auditTableMapping.isCascadeDeleteEnabled(),
				tableMapping.getInsertDetails(),
				tableMapping.getUpdateDetails(),
				tableMapping.getDeleteDetails(),
				List.of(),
				List.of(),
				Map.of(),
				identifierTableDescriptor.keyDescriptor()
		);
	}

	private int resolveAuditTableIndex(String tableName) {
		for ( int i = 0; i < auditTableMappings.length; i++ ) {
			if ( auditTableMappings[i] != null
					&& auditTableMappings[i].getTableName().equals( tableName ) ) {
				return i;
			}
		}
		throw new MappingException( "Unable to resolve audit table mapping for " + tableName );
	}

	private boolean[] applyAuditMask(boolean[] propertyInclusions) {
		final boolean[] masked = propertyInclusions.clone();
		for ( int i = 0; i < masked.length; i++ ) {
			if ( !auditedPropertyMask[i] ) {
				masked[i] = false;
			}
		}
		return masked;
	}

	private static boolean isValueGenerated(Generator generator) {
		return generator != null
				&& generator.generatesOnInsert()
				&& generator.generatedOnExecution();
	}

	private boolean isValueGenerationInSql(Generator generator) {
		assert isValueGenerated( generator );
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql( factory.getJdbcServices().getDialect() );
	}

	private void addSqlGeneratedValue(
			TableInsertBuilderStandard insertBuilder,
			AttributeMapping attributeMapping,
			OnExecutionGenerator generator) {
		final boolean writePropertyValue = generator.writePropertyValue();
		final String[] columnValues = writePropertyValue
				? null
				: generator.getReferencedColumnValues( factory.getJdbcServices().getDialect() );
		attributeMapping.forEachSelectable( (j, mapping) ->
				insertBuilder.addColumnAssignment( mapping, writePropertyValue ? "?" : columnValues[j] )
		);
	}
}
