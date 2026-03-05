/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import java.sql.SQLException;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.MappingException;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.persister.entity.mutation.InsertCoordinatorStandard.getPropertiesToInsert;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Base support for audit log insert coordinators.
 */
abstract class AbstractAuditCoordinator extends AbstractMutationCoordinator {
	protected final EntityTableMapping identifierTableMapping;
	protected final EntityTableMapping auditTableMapping;
	protected final String auditTableName;
	protected final boolean[] auditedPropertyMask;
	private final SelectableMapping transactionIdMapping;
	private final SelectableMapping modificationTypeMapping;
	protected final BasicBatchKey auditBatchKey;
	private final boolean useServerTransactionTimestamps;
	private final String currentTimestampFunctionName;
	private final MutationOperationGroup staticAuditInsertGroup;

	protected AbstractAuditCoordinator(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.identifierTableMapping = entityPersister.getIdentifierTableMapping();
		final var auditMapping = entityPersister.getAuditMapping();
		if ( auditMapping == null ) {
			throw new MappingException( "No audit mapping available for " + entityPersister.getEntityName() );
		}
		this.auditTableName = auditMapping.getTableName();
		this.auditTableMapping = createAuxiliaryTableMapping( identifierTableMapping, entityPersister, auditTableName );
		this.auditedPropertyMask = new boolean[entityPersister.getPropertySpan()];
		for ( int i = 0; i < this.auditedPropertyMask.length; i++ ) {
			this.auditedPropertyMask[i] = !entityPersister.isPropertyAuditedExcluded( i );
		}
		this.transactionIdMapping = auditMapping.getTransactionIdMapping();
		this.modificationTypeMapping = auditMapping.getModificationTypeMapping();
		this.useServerTransactionTimestamps = factory.getTransactionIdentifierService().isDisabled();
		this.currentTimestampFunctionName = useServerTransactionTimestamps ? dialect().currentTimestamp() : null;
		this.auditBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#AUDIT_INSERT" );
		this.staticAuditInsertGroup = entityPersister.isDynamicInsert()
				? null
				: buildAuditInsertGroup( applyAuditMask( entityPersister.getPropertyInsertability() ), null, null );
	}

	protected void insertAuditRow(
			Object entity,
			Object id,
			Object[] values,
			AuditStateManagement.ModificationType modificationType,
			SharedSessionContractImplementor session) {
		if ( values != null ) {
			final boolean dynamicInsert = entityPersister().isDynamicInsert();
			final boolean[] propertyInclusions = applyAuditMask(
					dynamicInsert
							? getPropertiesToInsert( entityPersister(), values )
							: entityPersister().getPropertyInsertability()
			);
			final MutationOperationGroup operationGroup = dynamicInsert
					? buildAuditInsertGroup( propertyInclusions, entity, session )
					: staticAuditInsertGroup;

			final Object resolvedId = id != null
					? id
					: entity != null ? entityPersister().getIdentifier( entity, session ) : null;
			if ( resolvedId == null || operationGroup == null ) {
				return;
			}

			final var mutationExecutor = mutationExecutorService.createExecutor(
					resolveBatchKeyAccess( dynamicInsert, session ),
					operationGroup,
					session
			);
			try {
				bindAuditValues( resolvedId, values, propertyInclusions, modificationType, session,
						mutationExecutor.getJdbcValueBindings() );
				mutationExecutor.execute( entity, null, null, AbstractAuditCoordinator::verifyOutcome, session );
			}
			finally {
				mutationExecutor.release();
			}
		}
	}

	protected BatchKey getAuditBatchKey() {
		return auditBatchKey;
	}

	@Override
	protected BatchKey getBatchKey() {
		return auditBatchKey;
	}

	private MutationOperationGroup buildAuditInsertGroup(
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		final var insertBuilder =
				new TableInsertBuilderStandard( entityPersister(), auditTableMapping, factory() );
		applyAuditInsertDetails( insertBuilder, propertyInclusions, entity, session );
		final var tableMutation = insertBuilder.buildMutation();
		return singleOperation(
				new MutationGroupSingle( MutationType.INSERT, entityPersister(), tableMutation ),
				tableMutation.createMutationOperation( null, factory() )
		);
	}

	private void applyAuditInsertDetails(
			TableInsertBuilderStandard insertBuilder,
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		final var attributeMappings = entityPersister().getAttributeMappings();
		for ( final int attributeIndex : identifierTableMapping.getAttributeIndexes() ) {
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

		if ( useServerTransactionTimestamps ) {
			insertBuilder.addValueColumn( currentTimestampFunctionName, getTransactionIdMapping() );
		}
		else {
			insertBuilder.addValueColumn( "?", getTransactionIdMapping() );
		}
		insertBuilder.addValueColumn( "?", getModificationTypeMapping() );

		identifierTableMapping.getKeyMapping().forEachKeyColumn( insertBuilder::addKeyColumn );
	}

	private void bindAuditValues(
			Object id,
			Object[] values,
			boolean[] propertyInclusions,
			AuditStateManagement.ModificationType modificationType,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final String tableName = auditTableName;
		auditTableMapping.getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> jdbcValueBindings.bindValue(
						jdbcValue,
						tableName,
						columnMapping.getColumnName(),
						ParameterUsage.SET
				),
				session
		);

		final var attributeMappings = entityPersister().getAttributeMappings();
		for ( final int attributeIndex : identifierTableMapping.getAttributeIndexes() ) {
			if ( propertyInclusions[attributeIndex] ) {
				final var attributeMapping = attributeMappings.get( attributeIndex );
				if ( !(attributeMapping instanceof PluralAttributeMapping) ) {
					attributeMapping.decompose(
							values[attributeIndex],
							0,
							jdbcValueBindings,
							null,
							(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
								if ( selectableMapping.isInsertable() && !selectableMapping.isFormula() ) {
									bindings.bindValue(
											jdbcValue,
											tableName,
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

		if ( !useServerTransactionTimestamps ) {
			jdbcValueBindings.bindValue(
				session.getCurrentTransactionIdentifier(),
				tableName,
				getTransactionIdMapping().getSelectionExpression(),
				ParameterUsage.SET
			);
		}

		jdbcValueBindings.bindValue(
				Integer.valueOf( modificationType.ordinal() ),
				tableName,
				getModificationTypeMapping().getSelectionExpression(),
				ParameterUsage.SET
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

	protected SelectableMapping getTransactionIdMapping() {
		return transactionIdMapping;
	}

	protected SelectableMapping getModificationTypeMapping() {
		return modificationTypeMapping;
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
