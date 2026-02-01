/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * @author Gavin King
 */
public class InsertCoordinatorTemporal extends InsertCoordinatorStandard {
	private final TemporalMapping temporalMapping;

	public InsertCoordinatorTemporal(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.temporalMapping = entityPersister.getTemporalMapping();
	}

	@Override
	protected void decomposeForInsert(
			MutationExecutor mutationExecutor,
			Object id, Object[] values,
			MutationOperationGroup mutationGroup,
			boolean[] propertyInclusions,
			TableInclusionChecker tableInclusionChecker,
			SharedSessionContractImplementor session) {
		super.decomposeForInsert(
				mutationExecutor,
				id, values,
				mutationGroup,
				propertyInclusions,
				tableInclusionChecker,
				session
		);

		if ( TemporalMutationHelper.isUsingParameters( session ) ) {
			mutationExecutor.getJdbcValueBindings().bindValue(
					session.getCurrentTransactionIdentifier(),
					entityPersister().physicalTableNameForMutation( temporalMapping.getStartingColumnMapping() ),
					temporalMapping.getStartingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}
}
