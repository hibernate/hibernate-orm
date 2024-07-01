/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.NoBatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.sql.model.ModelMutationLogging;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.ast.MutationGroup;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.builder.ColumnValuesTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.model.internal.MutationOperationGroupFactory;

/**
 * Base support for coordinating mutations against an entity
 *
 * @implNote Split simply to help minimize the size of {@link AbstractEntityPersister}
 *
 * @author Steve Ebersole
 */
@Internal
public abstract class AbstractMutationCoordinator {
	protected final AbstractEntityPersister entityPersister;
	protected final SessionFactoryImplementor factory;
	protected final MutationExecutorService mutationExecutorService;
	protected final Dialect dialect;

	public AbstractMutationCoordinator(AbstractEntityPersister entityPersister, SessionFactoryImplementor factory) {
		this.entityPersister = entityPersister;
		this.factory = factory;
		dialect = factory.getJdbcServices().getDialect();
		this.mutationExecutorService = factory.getServiceRegistry().getService( MutationExecutorService.class );
	}

	protected AbstractEntityPersister entityPersister() {
		return entityPersister;
	}

	protected SessionFactoryImplementor factory() {
		return factory;
	}

	protected Dialect dialect() {
		return dialect;
	}

	protected BatchKeyAccess resolveBatchKeyAccess(boolean dynamicUpdate, SharedSessionContractImplementor session) {
		if ( !dynamicUpdate
				&& !entityPersister().optimisticLockStyle().isAllOrDirty()
				&& session.getTransactionCoordinator() != null
				&& session.getTransactionCoordinator().isTransactionActive() ) {
			return this::getBatchKey;
		}

		return NoBatchKeyAccess.INSTANCE;
	}

	protected abstract BatchKey getBatchKey();

	protected MutationOperationGroup createOperationGroup(ValuesAnalysis valuesAnalysis, MutationGroup mutationGroup) {
		final int numberOfTableMutations = mutationGroup.getNumberOfTableMutations();
		switch ( numberOfTableMutations ) {
			case 0:
				return MutationOperationGroupFactory.noOperations( mutationGroup );
			case 1: {
				final MutationOperation operation = mutationGroup.getSingleTableMutation()
						.createMutationOperation( valuesAnalysis, factory() );
				return operation == null
						? MutationOperationGroupFactory.noOperations( mutationGroup )
						: MutationOperationGroupFactory.singleOperation( mutationGroup, operation );
			}
			default: {
				MutationOperation[] operations = new MutationOperation[numberOfTableMutations];
				int outputIndex = 0;
				int skipped = 0;
				for ( int i = 0; i < mutationGroup.getNumberOfTableMutations(); i++ ) {
					final TableMutation tableMutation = mutationGroup.getTableMutation( i );
					final MutationOperation operation = tableMutation.createMutationOperation( valuesAnalysis, factory );
					if ( operation != null ) {
						operations[outputIndex++] = operation;
					}
					else {
						skipped++;
						ModelMutationLogging.MODEL_MUTATION_LOGGER.debugf(
								"Skipping table update - %s",
								tableMutation.getTableName()
						);
					}
				}
				if ( skipped != 0 ) {
					final MutationOperation[] trimmed = new MutationOperation[outputIndex];
					System.arraycopy( operations, 0, trimmed, 0, outputIndex );
					operations = trimmed;
				}
				return MutationOperationGroupFactory.manyOperations( mutationGroup.getMutationType(), entityPersister, operations );
			}
		}
	}

	protected void handleValueGeneration(
			AttributeMapping attributeMapping,
			MutationGroupBuilder mutationGroupBuilder,
			OnExecutionGenerator generator) {
		final Dialect dialect = factory.getJdbcServices().getDialect();
		final boolean writePropertyValue = generator.writePropertyValue();
		final String[] columnValues = writePropertyValue ? null : generator.getReferencedColumnValues( dialect );
		attributeMapping.forEachSelectable( (j, mapping) -> {
			final String tableName = entityPersister.physicalTableNameForMutation( mapping );
			final ColumnValuesTableMutationBuilder tableUpdateBuilder = mutationGroupBuilder.findTableDetailsBuilder( tableName );
			tableUpdateBuilder.addValueColumn(
					mapping.getSelectionExpression(),
					writePropertyValue ? "?" : columnValues[j],
					mapping.getJdbcMapping(),
					mapping.isLob()
			);
		} );
	}

