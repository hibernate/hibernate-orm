package org.hibernate.sql.spi.mutation;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Extension to MutationOperation for cases where the operation wants to
 * handle execution itself.
 *
 * @author Steve Ebersole
 */
public interface SelfExecutingUpdateOperation extends MutationOperation {
	void performMutation(
			JdbcValueBindings jdbcValueBindings,
			ValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session);

	/**
	 * An operation which executes its own statements does not hand them
	 * to a generated values delegate.
	 */
	@Override
	default boolean supportsGeneratedValuesDelegate() {
		return false;
	}
}
