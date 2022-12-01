/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

import java.util.List;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.sql.model.ModelMutationLogging;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.ast.MutationGroup;
import org.hibernate.sql.model.ast.builder.ColumnValuesTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.internal.MutationOperationGroupNone;
import org.hibernate.sql.model.internal.MutationOperationGroupSingle;
import org.hibernate.sql.model.internal.MutationOperationGroupStandard;
import org.hibernate.tuple.InDatabaseGenerator;

/**
 * Base support for coordinating mutations against an entity
 *
 * @implNote Split simply to help minimize the size of {@link AbstractEntityPersister}
 *
 * @author Steve Ebersole
 */
@Internal
public abstract class AbstractMutationCoordinator {
	private final AbstractEntityPersister entityPersister;
	private final SessionFactoryImplementor factory;

	public AbstractMutationCoordinator(AbstractEntityPersister entityPersister, SessionFactoryImplementor factory) {
		this.entityPersister = entityPersister;
		this.factory = factory;
	}

	protected AbstractEntityPersister entityPersister() {
		return entityPersister;
	}

	protected SessionFactoryImplementor factory() {
		return factory;
	}

	protected Dialect dialect() {
		return factory().getJdbcServices().getDialect();
	}


	protected MutationOperationGroup createOperationGroup(ValuesAnalysis valuesAnalysis, MutationGroup mutationGroup) {
		if ( mutationGroup.getNumberOfTableMutations() == 0 ) {
			return new MutationOperationGroupNone( mutationGroup.getMutationType(), mutationGroup.getMutationTarget() );
		}

		if ( mutationGroup.getNumberOfTableMutations() == 1 ) {
			final MutationOperation operation = mutationGroup.getSingleTableMutation().createMutationOperation( valuesAnalysis, factory() );
			if ( operation == null ) {
				return new MutationOperationGroupNone( mutationGroup.getMutationType(), mutationGroup.getMutationTarget() );
			}
			return new MutationOperationGroupSingle(
					mutationGroup.getMutationType(),
					mutationGroup.getMutationTarget(),
					operation
			);
		}

		final List<MutationOperation> operations = CollectionHelper.arrayList( mutationGroup.getNumberOfTableMutations() );
		mutationGroup.forEachTableMutation( (integer, tableMutation) -> {
			final MutationOperation operation = tableMutation.createMutationOperation( valuesAnalysis, factory );
			if ( operation != null ) {
				operations.add( operation );
			}
			else {
				ModelMutationLogging.MODEL_MUTATION_LOGGER.debugf(
						"Skipping table update - %s",
						tableMutation.getTableName()
				);
			}
		} );

		return new MutationOperationGroupStandard(
				mutationGroup.getMutationType(),
				entityPersister,
				operations
		);
	}

	void handleValueGeneration(
			AttributeMapping attributeMapping,
			MutationGroupBuilder mutationGroupBuilder,
			InDatabaseGenerator generator) {
		final Dialect dialect = factory.getJdbcServices().getDialect();
		final boolean writePropertyValue = generator.writePropertyValue();
		final String[] columnValues = writePropertyValue ? null : generator.getReferencedColumnValues( dialect );
		attributeMapping.forEachSelectable( (j, mapping) -> {
			final String tableName = entityPersister.physicalTableNameForMutation( mapping );
			final ColumnValuesTableMutationBuilder tableUpdateBuilder = mutationGroupBuilder.findTableDetailsBuilder( tableName );
			tableUpdateBuilder.addValueColumn(
					mapping.getSelectionExpression(),
					writePropertyValue ? "?" : columnValues[j],
					mapping.getJdbcMapping()
			);
		} );
	}
}
