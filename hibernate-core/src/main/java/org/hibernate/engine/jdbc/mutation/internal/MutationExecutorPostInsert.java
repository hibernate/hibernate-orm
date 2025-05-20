/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
public class MutationExecutorPostInsert implements MutationExecutor, JdbcValueBindingsImpl.JdbcValueDescriptorAccess {
	protected final EntityMutationTarget mutationTarget;
	protected final MutationOperationGroup mutationOperationGroup;

	protected final SharedSessionContractImplementor session;
	protected final PreparedStatementDetails identityInsertStatementDetails;

	/**
	 * Any non-batched JDBC statements
	 */
	protected final PreparedStatementGroup secondaryTablesStatementGroup;

	protected final JdbcValueBindingsImpl valueBindings;

	public MutationExecutorPostInsert(EntityMutationOperationGroup mutationOperationGroup, SharedSessionContractImplementor session) {
		this.mutationTarget = mutationOperationGroup.getMutationTarget();
		this.valueBindings = new JdbcValueBindingsImpl(
				MutationType.INSERT,
				mutationTarget,
				this,
				session
		);
		this.mutationOperationGroup = mutationOperationGroup;
		this.session = session;

		final PreparableMutationOperation identityInsertOperation = (PreparableMutationOperation) mutationOperationGroup.getOperation( mutationTarget.getIdentifierTableName() );
		this.identityInsertStatementDetails = ModelMutationHelper.identityPreparation(
				identityInsertOperation,
				session
		);

		List<PreparableMutationOperation> secondaryTableMutations = null;

		for ( int i = 0; i < mutationOperationGroup.getNumberOfOperations(); i++ ) {
			final MutationOperation operation = mutationOperationGroup.getOperation( i );

			if ( operation.getTableDetails().isIdentifierTable() ) {
				// the identifier table is handled via `identityInsertStatementDetails`
				continue;
			}

			// SelfExecutingUpdateOperation are not legal for inserts...
			assert ! (operation instanceof SelfExecutingUpdateOperation );

			final PreparableMutationOperation preparableMutationOperation = (PreparableMutationOperation) operation;
			if ( secondaryTableMutations == null ) {
				secondaryTableMutations = new ArrayList<>();
			}
			secondaryTableMutations.add( preparableMutationOperation );
		}


		this.secondaryTablesStatementGroup = ModelMutationHelper.toPreparedStatementGroup(
				MutationType.INSERT,
				mutationTarget,
				secondaryTableMutations,
				session
		);
	}

	@Override
	public JdbcValueBindings getJdbcValueBindings() {
		return valueBindings;
	}

	@Override
	public JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage) {
		final MutationOperation operation = mutationOperationGroup.getOperation( tableName );
		if ( operation == null ) {
			return null;
		}

		return operation.getJdbcValueDescriptor( columnName, usage );
	}

	@Override
	public PreparedStatementDetails getPreparedStatementDetails(String tableName) {
		if ( mutationTarget.getIdentifierTableName().equals( tableName ) ) {
			return identityInsertStatementDetails;
		}

		return secondaryTablesStatementGroup.getPreparedStatementDetails( tableName );
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

		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.tracef(
					"Post-insert generated value : `%s` (%s)",
					id,
					mutationTarget.getNavigableRole().getFullPath()
			);
		}

		if ( secondaryTablesStatementGroup != null ) {
			secondaryTablesStatementGroup.forEachStatement( (tableName, statementDetails) -> executeWithId(
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
			if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
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
				(jdbcValue, columnMapping) -> {
					valueBindings.bindValue(
							jdbcValue,
							tableName,
							columnMapping.getColumnName(),
							ParameterUsage.SET
					);
				},
				session
		);

		session.getJdbcServices().getSqlStatementLogger().logStatement( statementDetails.getSqlString() );
		valueBindings.beforeStatement( statementDetails );

		try {
			final int affectedRowCount = session.getJdbcCoordinator()
					.getResultSetReturn()
					.executeUpdate( statementDetails.getStatement(), statementDetails.getSqlString() );

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
		identityInsertStatementDetails.releaseStatement( session );
		secondaryTablesStatementGroup.release();
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
