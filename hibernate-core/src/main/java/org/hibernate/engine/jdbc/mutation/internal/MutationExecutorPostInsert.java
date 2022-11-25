/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.engine.jdbc.batch.spi.Batch2;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER_TRACE_ENABLED;

/**
 * Specialized executor for the case of more than one table operation, with the
 * root table defining a post-insert id-generation strategy.
 *
 * @todo (mutation) : look to consolidate this into/with MutationExecutorStandard
 * 		- aside from the special handling for the IDENTITY table insert,
 * 	 			the code below is the same as MutationExecutorStandard.
 * 	 	- consolidating this into MutationExecutorStandard would simplify
 * 	 			creating "single table" variations - i.e. MutationExecutorStandard and
 * 	 			StandardSingleTableExecutor.  Otherwise we'd have MutationExecutorStandard,
 * 	 			StandardSingleTableExecutor, MutationExecutorPostInsert and
 * 	 			MutationExecutorPostInsertSingleTable variants
 *
 * @author Steve Ebersole
 */
public class MutationExecutorPostInsert implements MutationExecutor {
	private final EntityMutationTarget mutationTarget;
	private final MutationOperationGroup mutationOperationGroup;

	private final PreparedStatementDetails identityInsertStatementDetails;

	/**
	 * The batched statements
	 */
	private final Batch2 batch;

	/**
	 * Any non-batched JDBC statements
	 */
	private final PreparedStatementGroup nonBatchedStatementGroup;

	private final JdbcValueBindingsImpl valueBindings;

	private enum StatementLocation { IDENTITY, BATCHED, NON_BATCHED }
	private final Map<String, StatementLocation> statementLocationMap = new HashMap<>();

	public MutationExecutorPostInsert(
			MutationOperationGroup mutationOperationGroup,
			Supplier<BatchKey> batchKeySupplier,
			int batchSize,
			SharedSessionContractImplementor session) {
		this.mutationTarget = (EntityMutationTarget) mutationOperationGroup.getMutationTarget();
		this.valueBindings = new JdbcValueBindingsImpl(
				MutationType.INSERT,
				mutationTarget,
				this::findJdbcValueDescriptor
		);
		this.mutationOperationGroup = mutationOperationGroup;

		final PreparableMutationOperation identityInsertOperation = mutationOperationGroup.getOperation( mutationTarget.getIdentifierTableName() );
		this.identityInsertStatementDetails = ModelMutationHelper.identityPreparation(
				identityInsertOperation,
				session
		);
		statementLocationMap.put( mutationTarget.getIdentifierTableName(), StatementLocation.IDENTITY );

		final BatchKey batchKey = batchKeySupplier.get();

		List<PreparableMutationOperation> batchedJdbcMutations = null;
		List<PreparableMutationOperation> nonBatchedJdbcMutations = null;

		final List<MutationOperation> operations = mutationOperationGroup.getOperations();
		for ( int i = 0; i < operations.size(); i++ ) {
			final MutationOperation operation = operations.get( i );

			if ( operation.getTableDetails().isIdentifierTable() ) {
				// the identifier table is handled via `identityInsertStatementDetails`
				continue;
			}

			// SelfExecutingUpdateOperation are not legal for inserts...
			assert ! (operation instanceof SelfExecutingUpdateOperation );

			final PreparableMutationOperation preparableMutationOperation = (PreparableMutationOperation) operation;
			if ( preparableMutationOperation.canBeBatched( batchKey, batchSize ) ) {
				if ( batchedJdbcMutations == null ) {
					batchedJdbcMutations = new ArrayList<>();
				}
				batchedJdbcMutations.add( preparableMutationOperation );
				statementLocationMap.put( operation.getTableDetails().getTableName(), StatementLocation.BATCHED );
			}
			else {
				if ( nonBatchedJdbcMutations == null ) {
					nonBatchedJdbcMutations = new ArrayList<>();
				}
				nonBatchedJdbcMutations.add( preparableMutationOperation );
				statementLocationMap.put( operation.getTableDetails().getTableName(), StatementLocation.NON_BATCHED );
			}
		}

		// todo (mutation) : consider creating single PreparedStatementGroup for all
		//		batched and non-batched statements.  we then need a way to know whether a
		//		statement is batched or not.  `PreparedStatementDetails#isBatched`?

		if ( batchedJdbcMutations == null || batchedJdbcMutations.isEmpty() ) {
			this.batch = null;
		}
		else {
			final List<PreparableMutationOperation> batchedMutationsRef = batchedJdbcMutations;
			this.batch = session.getJdbcCoordinator().getBatch2(
					batchKey,
					batchSize,
					() -> ModelMutationHelper.toPreparedStatementGroup(
							MutationType.INSERT,
							mutationTarget,
							batchedMutationsRef,
							session
					)
			);
			assert batch != null;
		}

		this.nonBatchedStatementGroup = ModelMutationHelper.toPreparedStatementGroup(
				MutationType.INSERT,
				mutationTarget,
				nonBatchedJdbcMutations,
				session
		);
	}

