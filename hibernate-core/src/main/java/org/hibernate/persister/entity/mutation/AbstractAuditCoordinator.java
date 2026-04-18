/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.audit.AuditException;
import org.hibernate.audit.ModificationType;
import org.hibernate.audit.spi.AuditWriter;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.EntityKey;
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
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;
import org.hibernate.sql.model.internal.MutationGroupStandard;

import static org.hibernate.persister.entity.mutation.InsertCoordinatorStandard.getPropertiesToInsert;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Base support for audit log insert coordinators.
 * <p>
 * Supports all inheritance strategies: for SINGLE_TABLE / TABLE_PER_CLASS
 * there is one audit table; for JOINED there is one per entity table.
 * The static operation group is cached for reuse.
 */
abstract class AbstractAuditCoordinator extends AbstractMutationCoordinator implements AuditWriter {
	private final AuditMapping auditMapping;
	private final EntityTableMapping[] auditTableMappings;
	protected final boolean[] auditedPropertyMask;
	protected final BasicBatchKey auditBatchKey;
	private final boolean useServerTransactionTimestamps;
	private final String currentTimestampFunctionName;
	private final MutationOperationGroup staticAuditInsertGroup;
	private final MutationOperationGroup transactionEndUpdateGroup;

	protected AbstractAuditCoordinator(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.auditMapping = entityPersister.getAuditMapping();
		if ( auditMapping == null ) {
			throw new MappingException( "No audit mapping available for " + entityPersister.getEntityName() );
		}
		this.auditTableMappings = buildAuditTableMappings( entityPersister, auditMapping );
		this.auditedPropertyMask = new boolean[entityPersister.getPropertySpan()];
		for ( int i = 0; i < this.auditedPropertyMask.length; i++ ) {
			this.auditedPropertyMask[i] = !entityPersister.isPropertyAuditedExcluded( i );
		}
		this.useServerTransactionTimestamps =
				factory.getTransactionIdentifierService().useServerTimestamp( dialect() );
		this.currentTimestampFunctionName = useServerTransactionTimestamps ? dialect().currentTimestamp() : null;
		this.auditBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#AUDIT_INSERT" );
		this.staticAuditInsertGroup = entityPersister.isDynamicInsert()
				? null
				: buildAuditInsertGroup( applyAuditMask( entityPersister.getPropertyInsertability() ), null, null );
		this.transactionEndUpdateGroup = buildTransactionEndUpdateGroup();
	}

	private EntityTableMapping[] buildAuditTableMappings(EntityPersister persister, AuditMapping auditMapping) {
		final EntityTableMapping[] sourceMappings = persister.getTableMappings();
		final EntityTableMapping[] result = new EntityTableMapping[sourceMappings.length];
		for ( int i = 0; i < sourceMappings.length; i++ ) {
			final EntityTableMapping source = sourceMappings[i];
			if ( source.isInverse() ) {
				continue;
			}
			final String auditTableName = auditMapping.resolveTableName( source.getTableName() );
			result[i] = createAuxiliaryTableMapping( source, persister, auditTableName );
		}
		return result;
	}

	/**
	 * Enqueue an audit entry for deferred writing at transaction completion.
	 */
	protected void enqueueAuditEntry(
			Object entity,
			Object[] values,
			ModificationType modificationType,
			SharedSessionContractImplementor session) {
		final var entityEntry = session.getPersistenceContextInternal().getEntry( entity );
		session.getAuditWorkQueue().enqueue(
				entityEntry.getEntityKey(),
				entity,
				values,
				modificationType,
				this,
				session
		);
	}

	/**
	 * Write an audit row, called by {@link org.hibernate.audit.spi.AuditWorkQueue}
	 * at transaction completion.
	 */
	@Override
	public void writeAuditRow(
			EntityKey entityKey,
			Object entity,
			Object[] values,
			ModificationType modificationType,
			SharedSessionContractImplementor session) {
		final var id = entityKey.getIdentifier();
		updatePreviousTransactionEnd( id, modificationType, session );

		final boolean dynamicInsert = entityPersister().isDynamicInsert();
		final boolean[] propertyInclusions = applyAuditMask(
				dynamicInsert
						? getPropertiesToInsert( entityPersister(), values )
						: entityPersister().getPropertyInsertability()
		);
		final MutationOperationGroup operationGroup = dynamicInsert
				? buildAuditInsertGroup( propertyInclusions, entity, session )
				: staticAuditInsertGroup;
		if ( operationGroup == null ) {
			return;
		}

		final var mutationExecutor = mutationExecutorService.createExecutor(
				resolveBatchKeyAccess( dynamicInsert, session ),
				operationGroup,
				session
		);
		try {
			bindAuditValues(
					id,
					values,
					propertyInclusions,
					modificationType,
					session,
					mutationExecutor.getJdbcValueBindings()
			);
			mutationExecutor.execute( entity, null, null, AbstractAuditCoordinator::verifyOutcome, session );
		}
		finally {
			mutationExecutor.release();
		}
	}

