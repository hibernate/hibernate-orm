/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.identifiedResultsCheck;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

abstract class AbstractUpdateCoordinator extends AbstractMutationCoordinator implements UpdateCoordinator {
	AbstractUpdateCoordinator(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}

	boolean resultCheck(
			Object id,
			PreparedStatementDetails statementDetails,
			int affectedRowCount,
			int batchPosition) {
		return identifiedResultsCheck(
				statementDetails,
				affectedRowCount,
				batchPosition,
				entityPersister(),
				id,
				factory()
		);
	}

	StaleObjectStateException staleObjectStateException(Object id, StaleStateException cause) {
		return new StaleObjectStateException( entityPersister().getEntityName(), id, cause );
	}

	void applyPartitionKeyRestriction(RestrictedTableMutationBuilder<?, ?> tableMutationBuilder) {
		final var persister = entityPersister();
		if ( persister.hasPartitionedSelectionMapping() ) {
			final var attributeMappings = persister.getAttributeMappings();
			for ( int m = 0; m < attributeMappings.size(); m++ ) {
				final var attributeMapping = attributeMappings.get( m );
				final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
				for ( int i = 0; i < jdbcTypeCount; i++ ) {
					final var selectableMapping = attributeMapping.getSelectable( i );
					if ( selectableMapping.isPartitioned() ) {
						tableMutationBuilder.addKeyRestrictionLeniently( selectableMapping );
					}
				}
			}
		}
	}

	void applyOptimisticLocking(RestrictedTableMutationBuilder<?, ?> tableMutationBuilder) {
		if ( entityPersister().optimisticLockStyle() == OptimisticLockStyle.VERSION ) {
			applyVersionOptimisticLocking( tableMutationBuilder );
		}
	}

	void applyVersionOptimisticLocking(RestrictedTableMutationBuilder<?, ?> tableMutationBuilder) {
		final var versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping != null ) {
			tableMutationBuilder.addOptimisticLockRestriction( versionMapping );
		}
	}

	void applyTemporalEnding(TableUpdateBuilder<?> tableUpdateBuilder, TemporalMapping temporalMapping) {
		final var endingColumnReference =
				new ColumnReference( tableUpdateBuilder.getMutatingTable(), temporalMapping.getEndingColumnMapping() );
		tableUpdateBuilder.addValueColumn( temporalMapping.createEndingValueBinding( endingColumnReference ) );
		tableUpdateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumnReference ) );
	}

	MutationOperationGroup buildEndingUpdateGroup(EntityTableMapping tableMapping, TemporalMapping temporalMapping) {
		final var tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister(), tableMapping, factory() );

		applyKeyRestriction( null, entityPersister(), tableUpdateBuilder, tableMapping );
		applyTemporalEnding( tableUpdateBuilder, temporalMapping );
		applyPartitionKeyRestriction( tableUpdateBuilder );
		applyOptimisticLocking( tableUpdateBuilder );

		final var tableMutation = tableUpdateBuilder.buildMutation();
		final var mutationGroup = new MutationGroupSingle(
				MutationType.UPDATE,
				entityPersister(),
				tableMutation
		);

		return singleOperation( mutationGroup,
				tableMutation.createMutationOperation( null, factory() ) );
	}
}
