/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model;

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

}
