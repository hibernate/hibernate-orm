/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Service for creating executors for model mutation operations.
 * <p>
 * A custom {@code MutationExecutorService} may be selected either by setting the
 * configuration property
 * {@value org.hibernate.engine.jdbc.mutation.internal.MutationExecutorServiceInitiator#EXECUTOR_KEY},
 * or by registering it as a {@linkplain java.util.ServiceLoader Java service}.
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface MutationExecutorService extends Service {

	/**
	 * Create an executor for the given {@code operationGroup}, potentially using batching
	 */
	MutationExecutor createExecutor(
			BatchKeyAccess batchKeySupplier,
			MutationOperationGroup operationGroup,
			SharedSessionContractImplementor session);
}
