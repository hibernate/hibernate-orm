/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.EntityMutationOperationGroup;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * Standard {@link org.hibernate.engine.jdbc.mutation.MutationExecutor}
 *
 * @author Steve Ebersole
 */
public class MutationExecutorStandard extends AbstractMutationExecutor implements JdbcValueBindingsImpl.JdbcValueDescriptorAccess {
	private final MutationOperationGroup mutationOperationGroup;

	/**
	 * The batched statements
	 */
	private final Batch batch;

	/**
	 * Any non-batched JDBC statements
	 */
	private final PreparedStatementGroup nonBatchedStatementGroup;
	private final GeneratedValuesMutationDelegate generatedValuesDelegate;

	/**
	 * Operations which handle their own execution
	 */
	private final List<SelfExecutingUpdateOperation> selfExecutingMutations;

	private final JdbcValueBindingsImpl valueBindings;

	private enum StatementLocation { BATCHED, NON_BATCHED }
	private final Map<String,StatementLocation> statementLocationMap = new HashMap<>();

	public MutationExecutorStandard(
			MutationOperationGroup mutationOperationGroup,
			BatchKeyAccess batchKeySupplier,
			int batchSize,
			SharedSessionContractImplementor session) {
		this.mutationOperationGroup = mutationOperationGroup;
		this.generatedValuesDelegate = mutationOperationGroup.asEntityMutationOperationGroup() != null
				? mutationOperationGroup.asEntityMutationOperationGroup().getMutationDelegate()
				: null;

		final BatchKey batchKey = batchKeySupplier.getBatchKey();

		// split the table operations into batchable and non-batchable -
		// 		1. batchable statements are handle via Batch
		//		2. non-batchable statements are handled locally

		List<PreparableMutationOperation> batchedJdbcMutations = null;
		List<PreparableMutationOperation> nonBatchedJdbcMutations = null;
		List<SelfExecutingUpdateOperation> selfExecutingMutations = null;

		boolean hasAnyNonBatchedJdbcOperations = false;

		for ( int i = mutationOperationGroup.getNumberOfOperations() - 1; i >= 0; i-- ) {
			final MutationOperation operation = mutationOperationGroup.getOperation( i );
			if ( operation instanceof SelfExecutingUpdateOperation selfExecutingUpdateOperation ) {
				if ( selfExecutingMutations == null ) {
					selfExecutingMutations = new ArrayList<>();
				}
				selfExecutingMutations.add( 0, selfExecutingUpdateOperation );
			}
			else {
				final PreparableMutationOperation preparableMutationOperation = (PreparableMutationOperation) operation;
				final TableMapping tableDetails = operation.getTableDetails();
				final boolean canBeBatched;

				if ( tableDetails.isIdentifierTable() && hasAnyNonBatchedJdbcOperations ) {
					canBeBatched = false;
				}
				else {
					canBeBatched = preparableMutationOperation.canBeBatched( batchKey, batchSize );
				}

				if ( canBeBatched ) {
					if ( batchedJdbcMutations == null ) {
						batchedJdbcMutations = new ArrayList<>();
					}
					batchedJdbcMutations.add( 0, preparableMutationOperation );
					statementLocationMap.put( tableDetails.getTableName(), StatementLocation.BATCHED );
				}
				else {
					hasAnyNonBatchedJdbcOperations = true;
					if ( nonBatchedJdbcMutations == null ) {
						nonBatchedJdbcMutations = new ArrayList<>();
					}
					nonBatchedJdbcMutations.add( 0, preparableMutationOperation );
					statementLocationMap.put( tableDetails.getTableName(), StatementLocation.NON_BATCHED );
				}
			}
		}

		// todo (mutation) : consider creating single PreparedStatementGroup for all
		//		batched and non-batched statements.  we then need a way to know whether a
		//		statement is batched or not.  `PreparedStatementDetails#isBatched`?

		if ( batchedJdbcMutations == null || batchedJdbcMutations.isEmpty() ) {
			this.batch = null;
		}
		else {
			assert generatedValuesDelegate == null : "Unsupported batched mutation for entity target with generated values delegate";
			final List<PreparableMutationOperation> batchedMutationsRef = batchedJdbcMutations;
			this.batch = session.getJdbcCoordinator().getBatch(
					batchKey,
					batchSize,
					() -> ModelMutationHelper.toPreparedStatementGroup(
							mutationOperationGroup.getMutationType(),
							mutationOperationGroup.getMutationTarget(),
							null,
							batchedMutationsRef,
							session
					)
			);
			assert batch != null;
		}

		this.nonBatchedStatementGroup = ModelMutationHelper.toPreparedStatementGroup(
				mutationOperationGroup.getMutationType(),
				mutationOperationGroup.getMutationTarget(),
				generatedValuesDelegate,
				nonBatchedJdbcMutations,
				session
		);

		this.selfExecutingMutations = selfExecutingMutations;

		this.valueBindings = new JdbcValueBindingsImpl(
				mutationOperationGroup.getMutationType(),
				mutationOperationGroup.getMutationTarget(),
				this,
				session
		);

		if ( isNotEmpty( nonBatchedJdbcMutations ) || isNotEmpty( selfExecutingMutations ) ) {
			prepareForNonBatchedWork( batchKey, session );
		}
	}

