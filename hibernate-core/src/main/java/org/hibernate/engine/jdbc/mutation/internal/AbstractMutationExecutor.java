/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.sql.SQLException;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.StaleStateMapper;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.mutation.EntityTableMappingImpl;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;

import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.checkResults;
import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;
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
			StaleStateMapper staleStateMapper) {
		final var generatedValues = performNonBatchedOperations(
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
			StaleStateMapper staleStateMapper) {
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

		final var tableDetails = statementDetails.getMutatingTableDetails();
		if ( inclusionChecker != null && !inclusionChecker.include( tableDetails ) ) {
			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
				MODEL_MUTATION_LOGGER.skippingSecondaryInsert( tableDetails.getTableName() );
			}
			return;
		}

		if ( id != null ) {
			assert !tableDetails.isIdentifierTable() : "Unsupported identifier table with generated id";
			( (EntityTableMappingImpl) tableDetails ).getKeyMapping().breakDownKeyJdbcValues(
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
		session.getJdbcSessionContext().getStatementObserver().performingSql( statementDetails.getSqlString(), -1 );

		try {
			valueBindings.beforeStatement( statementDetails );

			final int affectedRowCount =
					session.getJdbcCoordinator()
							.getResultSetReturn()
							.executeUpdate( statementDetails.getStatement(), statementDetails.getSqlString() );

			if ( affectedRowCount == 0 && tableDetails.isOptional() ) {
				// the optional table did not have a row
				return;
			}

			checkResults( resultChecker, statementDetails, affectedRowCount, -1 );
		}
		catch (ConstraintViolationException cve) {
			if ( cve.getKind() == UNIQUE ) {
				// Assume this is a primary key violation
				try {
					// If this check does not throw an error, the statement must be retried
					resultChecker.checkResult( statementDetails, java.sql.Statement.EXECUTE_FAILED, -1 );

					// In a concurrent insert-or-update scenario, the insert part can fail, so we need to retry the
					// statement one last time to ensure we write the correct data for this transaction
					final int affectedRowCount =
							session.getJdbcCoordinator()
									.getResultSetReturn()
									.executeUpdate( statementDetails.getStatement(), statementDetails.getSqlString() );

					if ( affectedRowCount == 0 && tableDetails.isOptional() ) {
						// the optional table did not have a row
						return;
					}

					checkResults( resultChecker, statementDetails, affectedRowCount, -1 );
				}
				catch (RuntimeException | SQLException e) {
					cve.addSuppressed( e );
					throw cve;
				}
			}
			else {
				throw cve;
			}
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
