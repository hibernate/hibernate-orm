/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.util.Locale;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.identityPreparation;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Specialized form of {@link MutationExecutorPostInsert} for cases where there
 * is only the single identity table.  Allows us to skip references to things
 * we won't need (Batch, etc)
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
public class MutationExecutorPostInsertSingleTable implements MutationExecutor, JdbcValueBindingsImpl.JdbcValueDescriptorAccess {
	private final EntityMutationTarget mutationTarget;
	private final SharedSessionContractImplementor session;
	private final PreparableMutationOperation operation;
	private final PreparedStatementDetails identityInsertStatementDetails;

	private final JdbcValueBindingsImpl valueBindings;

	public MutationExecutorPostInsertSingleTable(
			MutationOperationGroup mutationOperationGroup,
			SharedSessionContractImplementor session) {
		this.mutationTarget = (EntityMutationTarget) mutationOperationGroup.getMutationTarget();
		this.session = session;

		assert mutationOperationGroup.getNumberOfOperations() == 1;

		this.operation = mutationOperationGroup.getOperation( mutationTarget.getIdentifierTableName() );
		this.identityInsertStatementDetails = identityPreparation( operation, session );

		this.valueBindings = new JdbcValueBindingsImpl(
				MutationType.INSERT,
				mutationTarget,
				this,
				session
		);
	}

	@Override
	public JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage) {
		assert identityInsertStatementDetails.getMutatingTableDetails().getTableName().equals( tableName );
		return operation.findValueDescriptor( columnName, usage );
	}

	@Override
	public JdbcValueBindings getJdbcValueBindings() {
		return valueBindings;
	}

	@Override
	public PreparedStatementDetails getPreparedStatementDetails(String tableName) {
		if ( mutationTarget.getIdentifierTableName().equals( tableName ) ) {
			return identityInsertStatementDetails;
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

		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.tracef(
					"Post-insert generated value : `%s` (%s)",
					id,
					mutationTarget.getNavigableRole().getFullPath()
			);
		}

		return id;
	}

	@Override
	public void release() {
		identityInsertStatementDetails.releaseStatement( session );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"MutationExecutorPostInsertSingleTable(`%s`)",
				mutationTarget.getNavigableRole().getFullPath()
		);
	}
}
