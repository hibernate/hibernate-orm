/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;


import org.hibernate.StaleStateException;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.DeleteOrUpsertOperation;
import org.hibernate.sql.model.jdbc.UpsertOperation;

import java.sql.PreparedStatement;


/**
 * @author Jan Schatteman
 */
@Deprecated(forRemoval = true)
public class MySQLDeleteOrUpsertOperation extends DeleteOrUpsertOperation {

	private Expectation customExpectation;

	public MySQLDeleteOrUpsertOperation(EntityMutationTarget mutationTarget, EntityTableMapping tableMapping, UpsertOperation upsertOperation, OptionalTableUpdate optionalTableUpdate) {
		super( mutationTarget, tableMapping, upsertOperation, optionalTableUpdate );
	}

	@Override
	public void performMutation(JdbcValueBindings jdbcValueBindings, ValuesAnalysis valuesAnalysis, SharedSessionContractImplementor session) {
		customExpectation = new MySQLRowCountExpectation();
		super.performMutation( jdbcValueBindings, valuesAnalysis, session );
	}

	@Override
	protected Expectation getExpectation() {
		return customExpectation;
	}

	private static class MySQLRowCountExpectation implements Expectation {
		@Override
		public final void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition, String sql) {
			if ( rowCount > 2 ) {
				throw new StaleStateException(
						"Unexpected row count"
						+ " (the expected row count for an ON DUPLICATE KEY UPDATE statement should be either 0, 1 or 2 )"
						+ " [" + sql + "]"
				);
			}
		}
	}

}
