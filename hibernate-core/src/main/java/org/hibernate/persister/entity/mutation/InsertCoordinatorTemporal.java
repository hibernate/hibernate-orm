/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Gavin King
 */
@org.hibernate.Internal
public class InsertCoordinatorTemporal extends InsertCoordinatorStandard {
	private final TemporalMapping temporalMapping;

	public InsertCoordinatorTemporal(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
	}

	@Override
	protected void decomposeForInsert(
			@Nonnull MutationExecutor mutationExecutor,
			@Nullable Object id,
			@Nonnull Object[] values,
			@Nonnull Object object,
			@Nonnull MutationOperationGroup mutationGroup,
			@Nonnull boolean[] propertyInclusions,
			@Nonnull TableInclusionChecker tableInclusionChecker,
			@Nonnull SharedSessionContractImplementor session) {
		super.decomposeForInsert(
				mutationExecutor,
				id, values,
				object,
				mutationGroup,
				propertyInclusions,
				tableInclusionChecker,
				session
		);

		if ( TemporalMutationHelper.isUsingParameters( session ) ) {
			mutationExecutor.getJdbcValueBindings().bindValue(
					session.getCurrentChangesetIdentifier(),
					entityPersister().physicalTableNameForMutation( temporalMapping.getStartingColumnMapping() ),
					temporalMapping.getStartingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}
}
