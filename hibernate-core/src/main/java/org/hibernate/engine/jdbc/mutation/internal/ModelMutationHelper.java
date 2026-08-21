/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.jdbc.TooManyRowsAffectedException;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.PreparableMutationOperation;

import static org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupNone.GROUP_OF_NONE;

/**
 * Helper functionality related to model mutations
 *
 * @author Steve Ebersole
 */
public class ModelMutationHelper {

	private ModelMutationHelper() {
		// disallow direct instantiation
	}

	public static void checkResults(
			OperationResultChecker resultChecker,
			PreparedStatementDetails statementDetails,
			int affectedRowCount,
			int batchPosition) throws SQLException {
		if ( resultChecker != null ) {
			resultChecker.checkResult( statementDetails, affectedRowCount, batchPosition );
		}
	}

	public static boolean identifiedResultsCheck(
			PreparedStatementDetails statementDetails,
			int affectedRowCount,
			int batchPosition,
			MutationTarget<?> mutationTarget,
			Object id,
			SessionFactoryImplementor sessionFactory) {
		try {
			statementDetails.getExpectation().verifyOutcome(
					affectedRowCount,
					statementDetails.getStatement(),
					batchPosition,
					statementDetails.getSqlString()
			);
			return true;
		}
		catch (StaleStateException e) {
			if ( !statementDetails.getMutatingTableDetails().isOptional() && affectedRowCount == 0 ) {
				final String fullPath = mutationTarget.getNavigableRole().getFullPath();
				final var statistics = sessionFactory.getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( fullPath );
				}
				throw new StaleObjectStateException( fullPath, id, e );
			}
			return false;
		}
		catch (TooManyRowsAffectedException e) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Duplicate identifier in table (%s) - %s#%s",
							statementDetails.getMutatingTableDetails().getTableName(),
							mutationTarget.getNavigableRole().getFullPath(),
							id
					)
			);
		}
		catch (Throwable t) {
			return false;
		}
	}

	public static PreparedStatementGroup toPreparedStatementGroup(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
			GeneratedValuesMutationDelegate delegate,
			List<PreparableMutationOperation> mutations,
			SharedSessionContractImplementor session) {
		if ( mutations == null || mutations.isEmpty() ) {
			return GROUP_OF_NONE;
		}

		if ( mutations.size() == 1 ) {
			return new PreparedStatementGroupSingleTable( mutations.get( 0 ), delegate, session );
		}

		return new PreparedStatementGroupStandard( mutationType, mutationTarget, delegate, mutations, session );
	}

	public static PreparedStatementDetails standardPreparation(
			PreparableMutationOperation jdbcMutation,
			GeneratedValuesMutationDelegate delegate,
			SharedSessionContractImplementor session) {
		return new PreparedStatementDetailsStandard(
				jdbcMutation,
				() -> delegate != null ?
						delegateStatementPreparation( jdbcMutation, delegate, session ) :
						standardStatementPreparation( jdbcMutation, session ),
				session.getJdbcServices()
		);
	}

	public static PreparedStatement delegateStatementPreparation(
			PreparableMutationOperation jdbcMutation,
			GeneratedValuesMutationDelegate delegate,
			SharedSessionContractImplementor session) {
		final var statement = delegate.prepareStatement( jdbcMutation.getSqlString(), session );
		session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().register( null, statement );
		return statement;
	}

	public static PreparedStatement standardStatementPreparation(
			PreparableMutationOperation jdbcMutation,
			SharedSessionContractImplementor session) {
		final var jdbcCoordinator = session.getJdbcCoordinator();
		final var statementPreparer = jdbcCoordinator.getMutationStatementPreparer();
		final var statement = statementPreparer.prepareStatement( jdbcMutation.getSqlString(), jdbcMutation.isCallable() );
		session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().register( null, statement );
		return statement;
	}
}
