/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.sql.SQLException;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;

import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.checkResults;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Base support for {@link MutationExecutor} implementations
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
	public final GeneratedValues execute(
			Object modelReference,
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
		return execute( modelReference, valuesAnalysis, inclusionChecker, resultChecker, session, null );
	}

	@Override
	public final GeneratedValues execute(
			Object modelReference,
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session,
			Batch.StaleStateMapper staleStateMapper) {
		final GeneratedValues generatedValues = performNonBatchedOperations(
				modelReference,
				valuesAnalysis,
				inclusionChecker,
				resultChecker,
				session
		);
		performSelfExecutingOperations( valuesAnalysis, inclusionChecker, session );
		performBatchedOperations( valuesAnalysis, inclusionChecker, staleStateMapper );
		return generatedValues;
	}



	protected GeneratedValues performNonBatchedOperations(
			Object modelReference,
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
		return null;
	}

	protected void performSelfExecutingOperations(
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			SharedSessionContractImplementor session) {
	}

	protected void performBatchedOperations(
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			Batch.StaleStateMapper staleStateMapper) {
	}

	/**
	 * Perform a non-batched mutation
	 */
	protected void performNonBatchedMutation(
			PreparedStatementDetails statementDetails,
			Object id,
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
						"Skipping execution of secondary insert: %s",
						tableDetails.getTableName()
				);
			}
			return;
		}

		if ( id != null ) {
			assert !tableDetails.isIdentifierTable() : "Unsupported identifier table with generated id";
			( (EntityTableMapping) tableDetails ).getKeyMapping().breakDownKeyJdbcValues(
					id,
					(jdbcValue, columnMapping) -> valueBindings.bindValue(
							jdbcValue,
							tableDetails.getTableName(),
							columnMapping.getColumnName(),
							ParameterUsage.SET
					),
					session
			);
		}

		// If we get here the statement is needed - make sure it is resolved
		session.getJdbcServices().getSqlStatementLogger().logStatement( statementDetails.getSqlString() );

		try {
			valueBindings.beforeStatement( statementDetails );

			final int affectedRowCount = session.getJdbcCoordinator()
					.getResultSetReturn()
					.executeUpdate( statementDetails.getStatement(), statementDetails.getSqlString() );

			if ( affectedRowCount == 0 && tableDetails.isOptional() ) {
				// the optional table did not have a row
				return;
			}

			checkResults( resultChecker, statementDetails, affectedRowCount, -1 );
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