	@Override
	protected BatchKey getBatchKey() {
		return auditBatchKey;
	}

	/**
	 * Build a {@link MutationOperationGroup} with one INSERT per audit table.
	 * Each table's insert builder gets only the attributes belonging to that
	 * table via the source table mapping's {@code getAttributeIndexes()}.
	 */
	private MutationOperationGroup buildAuditInsertGroup(
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		final EntityTableMapping[] sourceMappings = entityPersister().getTableMappings();
		final var attributeMappings = entityPersister().getAttributeMappings();
		final List<TableMutation<?>> mutations = new ArrayList<>( auditTableMappings.length );

		for ( int i = 0; i < auditTableMappings.length; i++ ) {
			if ( auditTableMappings[i] == null ) {
				continue;
			}
			final var insertBuilder =
					new TableInsertBuilderStandard( entityPersister(), auditTableMappings[i], factory() );

			// Route attributes to this builder using the source table's attribute indexes
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

			// Discriminator belongs to the identifier table only
			if ( sourceMapping.isIdentifierTable() ) {
				final var discriminatorMapping = entityPersister().getDiscriminatorMapping();
				if ( discriminatorMapping != null && discriminatorMapping.hasPhysicalColumn()
						&& entityPersister().getDiscriminatorValue() instanceof DiscriminatorValue.Literal ) {
					insertBuilder.addValueColumn(
							entityPersister().getDiscriminatorSQLValue(),
							discriminatorMapping
					);
				}
			}

			// Audit columns (on every audit table)
			final var sourceTableName = sourceMapping.getTableName();
			final var txIdMapping = auditMapping.getTransactionIdMapping( sourceTableName );
			final var modTypeMapping = auditMapping.getModificationTypeMapping( sourceTableName );
			if ( useServerTransactionTimestamps ) {
				insertBuilder.addValueColumn( currentTimestampFunctionName, txIdMapping );
			}
			else {
				insertBuilder.addValueColumn( "?", txIdMapping );
			}
			if ( modTypeMapping != null ) {
				insertBuilder.addValueColumn( "?", modTypeMapping );
			}

			// Key columns
			sourceMapping.getKeyMapping().forEachKeyColumn( insertBuilder::addKeyColumn );

			mutations.add( insertBuilder.buildMutation() );
		}

		if ( mutations.size() == 1 ) {
			return singleOperation(
					new MutationGroupSingle( MutationType.INSERT, entityPersister(), mutations.get( 0 ) ),
					mutations.get( 0 ).createMutationOperation( null, factory() )
			);
		}
		return createOperationGroup(
				null,
				new MutationGroupStandard( MutationType.INSERT, entityPersister(), mutations )
		);
	}

