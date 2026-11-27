package org.hibernate.sql.spi.mutation.jdbc;

import java.util.List;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;

/**
 * {@link JdbcMutationOperation} implementation for MERGE handling
 *
 * @author Steve Ebersole
 */
public final class MergeOperation extends AbstractJdbcMutation {
	private final boolean returnsGeneratedValues;

	public MergeOperation(
			TableMapping tableDetails,
			MutationTarget mutationTarget,
			String sql,
			Expectation expectation,
			List<? extends JdbcParameterBinder> parameterBinders) {
		this( tableDetails, mutationTarget, sql, expectation, parameterBinders, false );
	}

	public MergeOperation(
			TableMapping tableDetails,
			MutationTarget mutationTarget,
			String sql,
			Expectation expectation,
			List<? extends JdbcParameterBinder> parameterBinders,
			boolean returnsGeneratedValues) {
		super( tableDetails, mutationTarget, sql, false, expectation, parameterBinders );
		this.returnsGeneratedValues = returnsGeneratedValues;
	}

	/**
	 * A merge returns generated values only if it was rendered for the delegate.
	 */
	@Override
	public boolean supportsGeneratedValuesDelegate() {
		return returnsGeneratedValues;
	}

	@Override
	public final MutationType getMutationType() {
		return MutationType.UPDATE;
	}

	@Override
	public boolean canBeBatched(BatchKey batchKey, int batchSize) {
		// When the mutating table is optional, we generated a delete part for the merge statement
		// which makes the statement non-idempotent and hence not retryable, so also not batchable
		return !getTableDetails().isOptional() && super.canBeBatched( batchKey, batchSize );
	}
}
