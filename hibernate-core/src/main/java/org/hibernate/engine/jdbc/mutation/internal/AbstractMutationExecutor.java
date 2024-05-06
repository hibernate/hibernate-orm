/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.sql.SQLException;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Base support for MutationExecutor implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractMutationExecutor implements MutationExecutor {
	/**
	 * Executors with non-batched operations should call this to clean up any "previous" batch
	 * before starting their work
	 */
	protected void prepareForNonBatchedWork(BatchKey batchKey, SharedSessionContractImplementor session) {
		// if there is a current batch, make sure to execute it first
		session.getJdbcCoordinator().conditionallyExecuteBatch( batchKey );
	}

	/**
	 * Templated implementation of execution as <ol>
	 *     <li>{@link #performNonBatchedOperations}</li>
	 *     <li>{@link #performSelfExecutingOperations}</li>
	 *     <li>{@link #performBatchedOperations}</li>
	 * </ol>
	 */
	@Override
	public final Object execute(
			Object modelReference,
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
		performNonBatchedOperations( valuesAnalysis, inclusionChecker, resultChecker, session );
		performSelfExecutingOperations( valuesAnalysis, inclusionChecker, session );
		performBatchedOperations( valuesAnalysis, inclusionChecker );
		return null;
	}

	protected void performNonBatchedOperations(
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
	}

	protected void performSelfExecutingOperations(
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			SharedSessionContractImplementor session) {
	}

	protected void performBatchedOperations(
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker) {
	}

	/**
	 * Perform a non-batched mutation
	 */
	protected void performNonBatchedMutation(
			PreparedStatementDetails statementDetails,
			JdbcValueBindings valueBindings,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
		if ( statementDetails == null ) {
			return;
		}

		final TableMapping tableDetails = statementDetails.getMutatingTableDetails();
		if ( inclusionChecker != null && !inclusionChecker.include( tableDetails ) ) {
			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
				MODEL_MUTATION_LOGGER.tracef(
						"Skipping execution of secondary insert : %s",
						tableDetails.getTableName()
				);
			}
			return;
		}

		// If we get here the statement is needed - make sure it is resolved
		session.getJdbcServices().getSqlStatementLogger().logStatement( statementDetails.getSqlString() );
		valueBindings.beforeStatement( statementDetails );

		try {
			final int affectedRowCount = session.getJdbcCoordinator()
					.getResultSetReturn()
					.executeUpdate( statementDetails.getStatement(), statementDetails.getSqlString() );

			if ( affectedRowCount == 0 && tableDetails.isOptional() ) {
				// the optional table did not have a row
				return;
			}

			ModelMutationHelper.checkResults( resultChecker, statementDetails, affectedRowCount, -1 );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					String.format(
							"Unable to execute mutation PreparedStatement against table `%s`",
							tableDetails.getTableName()
					),
					statementDetails.getSqlString()
			);
		}
		finally {
			if ( statementDetails.getStatement() != null ) {
				statementDetails.releaseStatement( session );
			}
			valueBindings.afterStatement( tableDetails );
		}
	}
}