	@Override
	public JdbcValueBindings getJdbcValueBindings() {
		return valueBindings;
	}

	private JdbcValueDescriptor findJdbcValueDescriptor(String tableName, String columnName, ParameterUsage usage) {
		final MutationOperation operation = mutationOperationGroup.getOperation( tableName );
		if ( operation == null ) {
			return null;
		}

		return operation.getJdbcValueDescriptor( columnName, usage );
	}

	@Override
	public PreparedStatementDetails getPreparedStatementDetails(String tableName) {
		final StatementLocation statementLocation = statementLocationMap.get( tableName );
		if ( statementLocation == null ) {
			return null;
		}

		if ( statementLocation == StatementLocation.IDENTITY ) {
			assert mutationTarget.getIdentifierTableName().equals( tableName );
			return identityInsertStatementDetails;
		}

		if ( statementLocation == StatementLocation.BATCHED ) {
			assert batch != null;
			return batch.getStatementGroup().getPreparedStatementDetails( tableName );
		}

		if ( statementLocation == StatementLocation.NON_BATCHED ) {
			assert nonBatchedStatementGroup != null;
			return nonBatchedStatementGroup.getPreparedStatementDetails( tableName );
		}

		return null;
	}

	@Override
	public Object execute(
			Object modelReference,
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
		final InsertGeneratedIdentifierDelegate identityHandler = mutationTarget.getIdentityInsertDelegate();
		final Object id = identityHandler.performInsert( identityInsertStatementDetails, valueBindings, modelReference, session );

		if ( MODEL_MUTATION_LOGGER_TRACE_ENABLED ) {
			MODEL_MUTATION_LOGGER.tracef(
					"Post-insert generated value : `%s` (%s)",
					id,
					mutationTarget.getNavigableRole().getFullPath()
			);
		}

		if ( nonBatchedStatementGroup != null ) {
			nonBatchedStatementGroup.forEachStatement( (tableName, statementDetails) -> executeWithId(
					id,
					tableName,
					statementDetails,
					inclusionChecker,
					resultChecker,
					session
			) );
		}

		if ( batch != null ) {
			batch.getStatementGroup().forEachStatement( (tableName, statementDetails) -> executeWithId(
					id,
					tableName,
					statementDetails,
					inclusionChecker,
					resultChecker,
					session
			) );
		}

		return id;
	}

	private void executeWithId(
			Object id,
			String tableName,
			PreparedStatementDetails statementDetails,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
		if ( statementDetails == null ) {
			return;
		}

		final EntityTableMapping tableDetails = (EntityTableMapping) statementDetails.getMutatingTableDetails();
		assert !tableDetails.isIdentifierTable();

		if ( inclusionChecker != null && !inclusionChecker.include( tableDetails ) ) {
			if ( MODEL_MUTATION_LOGGER_TRACE_ENABLED ) {
				MODEL_MUTATION_LOGGER.tracef(
						"Skipping execution of secondary insert : %s",
						tableDetails.getTableName()
				);
			}
			return;
		}

		// If we get here the statement is needed - make sure it is resolved
		//noinspection resource
		statementDetails.resolveStatement();

		tableDetails.getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> valueBindings.bindValue(
						jdbcValue,
						tableName,
						columnMapping.getColumnName(),
						ParameterUsage.SET,
						session
				),
				session
		);

		session.getJdbcServices().getSqlStatementLogger().logStatement( statementDetails.getSqlString() );
		valueBindings.beforeStatement( statementDetails, session );

		try {
			final int affectedRowCount = session.getJdbcCoordinator()
					.getResultSetReturn()
					.executeUpdate( statementDetails.getStatement() );

			ModelMutationHelper.checkResults( resultChecker, statementDetails, affectedRowCount, -1 );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to execute mutation PreparedStatement against table `" + tableName + "`",
					statementDetails.getSqlString()
			);
		}
	}

	@Override
	public void release() {
		nonBatchedStatementGroup.release();
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"MutationExecutorPostInsert(`%s`)",
				mutationTarget.getNavigableRole().getFullPath()
		);
	}
}
