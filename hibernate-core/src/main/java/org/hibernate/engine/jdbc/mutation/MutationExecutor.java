/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.sql.model.ValuesAnalysis;

/**
 * Main contract for performing the mutation.  Accounts for various
 * moving parts such as:<ul>
 *     <li>Should the statements be batched or not?</li>
 *     <li>Should we "logically" group logging of the parameter bindings?</li>
 *     <li>...</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
@Incubating
public interface MutationExecutor {
	/**
	 * Get the delegate to be used to coordinate JDBC parameter binding.
	 */
	JdbcValueBindings getJdbcValueBindings();

	/**
	 * Details about the {@link java.sql.PreparedStatement} for mutating
	 * the given table.
	 */
	PreparedStatementDetails getPreparedStatementDetails(String tableName);

	/**
	 * Perform the execution, returning any generated value.
	 *
	 * @param inclusionChecker The ability to skip the execution for a
	 * 		specific table; passing {@code null} indicates no filtering
	 * @param resultChecker Custom result checking; pass {@code null} to perform
	 * 		the standard check using the statement's {@linkplain org.hibernate.jdbc.Expectation expectation}
	 */
	GeneratedValues execute(
			Object modelReference,
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session);

	GeneratedValues execute(
			Object modelReference,
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session,
			Batch.StaleStateMapper staleStateMapper);

	void release();
}