	protected void handleUpdateValueGeneration(
			AttributeMapping attributeMapping,
			MutationGroupBuilder mutationGroupBuilder,
			UpdateCoordinatorStandard.DirtinessChecker dirtinessChecker,
			OnExecutionGenerator generator) {
		final Dialect dialect = factory.getJdbcServices().getDialect();
		final boolean writePropertyValue = generator.writePropertyValue();
		final String[] columnValues = writePropertyValue ? null : generator.getReferencedColumnValues( dialect );
		attributeMapping.forEachUpdatable( (j, mapping) -> {
			final String tableName = entityPersister.physicalTableNameForMutation( mapping );
			final ColumnValuesTableMutationBuilder tableUpdateBuilder = mutationGroupBuilder.findTableDetailsBuilder( tableName );
			if ( !entityPersister().getEntityMetamodel().isDynamicUpdate()
					|| dirtinessChecker == null
					|| dirtinessChecker.isDirty( j, attributeMapping ).isDirty() ) {
				tableUpdateBuilder.addValueColumn(
						mapping.getSelectionExpression(),
						writePropertyValue ? "?" : columnValues[j],
						mapping.getJdbcMapping(),
						mapping.isLob()
				);
			}
		} );
	}

	protected void bindPartitionColumnValueBindings(
			Object[] loadedState,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final AbstractEntityPersister persister = entityPersister();
		if ( persister.hasPartitionedSelectionMapping() ) {
			final AttributeMappingsList attributeMappings = persister.getAttributeMappings();
			final int size = attributeMappings.size();
			for ( int i = 0; i < size; i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				if ( attributeMapping.hasPartitionedSelectionMapping() ) {
					attributeMapping.decompose(
							loadedState[i],
							0,
							jdbcValueBindings,
							null,
							(valueIndex, bindings, noop, value, jdbcValueMapping) -> {
								if ( jdbcValueMapping.isPartitioned() ) {
									bindings.bindValue(
											value,
											jdbcValueMapping,
											ParameterUsage.RESTRICT
									);
								}
							},
							session
					);
				}
			}
		}
	}

	protected static boolean needsRowId(AbstractEntityPersister entityPersister, EntityTableMapping tableMapping) {
		return entityPersister.getRowIdMapping() != null && tableMapping.isIdentifierTable();
	}

	protected static void applyKeyRestriction(
			Object rowId,
			AbstractEntityPersister entityPersister,
			RestrictedTableMutationBuilder<?, ?> tableMutationBuilder,
			EntityTableMapping tableMapping) {
		if ( rowId != null && needsRowId( entityPersister, tableMapping ) ) {
			tableMutationBuilder.addKeyRestrictionLeniently( entityPersister.getRowIdMapping() );
		}
		else {
			tableMutationBuilder.addKeyRestrictions( tableMapping.getKeyMapping() );
		}
	}

	protected void breakDownKeyJdbcValues(
			Object id,
			Object rowId,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableMapping) {
		if ( rowId != null && needsRowId( entityPersister(), tableMapping ) ) {
			jdbcValueBindings.bindValue(
					rowId,
					tableMapping.getTableName(),
					entityPersister().getRowIdMapping().getRowIdName(),
					ParameterUsage.RESTRICT
			);
		}
		else {
			tableMapping.getKeyMapping().breakDownKeyJdbcValues(
					id,
					(jdbcValue, columnMapping) -> {
						jdbcValueBindings.bindValue(
								jdbcValue,
								tableMapping.getTableName(),
								columnMapping.getColumnName(),
								ParameterUsage.RESTRICT
						);
					},
					session
			);
		}
	}
}