	private void bindAuditValues(
			Object id,
			Object[] values,
			boolean[] propertyInclusions,
			ModificationType modificationType,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMappings = entityPersister().getAttributeMappings();
		final EntityTableMapping[] sourceMappings = entityPersister().getTableMappings();

		for ( int tableIndex = 0; tableIndex < auditTableMappings.length; tableIndex++ ) {
			if ( auditTableMappings[tableIndex] == null ) {
				continue;
			}
			final String tableName = auditTableMappings[tableIndex].getTableName();

			// Key columns
			sourceMappings[tableIndex].getKeyMapping().breakDownKeyJdbcValues(
					id,
					(jdbcValue, columnMapping) -> jdbcValueBindings.bindValue(
							jdbcValue, tableName, columnMapping.getColumnName(), ParameterUsage.SET
					),
					session
			);

			// Attribute values for this table
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

			// Audit columns
			final String sourceTableName = sourceMappings[tableIndex].getTableName();
			if ( !useServerTransactionTimestamps ) {
				jdbcValueBindings.bindValue(
						session.getCurrentTransactionIdentifier(),
						tableName,
						auditMapping.getTransactionIdMapping( sourceTableName ).getSelectionExpression(),
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
	}

	/**
	 * Update the previous audit row's transaction end column for the validity strategy.
	 * Sets {@code REVEND = :currentTxId} on the row with
	 * {@code REVEND IS NULL} for the given entity ID.
	 * <p>
	 * Called before the new audit row INSERT, so the just-inserted row
	 * does not exist yet and there's no risk of self-update.
	 *
	 * @param id the entity identifier
	 * @param modificationType the modification type of the new audit row
	 * @param session the current session
	 */
	private void updatePreviousTransactionEnd(
			Object id,
			ModificationType modificationType,
			SharedSessionContractImplementor session) {
		if ( transactionEndUpdateGroup == null ) {
			return;
		}
		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> auditBatchKey,
				transactionEndUpdateGroup,
				session
		);
		try {
			final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			final EntityTableMapping[] sourceMappings = entityPersister().getTableMappings();
			for ( int tableIndex = 0; tableIndex < auditTableMappings.length; tableIndex++ ) {
				if ( auditTableMappings[tableIndex] == null ) {
					continue;
				}
				final String tableName = auditTableMappings[tableIndex].getTableName();
				final String sourceTableName = sourceMappings[tableIndex].getTableName();
				final var revEndMapping = auditMapping.getTransactionEndMapping( sourceTableName );
				if ( revEndMapping == null ) {
					continue;
				}

				// SET REVEND = :txId
				jdbcValueBindings.bindValue(
						session.getCurrentTransactionIdentifier(),
						tableName,
						revEndMapping.getSelectionExpression(),
						ParameterUsage.SET
				);

				// SET REVEND_TSTMP = :tstmp (if configured)
				final var revEndTsMapping = auditMapping.getTransactionEndTimestampMapping( sourceTableName );
				if ( revEndTsMapping != null ) {
					jdbcValueBindings.bindValue(
							java.time.Instant.now(), tableName,
							revEndTsMapping.getSelectionExpression(),
							ParameterUsage.SET
					);
				}

				// WHERE id = :id
				sourceMappings[tableIndex].getKeyMapping().breakDownKeyJdbcValues(
						id,
						(jdbcValue, columnMapping) -> jdbcValueBindings.bindValue(
								jdbcValue, tableName, columnMapping.getColumnName(), ParameterUsage.RESTRICT
						),
						session
				);
			}
			final String entityName = entityPersister().getEntityName();
			mutationExecutor.execute(
					null, null, null,
					(statementDetails, affectedRowCount, batchPosition) ->
							verifyTransactionEndOutcome( affectedRowCount, modificationType, entityName, id ),
					session
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	private static boolean verifyTransactionEndOutcome(
			int affectedRowCount,
			ModificationType modificationType,
			String entityName,
			Object id) {
		// ADD allows 0 (new entity) or 1 (ID reuse); MOD/DEL requires exactly 1
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

	private MutationOperationGroup buildTransactionEndUpdateGroup() {
		final EntityTableMapping[] sourceMappings = entityPersister().getTableMappings();
		final List<TableMutation<?>> mutations = new ArrayList<>();
		for ( int i = 0; i < auditTableMappings.length; i++ ) {
			if ( auditTableMappings[i] == null ) {
				continue;
			}
			final String sourceTableName = sourceMappings[i].getTableName();
			final var revEndMapping = auditMapping.getTransactionEndMapping( sourceTableName );
			if ( revEndMapping == null ) {
				continue;
			}
			final var updateBuilder =
					new TableUpdateBuilderStandard<>( entityPersister(), auditTableMappings[i], factory() );

			// SET REVEND = ?
			if ( useServerTransactionTimestamps ) {
				updateBuilder.addValueColumn( currentTimestampFunctionName, revEndMapping );
			}
			else {
				updateBuilder.addValueColumn( "?", revEndMapping );
			}

			// SET REVEND_TSTMP = ? (if configured)
			final var revEndTsMapping = auditMapping.getTransactionEndTimestampMapping( sourceTableName );
			if ( revEndTsMapping != null ) {
				updateBuilder.addValueColumn( "?", revEndTsMapping );
			}

			// WHERE id columns
			sourceMappings[i].getKeyMapping().forEachKeyColumn(
					(position, keyColumn) -> updateBuilder.addKeyRestrictionBinding( keyColumn )
			);

			// WHERE REVEND IS NULL (only update the current row)
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
		if ( mutations.size() == 1 ) {
			return singleOperation(
					new MutationGroupSingle( MutationType.UPDATE, entityPersister(), mutations.get( 0 ) ),
					mutations.get( 0 ).createMutationOperation( null, factory() )
			);
		}
		return createOperationGroup(
				null,
				new MutationGroupStandard( MutationType.UPDATE, entityPersister(), mutations )
		);
	}

	private boolean[] applyAuditMask(boolean[] propertyInclusions) {
		if ( auditedPropertyMask == null ) {
			return propertyInclusions;
		}
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
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql( dialect() );
	}

	private void addSqlGeneratedValue(
			TableInsertBuilderStandard insertBuilder,
			AttributeMapping attributeMapping,
			OnExecutionGenerator generator) {
		final boolean writePropertyValue = generator.writePropertyValue();
		final String[] columnValues =
				writePropertyValue
						? null
						: generator.getReferencedColumnValues( factory.getJdbcServices().getDialect() );
		attributeMapping.forEachSelectable( (j, mapping) ->
				insertBuilder.addValueColumn( writePropertyValue ? "?" : columnValues[j], mapping ) );
	}

	private static boolean verifyOutcome(
			PreparedStatementDetails statementDetails,
			int affectedRowCount,
			int batchPosition) throws SQLException {
		statementDetails.getExpectation().verifyOutcome(
				affectedRowCount,
				statementDetails.getStatement(),
				batchPosition,
				statementDetails.getSqlString()
		);
		return true;
	}
}
