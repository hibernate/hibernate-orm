/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction.batch;

import java.sql.SQLException;
import java.util.function.Supplier;

import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.hibernate.testing.orm.junit.SettingProvider;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBatchingTest {

	protected static BatchWrapper batchWrapper;
	protected static boolean wasReleaseCalled;
	protected static int numberOfStatementsBeforeRelease = -1;
	protected static int numberOfStatementsAfterRelease = -1;

	public static class Batch2BuilderSettingProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return BatchBuilderLocal.class.getName();
		}
	}

	public static class BatchBuilderLocal extends BatchBuilderImpl {
		private final boolean throwError;

		public BatchBuilderLocal() {
			this( false );
		}

		public BatchBuilderLocal(boolean throwError) {
			super( 50 );
			this.throwError = throwError;
		}

		@Override
		public Batch buildBatch(
				BatchKey key,
				Integer explicitBatchSize,
				Supplier<PreparedStatementGroup> statementGroupSupplier,
				JdbcCoordinator jdbcCoordinator) {
			batchWrapper = new BatchWrapper(
					throwError,
					super.buildBatch( key, explicitBatchSize, statementGroupSupplier, jdbcCoordinator ),
					jdbcCoordinator
			);
			return batchWrapper;
		}
	}

	public static class ErrorBatch2BuilderSettingProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return ErrorBatchBuilderLocal.class.getName();
		}
	}

	public static class ErrorBatchBuilderLocal extends BatchBuilderLocal {
		public ErrorBatchBuilderLocal() {
			super( true );
		}
	}

	public static class BatchWrapper implements Batch {
		private final boolean throwError;
		private final Batch wrapped;
		private final JdbcCoordinator jdbcCoordinator;

		private int numberOfBatches;
		private int numberOfSuccessfulBatches;

		public BatchWrapper(boolean throwError, Batch wrapped, JdbcCoordinator jdbcCoordinator) {
			this.throwError = throwError;
			this.wrapped = wrapped;
			this.jdbcCoordinator = jdbcCoordinator;
		}

		public int getNumberOfBatches() {
			return numberOfBatches;
		}

		public int getNumberOfSuccessfulBatches() {
			return numberOfSuccessfulBatches;
		}

		@Override
		public BatchKey getKey() {
			return wrapped.getKey();
		}

		@Override
		public void addObserver(BatchObserver observer) {
			wrapped.addObserver( observer );
		}

		@Override
		public PreparedStatementGroup getStatementGroup() {
			return wrapped.getStatementGroup();
		}

		@Override
		public void addToBatch(JdbcValueBindings jdbcValueBindings, TableInclusionChecker inclusionChecker, StaleStateMapper staleStateMapper) {
			addToBatch( jdbcValueBindings, inclusionChecker );
		}

		@Override
		public void addToBatch(JdbcValueBindings jdbcValueBindings, TableInclusionChecker inclusionChecker) {
			numberOfBatches++;
			wrapped.addToBatch( jdbcValueBindings, inclusionChecker );
			numberOfStatementsBeforeRelease = wrapped.getStatementGroup().getNumberOfStatements();

			if ( throwError  ) {
				// Implementations really should call abortBatch() before throwing an exception.
				// Purposely skipping the call to abortBatch() to ensure that Hibernate works properly when
				// a legacy implementation does not call abortBatch().
				final JdbcServices jdbcServices = jdbcCoordinator.getJdbcSessionOwner()
						.getJdbcSessionContext()
						.getJdbcServices();
				throw jdbcServices.getSqlExceptionHelper().convert(
						new SQLException( "fake SQLException" ),
						"could not perform addBatch",
						wrapped.getStatementGroup().getSingleStatementDetails().getSqlString()
				);
			}

			numberOfSuccessfulBatches++;
		}

		@Override
		public void execute() {
			wrapped.execute();
		}

		@Override
		public void release() {
			wasReleaseCalled = true;
			wrapped.release();
			numberOfStatementsAfterRelease = wrapped.getStatementGroup().getNumberOfActiveStatements();
		}
	}
}
