/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.Service;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Service for creating executors for model mutation operations
 *
 * @author Steve Ebersole
 */
public interface MutationExecutorService extends Service {

	/**
	 * Create an executor for the given {@code operationGroup}, potentially using batching
	 */
	MutationExecutor createExecutor(
			BatchKeyAccess batchKeySupplier,
			MutationOperationGroup operationGroup,
			SharedSessionContractImplementor session);
}
