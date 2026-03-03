/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.EntityAction;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.action.queue.graph.MutationDecomposer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.ast.MutationGroup;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.builder.ColumnValuesTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;

import static java.lang.System.arraycopy;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.manyOperations;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.noOperations;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDecomposer<T extends EntityAction> implements MutationDecomposer<T> {
	protected final EntityPersister entityPersister;
	protected final SessionFactoryImplementor sessionFactory;

	public AbstractDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister;
		this.sessionFactory = sessionFactory;
	}

	protected void handleValueGeneration(
			AttributeMapping attributeMapping,
			MutationGroupBuilder mutationGroupBuilder,
			OnExecutionGenerator generator) {
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
		final boolean writePropertyValue = generator.writePropertyValue();
		final String[] columnValues = writePropertyValue ? null : generator.getReferencedColumnValues( dialect );
		attributeMapping.forEachSelectable( (j, mapping) -> {
			final String tableName = entityPersister.physicalTableNameForMutation( mapping );
			final ColumnValuesTableMutationBuilder tableUpdateBuilder =
					mutationGroupBuilder.findTableDetailsBuilder( tableName );
			tableUpdateBuilder.addValueColumn(
					writePropertyValue ? "?" : columnValues[j],
					mapping
			);
		} );
	}

	protected MutationOperationGroup createOperationGroup(
			ValuesAnalysis valuesAnalysis,
			MutationGroup mutationGroup) {
		final int numberOfTableMutations = mutationGroup.getNumberOfTableMutations();
		switch ( numberOfTableMutations ) {
			case 0:
				return noOperations( mutationGroup );
			case 1: {
				final var operation = createOperation( valuesAnalysis, mutationGroup.getSingleTableMutation() );
				return operation == null
						? noOperations( mutationGroup )
						: singleOperation( mutationGroup, operation );
			}
			default: {
				var operations = new MutationOperation[numberOfTableMutations];
				int outputIndex = 0;
				int skipped = 0;
				for ( int i = 0; i < mutationGroup.getNumberOfTableMutations(); i++ ) {
					final var tableMutation = mutationGroup.getTableMutation( i );
					final var operation = tableMutation.createMutationOperation( valuesAnalysis, sessionFactory );
					if ( operation != null ) {
						operations[outputIndex++] = operation;
					}
					else {
						skipped++;
						MODEL_MUTATION_LOGGER.skippingUpdate( tableMutation.getTableName() );
					}
				}
				if ( skipped != 0 ) {
					final var trimmed = new MutationOperation[outputIndex];
					arraycopy( operations, 0, trimmed, 0, outputIndex );
					operations = trimmed;
				}
				return manyOperations( mutationGroup.getMutationType(), entityPersister, operations );
			}
		}
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected MutationOperation createOperation(ValuesAnalysis valuesAnalysis, TableMutation<?> singleTableMutation) {
		return singleTableMutation.createMutationOperation( valuesAnalysis, sessionFactory );
	}

}