	//Used by Hibernate Reactive
	protected PreparedStatementGroup getBatchedPreparedStatementGroup() {
		return this.batch != null ? this.batch.getStatementGroup() : null;
	}

	//Used by Hibernate Reactive
	protected PreparedStatementGroup getNonBatchedStatementGroup() {
		return nonBatchedStatementGroup;
	}

	//Used by Hibernate Reactive
	protected List<SelfExecutingUpdateOperation> getSelfExecutingMutations() {
		return selfExecutingMutations;
	}

	@Override
	public JdbcValueBindings getJdbcValueBindings() {
		return valueBindings;
	}

	@Override
	public JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage) {
		return mutationOperationGroup.getOperation( tableName ).findValueDescriptor( columnName, usage );
	}

	@Override
	public PreparedStatementDetails getPreparedStatementDetails(String tableName) {
		final StatementLocation statementLocation = statementLocationMap.get( tableName );
		if ( statementLocation == null ) {
			return null;
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
	public void release() {
		nonBatchedStatementGroup.release();
	}

	@Override
	protected GeneratedValues performNonBatchedOperations(
			Object modelReference,
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
		if ( nonBatchedStatementGroup == null || nonBatchedStatementGroup.getNumberOfStatements() <= 0 ) {
			return null;
		}

		final GeneratedValues generatedValues;
		if ( generatedValuesDelegate != null ) {
			final EntityMutationOperationGroup entityGroup = mutationOperationGroup.asEntityMutationOperationGroup();
			final EntityMutationTarget entityTarget = entityGroup.getMutationTarget();
			final PreparedStatementDetails details = nonBatchedStatementGroup.getPreparedStatementDetails(
					entityTarget.getIdentifierTableName()
			);
			generatedValues = generatedValuesDelegate.performMutation(
					details,
					valueBindings,
					modelReference,
					session
			);

			final Object id =
					entityGroup.getMutationType() == MutationType.INSERT
						&& details.getMutatingTableDetails().isIdentifierTable()
							? generatedValues.getGeneratedValue( entityTarget.getTargetPart().getIdentifierMapping() )
							: null;
			nonBatchedStatementGroup.forEachStatement( (tableName, statementDetails) -> {
				if ( !statementDetails.getMutatingTableDetails().isIdentifierTable() ) {
					performNonBatchedMutation(
							statementDetails,
							id,
							valueBindings,
							inclusionChecker,
							resultChecker,
							session
					);
				}
			} );
		}
		else {
			generatedValues = null;
			nonBatchedStatementGroup.forEachStatement( (tableName, statementDetails) -> performNonBatchedMutation(
					statementDetails,
					null,
					valueBindings,
					inclusionChecker,
					resultChecker,
					session
			) );
		}

		return generatedValues;
	}

	@Override
	protected void performSelfExecutingOperations(ValuesAnalysis valuesAnalysis, TableInclusionChecker inclusionChecker, SharedSessionContractImplementor session) {
		if ( selfExecutingMutations == null || selfExecutingMutations.isEmpty() ) {
			return;
		}

		for ( int i = 0; i < selfExecutingMutations.size(); i++ ) {
			final SelfExecutingUpdateOperation operation = selfExecutingMutations.get( i );
			if ( inclusionChecker.include( operation.getTableDetails() ) ) {
				operation.performMutation( valueBindings, valuesAnalysis, session );
			}
		}
	}

	@Override
	protected void performBatchedOperations(
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			Batch.StaleStateMapper staleStateMapper) {
		if ( batch == null ) {
			return;
		}
		batch.addToBatch( valueBindings, inclusionChecker, staleStateMapper );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"MutationExecutorStandard(`%s:%s`)",
				mutationOperationGroup.getMutationType().name(),
				mutationOperationGroup.getMutationTarget().getRolePath()
		);
	}
}
